package io.dataease.plugins.datasource.dm.provider;

import com.google.gson.Gson;
import io.dataease.plugins.common.base.domain.DeDriver;
import io.dataease.plugins.common.base.mapper.DeDriverMapper;
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
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@Component()
public class TrinoDsProvider extends DefaultJdbcProvider {
    @Resource
    private DeDriverMapper deDriverMapper;

    public TrinoDsProvider() {
    }

    public String getType() {
        return "trino";
    }

    public boolean isUseDatasourcePool() {
        return false;
    }

    public Connection getConnection(DatasourceRequest datasourceRequest) throws Exception {
        TrinoConfig trinoConfig = (TrinoConfig)(new Gson()).fromJson(datasourceRequest.getDatasource().getConfiguration(), TrinoConfig.class);
        String defaultDriver = trinoConfig.getDriver();
        String customDriver = trinoConfig.getCustomDriver();
        String url = trinoConfig.getJdbc();
        Properties props = new Properties();
        DeDriver deDriver = null;
        if (StringUtils.isNotEmpty(trinoConfig.getAuthMethod()) && trinoConfig.getAuthMethod().equalsIgnoreCase("kerberos")) {
            System.setProperty("java.security.krb5.conf", "/opt/dataease/conf/krb5.conf");
            ExtendedJdbcClassLoader classLoader;
            if (this.isDefaultClassLoader(customDriver)) {
                classLoader = this.extendedJdbcClassLoader;
            } else {
                deDriver = this.deDriverMapper.selectByPrimaryKey(customDriver);
                classLoader = this.getCustomJdbcClassLoader(deDriver);
            }

            Class<?> ConfigurationClass = classLoader.loadClass("org.apache.hadoop.conf.Configuration");
            Method set = ConfigurationClass.getMethod("set", String.class, String.class);
            Object obj = ConfigurationClass.newInstance();
            set.invoke(obj, "hadoop.security.authentication", "Kerberos");
            Class<?> UserGroupInformationClass = classLoader.loadClass("org.apache.hadoop.security.UserGroupInformation");
            Method setConfiguration = UserGroupInformationClass.getMethod("setConfiguration", ConfigurationClass);
            Method loginUserFromKeytab = UserGroupInformationClass.getMethod("loginUserFromKeytab", String.class, String.class);
            setConfiguration.invoke((Object)null, obj);
            loginUserFromKeytab.invoke((Object)null, trinoConfig.getUsername(), "/opt/dataease/conf/" + trinoConfig.getPassword());
        } else if (StringUtils.isNotBlank(trinoConfig.getUsername())) {
            props.setProperty("user", trinoConfig.getUsername());
            if (StringUtils.isNotBlank(trinoConfig.getPassword())) {
                props.setProperty("password", trinoConfig.getPassword());
            }
        }

        String driverClassName;
        ExtendedJdbcClassLoader jdbcClassLoader;
        if (this.isDefaultClassLoader(customDriver)) {
            driverClassName = defaultDriver;
            jdbcClassLoader = this.extendedJdbcClassLoader;
        } else {
            if (deDriver == null) {
                deDriver = this.deDriverMapper.selectByPrimaryKey(customDriver);
            }

            driverClassName = deDriver.getDriverClass();
            jdbcClassLoader = this.getCustomJdbcClassLoader(deDriver);
        }

        Driver driverClass = (Driver)jdbcClassLoader.loadClass(driverClassName).newInstance();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Connection conn;
        try {
            Thread.currentThread().setContextClassLoader(jdbcClassLoader);
            conn = driverClass.connect(url, props);
        } catch (Exception var18) {
            var18.printStackTrace();
            throw var18;
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        return conn;
    }

    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) throws Exception {
        List<TableDesc> tables = new ArrayList();
        String queryStr = this.getTablesSql(datasourceRequest);
        JdbcConfiguration jdbcConfiguration = (JdbcConfiguration)(new Gson()).fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        int queryTimeout = jdbcConfiguration.getQueryTimeout() > 0 ? jdbcConfiguration.getQueryTimeout() : 0;

        try {
            Connection con = this.getConnectionFromPool(datasourceRequest);
            Throwable var7 = null;

            try {
                Statement statement = this.getStatement(con, queryTimeout);
                Throwable var9 = null;

                try {
                    ResultSet resultSet = statement.executeQuery(queryStr);
                    Throwable var11 = null;

                    try {
                        while(resultSet.next()) {
                            tables.add(this.getTableDesc(datasourceRequest, resultSet));
                        }
                    } catch (Throwable var58) {
                        var11 = var58;
                        throw var58;
                    } finally {
                        if (resultSet != null) {
                            if (var11 != null) {
                                try {
                                    resultSet.close();
                                } catch (Throwable var57) {
                                    var11.addSuppressed(var57);
                                }
                            } else {
                                resultSet.close();
                            }
                        }

                    }
                } catch (Throwable var60) {
                    var9 = var60;
                    throw var60;
                } finally {
                    if (statement != null) {
                        if (var9 != null) {
                            try {
                                statement.close();
                            } catch (Throwable var56) {
                                var9.addSuppressed(var56);
                            }
                        } else {
                            statement.close();
                        }
                    }

                }
            } catch (Throwable var62) {
                var7 = var62;
                throw var62;
            } finally {
                if (con != null) {
                    if (var7 != null) {
                        try {
                            con.close();
                        } catch (Throwable var55) {
                            var7.addSuppressed(var55);
                        }
                    } else {
                        con.close();
                    }
                }

            }
        } catch (Exception var64) {
            DataEaseException.throwException(var64);
        }

        return tables;
    }

