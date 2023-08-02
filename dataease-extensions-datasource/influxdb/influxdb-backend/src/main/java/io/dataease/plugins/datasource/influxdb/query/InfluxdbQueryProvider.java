package io.dataease.plugins.datasource.influxdb.query;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.dataease.plugins.common.base.domain.ChartViewWithBLOBs;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.base.domain.DatasetTableFieldExample;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.base.mapper.DatasetTableFieldMapper;
import io.dataease.plugins.common.constants.DeTypeConstants;
import io.dataease.plugins.common.constants.datasource.SQLConstants;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.plugins.common.dto.datasource.DeSortField;
import io.dataease.plugins.common.dto.sqlObj.SQLObj;
import io.dataease.plugins.common.request.chart.ChartExtFilterRequest;
import io.dataease.plugins.common.request.permission.DataSetRowPermissionsTreeDTO;
import io.dataease.plugins.common.request.permission.DatasetRowPermissionsTreeItem;
import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import io.dataease.plugins.datasource.entity.PageInfo;
import io.dataease.plugins.datasource.influxdb.dto.InfluxdbSQLObj;
import io.dataease.plugins.datasource.influxdb.provider.InfluxdbConfig;
import io.dataease.plugins.datasource.query.QueryProvider;
import io.dataease.plugins.datasource.query.Utils;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author gin
 * @Date 2021/5/17 2:43 下午
 */
@Component()
public class InfluxdbQueryProvider extends QueryProvider {

    @Resource
    private DatasetTableFieldMapper datasetTableFieldMapper;

    @Value("${dataease.plugin.dir:/opt/dataease/plugins/}")
    private String pluginDir="/opt/dataease/plugins/";

    @Override
    public Integer transFieldType(String field) {
        switch (field) {
            case "STRING":
                return 0;// 文本
            case "DATE":
                return 1;// 时间
            case "INTEGER":
                return 2;// 整型
            case "FLOAT":
                return 3;// 浮点
            case "BOOLEAN":
                return 4;// 布尔
            default:
                return 0;
        }
    }

    @Override
    public String createSQLPreview(String sql, String orderBy) {
        return replaceSql(MessageFormat.format("SELECT * FROM ({0}) {1} LIMIT 1000", sqlFix(sql), previewOrderBy(sql)));
    }

    private String previewOrderBy(String sql) {
        // 子查询必须按照与查询本身相同的方向排序，influxdb默认使用asc升序
        if (sql.contains("desc") || sql.contains("DESC")) {
            return "order by desc";
        }
        return "";
    }

    @Override
    public String createQuerySQL(String table, List<DatasetTableField> fields, boolean isGroup, Datasource ds,
                                 List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<DeSortField> sortFields) {
        SQLObj tableObj = SQLObj.builder()
                .tableName(table)
                .build();

        setSchema(tableObj, ds);
        List<SQLObj> xFields = xFields(table, fields);

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("previewSql");
        st_sql.add("isGroup", isGroup);
        if (CollectionUtils.isNotEmpty(xFields))
            st_sql.add("groups", xFields);
//        if (CollectionUtils.isNotEmpty(xFields))
//            st_sql.add("aggregators", xFields);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);

        List<SQLObj> xOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sortFields)) {
            int step = fields.size();
            for (int i = step; i < (step + sortFields.size()); i++) {
                DeSortField deSortField = sortFields.get(i - step);
                SQLObj order = buildSortField(deSortField, tableObj, i);
                xOrders.add(order);
            }
        }
        if (ObjectUtils.isNotEmpty(xOrders)) {
            st_sql.add("orders", xOrders);
        }
        return replaceSql(st_sql.render());
    }

    private String replaceSql(String sql) {
        Map<String, String> map = InfluxdbConstants.replaceMap;
        for (String key : map.keySet()) {
            sql = sql.replace(key, map.get(key));
        }
        return sql;
    }

    @Override
    public String createQuerySQL(String table, List<DatasetTableField> fields, boolean isGroup, Datasource ds,
                                 List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return createQuerySQL(table, fields, isGroup, ds, fieldCustomFilter, rowPermissionsTree, null);
    }

    @Override
    public String createQuerySQLAsTmp(String sql, List<DatasetTableField> fields, boolean isGroup,
                                      List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<DeSortField> sortFields) {
        return createQuerySQL("(" + sqlFix(sql) + ")", fields, isGroup, null, fieldCustomFilter, rowPermissionsTree, sortFields);
    }

    public void setSchema(SQLObj tableObj, Datasource ds) {
        if (ds != null && !tableObj.getTableName().startsWith("(") && !tableObj.getTableName().endsWith(")")) {
            tableObj.setTableName(tableObj.getTableName());
        }
    }

    private SQLObj buildSortField(DeSortField f, SQLObj tableObj, int index) {
        String originField;
        if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 2) {
            // 解析origin name中有关联的字段生成sql表达式
            originField = calcFieldRegex(f.getOriginName(), tableObj);
        } else if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 1) {
            originField = String.format(InfluxdbConstants.KEYWORD_FIX, f.getOriginName());
        } else {
            originField = String.format(InfluxdbConstants.KEYWORD_FIX, f.getOriginName());
        }
        String fieldName = "";
        // 处理横轴字段
        if (f.getDeExtractType() == 1) {
            if (f.getDeType() == 2 || f.getDeType() == 3) {
                fieldName = String.format(InfluxdbConstants.UNIX_TIMESTAMP, originField) + "*1000";
            } else {
                fieldName = originField;
            }
        } else if (f.getDeExtractType() == 0) {
            if (f.getDeType() == 2) {
                fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
            } else if (f.getDeType() == 3) {
                fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
            } else if (f.getDeType() == 1) {
                fieldName = String.format(InfluxdbConstants.DATE_FORMAT, originField, InfluxdbConstants.DEFAULT_DATE_FORMAT);
            } else {
                fieldName = originField;
            }
        } else {
            if (f.getDeType() == 1) {
                String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT)
                        + "/1000";
                fieldName = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
            } else if (f.getDeType() == 2) {
                fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
            } else {
                fieldName = originField;
            }
        }
        SQLObj result = SQLObj.builder().orderField(originField).orderAlias(originField)
                .orderDirection(f.getOrderDirection()).build();
        return result;
    }

    private List<SQLObj> xFields(String table, List<DatasetTableField> fields) {
        SQLObj tableObj = SQLObj.builder()
                .tableName(table)
                .build();
        List<SQLObj> xFields = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fields)) {
            for (int i = 0; i < fields.size(); i++) {
                DatasetTableField f = fields.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(f.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, f.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, f.getOriginName());
                }
                String fieldName = "";
                // 处理横轴字段
                if (f.getDeExtractType() == 1) {
                    fieldName = originField;
                } else if (f.getDeExtractType() == 0) {
                    if (f.getDeType() == 2) {
                        fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                    } else if (f.getDeType() == 3) {
                        fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                    } else if (f.getDeType() == 1) {
                        // 不支持日期转换
                        fieldName = originField;
                    } else {
                        fieldName = originField;
                    }
                } else {
                    if (f.getDeType() == 1) {
                        fieldName = originField;
                    } else if (f.getDeType() == 2) {
                        fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                    } else {
                        fieldName = originField;
                    }
                }
                xFields.add(SQLObj.builder()
                        .fieldName(fieldName)
                        .build());
            }
        }
        return xFields;
    }

    private String sqlColumn(List<SQLObj> xFields) {
        String[] array = xFields.stream().map(f -> {
            return f.getFieldName();
        }).toArray(String[]::new);
        return replaceSql(StringUtils.join(array, ","));
    }

    @Override
    public String createQuerySQLAsTmp(String sql, List<DatasetTableField> fields, boolean isGroup,
                                      List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return createQuerySQL("(" + sqlFix(sql) + ")", fields, isGroup, null, fieldCustomFilter, rowPermissionsTree);
    }

    @Override
    public String createQueryTableWithPage(String table, List<DatasetTableField> fields, Integer page, Integer pageSize,
                                           Integer realSize, boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        List<SQLObj> xFields = xFields(table, fields);

        return MessageFormat.format(
                "SELECT {0} FROM ( SELECT * FROM ( {1} ) ) LIMIT {2} OFFSET {3} ",
                sqlColumn(xFields), createQuerySQL(table, fields, isGroup, ds, fieldCustomFilter, rowPermissionsTree),
                Integer.valueOf(realSize).toString(), Integer.valueOf((page - 1) * pageSize).toString());
    }

    @Override
    public String createQuerySQLWithPage(String sql, List<DatasetTableField> fields, Integer page, Integer pageSize,
                                         Integer realSize, boolean isGroup, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        List<SQLObj> xFields = xFields("(" + sqlFix(sql) + ")", fields);
        return MessageFormat.format(
                "SELECT {0} FROM ( SELECT * FROM ( {1} ) ) LIMIT {2} OFFSET {3} ",
                sqlColumn(xFields), createQuerySQLAsTmp(sql, fields, isGroup, fieldCustomFilter, rowPermissionsTree),
                Integer.valueOf(realSize).toString(), Integer.valueOf((page - 1) * pageSize).toString());
    }

    @Override
    public String createQueryTableWithLimit(String table, List<DatasetTableField> fields, Integer limit,
                                            boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return String.format("SELECT *  from %s limit %s ", String.format(InfluxdbConstants.KEYWORD_TABLE, table), limit.toString());
    }

    @Override
    public String createQuerySqlWithLimit(String sql, List<DatasetTableField> fields, Integer limit, boolean isGroup,
                                          List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return replaceSql(String.format("SELECT * from %s limit %s ", "(" + sqlFix(sql) + ")", limit.toString()));
    }

    @Override
    public String getSQL(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis,
                         List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                         Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();

        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        List<SQLObj> gFields = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                }
                // 处理横轴字段
                String fieldAlias = originField;
                xFields.add(getXFields(x, originField, fieldAlias));
                gFields.add(getGFields(x, originField, fieldAlias));

                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && Utils.joinSort(x.getSort())) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }

        List<SQLObj> yFields = new ArrayList<>();
        List<String> yWheres = new ArrayList<>();
        List<SQLObj> yOrders = new ArrayList<>();
        List<SQLObj> sortOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(yAxis)) {
            for (int i = 0; i < yAxis.size(); i++) {
                ChartViewFieldDTO y = yAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                // 处理纵轴字段
                yFields.add(getYFields(y, originField, fieldAlias, ds, table));
                // 处理纵轴过滤yOrders
                yWheres.add(getYWheres(y, originField));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && Utils.joinSort(y.getSort())) {
                    // 排序字段处理，time 以外的字段排序使用 top / bottom 函数处理
                    if (StringUtils.equals(originField, "time")) {
                        yOrders.add(SQLObj.builder()
                                .orderField(originField)
                                .orderAlias(fieldAlias)
                                .orderDirection(y.getSort())
                                .build());
                    } else {
                        sortOrders.add(SQLObj.builder()
                                .orderField(originField)
                                .orderAlias(fieldAlias)
                                .orderDirection(y.getSort())
                                .build());
                    }
                }
            }
        }

        // 处理视图中字段过滤
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // 处理仪表板字段过滤
        String extWheres = transExtFilterList(tableObj, extFilterRequestList);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        // 构建sql所有参数
        List<SQLObj> fields = new ArrayList<>();
