package io.dataease.plugins.datasource.influxdb.provider;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.plugins.common.dto.datasource.TableDesc;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.exception.DataEaseException;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import io.dataease.plugins.datasource.provider.DefaultJdbcProvider;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.logging.LogException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component()
public class InfluxdbDsProvider extends DefaultJdbcProvider {
    @Override
    public String getType() {
        return "influxdb";
    }

    @Override
    public boolean isUseDatasourcePool() {
        return false;
    }

    @Override
    public List<TableDesc> getTables(DatasourceRequest datasourceRequest) {
        List<TableDesc> tables = new ArrayList<>();
        String queryStr = getTablesSql(datasourceRequest);
        JdbcConfiguration jdbcConfiguration = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        try {
            List<QueryResult.Result> resultList = executeSql(jdbcConfiguration, queryStr);

            List<QueryResult.Series> seriesList = Optional.ofNullable(resultList)
                    .filter(CollectionUtils::isNotEmpty).map(s -> s.get(0))
                    .map(QueryResult.Result::getSeries).orElse(Lists.newArrayList());

            tables.addAll(getTableDesc(seriesList));
        } catch (Exception e) {
            DataEaseException.throwException(e);
        }

        return tables;
    }

    private List<TableDesc> getTableDesc(List<QueryResult.Series> seriesList) {
        List<TableDesc> tables = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(seriesList)) {
            for (QueryResult.Series series : seriesList) {
                Optional.ofNullable(series.getValues()).orElse(new ArrayList<>()).forEach(value -> {
                    TableDesc tableDesc = new TableDesc();
                    tableDesc.setName(value.get(0).toString());
                    tables.add(tableDesc);
                });
            }
        }
        return tables;
    }

    @Override
    public List<TableField> getTableFields(DatasourceRequest datasourceRequest) {
        List<TableField> list = new LinkedList<>();
        try {
            InfluxdbConfig influxdbConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), InfluxdbConfig.class);

            list.add(getTableDefaultFiled());

            // 查询当前表的所有标签字段
            String showTagSql = "show tag keys from " + datasourceRequest.getTable();
            List<QueryResult.Result> tagResultList = executeSql(influxdbConfig, showTagSql);
            List<QueryResult.Series> seriesList = getSeriesListFromResult(tagResultList);
            if (CollectionUtils.isNotEmpty(seriesList)) {
                for (QueryResult.Series series : seriesList) {
                    Optional.ofNullable(series.getValues()).orElse(new ArrayList<>()).forEach(value -> {
                        TableField tableField = getTableTagFiled(value.get(0).toString());
                        list.add(tableField);
                    });
                }
            }

            // 查询当前表的数值字段
            String showFieldSql = "show field keys from " + datasourceRequest.getTable();
            List<QueryResult.Result> fieldResultList = executeSql(influxdbConfig, showFieldSql);
            List<QueryResult.Series> fieldSeriesList = getSeriesListFromResult(fieldResultList);
            if (CollectionUtils.isNotEmpty(fieldSeriesList)) {
                for (QueryResult.Series series : fieldSeriesList) {
                    Optional.ofNullable(series.getValues()).orElse(new ArrayList<>()).forEach(value -> {
                        TableField tableField = getTableFieldFiled(value);
                        list.add(tableField);
                    });
                }
            }
        } catch (Exception e) {
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }
        return list;
    }

    private List<QueryResult.Series> getSeriesListFromResult(List<QueryResult.Result> resultList) {
        // 解析异常
        List<QueryResult.Result> errorList = Optional.ofNullable(resultList).orElse(new ArrayList<>()).stream().filter(res -> StringUtils.isNotEmpty(res.getError())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(errorList)) {
            throw new RuntimeException(errorList.get(0).getError());
        }

        List<QueryResult.Series> seriesList = Optional.ofNullable(resultList)
                .filter(CollectionUtils::isNotEmpty).map(s -> s.get(0))
                .map(QueryResult.Result::getSeries).orElse(Lists.newArrayList());
        return seriesList;
    }

    private TableField getTableTagFiled(String key) {
        TableField tableField = new TableField();
        tableField.setFieldName(key);
        tableField.setRemarks(key);
        tableField.setFieldType("STRING");
        return tableField;
    }

    private TableField getTableFieldFiled(List<Object> values) {
        TableField tableField = new TableField();
        tableField.setFieldName(values.get(0).toString());
        tableField.setRemarks(values.get(0).toString());
        tableField.setFieldType(values.get(1).toString().toUpperCase(Locale.ROOT));
        return tableField;
    }

    private TableField getTableDefaultFiled() {
        TableField tableField = new TableField();
        tableField.setFieldName("time");
        tableField.setRemarks("time");
        tableField.setFieldType("DATE");
        return tableField;
    }

    @Override
    public String checkStatus(DatasourceRequest datasourceRequest) {
        String queryStr = getCheckSql();
        JdbcConfiguration influxdbConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), JdbcConfiguration.class);
        try {
            executeSql(influxdbConfig, queryStr);
        } catch (Exception e) {
            e.printStackTrace();
            DataEaseException.throwException(e.getMessage());
        }
        return "Success";
    }

    private String getCheckSql() {
        return "show databases";
    }

    @Override
    public String getTablesSql(DatasourceRequest datasourceRequest) {
        return "show measurements";
    }

    /**
     * 使用 SQL 查询数据
     *
     * @param sql
     * @return
     */
    private List<QueryResult.Result> executeSql(JdbcConfiguration jdbcConfiguration, String sql) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS);

        InfluxDB influxDB = InfluxDBFactory.connect(getUrl(jdbcConfiguration), jdbcConfiguration.getUsername(), jdbcConfiguration.getPassword(), builder);
        influxDB.setDatabase(jdbcConfiguration.getDataBase());
        QueryResult queryResult = influxDB.query(new Query(sql));
        return queryResult.getResults();
    }

    private String getUrl(JdbcConfiguration jdbcConfiguration) {
        return "http://" + jdbcConfiguration.getHost() + ":" + jdbcConfiguration.getPort();
    }

    @Override
    public Map<String, List> fetchResultAndField(DatasourceRequest datasourceRequest) {
        try {
            List<TableField> allFieldList = getTableFields(datasourceRequest);
            return getDataResult(datasourceRequest, allFieldList);
        } catch (Exception e) {
            DataEaseException.throwException(e);
        }
        return new HashMap<>();
    }

    /**
     * 获取数据集合
     *
     * @param datasourceRequest
     * @param allFieldList
     * @return
     */
    private Map<String, List> getDataResult(DatasourceRequest datasourceRequest, List<TableField> allFieldList) {
        Map<String, List> resultMap = new HashMap<>();

        try {
            /*获取数据源中的数据*/
            InfluxdbConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), InfluxdbConfig.class);
            String sql = datasourceRequest.getQuery();
            List<QueryResult.Result> resultList = executeSql(dmConfig, sql);
            List<QueryResult.Series> seriesList = getSeriesListFromResult(resultList);

            // 当所有字段仅有时间字段时，根据表名重新获取所有字段
            if (allFieldList.stream().allMatch(field -> StringUtils.equals("time", field.getFieldName()))
                    && CollectionUtils.isNotEmpty(seriesList)) {
                datasourceRequest.setTable(seriesList.get(0).getName());
                allFieldList = getTableFields(datasourceRequest);
            }

            /*组织字段数据，包括查询返回的列字段和标签字段*/
            List<TableField> fieldList = dealWithFields(seriesList, allFieldList);
            resultMap.put("fieldList", fieldList);

            /*组织数据行数据*/
            List<ArrayList> dataRowList = new LinkedList<>();
            if (CollectionUtils.isNotEmpty(seriesList)) {
                dataRowList = resolveSeriesForFetchResultAndField(seriesList, datasourceRequest);
            }

            List<String[]> dataList = new LinkedList<>();
            Optional.ofNullable(dataRowList).orElse(new ArrayList<>()).forEach(bo -> {
                String[] row = new String[bo.size()];
                for (int i = 0; i < bo.size(); i++) {
                    row[i] = bo.get(i).toString();
                }
                dataList.add(row);
            });
            resultMap.put("dataList", dataList);
        } catch (LogException e) {
            throw e;
        }

        return resultMap;
    }

    private List<ArrayList> resolveSeriesForFetchResultAndField(List<QueryResult.Series> seriesList, DatasourceRequest datasourceRequest) {
        List<ArrayList> dataRowList = new LinkedList<>();

        for (QueryResult.Series series : seriesList) {
            List<DatasetTableField> permissionFields = datasourceRequest.getPermissionFields();
            if (CollectionUtils.isNotEmpty(permissionFields) && !datasourceRequest.isPreviewData()) {
                /*过滤组件处理，取标签字段值*/
                ArrayList rowValues = resolveTagValuesForFilter(series.getTags());
                dataRowList.add(rowValues);
            } else {
                /*行数据集合处理，遍历行数据集合，处理空时间字段以及字段排序问题*/
                List<List<Object>> rows = series.getValues();
                Optional.ofNullable(rows).orElse(new ArrayList<>()).forEach(rowValues -> {
                    // 处理之后的行数据
                    ArrayList dealAfterRow = new ArrayList();

                    // 查询总页数的情况下取下标为 1 的列
                    if (datasourceRequest.isTotalPageFlag()) {
                        if (rowValues.size() > 1) {
                            dealAfterRow.add(((Double) rowValues.get(1)).intValue());
                        }
                    } else {
                        Stream.iterate(0, i -> i + 1).limit(rowValues.size()).forEach(index -> {
                            Object rowValue = fetchValueForRow(rowValues.get(index), index, rowValues.get(0).toString(), series.getColumns().get(index).contains("count"));
                            if (rowValue != null) {
                                dealAfterRow.add(rowValue);
                            }
                        });
                        // 标签值处理，与字段列对应，在行数据最后添加
                        dealAfterRow.addAll(resolveTagValuesForFilter(series.getTags()));
                    }

                    dataRowList.add(dealAfterRow);
                });
            }
        }

        return dataRowList;
    }


    private Object fetchValueForRow(Object value, Integer index, String time, Boolean isCountColumn) {
        if (index == 0) {
//            return dataFormat(time);
            return time;
        }
        if (value == null) {
            return "";
        } else {
            // double类型数值科学计数法转换
            if (value.getClass().equals(Double.class) &&
                    Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$").matcher(value.toString()).find()) {
                return new BigDecimal(((Double) value).doubleValue());
            } else if (isCountColumn && value instanceof Double) {
                return ((Double) value).intValue();
            } else {
                return value;
            }
        }
    }


    private List<TableField> dealWithFields(List<QueryResult.Series> seriesList, List<TableField> allFieldList) {
        List<TableField> tableFieldList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(seriesList)) {
            // 数据列名
            List<String> columns = seriesList.get(0).getColumns();
            // 标签数据
            Map<String, String> tags = seriesList.get(0).getTags();
            if (CollectionUtils.isNotEmpty(columns)) {
                fetchFields(columns, allFieldList, tableFieldList);
                if (tags != null) {
                    fetchTags(tags, tableFieldList);
                }
            }
        }

        return tableFieldList;
    }

    private Integer validateContainsTimeField(DatasourceRequest datasourceRequest) {
        Integer timeIndex = null;
        List<ChartViewFieldDTO> xAxis = datasourceRequest.getXAxis();
        if (CollectionUtils.isNotEmpty(xAxis) && xAxis.stream().anyMatch(x -> StringUtils.equals(x.getOriginName(), "time"))) {
            for (int i = 0; i < xAxis.size(); i++) {
                if (StringUtils.equals(xAxis.get(i).getOriginName(), "time")) {
                    timeIndex = i + 1;
                    break;
                }
            }
        }
        return timeIndex;
    }

    /**
     * 过滤组件处理，取标签字段值
     * @param tagMap
     * @return
     */
    private ArrayList resolveTagValuesForFilter(Map<String, String> tagMap) {
        ArrayList rowVales = new ArrayList();
        if (tagMap != null && !tagMap.isEmpty()) {
            for (String key : tagMap.keySet()) {
                rowVales.add(StringUtils.isNotEmpty(tagMap.get(key)) ? tagMap.get(key) : "");
            }
        }
        return rowVales;
    }

    private List<Object> sortValueForRow(Object value, Integer timeIndex, Integer index, String time, Boolean isCountColumn) {
        List<Object> values = new ArrayList<>();
        if (index == 0) {
            if (timeIndex != null && timeIndex - 1 == 0) {
                values.add(time);
                return values;
            }
            return null;
        }

        if (value == null) {
            values.add("");
        } else {
            // double类型数值科学计数法转换
            if (value.getClass().equals(Double.class) &&
                    Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$").matcher(value.toString()).find()) {
                values.add(new BigDecimal(((Double) value).doubleValue()));
            } else if (isCountColumn && value instanceof Double) {
                values.add(((Double) value).intValue());
            } else {
                values.add(value);
            }
        }

        // 明细表中有时间列
        if (timeIndex != null && timeIndex-1 == index) {
            values.add(time);
        }
        return values;
    }

    private Object dealWithValueForRow(Object value, Integer timeIndex, Integer index, String time, Boolean isCountColumn) {
        if (index == 0) {
            return null;
        }
        // 明细表中有时间列
        if (timeIndex != null && timeIndex == index) {
            return time;
        }
        if (value == null) {
            return "";
        } else {
            // double类型数值科学计数法转换
            if (value.getClass().equals(Double.class) &&
                    Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$").matcher(value.toString()).find()) {
                return new BigDecimal(((Double) value).doubleValue());
            } else if (isCountColumn && value instanceof Double) {
                return ((Double) value).intValue();
            } else {
                return value;
            }
        }
    }

    private List<ArrayList> resolveSeries(List<QueryResult.Series> seriesList, Integer timeIndex, DatasourceRequest datasourceRequest) {
        List<ArrayList> dataRowList = new LinkedList<>();

        Integer xCount = datasourceRequest.getXAxis() != null ? datasourceRequest.getXAxis().size() : 0;
        Integer yCount = datasourceRequest.getYAxis() != null ? datasourceRequest.getYAxis().size() : 0;

        for (QueryResult.Series series : seriesList) {
            List<DatasetTableField> permissionFields = datasourceRequest.getPermissionFields();
            if (CollectionUtils.isNotEmpty(permissionFields) && !datasourceRequest.isPreviewData()) {
                /*过滤组件处理，取标签字段值*/
                ArrayList rowValues = resolveTagValuesForFilter(series.getTags());
                dataRowList.add(rowValues);
            } else {
                /*行数据集合处理，遍历行数据集合，处理空时间字段以及字段排序问题*/
                List<List<Object>> rows = series.getValues();
                Optional.ofNullable(rows).orElse(new ArrayList<>()).forEach(rowValues -> {
                    // 处理之后的行数据
                    ArrayList dealAfterRow = new ArrayList();

                    // 查询总页数的情况下取下标为 1 的列
                    if (datasourceRequest.isTotalPageFlag()) {
                        if (rowValues.size() > 1) {
                            dealAfterRow.add(((Double) rowValues.get(1)).intValue());
                        }
                    } else {
                        Stream.iterate(0, i -> i + 1).limit(rowValues.size()).forEach(index -> {
                            if (datasourceRequest.isPreviewData()) {
                                if (CollectionUtils.isEmpty(datasourceRequest.getPermissionFields())) {
//                                    Object rowValue = fetchValueForRow(rowValues.get(index), index, dataFormat(rowValues.get(0).toString()), series.getColumns().get(index).contains("count"));
                                    Object rowValue = fetchValueForRow(rowValues.get(index), index, rowValues.get(0).toString(), series.getColumns().get(index).contains("count"));
                                    if (rowValue != null) {
                                        dealAfterRow.add(rowValue);
                                    }
                                } else if (datasourceRequest.getPermissionFields().stream().anyMatch(field -> StringUtils.equals(series.getColumns().get(index), field.getOriginName()))) {
//                                    Object rowValue = fetchValueForRow(rowValues.get(index), index, dataFormat(rowValues.get(0).toString()), series.getColumns().get(index).contains("count"));
                                    Object rowValue = fetchValueForRow(rowValues.get(index), index, rowValues.get(0).toString(), series.getColumns().get(index).contains("count"));
                                    if (rowValue != null) {
                                        dealAfterRow.add(rowValue);
                                    }
                                }
                            } else {
                                if (series.getColumns().size() == xCount + yCount) {
//                                    List<Object> rowValue = sortValueForRow(rowValues.get(index), timeIndex, index, dataFormat(rowValues.get(0).toString()), series.getColumns().get(index).contains("count"));
                                    List<Object> rowValue = sortValueForRow(rowValues.get(index), timeIndex, index, rowValues.get(0).toString(), series.getColumns().get(index).contains("count"));
                                    if (rowValue != null) {
                                        dealAfterRow.addAll(rowValue);
                                    }
                                } else {
                                    // timeIndex+1 是因为influxdb返回的第一个字段time是自带的，不算在下标范围内
//                                    Object rowValue = dealWithValueForRow(rowValues.get(index), timeIndex, index, dataFormat(rowValues.get(0).toString()), series.getColumns().get(index).contains("count"));
                                    Object rowValue = dealWithValueForRow(rowValues.get(index), timeIndex, index, rowValues.get(0).toString(), series.getColumns().get(index).contains("count"));
                                    if (rowValue != null) {
                                        dealAfterRow.add(rowValue);
                                    }
                                }
                            }
                        });
                        // 标签值处理，与字段列对应，在行数据最后添加
                        dealAfterRow.addAll(resolveTagValuesForFilter(series.getTags()));
                    }

                    dataRowList.add(dealAfterRow);
                });
            }
        }

        return dataRowList;
    }

    /**
     * 封装字段信息
     * @param columns
     * @param allFieldList
     * @param fieldList
     */
    private void fetchFields(List<String> columns, List<TableField> allFieldList, List<TableField> fieldList) {
        if (allFieldList.size() == 0) {
            return;
        }
        Optional.ofNullable(columns).orElse(new ArrayList<>()).forEach(column -> {
            List<TableField> tableFields = allFieldList.stream().filter(field -> StringUtils.equalsIgnoreCase(field.getFieldName(), column)).collect(Collectors.toList());
            if (tableFields.size() > 0) {
                fieldList.add(tableFields.get(0));
            } else {
                TableField tableField = new TableField();
                tableField.setFieldName(column);
                tableField.setRemarks(column);
                tableField.setFieldType("STRING");
                fieldList.add(tableField);
            }
        });
    }

    private void fetchTags(Map<String, String> tags, List<TableField> fieldList) {
        if (tags.isEmpty()) {
            return;
        }

        for (String key : tags.keySet()) {
            TableField tableField = new TableField();
            tableField.setFieldName(key);
            tableField.setRemarks(key);
            tableField.setFieldType("STRING");
            fieldList.add(tableField);
        }
    }

    @Override
    public List<String[]> getData(DatasourceRequest request) {
        List<String[]> resultList = new LinkedList<>();
        try {
            List<ArrayList> dataRowList = new LinkedList<>();
            try {
                /*获取数据源中的数据*/
                InfluxdbConfig dmConfig = new Gson().fromJson(request.getDatasource().getConfiguration(), InfluxdbConfig.class);
                String sql = request.getQuery();
                List<QueryResult.Result> influxDBResultList = executeSql(dmConfig, sql);
                List<QueryResult.Series> seriesList = getSeriesListFromResult(influxDBResultList);

                /*组织数据行数据*/
                if (CollectionUtils.isNotEmpty(seriesList)) {
                    Integer timeIndex = validateContainsTimeField(request);
                    dataRowList = resolveSeries(seriesList, timeIndex, request);
                }
            } catch (LogException e) {
                throw e;
            }

            /*数据格式转换为字符数组集合*/
            resultList = dataFormatToStringArrayList(dataRowList, request.getQuery());
        } catch (Exception e) {
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }
        return resultList;
    }

    /**
     * 数据格式转换为字符数组集合
     * @param list
     * @param querySql
     * @return
     */
    private List<String[]> dataFormatToStringArrayList(List<ArrayList> list, String querySql) {
        List<String[]> resultList = new LinkedList<>();
        Optional.ofNullable(list).orElse(new ArrayList<>()).forEach(bo -> {
            String[] row = new String[bo.size()];
            for (int i = 0; i < bo.size(); i++) {
                row[i] = bo.get(i).toString();
            }
            resultList.add(row);
        });
        return resultList;
    }

    @Override
    public List<TableField> fetchResultField(DatasourceRequest datasourceRequest) {
        List<TableField> fieldList = new ArrayList<>();

        InfluxdbConfig dmConfig = new Gson().fromJson(datasourceRequest.getDatasource().getConfiguration(), InfluxdbConfig.class);
        try {
            List<TableField> allFieldList = getTableFields(datasourceRequest);

            String sql = datasourceRequest.getQuery();

            List<QueryResult.Result> resultList = executeSql(dmConfig, sql);

            List<QueryResult.Series> seriesList = getSeriesListFromResult(resultList);


            if (CollectionUtils.isNotEmpty(seriesList)) {
                List<String> columns = seriesList.get(0).getColumns();
                Map<String, String> tags = seriesList.get(0).getTags();
                if (CollectionUtils.isNotEmpty(columns)) {
                    fetchFields(columns, allFieldList, fieldList);
                    // 标签字段
                    if (tags != null) {
                        fetchTags(tags, fieldList);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            DataEaseException.throwException("Data source connection exception: " + e.getMessage());
        }
        return fieldList;
    }

    private String dataFormat(String time){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date myDate = dateFormat.parse(time.replace("Z","+0000"));
            //转换为年月日时分秒
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            time = df.format(myDate);
        } catch (Exception exception) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date after = df.parse(time);

                System.out.println(after);
                df.applyPattern("yyyy-MM-dd HH:mm:ss");
                df.setTimeZone(TimeZone.getDefault());
                time = df.format(after);
            } catch (Exception exception1) {
            }
        }
        return time;
    }
}
