package io.dataease.plugins.datasource.sls.provider;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.IndexKey;
import com.aliyun.openservices.log.common.LogContent;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.common.QueriedLog;
import com.aliyun.openservices.log.response.GetIndexResponse;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.google.gson.Gson;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.exception.DataEaseException;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.provider.DefaultJdbcProvider;
import io.dataease.plugins.datasource.sls.commons.utils.TimeRangeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.logging.LogException;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.Date;


@Component()
public class SlsDsProvider extends DefaultJdbcProvider {

    @Override
    public String getType() {
        return "sls";
    }

    @Override
    public boolean isUseDatasourcePool() {
        return false;
    }

    @Override
    public List<String> getSchema(DatasourceRequest datasourceRequest) throws Exception {
        List<String> schemas = new ArrayList<>();

        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
            Client client = getSlsClient(dmConfig);

            ArrayList<String> logStores = client.ListLogStores(dmConfig.getProject(),0,500,"")
                    .GetLogStores();
            for (String store : logStores) {
                schemas.add(store);
            }
        } catch (LogException e) {
            DataEaseException.throwException(e);
        }

        return schemas;
    }

    @Override
    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) throws Exception {
        List<TableDesc> tables = new ArrayList<>();

        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
            Client client = getSlsClient(dmConfig);

            ArrayList<String> logStores = client.ListLogStores(dmConfig.getProject(),0,500,"")
                    .GetLogStores();
            for (String store : logStores) {
                if (StringUtils.equals(store, dmConfig.getLogStore())) {
                    tables.add(getTableDesc(store));
                }
            }
        } catch (LogException e) {
            DataEaseException.throwException(e);
        }

        return tables;
    }

    private TableDesc getTableDesc(String logStore) {
        TableDesc tableDesc = new TableDesc();
        tableDesc.setName(logStore);
        return tableDesc;
    }

    @Override
    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) {
        List<TableField> list = new LinkedList<>();
        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
            Client client = getSlsClient(dmConfig);
            ArrayList<String> logStores = client.ListLogStores(dmConfig.getProject(),0,500,"")
                    .GetLogStores();
            for (String store : logStores) {
                if (store.equals(datasourceRequest.getTable())) {
                    GetIndexResponse getIndexResponse = client.GetIndex(dmConfig.getProject(), store);
                    Map<String, IndexKey> keyMap = getIndexResponse.GetIndex().GetKeys().GetKeys();
                    for (String key : keyMap.keySet()) {
                        list.add(getTableFiled(keyMap, key));
                    }
                }
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 500);
            calendar.set(Calendar.MINUTE,(calendar.get(Calendar.MINUTE) - 20));
            int from = (int) (calendar.getTime().getTime() / 1000 - 600);
            int to = (int) (new Date().getTime() / 1000);

            GetLogsResponse logsResponse = client.executeLogstoreSql(dmConfig.getProject(), dmConfig.getLogStore(), from, to, getFieldsSql(), true);
            if (CollectionUtils.isNotEmpty(logsResponse.getKeys())) {
                for (String key : logsResponse.getKeys()) {
                    if (!list.stream().anyMatch(field -> StringUtils.equalsIgnoreCase(field.getFieldName(), key))) {
                        TableField tableField = new TableField();
                        tableField.setFieldName(key);
                        tableField.setRemarks(key);
                        tableField.setFieldType("text");
                        list.add(tableField);
                    }
                }
            }
        } catch (LogException e) {
            DataEaseException.throwException("SLS DataSource GetTableFields LogException:" + e.getMessage());
        } catch (Exception e) {
            DataEaseException.throwException(e);
        }
        return list;
    }

    private TableField getTableFiled(Map<String, IndexKey> keyMap, String key) {
        TableField tableField = new TableField();

        IndexKey indexKey = keyMap.get(key);
        String dbType = indexKey.GetType();

        tableField.setFieldName(key);
        tableField.setRemarks(StringUtils.isNotEmpty(indexKey.getAlias()) ? indexKey.getAlias() : key);
        tableField.setFieldType(dbType);
        if (dbType.equalsIgnoreCase("LONG")) {
            tableField.setFieldSize(65533);
        }
        if (StringUtils.isNotEmpty(dbType) && dbType.toLowerCase().contains("date") && tableField.getFieldSize() < 50) {
            tableField.setFieldSize(50);
        }

        if (dbType.equalsIgnoreCase("BOOLEAN")) {
            tableField.setFieldSize(1);
        }
        return tableField;
    }

    @Override
    public String checkStatus(DatasourceRequest datasourceRequest) {
        String queryStr = getCheckSql();

        SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
        try {
            Client client = getSlsClient(dmConfig);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 500);
            calendar.set(Calendar.MINUTE,(calendar.get(Calendar.MINUTE) - 20));
            int from = (int) (calendar.getTime().getTime() / 1000 - 600);
            int to = (int) (new Date().getTime() / 1000);

            GetLogsResponse logsResponse = client.executeLogstoreSql(dmConfig.getProject(), dmConfig.getLogStore(), from, to, queryStr, true);
            System.out.println("Returned sql result count:" + logsResponse.GetCount());
        } catch (Exception e) {
            e.printStackTrace();
            DataEaseException.throwException(e.getMessage());
        }
        return "Success";
    }

    private String getCheckSql() {
        return "* | select COUNT(*) limit 1";
    }

    private String getFieldsSql() {
        return "* | select * limit 1";
    }

    private Client getSlsClient(SlsConfig dmConfig) {
        return new Client(dmConfig.getHost(), dmConfig.getAccessId(), dmConfig.getAccessKey());
    }

    @Override
    public Map<String, List> fetchResultAndField(DatasourceRequest datasourceRequest) {
        Map<String, List> result = new HashMap<>();

        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
            Client client = new Client(dmConfig.getHost(), dmConfig.getAccessId(), dmConfig.getAccessKey());
            ArrayList<String> logStores = client.ListLogStores(dmConfig.getProject(),0,500,"")
                    .GetLogStores();

            // 获取表名
            if (StringUtils.isBlank(datasourceRequest.getTable())) {
                datasourceRequest.setTable(dmConfig.getLogStore());
            }

            List<TableField> fieldList = new ArrayList<>();

            List<ArrayList> list = getDataResult(datasourceRequest, fieldList);
            List<String[]> dataList = new LinkedList<>();
            Optional.ofNullable(list).orElse(new ArrayList<>()).forEach(bo -> {
                String[] row = new String[bo.size()];
                for (int i = 0; i < bo.size(); i++) {
                    row[i] = bo.get(i).toString();
                }
                dataList.add(row);
            });

            result.put("dataList", dataList);
            result.put("fieldList", fieldList);
            return result;
        } catch (Exception e) {
            DataEaseException.throwException(e);
        }
        return new HashMap<>();
    }

    private List<ArrayList> getDataResult(DatasourceRequest datasourceRequest, List<TableField> fieldList) throws Exception {
        List<ArrayList> list = new LinkedList<>();

        // 获取数据
        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
            Client client = getSlsClient(dmConfig);
            String sql = datasourceRequest.getQuery();

            // 当前表的所有字段
            List<TableField> allFieldList = getTableFields(datasourceRequest);

            // 时间参数处理
            Date currentDate = new Date();
            int from = TimeRangeUtils.getTimeByTimeRange(dmConfig.getDefaultTimeRange(), currentDate);
            int to = TimeRangeUtils.getCurrentTime(currentDate);

            GetLogsResponse logsResponse = client.executeLogstoreSql(dmConfig.getProject(), datasourceRequest.getTable(), from, to, sql, true);
            List<String> keys = logsResponse.getKeys();
            for (QueriedLog log : logsResponse.getLogs()) {
                LogItem item = log.GetLogItem();
                ArrayList row = new ArrayList();
                Optional.ofNullable(keys).orElse(new ArrayList<>()).forEach(key -> {
                    if (item.mContents.stream().anyMatch(content -> checkFieldIsSame(key, content.mKey))) {
                        LogContent logContent = item.mContents.stream().filter(content -> checkFieldIsSame(key, content.mKey)).findFirst().get();
                        row.add(logContent.mValue);
                    } else {
                        if (StringUtils.equals(key, "__source__")) {
                            row.add(log.mSource);
                        } else {
                            row.add(null);
                        }
                    }
                    addTableField(fieldList, key, allFieldList);
                });

                list.add(row);
            }
        } catch (LogException e) {
            throw e;
        }

        return list;
    }

    private Boolean checkFieldIsSame(String fieldName, String contentKey) {
        if (StringUtils.equals(fieldName, contentKey)) {
            return Boolean.TRUE;
        }
        if (fieldName.contains("__") && contentKey.contains("__") && contentKey.contains(fieldName)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public List<String[]> getData(DatasourceRequest datasourceRequest) {
        List<ArrayList> list = new LinkedList<>();

        try {
            SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);

            // 获取表名
            if (StringUtils.isBlank(datasourceRequest.getTable())) {
                datasourceRequest.setTable(dmConfig.getLogStore());
            }

            // 获取数据
            list = getDataResult(datasourceRequest, new ArrayList<>());
        } catch (Exception e) {
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }

        List<String[]> result = new LinkedList<>();
        Optional.ofNullable(list).orElse(new ArrayList<>()).forEach(bo -> {
            String[] row = new String[bo.size()];
            for (int i = 0; i < bo.size(); i++) {
                row[i] = bo.get(i).toString();
            }
            result.add(row);
        });

        return result;
    }

    @Override
    public List<TableField> fetchResultField(DatasourceRequest datasourceRequest) {

        List<TableField> fieldList = new ArrayList<>();

        SlsConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), SlsConfig.class);
        try {
            Client client = new Client(dmConfig.getHost(), dmConfig.getAccessId(), dmConfig.getAccessKey());
            // 获取表名
            if (StringUtils.isBlank(datasourceRequest.getTable())) {
                datasourceRequest.setTable(dmConfig.getLogStore());
            }

            // 当前表的所有字段
            List<TableField> allFieldList = getTableFields(datasourceRequest);

            // 时间参数处理
            Date currentDate = new Date();
            int from = TimeRangeUtils.getTimeByTimeRange(dmConfig.getDefaultTimeRange(), currentDate);
            int to = TimeRangeUtils.getCurrentTime(currentDate);

            GetLogsResponse logsResponse = client.executeLogstoreSql(dmConfig.getProject(), datasourceRequest.getTable(), from, to, datasourceRequest.getQuery(), true);
            if (CollectionUtils.isNotEmpty(logsResponse.getKeys())) {
                List<String> keys = logsResponse.getKeys();
                Optional.ofNullable(keys).orElse(new ArrayList<>()).forEach(key -> {
                    addTableField(fieldList, key, allFieldList);
                });
            }
        } catch (SQLException e) {
            DataEaseException.throwException(e);
        } catch (Exception e) {
            e.printStackTrace();
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }
        return fieldList;
    }

    private void addTableField(List<TableField> fieldList, String key, List<TableField> allFieldList) {
        if (!fieldList.stream().anyMatch(field -> StringUtils.equalsIgnoreCase(field.getFieldName(), key))) {
            if (allFieldList.stream().anyMatch(field -> StringUtils.equalsIgnoreCase(field.getFieldName(), key))) {
                TableField curField = allFieldList.stream().filter(field -> StringUtils.equalsIgnoreCase(field.getFieldName(), key)).findFirst().get();
                fieldList.add(curField);
            } else {
                TableField tableField = new TableField();
                tableField.setFieldName(key);
                tableField.setRemarks(key);
                tableField.setFieldType("text");
                fieldList.add(tableField);
            }
        }
    }

}