//        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (extWheres != null)
            wheres.add(extWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(gFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(gFields))
            st_sql.add("groups", gFields);
        if (CollectionUtils.isNotEmpty(fields))
            st_sql.add("aggregators", fields);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(InfluxdbConstants.BRACKETS, sql))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres))
            st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders))
            st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL))
            st.add("table", tableSQL);
        return replaceSql(influxdbSqlLimit(st.render(), view, xFields, yFields, sortOrders));
    }


    private SQLObj createSQLObj(InfluxdbSQLObj influxdbSQLObj) {
        SQLObj sqlObj = SQLObj.builder()
                .tableName(influxdbSQLObj.getTableName())
                .tableAlias(influxdbSQLObj.getTableAlias())
                .fieldName(influxdbSQLObj.getFieldName())
                .fieldAlias(influxdbSQLObj.getFieldAlias())
                .groupField(influxdbSQLObj.getGroupField())
                .groupAlias(influxdbSQLObj.getGroupAlias())
                .orderField(influxdbSQLObj.getOrderField())
                .orderAlias(influxdbSQLObj.getOrderAlias())
                .orderDirection(influxdbSQLObj.getOrderDirection())
                .whereField(influxdbSQLObj.getWhereField())
                .whereAlias(influxdbSQLObj.getWhereAlias())
                .whereTermAndValue(influxdbSQLObj.getWhereTermAndValue())
                .limitFiled(influxdbSQLObj.getLimitFiled())
                .build();

        return sqlObj;
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

    private String readConfigFile(String fileName) {
        InputStream is = null;
        BufferedReader br = null;
        try {
            URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            JarFile jarFile = new JarFile(url.getPath());
            is = jarFile.getInputStream(jarFile.getEntry(fileName));
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
                is = null;
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
                br = null;
            }
        }
    }

    private String originalTableInfo(String table, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> gFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                } else {
//                    if (x.getDeType() == 2 || x.getDeType() == 3) {
//                        originField = String.format(InfluxdbConstants.CAST,
//                                String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName()),InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
//                    } else {
//                        originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
//                    }
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                }
                // 处理横轴字段
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                xFields.add(getXFields(x, originField, fieldAlias));
                gFields.add(getGFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && Utils.joinSort(x.getSort())) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(originField)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }
        // 处理视图中字段过滤
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // 处理仪表板字段过滤
        String extWheres = transExtFilterList(tableObj, extFilterRequestList);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        // 构建sql所有参数
        List<SQLObj> fields = new ArrayList<>();
        fields.addAll(xFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (extWheres != null)
            wheres.add(extWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("previewSql");
        st_sql.add("isGroup", false);
        if (CollectionUtils.isNotEmpty(xFields))
            st_sql.add("aggregators", xFields);
        if (CollectionUtils.isNotEmpty(gFields))
            st_sql.add("groups", gFields);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("previewSql");
        st.add("isGroup", false);
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(InfluxdbConstants.BRACKETS, sql))
                .build();
        if (CollectionUtils.isNotEmpty(orders))
            st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL))
            st.add("table", tableSQL);
        return st.render();
    }

    @Override
    public String getSQLTableInfo(String table, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                } else {
                    if (x.getDeType() == 2 || x.getDeType() == 3) {
                        originField = String.format(InfluxdbConstants.CAST,
                                String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName()), InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                    } else {
                        originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                    }
                }
                // 处理横轴字段
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                xFields.add(getXFields(x, originField, fieldAlias));
            }
        }
        return replaceSql(sqlLimit(originalTableInfo(table, xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view), view, xFields, new ArrayList<>()));
    }

    @Override
    public String getSQLWithPage(boolean isTable, String table, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view, PageInfo pageInfo) {
        String limit = ((pageInfo.getGoPage() != null && pageInfo.getPageSize() != null) ? " LIMIT " + pageInfo.getPageSize() + " OFFSET " + (pageInfo.getGoPage() - 1) * pageInfo.getPageSize() : "");
        if (isTable) {
            return replaceSql(originalTableInfo(table, xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view) + limit);
        } else {
            return replaceSql(originalTableInfo("(" + sqlFix(table) + ")", xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view) + limit);
        }
    }

    @Override
    public String getSQLAsTmpTableInfo(String sql, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        return getSQLTableInfo("(" + sqlFix(sql) + ")", xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, null, view);
    }

    @Override
    public String getSQLAsTmp(String sql, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, ChartViewWithBLOBs view) {
        return getSQL("(" + sqlFix(sql) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, null, view);
    }

    @Override
    public String getSQLStack(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, List<ChartViewFieldDTO> extStack, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> gFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        List<ChartViewFieldDTO> xList = new ArrayList<>();
        xList.addAll(xAxis);
        xList.addAll(extStack);
        if (CollectionUtils.isNotEmpty(xList)) {
            for (int i = 0; i < xList.size(); i++) {
                ChartViewFieldDTO x = xList.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                }
                // 处理横轴字段
                String fieldAlias = originField;
                xFields.add(getXFields(x, originField, fieldAlias));
                gFields.add(getGFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && Utils.joinSort(x.getSort())) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }
        List<SQLObj> yFields = new ArrayList<>();
        List<String> yWheres = new ArrayList<>();
        List<SQLObj> yOrders = new ArrayList<>();
        List<SQLObj> sortOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(yAxis)) {
            for (int i = 0; i < yAxis.size(); i++) {
                ChartViewFieldDTO y = yAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                }

                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_Y_PREFIX, i);
