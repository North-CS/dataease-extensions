package io.dataease.plugins.datasource.dremio.provider;

import com.google.gson.Gson;
import io.dataease.plugins.common.base.domain.DeDriver;
import io.dataease.plugins.common.base.mapper.DeDriverMapper;
import io.dataease.plugins.common.constants.DatasourceTypes;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.exception.DataEaseException;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import io.dataease.plugins.datasource.provider.DefaultJdbcProvider;
import io.dataease.plugins.datasource.provider.ExtendedJdbcClassLoader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


@Component()
public class DremioDsProvider extends DefaultJdbcProvider {
    @Resource
    private DeDriverMapper deDriverMapper;

    @Override
    public String getType() {
        return "dremio";
    }

    @Override
    public boolean isUseDatasourcePool() {
        return false;
    }

    @Override
    public Connection getConnection(DatasourceRequest datasourceRequest) throws Exception {
        DremioConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), DremioConfig.class);

        String defaultDriver = dmConfig.getDriver();
        String customDriver = dmConfig.getCustomDriver();

        String url = dmConfig.getJdbc();
        Properties props = new Properties();
        DeDriver deDriver = null;

        if (StringUtils.isNotBlank(dmConfig.getUsername())) {
            props.setProperty("user", dmConfig.getUsername());
            if (StringUtils.isNotBlank(dmConfig.getPassword())) {
                props.setProperty("password", dmConfig.getPassword());
            }
        }

        Connection conn;
//        conn = DriverManager.getConnection(url, props);
        String driverClassName;
        ExtendedJdbcClassLoader jdbcClassLoader;
        if (isDefaultClassLoader(customDriver)) {
            driverClassName = defaultDriver;
            jdbcClassLoader = extendedJdbcClassLoader;
        } else {
            if (deDriver == null) {
                deDriver = deDriverMapper.selectByPrimaryKey(customDriver);
            }
            driverClassName = deDriver.getDriverClass();
            jdbcClassLoader = getCustomJdbcClassLoader(deDriver);
        }

        Driver driverClass = (Driver) jdbcClassLoader.loadClass(driverClassName).newInstance();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jdbcClassLoader);
            conn = driverClass.connect(url, props);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return conn;
    }

    @Override
    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) throws Exception {
        List<TableDesc> tables = new ArrayList<>();
        String queryStr = getTablesSql(datasourceRequest);
        JdbcConfiguration jdbcConfiguration = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        int queryTimeout = jdbcConfiguration.getQueryTimeout() > 0 ? jdbcConfiguration.getQueryTimeout() : 0;
        try (Connection con = getConnectionFromPool(datasourceRequest); Statement statement = getStatement(con, queryTimeout); ResultSet resultSet = statement.executeQuery(queryStr)) {
            while (resultSet.next()) {
                tables.add(getTableDesc(datasourceRequest, resultSet));
            }
        } catch (Exception e) {
            DataEaseException.throwException(e);
        }

        return tables;
    }

    private TableDesc getTableDesc(DatasourceRequest datasourceRequest, ResultSet resultSet) throws SQLException {
        TableDesc tableDesc = new TableDesc();
        tableDesc.setName(resultSet.getString(2));
//        tableDesc.setRemark(resultSet.getString(1));
        return tableDesc;
    }

    private Connection getConn(DatasourceRequest datasourceRequest) {
        DremioConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), DremioConfig.class);
        Properties props = new Properties();
        if (StringUtils.isNotBlank(dmConfig.getUsername())) {
            props.setProperty("user", dmConfig.getUsername());
            if (StringUtils.isNotBlank(dmConfig.getPassword())) {
                props.setProperty("password", dmConfig.getPassword());
            }
        }

        Connection conn = null;
        String DB_URL = dmConfig.getJdbc();
        try {
            Class.forName("com.dremio.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, props);
        } catch (SQLException e) {
            e.printStackTrace();
            DataEaseException.throwException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return conn;

    }

    @Override
    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) throws Exception {
        List<TableField> list = new LinkedList<>();
//        try (Connection connection = getConn(datasourceRequest)) {
        try (Connection connection = getConnectionFromPool(datasourceRequest)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getColumns(null, "%", datasourceRequest.getTable(), "%");
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                String schema = resultSet.getString("TABLE_SCHEM");
                if (tableName.equals(datasourceRequest.getTable()) && schema.equalsIgnoreCase(getDatabase(datasourceRequest))) {
                    TableField tableField = getTableFiled(resultSet, datasourceRequest);
                    list.add(tableField);
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            DataEaseException.throwException(e);
        } catch (Exception e) {
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }
        return list;
    }

    private String getDatabase(DatasourceRequest datasourceRequest) {
        JdbcConfiguration jdbcConfiguration = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        return jdbcConfiguration.getDataBase();
    }

    private String getDsSchema(DatasourceRequest datasourceRequest) {
        JdbcConfiguration jdbcConfiguration = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        return jdbcConfiguration.getSchema();
    }


    private TableField getTableFiled(ResultSet resultSet, DatasourceRequest datasourceRequest) throws SQLException {
        TableField tableField = new TableField();
        String colName = resultSet.getString("COLUMN_NAME");
        tableField.setFieldName(colName);
        String remarks = resultSet.getString("REMARKS");
        if (remarks == null || remarks.equals("")) {
            remarks = colName;
        }
        tableField.setRemarks(remarks);
        String dbType = resultSet.getString("TYPE_NAME").toUpperCase();
        tableField.setFieldType(dbType);
        if (dbType.equalsIgnoreCase("LONG")) {
            tableField.setFieldSize(65533);
        }
        if (StringUtils.isNotEmpty(dbType) && dbType.toLowerCase().contains("date") && tableField.getFieldSize() < 50) {
            tableField.setFieldSize(50);
        }

        if (datasourceRequest.getDatasource().getType().equalsIgnoreCase(DatasourceTypes.hive.name()) && tableField.getFieldType().equalsIgnoreCase("BOOLEAN")) {
            tableField.setFieldSize(1);
        } else {
            String size = resultSet.getString("COLUMN_SIZE");
            if (size == null) {
                tableField.setFieldSize(1);
            } else {
                tableField.setFieldSize(Integer.valueOf(size));
            }
        }
        return tableField;
    }

    @Override
    public String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        String queryStr = getTablesSql(datasourceRequest);
        DremioConfig jdbcConfiguration = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), DremioConfig.class);
        int queryTimeout = jdbcConfiguration.getQueryTimeout() > 0 ? jdbcConfiguration.getQueryTimeout() : 0;