    private TableDesc getTableDesc(DatasourceRequest datasourceRequest, ResultSet resultSet) throws SQLException {
        TableDesc tableDesc = new TableDesc();
        tableDesc.setName(resultSet.getString(1));
        return tableDesc;
    }

    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) throws Exception {
        datasourceRequest.setQuery("select * from " + datasourceRequest.getTable() + " limit 0");
        return this.fetchResultField(datasourceRequest);
    }

    public String checkStatus(DatasourceRequest datasourceRequest) throws Exception {
        String queryStr = this.getTablesSql(datasourceRequest);
        JdbcConfiguration jdbcConfiguration = (JdbcConfiguration)(new Gson()).fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        int queryTimeout = jdbcConfiguration.getQueryTimeout() > 0 ? jdbcConfiguration.getQueryTimeout() : 0;

        try {
            Connection con = this.getConnection(datasourceRequest);
            Throwable var6 = null;

            try {
                Statement statement = this.getStatement(con, queryTimeout);
                Throwable var8 = null;

                try {
                    ResultSet resultSet = statement.executeQuery(queryStr);
                    Object var10 = null;
                    if (resultSet != null) {
                        if (var10 != null) {
                            try {
                                resultSet.close();
                            } catch (Throwable var37) {
                                ((Throwable)var10).addSuppressed(var37);
                            }
                        } else {
                            resultSet.close();
                        }
                    }
                } catch (Throwable var38) {
                    var8 = var38;
                    throw var38;
                } finally {
                    if (statement != null) {
                        if (var8 != null) {
                            try {
                                statement.close();
                            } catch (Throwable var36) {
                                var8.addSuppressed(var36);
                            }
                        } else {
                            statement.close();
                        }
                    }

                }
            } catch (Throwable var40) {
                var6 = var40;
                throw var40;
            } finally {
                if (con != null) {
                    if (var6 != null) {
                        try {
                            con.close();
                        } catch (Throwable var35) {
                            var6.addSuppressed(var35);
                        }
                    } else {
                        con.close();
                    }
                }

            }
        } catch (Exception var42) {
            var42.printStackTrace();
            DataEaseException.throwException(var42.getMessage());
        }

        return "Success";
    }

    public String getTablesSql(DatasourceRequest datasourceRequest) throws Exception {
        TrinoConfig trinoConfig = (TrinoConfig)(new Gson()).fromJson(datasourceRequest.getDatasource().getConfiguration(), TrinoConfig.class);
        return "show tables in " + trinoConfig.getDataBase() + "." + trinoConfig.getSchema();
    }

}