//                String fieldAlias = y.getDataeaseName();
                // 处理纵轴字段
                yFields.add(getYFields(y, originField, fieldAlias, ds, table));
                // 处理纵轴过滤
                yWheres.add(getYWheres(y, originField));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && Utils.joinSort(y.getSort())) {
                    // 排序字段处理，time 以外的字段排序使用 top / bottom 函数处理
                    if (StringUtils.equals(originField, "time")) {
                        yOrders.add(SQLObj.builder()
                                .orderField(originField)
                                .orderAlias(fieldAlias)
                                .orderDirection(y.getSort())
                                .build());
                    } else {
                        sortOrders.add(SQLObj.builder()
                                .orderField(originField)
                                .orderAlias(fieldAlias)
                                .orderDirection(y.getSort())
                                .build());
                    }
                }
            }
        }
        // 处理视图中字段过滤
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // 处理仪表板字段过滤
        String extWheres = transExtFilterList(tableObj, extFilterRequestList);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        // 构建sql所有参数
        List<SQLObj> fields = new ArrayList<>();
//        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (extWheres != null)
            wheres.add(extWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(gFields))
            st_sql.add("groups", gFields);
        if (CollectionUtils.isNotEmpty(yFields))
            st_sql.add("aggregators", fields);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(InfluxdbConstants.BRACKETS, sql))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres))
            st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders))
            st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL))
            st.add("table", tableSQL);
        return replaceSql(influxdbSqlLimit(st.render(), view, xFields, yFields, sortOrders));
    }

    @Override
    public String getSQLAsTmpStack(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis,
                                   List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                                   List<ChartViewFieldDTO> extStack, ChartViewWithBLOBs view) {
        return getSQLStack("(" + sqlFix(table) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, extStack,
                null, view);
    }

    @Override
    public String getSQLScatter(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis,
                                List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                                List<ChartViewFieldDTO> extBubble, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> gFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, x.getOriginName());
                }
                // 处理横轴字段
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                xFields.add(getXFields(x, originField, fieldAlias));
                gFields.add(getGFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && Utils.joinSort(x.getSort())) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }
        List<SQLObj> yFields = new ArrayList<>();
        List<String> yWheres = new ArrayList<>();
        List<SQLObj> yOrders = new ArrayList<>();
        List<ChartViewFieldDTO> yList = new ArrayList<>();
        yList.addAll(yAxis);
        yList.addAll(extBubble);
        if (CollectionUtils.isNotEmpty(yList)) {
            for (int i = 0; i < yList.size(); i++) {
                ChartViewFieldDTO y = yList.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                }

                // 处理纵轴字段
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                yFields.add(getYFields(y, originField, fieldAlias, ds, table));
                // 处理纵轴过滤
                yWheres.add(getYWheres(y, originField));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && Utils.joinSort(y.getSort())) {
                    yOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderDirection(y.getSort())
                            .build());
                }
            }
        }
        // 处理视图中字段过滤
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // 处理仪表板字段过滤
        String extWheres = transExtFilterList(tableObj, extFilterRequestList);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        // 构建sql所有参数
        List<SQLObj> fields = new ArrayList<>();