//        Class.forName("com.dremio.jdbc.Driver");
//        DremioConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), DremioConfig.class);
//        String DB_URL = dmConfig.getJdbc();
//        String USER = "zq";// ak
//        String PASS = "Calong@2015"; // sk
//
//        Properties props = new Properties();
//        props.setProperty("user", USER);
//        props.setProperty("password", PASS);
//        Connection connection = null;
//        Statement stmt = null;
//        ResultSet resultSet = null;
//        try {
//            connection = DriverManager.getConnection(DB_URL, props);
//            stmt = connection.createStatement();
//            resultSet = stmt.executeQuery(queryStr);
//
//            while (resultSet.next()) {
//                System.out.println("字段名\t\t字段值\t\t");
//                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
//                    System.out.println(resultSet.getMetaData().getColumnLabel(i) + "\t\t" + resultSet.getString(i));
//                }
//                System.out.println("----------------------");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            DataEaseException.throwException(e.getMessage());
//        } finally {
//            try {
//                if (resultSet != null) {
//                    resultSet.close();
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            try {
//                if (stmt != null) {
//                    stmt.close();
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }


        try (Connection con = getConnection(datasourceRequest); Statement statement = getStatement(con, queryTimeout); ResultSet resultSet = statement.executeQuery(queryStr)) {
        } catch (Exception e) {
            e.printStackTrace();
            DataEaseException.throwException(e.getMessage());
        }
        return "Success";
    }


    @Override
    public String getTablesSql(DatasourceRequest datasourceRequest) throws Exception {
        return "show tables;";
    }

    @Override
    public String getSchemaSql(DatasourceRequest datasourceRequest) {
        return "select OBJECT_NAME from dba_objects where object_type='SCH'";
    }

}