//        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (extWheres != null)
            wheres.add(extWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(gFields))
            st_sql.add("groups", gFields);
        if (CollectionUtils.isNotEmpty(fields))
            st_sql.add("aggregators", fields);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(InfluxdbConstants.BRACKETS, sql))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres))
            st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders))
            st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL))
            st.add("table", tableSQL);
        return replaceSql(sqlLimit(st.render(), view, xFields, yFields));
    }

    @Override
    public String getSQLAsTmpScatter(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis,
                                     List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                                     List<ChartViewFieldDTO> extBubble, ChartViewWithBLOBs view) {
        return getSQLScatter("(" + sqlFix(table) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList,
                extBubble, null, view);
    }

    @Override
    public String searchTable(String table) {
        return "SELECT table_name FROM information_schema.TABLES WHERE table_name ='" + table + "'";
    }

    @Override
    public String getSQLSummary(String table, List<ChartViewFieldDTO> yAxis,
                                List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                                ChartViewWithBLOBs view, Datasource ds) {
        // 字段汇总 排序等
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table
                        : String.format(InfluxdbConstants.KEYWORD_TABLE, table))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> yFields = new ArrayList<>();
        List<String> yWheres = new ArrayList<>();
        List<SQLObj> yOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(yAxis)) {
            for (int i = 0; i < yAxis.size(); i++) {
                ChartViewFieldDTO y = yAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                } else {
                    originField = String.format(InfluxdbConstants.KEYWORD_FIX, y.getOriginName());
                }

                // 处理纵轴字段
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                yFields.add(getYFields(y, originField, fieldAlias, ds, table));
                // 处理纵轴过滤
                yWheres.add(getYWheres(y, originField));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && Utils.joinSort(y.getSort())) {
                    yOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderDirection(y.getSort())
                            .build());
                }
            }
        }
        // 处理视图中字段过滤
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // 处理仪表板字段过滤
        String extWheres = transExtFilterList(tableObj, extFilterRequestList);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        // 构建sql所有参数
        List<SQLObj> fields = new ArrayList<>();
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null)
            wheres.add(customWheres);
        if (extWheres != null)
            wheres.add(extWheres);
        if (whereTrees != null)
            wheres.add(whereTrees);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(pluginDir + InfluxdbConstants.INFLUXDB_SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(yFields))
            st_sql.add("aggregators", yFields);
        if (CollectionUtils.isNotEmpty(wheres))
            st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj))
            st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(InfluxdbConstants.BRACKETS, sql))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres))
            st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders))
            st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL))
            st.add("table", tableSQL);
        return replaceSql(sqlLimit(st.render(), view, fields, new ArrayList<>()));
    }

    @Override
    public String getSQLSummaryAsTmp(String sql, List<ChartViewFieldDTO> yAxis,
                                     List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList,
                                     ChartViewWithBLOBs view) {
        return getSQLSummary("(" + sqlFix(sql) + ")", yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, view, null);
    }

    @Override
    public String wrapSql(String sql) {
        sql = sql.trim();
        if (sql.lastIndexOf(";") == (sql.length() - 1)) {
            sql = sql.substring(0, sql.length() - 1);
        }
        String tmpSql = "SELECT * FROM (" + sql + ") " + " limit 0";
        return tmpSql;
    }

    @Override
    public String createRawQuerySQL(String table, List<DatasetTableField> fields, Datasource ds) {
        String[] array = fields.stream().map(f -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" \"").append(f.getOriginName()).append("\"");
            return stringBuilder.toString();
        }).toArray(String[]::new);
        InfluxdbConfig dmConfig = new Gson().fromJson(ds.getConfiguration(), InfluxdbConfig.class);
        return MessageFormat.format("SELECT {0} FROM {1}", StringUtils.join(array, ","), table + "\"");
    }

    @Override
    public String createRawQuerySQLAsTmp(String sql, List<DatasetTableField> fields) {
        String[] array = fields.stream().map(f -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" \"").append(f.getOriginName()).append("\"");
            return stringBuilder.toString();
        }).toArray(String[]::new);
        return MessageFormat.format("SELECT {0} FROM {1}", StringUtils.join(array, ","),
                " (" + sqlFix(sql) + ") ");
    }

    @Override
    public String transTreeItem(SQLObj tableObj, DatasetRowPermissionsTreeItem item) {
        String res = null;
        DatasetTableField field = item.getField();
        if (ObjectUtils.isEmpty(field)) {
            return null;
        }
        String whereName = "";
        String originName;
        if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 2) {
            // 解析origin name中有关联的字段生成sql表达式
            originName = calcFieldRegex(field.getOriginName(), tableObj);
        } else if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 1) {
            if (StringUtils.isNotEmpty(tableObj.getTableAlias())) {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
            } else {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
            }

        } else {
            if (StringUtils.isNotEmpty(tableObj.getTableAlias())) {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, tableObj.getTableAlias());
            } else {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
            }
        }

        if (field.getDeType() == 1) {
            if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                whereName = String.format(InfluxdbConstants.TO_DATE, originName, StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT);
            }
            if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                String cast = String.format(InfluxdbConstants.CAST, originName, InfluxdbConstants.DEFAULT_INT_FORMAT)
                        + "/1000";
                whereName = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
            }
            if (field.getDeExtractType() == 1) {
                whereName = originName;
            }
        } else if (field.getDeType() == 2 || field.getDeType() == 3) {
            if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                whereName = String.format(InfluxdbConstants.CAST, originName, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
            }
            if (field.getDeExtractType() == 1) {
                whereName = String.format(InfluxdbConstants.UNIX_TIMESTAMP, originName) + "*1000";
            }
            if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                whereName = originName;
            }
        } else {
            whereName = originName;
        }

        if (StringUtils.equalsIgnoreCase(item.getFilterType(), "enum")) {
            if (CollectionUtils.isNotEmpty(item.getEnumValue())) {
                res = "(" + whereName + " =~/^" + String.join("|", item.getEnumValue()) + "$/)";
            }
        } else {
            String value = item.getValue();
            String whereTerm = transMysqlFilterTerm(item.getTerm());
            String whereValue = "";

            if (StringUtils.equalsIgnoreCase(item.getTerm(), "null")) {
                whereValue = "";
            } else if (StringUtils.equalsIgnoreCase(item.getTerm(), "not_null")) {
                whereValue = "";
            } else if (StringUtils.equalsIgnoreCase(item.getTerm(), "empty")) {
                whereValue = "''";
            } else if (StringUtils.equalsIgnoreCase(item.getTerm(), "not_empty")) {
                whereValue = "''";
            } else if (StringUtils.containsIgnoreCase(item.getTerm(), "in")
                    || StringUtils.containsIgnoreCase(item.getTerm(), "not in")) {
                whereValue = "~/^" + String.join("|", value.split(",")) + "$/";
            } else if (StringUtils.containsIgnoreCase(item.getTerm(), "like")) {
                whereValue = "~ /" + value + "*/";
            } else {
                if (field.getDeType() == 1) {
                    whereValue = String.format(InfluxdbConstants.TO_DATE, "'" + value + "'", InfluxdbConstants.DEFAULT_DATE_FORMAT);
                } else {
                    whereValue = String.format(InfluxdbConstants.WHERE_VALUE_VALUE, value);
                }
            }
            SQLObj build = SQLObj.builder()
                    .whereField(whereName)
                    .whereTermAndValue(whereTerm + whereValue)
                    .build();
            res = build.getWhereField() + " " + build.getWhereTermAndValue();
        }
        return res;
    }

    @Override
    public String convertTableToSql(String tableName, Datasource ds) {
        return createSQLPreview("SELECT * FROM " + String.format(InfluxdbConstants.KEYWORD_TABLE, tableName), null);
    }

    public String transMysqlFilterTerm(String term) {
        switch (term) {
            case "eq":
                return " = ";
            case "not_eq":
                return " <> ";
            case "lt":
                return " < ";
            case "le":
                return " <= ";
            case "gt":
                return " > ";
            case "ge":
                return " >= ";
            case "in":
                return " =";
            case "not in":
                return " !";
            case "like":
                return " =";
            case "not like":
                return " !";
            case "null":
                return " = ";
            case "not_null":
                return " = ";
            case "empty":
                return " = ";
            case "not_empty":
                return " <> ";
            default:
                return "";
        }
    }

    public String transCustomFilterList(SQLObj tableObj, List<ChartFieldCustomFilterDTO> requestList) {
        if (CollectionUtils.isEmpty(requestList)) {
            return null;
        }
        List<String> res = new ArrayList<>();
        for (ChartFieldCustomFilterDTO request : requestList) {
            Boolean numberValueFlag = false;
            List<SQLObj> list = new ArrayList<>();
            DatasetTableField field = request.getField();

            if (ObjectUtils.isEmpty(field)) {
                continue;
            }
            String whereName = "";
            String originName;
            if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 2) {
                // 解析origin name中有关联的字段生成sql表达式
                originName = calcFieldRegex(field.getOriginName(), tableObj);
            } else if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 1) {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
            } else {
                originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
            }

            if (field.getDeType() == 1) {
                if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                    whereName = String.format(InfluxdbConstants.TO_DATE, originName, StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT);
                }
                if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                    String cast = String.format(InfluxdbConstants.CAST, originName, InfluxdbConstants.DEFAULT_INT_FORMAT)
                            + "/1000";
                    whereName = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
                }
                if (field.getDeExtractType() == 1) {
                    whereName = originName;
                }
            } else if (field.getDeType() == 2 || field.getDeType() == 3) {
                if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                    whereName = String.format(InfluxdbConstants.CAST, originName, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                }
                if (field.getDeExtractType() == 1) {
                    whereName = String.format(InfluxdbConstants.UNIX_TIMESTAMP, originName) + "*1000";
                }
                if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                    whereName = originName;
                }
                numberValueFlag = true;
            } else {
                whereName = originName;
            }

            if (StringUtils.equalsIgnoreCase(request.getFilterType(), "enum")) {
                if (CollectionUtils.isNotEmpty(request.getEnumCheckField())) {
                    res.add("(" + whereName + " =~/^" + String.join("|", request.getEnumCheckField()) + "$/)");
                }
            } else {
                List<ChartCustomFilterItemDTO> filter = request.getFilter();
                for (ChartCustomFilterItemDTO filterItemDTO : filter) {
                    String value = filterItemDTO.getValue();
                    String whereTerm = transMysqlFilterTerm(filterItemDTO.getTerm());
                    String whereValue = "";

                    if (StringUtils.equalsIgnoreCase(filterItemDTO.getTerm(), "null")) {
                        whereValue = "";
                    } else if (StringUtils.equalsIgnoreCase(filterItemDTO.getTerm(), "not_null")) {
                        whereValue = "";
                    } else if (StringUtils.equalsIgnoreCase(filterItemDTO.getTerm(), "empty")) {
                        whereValue = "''";
                    } else if (StringUtils.equalsIgnoreCase(filterItemDTO.getTerm(), "not_empty")) {
                        whereValue = "''";
                    } else if (StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "in")
                            || StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "not in")) {
                        whereValue = "~/^" + String.join("|", value.split(",")) + "$/";
                    } else if (StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "like")) {
                        whereValue = "~ /" + value + "*/";
                    } else {
                        if (field.getDeType() == 1) {
                            whereValue = String.format(InfluxdbConstants.TO_DATE, "'" + value + "'", StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT);
                        } else {
                            if (numberValueFlag || StringUtils.equalsIgnoreCase(value, "null")) {
                                whereValue = String.format(InfluxdbConstants.WHERE_NUMBER_VALUE, value);
                            } else {
                                whereValue = String.format(InfluxdbConstants.WHERE_VALUE_VALUE, value);
                            }
                        }
                    }
                    list.add(SQLObj.builder()
                            .whereField(whereName)
                            .whereTermAndValue(whereTerm + whereValue)
                            .build());
                }

                List<String> strList = new ArrayList<>();
                list.forEach(ele -> strList.add(ele.getWhereField() + " " + ele.getWhereTermAndValue()));
                if (CollectionUtils.isNotEmpty(list)) {
                    res.add("(" + String.join(" " + getLogic(request.getLogic()) + " ", strList) + ")");
                }
            }
        }
        return CollectionUtils.isNotEmpty(res) ? "(" + String.join(" AND ", res) + ")" : null;
    }

    public String transExtFilterList(SQLObj tableObj, List<ChartExtFilterRequest> requestList) {
        if (CollectionUtils.isEmpty(requestList)) {
            return null;
        }
        List<SQLObj> list = new ArrayList<>();
        for (ChartExtFilterRequest request : requestList) {
            List<String> value = request.getValue();

            List<String> whereNameList = new ArrayList<>();
            List<DatasetTableField> fieldList = new ArrayList<>();
            if (request.getIsTree()) {
                fieldList.addAll(request.getDatasetTableFieldList());
            } else {
                fieldList.add(request.getDatasetTableField());
            }

            Boolean numberValueFlag = false;
            for (DatasetTableField field : fieldList) {
                if (CollectionUtils.isEmpty(value) || ObjectUtils.isEmpty(field)) {
                    continue;
                }
                String whereName = "";

                String originName;
                if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originName = calcFieldRegex(field.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 1) {
                    originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
                } else {
                    originName = String.format(InfluxdbConstants.KEYWORD_FIX, field.getOriginName());
                }

                if (field.getDeType() == 1) {
                    if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                        whereName = String.format(InfluxdbConstants.TO_DATE, originName, StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT);
                    }
                    if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3
                            || field.getDeExtractType() == 4) {
//                        String cast = String.format(InfluxdbConstants.CAST, originName,InfluxdbConstants.DEFAULT_INT_FORMAT) + "/1000";
//                        whereName = String.format(InfluxdbConstants.FROM_UNIXTIME, cast,InfluxdbConstants.DEFAULT_DATE_FORMAT);
                        whereName = originName;
                    }
                    if (field.getDeExtractType() == 1) {
                        whereName = originName;
                    }
                } else if (field.getDeType() == 2 || field.getDeType() == 3) {
                    if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                        whereName = String.format(InfluxdbConstants.CAST, originName, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                    }
                    if (field.getDeExtractType() == 1) {
//                        whereName = String.format(InfluxdbConstants.UNIX_TIMESTAMP, originName) + "*1000";
                        whereName = originName;
                    }
                    if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3
                            || field.getDeExtractType() == 4) {
                        whereName = originName;
                    }
                    if (field.getDeExtractType() == DeTypeConstants.DE_INT || field.getDeExtractType() == DeTypeConstants.DE_FLOAT) {
                        numberValueFlag = true;
                    }
                } else {
                    whereName = originName;
                }
                whereNameList.add(whereName);
            }

            String whereName = "";
            if (request.getIsTree()) {
                whereName = StringUtils.join(whereNameList, "||','||");
            } else {
                whereName = whereNameList.get(0);
            }
            String whereTerm = transMysqlFilterTerm(request.getOperator());
            String whereValue = "";

            if (StringUtils.containsIgnoreCase(request.getOperator(), "in")) {
                whereValue = "~/^" + StringUtils.join(value, "|") + "$/";
            } else if (StringUtils.containsIgnoreCase(request.getOperator(), "like")) {
                whereValue = "~ /" + value.get(0) + "*/";
            } else if (StringUtils.containsIgnoreCase(request.getOperator(), "between")) {
                if (request.getDatasetTableField().getDeType() == 1) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String startTime = simpleDateFormat.format(new Date(Long.parseLong(value.get(0))));
                    String endTime = simpleDateFormat.format(new Date(Long.parseLong(value.get(1))));

                    String st = String.format(InfluxdbConstants.TO_DATE, "'" + startTime + "'");
                    String et = String.format(InfluxdbConstants.TO_DATE, "'" + endTime + "'");
                    String whereTerm1 = ">";
                    String whereValue1 = st;
                    list.add(SQLObj.builder()
                            .whereField(whereName)
                            .whereTermAndValue(whereTerm1 + whereValue1)
                            .build());

                    whereTerm = "<";
                    whereValue = et;
                } else {
                    String whereTerm1 = ">";
                    String whereValue1 = value.get(0);
                    list.add(SQLObj.builder()
                            .whereField(whereName)
                            .whereTermAndValue(whereTerm1 + whereValue1)
                            .build());

                    whereTerm = "<";
                    whereValue = value.get(1);
                }
            } else {
                if (numberValueFlag || StringUtils.equalsIgnoreCase(value.get(0), "null")) {
                    whereValue = String.format(InfluxdbConstants.WHERE_NUMBER_VALUE, value.get(0));
                } else {
                    whereValue = String.format(InfluxdbConstants.WHERE_VALUE_VALUE, value.get(0));
                }
            }
            list.add(SQLObj.builder()
                    .whereField(whereName)
                    .whereTermAndValue(whereTerm + whereValue)
                    .build());
        }
        List<String> strList = new ArrayList<>();
        list.forEach(ele -> strList.add(ele.getWhereField() + " " + ele.getWhereTermAndValue()));
        return CollectionUtils.isNotEmpty(list) ? "(" + String.join(" AND ", strList) + ")" : null;
    }

    private String sqlFix(String sql) {
        if (sql.lastIndexOf(";") == (sql.length() - 1)) {
            sql = sql.substring(0, sql.length() - 1);
        }
        return sql;
    }

    private String transGroupTime(String dateStyle) {
        if (StringUtils.isEmpty(dateStyle)) {
            return InfluxdbConstants.DEFAULT_GROUP_TIME;
        }

        switch (dateStyle) {
            case "y":
                return "(365d)";
            case "y_Q":
                return "(90d)";
            case "y_M":
                return "(30d)";
            case "y_W":
                return "(7d)";
            case "y_M_d":
                return "(1d)";
            case "H_m_s":
            case "y_M_d_H_m_s":
                return "(1s)";
            case "y_M_d_H_m":
                return "(1m)";
            default:
                return InfluxdbConstants.DEFAULT_GROUP_TIME;
        }
    }

    private String transDateFormat(String dateStyle, String datePattern) {
        String split = "-";
        if (StringUtils.equalsIgnoreCase(datePattern, "date_sub")) {
            split = "-";
        } else if (StringUtils.equalsIgnoreCase(datePattern, "date_split")) {
            split = "/";
        } else {
            split = "-";
        }

        if (StringUtils.isEmpty(dateStyle)) {
            return InfluxdbConstants.DEFAULT_DATE_FORMAT;
        }

        switch (dateStyle) {
            case "y":
                return "YYYY";
            case "y_M":
                return "YYYY" + split + "MM";
            case "y_M_d":
                return "YYYY" + split + "MM" + split + "DD";
            case "H_m_s":
                return "HH24:MI:SS";
            case "y_M_d_H_m":
                return "YYYY" + split + "MM" + split + "DD" + " HH24:MI";
            case "y_M_d_H_m_s":
                return "YYYY" + split + "MM" + split + "DD" + " HH24:MI:SS";
            default:
                return InfluxdbConstants.DEFAULT_DATE_FORMAT;
        }
    }

    private InfluxdbSQLObj getInfluxdbXFields(ChartViewFieldDTO x, String originField, String fieldAlias) {
        String fieldName = "";
        if (x.getDeExtractType() == 1) {
            if (x.getDeType() == 2 || x.getDeType() == 3) {
                // todo: 不支持时间戳
                fieldName = originField;
            } else if (x.getDeType() == 1) {
                // todo：时间字段格式转换
//                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
//                fieldName = String.format(InfluxdbConstants.DATE_FORMAT, originField, format);
                fieldName = originField;
            } else {
                fieldName = originField;
            }
        } else {
            if (x.getDeType() == 1) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                if (x.getDeExtractType() == 0) {
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, String.format(InfluxdbConstants.STR_TO_DATE, originField, StringUtils.isNotEmpty(x.getDateFormat()) ? x.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT), format);
                    fieldName = originField;
                } else {
//                    String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT) + "/1000";
//                    String from_unixtime = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, from_unixtime, format);
                    fieldName = originField;
                }
            } else {
                if (x.getDeType() == DeTypeConstants.DE_INT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                } else if (x.getDeType() == DeTypeConstants.DE_FLOAT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                } else {
                    fieldName = originField;
                }
            }
        }
        return InfluxdbSQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private SQLObj getXFields(ChartViewFieldDTO x, String originField, String fieldAlias) {
        String fieldName = "";
        if (x.getDeExtractType() == 1) {
            if (x.getDeType() == 2 || x.getDeType() == 3) {
                // todo: 不支持时间戳
                fieldName = originField;
            } else if (x.getDeType() == 1) {
                // todo：时间字段格式转换
//                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
//                fieldName = String.format(InfluxdbConstants.DATE_FORMAT, originField, format);
                fieldName = originField;
            } else {
                fieldName = originField;
            }
        } else {
            if (x.getDeType() == 1) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                if (x.getDeExtractType() == 0) {
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, String.format(InfluxdbConstants.STR_TO_DATE, originField, StringUtils.isNotEmpty(x.getDateFormat()) ? x.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT), format);
                    fieldName = originField;
                } else {
//                    String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT) + "/1000";
//                    String from_unixtime = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, from_unixtime, format);
                    fieldName = originField;
                }
            } else {
                if (x.getDeType() == DeTypeConstants.DE_INT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                } else if (x.getDeType() == DeTypeConstants.DE_FLOAT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                } else {
                    fieldName = originField;
                }
            }
        }
        return SQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private SQLObj getGFields(ChartViewFieldDTO x, String originField, String fieldAlias) {
        String fieldName = "";
        if (x.getDeExtractType() == 1) {
            if (x.getDeType() == 2 || x.getDeType() == 3) {
                // todo: 不支持时间戳
                fieldName = originField + "(120m)";
            } else if (x.getDeType() == 1) {
                // todo：时间字段格式转换
                String groupTime = transGroupTime(x.getDateStyle());
                fieldName = String.format(InfluxdbConstants.GROUP_TIME_FORMAT, originField, groupTime);
//                fieldName = originField + "(120m)";
            } else {
//                fieldName = originField + "(120m)";
                fieldName = String.format(InfluxdbConstants.GROUP_TIME_FORMAT, originField, InfluxdbConstants.DEFAULT_GROUP_TIME);
            }
        } else {
            if (x.getDeType() == 1) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                if (x.getDeExtractType() == 0) {
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, String.format(InfluxdbConstants.STR_TO_DATE, originField, StringUtils.isNotEmpty(x.getDateFormat()) ? x.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT), format);
                    fieldName = originField;
                } else {
//                    String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT) + "/1000";
//                    String from_unixtime = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, from_unixtime, format);
                    fieldName = originField;
                }
            } else {
                if (x.getDeType() == DeTypeConstants.DE_INT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                } else if (x.getDeType() == DeTypeConstants.DE_FLOAT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                } else {
                    fieldName = originField;
                }
            }
        }
        return SQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private InfluxdbSQLObj getInfluxdbGFields(ChartViewFieldDTO x, String originField, String fieldAlias) {
        String fieldName = "";
        if (x.getDeExtractType() == 1) {
            if (x.getDeType() == 2 || x.getDeType() == 3) {
                // todo: 不支持时间戳
                fieldName = originField + "(120m)";
            } else if (x.getDeType() == 1) {
                // todo：时间字段格式转换
                String groupTime = transGroupTime(x.getDateStyle());
                fieldName = String.format(InfluxdbConstants.GROUP_TIME_FORMAT, originField, groupTime);
//                fieldName = originField + "(120m)";
            } else {
//                fieldName = originField + "(120m)";
                fieldName = String.format(InfluxdbConstants.GROUP_TIME_FORMAT, originField, InfluxdbConstants.DEFAULT_GROUP_TIME);
            }
        } else {
            if (x.getDeType() == 1) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                if (x.getDeExtractType() == 0) {
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, String.format(InfluxdbConstants.STR_TO_DATE, originField, StringUtils.isNotEmpty(x.getDateFormat()) ? x.getDateFormat() : InfluxdbConstants.DEFAULT_DATE_FORMAT), format);
                    fieldName = originField;
                } else {
//                    String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT) + "/1000";
//                    String from_unixtime = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
//                    fieldName = String.format(InfluxdbConstants.DATE_FORMAT, from_unixtime, format);
                    fieldName = originField;
                }
            } else {
                if (x.getDeType() == DeTypeConstants.DE_INT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT);
                } else if (x.getDeType() == DeTypeConstants.DE_FLOAT) {
                    fieldName = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                } else {
                    fieldName = originField;
                }
            }
        }
        return InfluxdbSQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private List<SQLObj> getXWheres(ChartViewFieldDTO x, String originField, String fieldAlias) {
        List<SQLObj> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(x.getFilter()) && x.getFilter().size() > 0) {
            x.getFilter().forEach(f -> {
                String whereName = "";
                String whereTerm = transMysqlFilterTerm(f.getTerm());
                String whereValue = "";
                if (x.getDeType() == 1 && x.getDeExtractType() != 1) {
                    String cast = String.format(InfluxdbConstants.CAST, originField, InfluxdbConstants.DEFAULT_INT_FORMAT)
                            + "/1000";
                    whereName = String.format(InfluxdbConstants.FROM_UNIXTIME, cast, InfluxdbConstants.DEFAULT_DATE_FORMAT);
                } else {
                    whereName = originField;
                }
                if (StringUtils.equalsIgnoreCase(f.getTerm(), "null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "empty")) {
                    whereValue = "''";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_empty")) {
                    whereValue = "''";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "in")) {
                    whereValue = "~/^" + StringUtils.join(f.getValue(), "|") + "$/";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "like")) {
                    whereValue = "~ /" + f.getValue() + "*/";
                } else {
                    whereValue = String.format(InfluxdbConstants.WHERE_VALUE_VALUE, f.getValue());
                }
                list.add(SQLObj.builder()
                        .whereField(whereName)
                        .whereAlias(fieldAlias)
                        .whereTermAndValue(whereTerm + whereValue)
                        .build());
            });
        }
        return list;
    }

    private SQLObj getYFields(ChartViewFieldDTO y, String originField, String fieldAlias, Datasource ds, String table) {
        String fieldName = "";

        if (StringUtils.equalsIgnoreCase(y.getOriginName(), "*")) {
            fieldName = "COUNT(*)";
            InfluxdbConfig influxdbConfig = new Gson().fromJson(ds.getConfiguration(), InfluxdbConfig.class);
            String showFieldSql = "show field keys from " + table;
            List<QueryResult.Result> fieldResultList = executeSql(influxdbConfig, showFieldSql);
            List<QueryResult.Series> fieldSeriesList = getSeriesListFromResult(fieldResultList);
            if (CollectionUtils.isNotEmpty(fieldSeriesList)) {
                QueryResult.Series series = fieldSeriesList.get(0);
                if (CollectionUtils.isNotEmpty(series.getValues())) {
                    String field = series.getValues().get(0).get(0).toString();
                    fieldName = String.format(InfluxdbConstants.AGG_COUNT, field);
                }
            }
        } else if (SQLConstants.DIMENSION_TYPE.contains(y.getDeType())) {
            if (StringUtils.equalsIgnoreCase(y.getSummary(), "count_distinct")) {
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, "COUNT", "DISTINCT " + originField);
            } else if (StringUtils.equalsIgnoreCase(y.getSummary(), "group_concat")) {
//                fieldName = String.format(InfluxdbConstants.GROUP_CONCAT, originField);
                // todo：不支持 group_concat
                fieldName = originField;
            } else {
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), originField);
            }
        } else {
            if (StringUtils.equalsIgnoreCase(y.getSummary(), "avg")) {
                String cast = String.format(InfluxdbConstants.CAST, originField, y.getDeType() == 2 ? InfluxdbConstants.DEFAULT_INT_FORMAT : InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, InfluxdbConstants.AVG_FUNC, cast);
            } else if (StringUtils.containsIgnoreCase(y.getSummary(), "pop")) {
                String cast = String.format(InfluxdbConstants.CAST, originField, y.getDeType() == 2 ? InfluxdbConstants.DEFAULT_INT_FORMAT : InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), cast);
            } else {
                String cast = String.format(InfluxdbConstants.CAST, originField, y.getDeType() == 2 ? InfluxdbConstants.DEFAULT_INT_FORMAT : InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                if (StringUtils.equalsIgnoreCase(y.getSummary(), "count_distinct")) {
                    fieldName = String.format(InfluxdbConstants.AGG_FIELD, "COUNT", "DISTINCT " + cast);
                } else {
                    fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), cast);
                }
            }
        }
        return SQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private InfluxdbSQLObj getInfluxdbYFields(ChartViewFieldDTO y, String originField, String fieldAlias) {
        String fieldName = "";

        if (StringUtils.equalsIgnoreCase(y.getOriginName(), "*")) {
            fieldName = InfluxdbConstants.AGG_COUNT;
        } else if (SQLConstants.DIMENSION_TYPE.contains(y.getDeType())) {
            if (StringUtils.equalsIgnoreCase(y.getSummary(), "count_distinct")) {
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, "COUNT", "DISTINCT " + originField);
            } else if (StringUtils.equalsIgnoreCase(y.getSummary(), "group_concat")) {
//                fieldName = String.format(InfluxdbConstants.GROUP_CONCAT, originField);
                // todo：不支持 group_concat
                fieldName = originField;
            } else {
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), originField);
            }
        } else {
            if (StringUtils.equalsIgnoreCase(y.getSummary(), "avg") || StringUtils.containsIgnoreCase(y.getSummary(), "pop")) {
                String cast = String.format(InfluxdbConstants.CAST, originField, y.getDeType() == 2 ? InfluxdbConstants.DEFAULT_INT_FORMAT : InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), cast);
            } else {
                String cast = String.format(InfluxdbConstants.CAST, originField, y.getDeType() == 2 ? InfluxdbConstants.DEFAULT_INT_FORMAT : InfluxdbConstants.DEFAULT_FLOAT_FORMAT);
                if (StringUtils.equalsIgnoreCase(y.getSummary(), "count_distinct")) {
                    fieldName = String.format(InfluxdbConstants.AGG_FIELD, "COUNT", "DISTINCT " + cast);
                } else {
                    fieldName = String.format(InfluxdbConstants.AGG_FIELD, y.getSummary(), cast);
                }
            }
        }
        return InfluxdbSQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private String getYWheres(ChartViewFieldDTO y, String originField) {
        List<SQLObj> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(y.getFilter()) && y.getFilter().size() > 0) {
            y.getFilter().forEach(f -> {
                String whereTerm = transMysqlFilterTerm(f.getTerm());
                String whereValue = "";
                // 原始类型不是时间，在de中被转成时间的字段做处理
                if (StringUtils.equalsIgnoreCase(f.getTerm(), "null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "empty")) {
                    whereValue = "''";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_empty")) {
                    whereValue = "''";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "in")) {
                    whereValue = "~/^" + StringUtils.join(f.getValue(), "|") + "$/";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "like")) {
                    whereValue = "~ /" + f.getValue() + "*/";
                } else {
                    whereValue = String.format(InfluxdbConstants.WHERE_VALUE_VALUE, f.getValue());
                }
                list.add(SQLObj.builder()
                        .whereField(originField)
                        .whereTermAndValue(whereTerm + whereValue)
                        .build());
            });
        }
        List<String> strList = new ArrayList<>();
        list.forEach(ele -> strList.add(ele.getWhereField() + " " + ele.getWhereTermAndValue()));
        return CollectionUtils.isNotEmpty(list) ? "(" + String.join(" " + getLogic(y.getLogic()) + " ", strList) + ")"
                : null;
    }

    private String calcFieldRegex(String originField, SQLObj tableObj) {
        originField = originField.replaceAll("[\\t\\n\\r]]", "");
        // 正则提取[xxx]
        String regex = "\\[(.*?)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(originField);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            ids.add(id);
        }
        if (CollectionUtils.isEmpty(ids)) {
            return originField;
        }
        DatasetTableFieldExample datasetTableFieldExample = new DatasetTableFieldExample();
        datasetTableFieldExample.createCriteria().andIdIn(new ArrayList<>(ids));
        List<DatasetTableField> calcFields = datasetTableFieldMapper.selectByExample(datasetTableFieldExample);
        for (DatasetTableField ele : calcFields) {
            originField = originField.replaceAll("\\[" + ele.getId() + "]",
                    String.format(InfluxdbConstants.KEYWORD_FIX, ele.getOriginName()));
        }
        return originField;
    }

    private String sqlLimit(String sql, ChartViewWithBLOBs view, List<SQLObj> xFields, List<SQLObj> yFields) {
        if (StringUtils.equalsIgnoreCase(view.getResultMode(), "custom")) {
            StringBuilder queryFields = new StringBuilder();
            List<SQLObj> fields = new ArrayList<>();
            fields.addAll(xFields);
            fields.addAll(yFields);

            for (int i = 0; i < fields.size(); i++) {
                SQLObj field = fields.get(i);
                if (StringUtils.isNotEmpty(field.getFieldName())) {
                    if (StringUtils.isNotEmpty(field.getFieldAlias())) {
                        queryFields.append(field.getFieldAlias());
                    } else {
                        queryFields.append(field.getFieldName());
                    }
                    if (i != fields.size() - 1) {
                        queryFields.append(",");
                    }
                }
            }
            return MessageFormat.format("SELECT {0} FROM ({1}) {2} LIMIT ", queryFields.toString(), sqlFix(sql), previewOrderBy(sql)) + view.getResultCount();
        } else {
            return sql;
        }
    }

    private String influxdbSqlLimit(String sql, ChartViewWithBLOBs view, List<SQLObj> xFields, List<SQLObj> yFields, List<SQLObj> sortOrders) {
        if (StringUtils.equalsIgnoreCase(view.getResultMode(), "custom")) {
            Integer resultCount = view.getResultCount();
            StringBuilder queryFields = new StringBuilder();
            List<SQLObj> fields = new ArrayList<>();
            fields.addAll(xFields);
            fields.addAll(yFields);

            SQLObj influxdbSQLObj = null;
            if (CollectionUtils.isNotEmpty(sortOrders)) {
                influxdbSQLObj = sortOrders.get(0);
            }
            for (int i = 0; i < fields.size(); i++) {
                SQLObj field = fields.get(i);
                if (StringUtils.isNotEmpty(field.getFieldName())) {
                    if (StringUtils.isNotEmpty(field.getFieldAlias())) {
                        if (influxdbSQLObj != null && StringUtils.equals(influxdbSQLObj.getOrderAlias(), field.getFieldAlias())) {
                            queryFields.append(getDirectionField(influxdbSQLObj.getOrderDirection(), influxdbSQLObj.getOrderAlias(), resultCount));
                        } else {
                            queryFields.append(field.getFieldAlias());
                        }
                    } else {
                        if (influxdbSQLObj != null && StringUtils.equals(influxdbSQLObj.getOrderField(), field.getFieldName())) {
                            queryFields.append(getDirectionField(influxdbSQLObj.getOrderDirection(), influxdbSQLObj.getOrderField(), resultCount));
                        } else {
                            queryFields.append(field.getFieldName());
                        }
                    }
                    if (i != fields.size() - 1) {
                        queryFields.append(",");
                    }
                }
            }
            return MessageFormat.format("SELECT {0} FROM ({1}) {2} LIMIT ", queryFields.toString(), sqlFix(sql), previewOrderBy(sql)) + view.getResultCount();
        } else {
            if (StringUtils.equals(view.getResultMode(), "all")) {
                if (CollectionUtils.isNotEmpty(sortOrders)) {
                    throw new RuntimeException("InfluxDB data source does not support sorting when all results are displayed in the view");
                }
                StringBuilder queryFields = new StringBuilder();
                List<SQLObj> fields = new ArrayList<>();
                fields.addAll(xFields);
                fields.addAll(yFields);

                for (int i = 0; i < fields.size(); i++) {
                    SQLObj field = fields.get(i);
                    if (StringUtils.isNotEmpty(field.getFieldName())) {
                        if (StringUtils.isNotEmpty(field.getFieldAlias())) {
                            queryFields.append(field.getFieldAlias());
                        } else {
                            queryFields.append(field.getFieldName());
                        }
                        if (i != fields.size() - 1) {
                            queryFields.append(",");
                        }
                    }
                }

                return MessageFormat.format("SELECT {0} FROM ({1}) {2}", queryFields.toString(), sqlFix(sql), previewOrderBy(sql));
            }
            return sql;
        }
    }

    private String getDirectionField(String orderDirection, String field, Integer resultCount) {
        String directionField = field;

        if (StringUtils.equals(orderDirection, "asc")) {
            directionField = String.format(InfluxdbConstants.BOTTOM_VALUE, field, resultCount, field);
        } else {
            directionField = String.format(InfluxdbConstants.TOP_VALUE, field, resultCount, field);
        }

        return directionField;
    }

    @Override
    public String getResultCount(boolean isTable, String sql, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        if (isTable) {
            String subSql = getSQLTableInfo(sql, xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view);
            return "SELECT COUNT(*) from (" + subSql + ") " + previewOrderBy(subSql);
        } else {
            return "SELECT COUNT(*) from (" + getSQLAsTmpTableInfo(sql, xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view) + ")";
        }
    }
}
