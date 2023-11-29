package io.dataease.plugins.datasource.dm.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.dataease.plugins.common.base.domain.ChartViewWithBLOBs;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.base.domain.DatasetTableFieldExample;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.base.mapper.DatasetTableFieldMapper;
import io.dataease.plugins.common.constants.DeTypeConstants;
import io.dataease.plugins.common.constants.datasource.SQLConstants;
import io.dataease.plugins.common.constants.datasource.SqlServerSQLConstants;
import io.dataease.plugins.common.dto.chart.ChartCustomFilterItemDTO;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.dto.chart.ChartViewFieldDTO;
import io.dataease.plugins.common.dto.datasource.DeSortField;
import io.dataease.plugins.common.dto.sqlObj.SQLObj;
import io.dataease.plugins.common.request.chart.ChartExtFilterRequest;
import io.dataease.plugins.common.request.permission.DataSetRowPermissionsTreeDTO;
import io.dataease.plugins.common.request.permission.DatasetRowPermissionsTreeItem;
import io.dataease.plugins.datasource.dm.provider.TrinoConfig;
import io.dataease.plugins.datasource.entity.Dateformat;
import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import io.dataease.plugins.datasource.entity.PageInfo;
import io.dataease.plugins.datasource.query.QueryProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.dataease.plugins.common.constants.datasource.SQLConstants.TABLE_ALIAS_PREFIX;


@Component()
public class TrinoQueryProvider extends QueryProvider {
    @Resource
    private DatasetTableFieldMapper datasetTableFieldMapper;
    private static final Gson json = new Gson();

    public TrinoQueryProvider() {
    }

    public Integer transFieldType(String field) {
        field = field.contains("(") ? field.split("\\(")[0] : field;
        switch (field.toUpperCase()) {
            case "DATE":
            case "DATETIME":
            case "TIMESTAMP":
            case "TIME":
                return DeTypeConstants.DE_TIME;
            case "TINYINT":
            case "SMALLINT":
            case "INT":
            case "BIGINT":
            case "INTEGER":
                return DeTypeConstants.DE_INT;
            case "FLOAT":
            case "DOUBLE":
            case "DECIMAL":
                return DeTypeConstants.DE_FLOAT;
            case "BOOLEAN":
                return DeTypeConstants.DE_BOOL;
            case "BINARY":
                return DeTypeConstants.DE_BINARY;
            default:
                return DeTypeConstants.DE_STRING;
        }
    }

    public String createSQLPreview(String sql, String orderBy) {
        return "SELECT * FROM (" + this.sqlFix(sql) + ") AS DE_TMP    LIMIT 1000";
    }

    public String createQuerySQL(String table, List<DatasetTableField> fields, boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQL(table, fields, isGroup, ds, fieldCustomFilter, rowPermissionsTree, (List)null);
    }

    @Override
    public String createQuerySQL(String table, List<DatasetTableField> fields, boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<DeSortField> sortFields) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table : String.format(TrinoConstants.KEYWORD_TABLE, table))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 0))
                .build();

        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fields)) {
            for (int i = 0; i < fields.size(); i++) {
                DatasetTableField f = fields.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(f.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 1) {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), f.getOriginName());
                } else {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), f.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                String fieldName = "";
                // 处理横轴字段
                if (f.getDeExtractType() == DeTypeConstants.DE_TIME) {
                    if (f.getDeType() == DeTypeConstants.DE_INT || f.getDeType() == DeTypeConstants.DE_FLOAT) {
                        fieldName = String.format(TrinoConstants.UNIX_TIMESTAMP, originField);
                    } else {
                        fieldName = originField;
                    }
                } else if (f.getDeExtractType() == DeTypeConstants.DE_STRING) {
                    if (f.getDeType() == DeTypeConstants.DE_INT) {
                        fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_INT_FORMAT);
                    } else if (f.getDeType() == DeTypeConstants.DE_FLOAT) {
                        fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_FLOAT_FORMAT);
                    } else if (f.getDeType() == DeTypeConstants.DE_TIME) {
                        fieldName = String.format(TrinoConstants.to_date, originField, StringUtils.isNotEmpty(f.getDateFormat()) ? f.getDateFormat() : TrinoConstants.DEFAULT_DATE_FORMAT);
                    } else {
                        fieldName = originField;
                    }
                } else {
                    if (f.getDeType() == DeTypeConstants.DE_TIME) {
                        fieldName = String.format(TrinoConstants.FORMAT_DATETIME, String.format(TrinoConstants.FROM_UNIXTIME, originField + "/1000"), TrinoConstants.DEFAULT_DATE_FORMAT);
                    } else if (f.getDeType() == DeTypeConstants.DE_INT) {
                        fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_INT_FORMAT);
                    } else {
                        fieldName = originField;
                    }
                }
                xFields.add(SQLObj.builder()
                        .fieldName(fieldName)
                        .fieldAlias(fieldAlias)
                        .build());
            }
        }

        STGroup stg = new STGroupFile(SQLConstants.SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("previewSql");
        st_sql.add("isGroup", isGroup);
        if (CollectionUtils.isNotEmpty(xFields)) st_sql.add("groups", xFields);
        if (ObjectUtils.isNotEmpty(tableObj)) st_sql.add("table", tableObj);
        String customWheres = transCustomFilterList(tableObj, fieldCustomFilter);
        // row permissions tree
        String whereTrees = transFilterTrees(tableObj, rowPermissionsTree);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null) wheres.add(customWheres);
        if (whereTrees != null) wheres.add(whereTrees);
        if (CollectionUtils.isNotEmpty(wheres)) st_sql.add("filters", wheres);

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

        return st_sql.render();
    }


    private SQLObj buildSortField(DeSortField f, SQLObj tableObj, int i) {
        String originField;
        if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 2) {
            // 解析origin name中有关联的字段生成sql表达式
            originField = calcFieldRegex(f.getOriginName(), tableObj);
        } else if (ObjectUtils.isNotEmpty(f.getExtField()) && f.getExtField() == 1) {
            originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), f.getOriginName());
        } else {
            originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), f.getOriginName());
        }
        String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
        String fieldName = "";
        // 处理横轴字段
        if (f.getDeExtractType() == DeTypeConstants.DE_TIME) {
            if (f.getDeType() == DeTypeConstants.DE_INT || f.getDeType() == DeTypeConstants.DE_FLOAT) {
                fieldName = String.format(TrinoConstants.UNIX_TIMESTAMP, originField);
            } else {
                fieldName = originField;
            }
        } else if (f.getDeExtractType() == DeTypeConstants.DE_STRING) {
            if (f.getDeType() == DeTypeConstants.DE_INT) {
                fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_INT_FORMAT);
            } else if (f.getDeType() == DeTypeConstants.DE_FLOAT) {
                fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_FLOAT_FORMAT);
            } else if (f.getDeType() == DeTypeConstants.DE_TIME) {
                fieldName = String.format(TrinoConstants.date_parse, originField, StringUtils.isNotEmpty(f.getDateFormat()) ? f.getDateFormat() : TrinoConstants.DEFAULT_DATE_FORMAT);
            } else {
                fieldName = originField;
            }
        } else {
            if (f.getDeType() == DeTypeConstants.DE_TIME) {
                fieldName = String.format(TrinoConstants.FORMAT_DATETIME, String.format(TrinoConstants.FROM_UNIXTIME, originField + "/1000"), TrinoConstants.DEFAULT_DATE_FORMAT);
            } else if (f.getDeType() == DeTypeConstants.DE_INT) {
                fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_INT_FORMAT);
            } else {
                fieldName = originField;
            }
        }
        SQLObj result = SQLObj.builder().orderField(originField).orderAlias(originField).orderDirection(f.getOrderDirection()).build();
        return result;
    }

    public String createQuerySQLAsTmp(String sql, List<DatasetTableField> fields, boolean isGroup, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<DeSortField> sortFields) {
        return this.createQuerySQL("(" + this.sqlFix(sql) + ")", fields, isGroup, (Datasource)null, fieldCustomFilter, rowPermissionsTree, sortFields);
    }

    public String createQuerySQLAsTmp(String sql, List<DatasetTableField> fields, boolean isGroup, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQL("(" + this.sqlFix(sql) + ")", fields, isGroup, (Datasource)null, fieldCustomFilter, rowPermissionsTree);
    }

    public String createQueryTableWithPage(String table, List<DatasetTableField> fields, Integer page, Integer pageSize, Integer realSize, boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQL(table, fields, isGroup, ds, fieldCustomFilter, rowPermissionsTree) + " LIMIT " + realSize;
    }

    public String createQuerySQLWithPage(String sql, List<DatasetTableField> fields, Integer page, Integer pageSize, Integer realSize, boolean isGroup, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQLAsTmp(sql, fields, isGroup, fieldCustomFilter, rowPermissionsTree) + " LIMIT " + realSize;
    }

    public String createQueryTableWithLimit(String table, List<DatasetTableField> fields, Integer limit, boolean isGroup, Datasource ds, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQL(table, fields, isGroup, ds, fieldCustomFilter, rowPermissionsTree) + " LIMIT " + limit;
    }

    public String createQuerySqlWithLimit(String sql, List<DatasetTableField> fields, Integer limit, boolean isGroup, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree) {
        return this.createQuerySQLAsTmp(sql, fields, isGroup, fieldCustomFilter, rowPermissionsTree) + " LIMIT " + limit;
    }

    @Override
    public String getSQL(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table : String.format(TrinoConstants.KEYWORD_TABLE, table))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 0))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                } else {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                // 处理横轴字段
                xFields.add(getXFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && !StringUtils.equalsIgnoreCase(x.getSort(), "none")) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(fieldAlias)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }
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
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), y.getOriginName());
                } else {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), y.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_Y_PREFIX, i);
                // 处理纵轴字段
                yFields.add(getYFields(y, originField, fieldAlias));
                // 处理纵轴过滤
                yWheres.add(getYWheres(y, originField, fieldAlias));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && !StringUtils.equalsIgnoreCase(y.getSort(), "none")) {
                    yOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(fieldAlias)
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
        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null) wheres.add(customWheres);
        if (extWheres != null) wheres.add(extWheres);
        if (whereTrees != null) wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(SQLConstants.SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(xFields)) st_sql.add("groups", xFields);
        if (CollectionUtils.isNotEmpty(yFields)) st_sql.add("aggregators", yFields);
        if (CollectionUtils.isNotEmpty(wheres)) st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj)) st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(TrinoConstants.BRACKETS, sql))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 1))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres)) st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders)) st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL)) st.add("table", tableSQL);
        return sqlLimit(st.render(), view);
    }

    private String originalTableInfo(String table, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table : String.format(TrinoConstants.KEYWORD_TABLE, table))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 0))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
        List<SQLObj> xOrders = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for (int i = 0; i < xAxis.size(); i++) {
                ChartViewFieldDTO x = xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    // 解析origin name中有关联的字段生成sql表达式
                    originField = calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                } else {
                    if (x.getDeType() == 2 || x.getDeType() == 3) {
                        originField = String.format(TrinoConstants.CAST, String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName()), TrinoConstants.DEFAULT_FLOAT_FORMAT);
                    } else {
                        originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                    }
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                // 处理横轴字段
                xFields.add(getXFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && !StringUtils.equalsIgnoreCase(x.getSort(), "none")) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(fieldAlias)
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
        if (customWheres != null) wheres.add(customWheres);
        if (extWheres != null) wheres.add(extWheres);
        if (whereTrees != null) wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);

        STGroup stg = new STGroupFile(SQLConstants.SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("previewSql");
        st_sql.add("isGroup", false);
        if (CollectionUtils.isNotEmpty(xFields)) st_sql.add("groups", xFields);
        if (CollectionUtils.isNotEmpty(wheres)) st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj)) st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("previewSql");
        st.add("isGroup", false);
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(TrinoConstants.BRACKETS, sql))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 1))
                .build();
        if (CollectionUtils.isNotEmpty(orders)) st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL)) st.add("table", tableSQL);
        return st.render();
    }

    public String getSQLTableInfo(String table, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        return this.sqlLimit(this.originalTableInfo(table, xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view), view);
    }

    public String getSQLAsTmpTableInfo(String sql, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        return this.getSQLTableInfo("(" + this.sqlFix(sql) + ")", xAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, ds, view);
    }

    public String getSQLAsTmp(String sql, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, ChartViewWithBLOBs view) {
        return this.getSQL("(" + this.sqlFix(sql) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, (Datasource)null, view);
    }

    @Override
    public String getSQLStack(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, List<ChartViewFieldDTO> extStack, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? table : String.format(TrinoConstants.KEYWORD_TABLE, table))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 0))
                .build();
        setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList<>();
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
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                } else {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), x.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, i);
                // 处理横轴字段
                xFields.add(getXFields(x, originField, fieldAlias));
                // 处理横轴排序
                if (StringUtils.isNotEmpty(x.getSort()) && !StringUtils.equalsIgnoreCase(x.getSort(), "none")) {
                    xOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(fieldAlias)
                            .orderDirection(x.getSort())
                            .build());
                }
            }
        }
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
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), y.getOriginName());
                } else {
                    originField = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), y.getOriginName());
                }
                String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_Y_PREFIX, i);
                // 处理纵轴字段
                yFields.add(getYFields(y, originField, fieldAlias));
                // 处理纵轴过滤
                yWheres.add(getYWheres(y, originField, fieldAlias));
                // 处理纵轴排序
                if (StringUtils.isNotEmpty(y.getSort()) && !StringUtils.equalsIgnoreCase(y.getSort(), "none")) {
                    yOrders.add(SQLObj.builder()
                            .orderField(originField)
                            .orderAlias(fieldAlias)
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
        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList<>();
        if (customWheres != null) wheres.add(customWheres);
        if (extWheres != null) wheres.add(extWheres);
        if (whereTrees != null) wheres.add(whereTrees);
        List<SQLObj> groups = new ArrayList<>();
        groups.addAll(xFields);
        // 外层再次套sql
        List<SQLObj> orders = new ArrayList<>();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList<>();
        aggWheres.addAll(yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));

        STGroup stg = new STGroupFile(SQLConstants.SQL_TEMPLATE);
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(xFields)) st_sql.add("groups", xFields);
        if (CollectionUtils.isNotEmpty(yFields)) st_sql.add("aggregators", yFields);
        if (CollectionUtils.isNotEmpty(wheres)) st_sql.add("filters", wheres);
        if (ObjectUtils.isNotEmpty(tableObj)) st_sql.add("table", tableObj);
        String sql = st_sql.render();

        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder()
                .tableName(String.format(TrinoConstants.BRACKETS, sql))
                .tableAlias(String.format(TABLE_ALIAS_PREFIX, 1))
                .build();
        if (CollectionUtils.isNotEmpty(aggWheres)) st.add("filters", aggWheres);
        if (CollectionUtils.isNotEmpty(orders)) st.add("orders", orders);
        if (ObjectUtils.isNotEmpty(tableSQL)) st.add("table", tableSQL);
        return sqlLimit(st.render(), view);
    }

    public String getSQLAsTmpStack(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, List<ChartViewFieldDTO> extStack, ChartViewWithBLOBs view) {
        return this.getSQLStack("(" + this.sqlFix(table) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, extStack, (Datasource)null, view);
    }

    public String getSQLScatter(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, List<ChartViewFieldDTO> extBubble, Datasource ds, ChartViewWithBLOBs view) {
        SQLObj tableObj = SQLObj.builder().tableName(table.startsWith("(") && table.endsWith(")") ? table : String.format("%s", table)).tableAlias(String.format("t_a_%s", 0)).build();
        this.setSchema(tableObj, ds);
        List<SQLObj> xFields = new ArrayList();
        List<SQLObj> xOrders = new ArrayList();
        if (CollectionUtils.isNotEmpty(xAxis)) {
            for(int i = 0; i < xAxis.size(); ++i) {
                ChartViewFieldDTO x = (ChartViewFieldDTO)xAxis.get(i);
                String originField;
                if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 2) {
                    originField = this.calcFieldRegex(x.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(x.getExtField()) && x.getExtField() == 1) {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), x.getOriginName());
                } else {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), x.getOriginName());
                }

                String fieldAlias = String.format("f_ax_%s", i);
                xFields.add(this.getXFields(x, originField, fieldAlias));
                if (StringUtils.isNotEmpty(x.getSort()) && !StringUtils.equalsIgnoreCase(x.getSort(), "none")) {
                    xOrders.add(SQLObj.builder().orderField(originField).orderAlias(fieldAlias).orderDirection(x.getSort()).build());
                }
            }
        }

        List<SQLObj> yFields = new ArrayList();
        List<String> yWheres = new ArrayList();
        List<SQLObj> yOrders = new ArrayList();
        List<ChartViewFieldDTO> yList = new ArrayList();
        yList.addAll(yAxis);
        yList.addAll(extBubble);
        String originField;
        if (CollectionUtils.isNotEmpty(yList)) {
            for(int i = 0; i < yList.size(); ++i) {
                ChartViewFieldDTO y = (ChartViewFieldDTO)yList.get(i);
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    originField = this.calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), y.getOriginName());
                } else {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), y.getOriginName());
                }

                String fieldAlias = String.format("f_ay_%s", i);
                yFields.add(this.getYFields(y, originField, fieldAlias));
                yWheres.add(this.getYWheres(y, originField, fieldAlias));
                if (StringUtils.isNotEmpty(y.getSort()) && !StringUtils.equalsIgnoreCase(y.getSort(), "none")) {
                    yOrders.add(SQLObj.builder().orderField(originField).orderAlias(fieldAlias).orderDirection(y.getSort()).build());
                }
            }
        }

        String customWheres = this.transCustomFilterList(tableObj, fieldCustomFilter);
        String extWheres = this.transExtFilterList(tableObj, extFilterRequestList);
        originField = this.transFilterTrees(tableObj, rowPermissionsTree);
        List<SQLObj> fields = new ArrayList();
        fields.addAll(xFields);
        fields.addAll(yFields);
        List<String> wheres = new ArrayList();
        if (customWheres != null) {
            wheres.add(customWheres);
        }

        if (extWheres != null) {
            wheres.add(extWheres);
        }

        if (originField != null) {
            wheres.add(originField);
        }

        List<SQLObj> groups = new ArrayList();
        groups.addAll(xFields);
        List<SQLObj> orders = new ArrayList();
        orders.addAll(xOrders);
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList();
        aggWheres.addAll((Collection)yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));
        STGroup stg = new STGroupFile("pluginSqltemplate.stg");
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(xFields)) {
            st_sql.add("groups", xFields);
        }

        if (CollectionUtils.isNotEmpty(yFields)) {
            st_sql.add("aggregators", yFields);
        }

        if (CollectionUtils.isNotEmpty(wheres)) {
            st_sql.add("filters", wheres);
        }

        if (ObjectUtils.isNotEmpty(tableObj)) {
            st_sql.add("table", tableObj);
        }

        String sql = st_sql.render();
        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder().tableName(String.format("(%s)", sql)).tableAlias(String.format("t_a_%s", 1)).build();
        if (CollectionUtils.isNotEmpty(aggWheres)) {
            st.add("filters", aggWheres);
        }

        if (CollectionUtils.isNotEmpty(orders)) {
            st.add("orders", orders);
        }

        if (ObjectUtils.isNotEmpty(tableSQL)) {
            st.add("table", tableSQL);
        }

        return this.sqlLimit(st.render(), view);
    }

    public String getSQLAsTmpScatter(String table, List<ChartViewFieldDTO> xAxis, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, List<ChartViewFieldDTO> extBubble, ChartViewWithBLOBs view) {
        return this.getSQLScatter("(" + this.sqlFix(table) + ")", xAxis, yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, extBubble, (Datasource)null, view);
    }

    public String searchTable(String table) {
        return "SELECT table_name FROM information_schema.TABLES WHERE table_name ='" + table + "'";
    }

    public String getSQLSummary(String table, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, ChartViewWithBLOBs view, Datasource ds) {
        SQLObj tableObj = SQLObj.builder().tableName(table.startsWith("(") && table.endsWith(")") ? table : String.format("%s", table)).tableAlias(String.format("t_a_%s", 0)).build();
        this.setSchema(tableObj, ds);
        List<SQLObj> yFields = new ArrayList();
        List<String> yWheres = new ArrayList();
        List<SQLObj> yOrders = new ArrayList();
        String originField;
        if (CollectionUtils.isNotEmpty(yAxis)) {
            for(int i = 0; i < yAxis.size(); ++i) {
                ChartViewFieldDTO y = (ChartViewFieldDTO)yAxis.get(i);
                if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 2) {
                    originField = this.calcFieldRegex(y.getOriginName(), tableObj);
                } else if (ObjectUtils.isNotEmpty(y.getExtField()) && y.getExtField() == 1) {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), y.getOriginName());
                } else {
                    originField = String.format("%s.%s", tableObj.getTableAlias(), y.getOriginName());
                }

                String fieldAlias = String.format("f_ay_%s", i);
                yFields.add(this.getYFields(y, originField, fieldAlias));
                yWheres.add(this.getYWheres(y, originField, fieldAlias));
                if (StringUtils.isNotEmpty(y.getSort()) && !StringUtils.equalsIgnoreCase(y.getSort(), "none")) {
                    yOrders.add(SQLObj.builder().orderField(originField).orderAlias(fieldAlias).orderDirection(y.getSort()).build());
                }
            }
        }

        String customWheres = this.transCustomFilterList(tableObj, fieldCustomFilter);
        String extWheres = this.transExtFilterList(tableObj, extFilterRequestList);
        originField = this.transFilterTrees(tableObj, rowPermissionsTree);
        List<SQLObj> fields = new ArrayList();
        fields.addAll(yFields);
        List<String> wheres = new ArrayList();
        if (customWheres != null) {
            wheres.add(customWheres);
        }

        if (extWheres != null) {
            wheres.add(extWheres);
        }

        if (originField != null) {
            wheres.add(originField);
        }

        new ArrayList();
        List<SQLObj> orders = new ArrayList();
        orders.addAll(yOrders);
        List<String> aggWheres = new ArrayList();
        aggWheres.addAll((Collection)yWheres.stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList()));
        STGroup stg = new STGroupFile("pluginSqltemplate.stg");
        ST st_sql = stg.getInstanceOf("querySql");
        if (CollectionUtils.isNotEmpty(yFields)) {
            st_sql.add("aggregators", yFields);
        }

        if (CollectionUtils.isNotEmpty(wheres)) {
            st_sql.add("filters", wheres);
        }

        if (ObjectUtils.isNotEmpty(tableObj)) {
            st_sql.add("table", tableObj);
        }

        String sql = st_sql.render();
        ST st = stg.getInstanceOf("querySql");
        SQLObj tableSQL = SQLObj.builder().tableName(String.format("(%s)", sql)).tableAlias(String.format("t_a_%s", 1)).build();
        if (CollectionUtils.isNotEmpty(aggWheres)) {
            st.add("filters", aggWheres);
        }

        if (CollectionUtils.isNotEmpty(orders)) {
            st.add("orders", orders);
        }

        if (ObjectUtils.isNotEmpty(tableSQL)) {
            st.add("table", tableSQL);
        }

        return this.sqlLimit(st.render(), view);
    }

    public String getSQLSummaryAsTmp(String sql, List<ChartViewFieldDTO> yAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, ChartViewWithBLOBs view) {
        return this.getSQLSummary("(" + this.sqlFix(sql) + ")", yAxis, fieldCustomFilter, rowPermissionsTree, extFilterRequestList, view, (Datasource)null);
    }

    public String wrapSql(String sql) {
        sql = sql.trim();
        if (sql.lastIndexOf(";") == sql.length() - 1) {
            sql = sql.substring(0, sql.length() - 1);
        }

        String tmpSql = "SELECT * FROM (" + sql + ") AS tmp  LIMIT 0 ";
        return tmpSql;
    }

    public String createRawQuerySQL(String table, List<DatasetTableField> fields, Datasource ds) {
        String[] array = (String[])fields.stream().map((f) -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"").append(f.getOriginName()).append("\" AS ").append(f.getDataeaseName());
            return stringBuilder.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        });
        if (ds != null) {
            String schema = ((JdbcConfiguration)(new Gson()).fromJson(ds.getConfiguration(), JdbcConfiguration.class)).getSchema();
            String tableWithSchema = String.format(SqlServerSQLConstants.KEYWORD_TABLE, schema) + "." + String.format(SqlServerSQLConstants.KEYWORD_TABLE, table);
            return MessageFormat.format("SELECT {0} FROM {1}  ", StringUtils.join(array, ","), tableWithSchema);
        } else {
            return MessageFormat.format("SELECT {0} FROM {1}  ", StringUtils.join(array, ","), table);
        }
    }

    public String createRawQuerySQLAsTmp(String sql, List<DatasetTableField> fields) {
        return this.createRawQuerySQL(" (" + this.sqlFix(sql) + ") AS tmp ", fields, (Datasource)null);
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
            originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
        } else {
            originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
        }
        if (field.getDeType() == 1) {
            if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                whereName = String.format(TrinoConstants.CAST, originName, "timestamp");
            }
            if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                String cast = String.format(TrinoConstants.CAST, originName, "bigint");
                whereName = String.format(TrinoConstants.FROM_UNIXTIME, cast);
            }
            if (field.getDeExtractType() == 1) {
                whereName = originName;
            }
        } else if (field.getDeType() == 2 || field.getDeType() == 3) {
            if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                whereName = String.format(TrinoConstants.CAST, originName, TrinoConstants.DEFAULT_FLOAT_FORMAT);
            }
            if (field.getDeExtractType() == 1) {
                whereName = String.format(TrinoConstants.UNIX_TIMESTAMP, originName);
            }
            if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                whereName = originName;
            }
        } else {
            whereName = originName;
        }

        if (StringUtils.equalsIgnoreCase(item.getFilterType(), "enum")) {
            if (CollectionUtils.isNotEmpty(item.getEnumValue())) {
                res = "(" + whereName + " IN ('" + String.join("','", item.getEnumValue()) + "'))";
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
            } else if (StringUtils.containsIgnoreCase(item.getTerm(), "in") || StringUtils.containsIgnoreCase(item.getTerm(), "not in")) {
                whereValue = "('" + String.join("','", value.split(",")) + "')";
            } else if (StringUtils.containsIgnoreCase(item.getTerm(), "like")) {
                whereValue = "'%" + value + "%'";
            } else {
                whereValue = String.format(TrinoConstants.WHERE_VALUE_VALUE, value);
            }
            SQLObj build = SQLObj.builder()
                    .whereField(whereName)
                    .whereTermAndValue(whereTerm + whereValue)
                    .build();
            res = build.getWhereField() + " " + build.getWhereTermAndValue();
        }
        return res;
    }

    public String convertTableToSql(String tableName, Datasource ds) {
        String schema = ((JdbcConfiguration)(new Gson()).fromJson(ds.getConfiguration(), JdbcConfiguration.class)).getSchema();
        schema = String.format("%s", schema);
        return this.createSQLPreview("SELECT * FROM " + schema + "." + String.format("%s", tableName), (String)null);
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
                return " IN ";
            case "not in":
                return " NOT IN ";
            case "like":
                return " LIKE ";
            case "not like":
                return " NOT LIKE ";
            case "null":
                return " IS NULL ";
            case "not_null":
                return " IS NOT NULL ";
            case "empty":
                return " = ";
            case "not_empty":
                return " <> ";
            case "between":
                return " BETWEEN ";
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
                originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
            } else {
                originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
            }
            if (field.getDeType() == 1) {
                if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                    whereName = String.format(TrinoConstants.date_parse, originName, StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : TrinoConstants.DEFAULT_DATE_FORMAT);
                }
                if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                    String cast = String.format(TrinoConstants.CAST, originName, "bigint");
                    whereName = String.format(TrinoConstants.FROM_UNIXTIME, cast);
                }
                if (field.getDeExtractType() == 1) {
                    whereName = originName;
                }
            } else if (field.getDeType() == 2 || field.getDeType() == 3) {
                if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                    whereName = String.format(TrinoConstants.CAST, originName, TrinoConstants.DEFAULT_FLOAT_FORMAT);
                }
                if (field.getDeExtractType() == 1) {
                    whereName = String.format(TrinoConstants.UNIX_TIMESTAMP, originName);
                }
                if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                    whereName = originName;
                }
            } else {
                whereName = originName;
            }

            if (StringUtils.equalsIgnoreCase(request.getFilterType(), "enum")) {
                if (CollectionUtils.isNotEmpty(request.getEnumCheckField())) {
                    res.add("(" + whereName + " IN ('" + String.join("','", request.getEnumCheckField()) + "'))");
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
                    } else if (StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "in") || StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "not in")) {
                        whereValue = "('" + String.join("','", value.split(",")) + "')";
                    } else if (StringUtils.containsIgnoreCase(filterItemDTO.getTerm(), "like")) {
                        whereValue = "'%" + value + "%'";
                    } else {
                        whereValue = String.format(TrinoConstants.WHERE_VALUE_VALUE, value);
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
                    originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
                } else {
                    originName = String.format(TrinoConstants.KEYWORD_FIX, tableObj.getTableAlias(), field.getOriginName());
                }

                if (field.getDeType() == 1) {
                    if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                        whereName = String.format(TrinoConstants.date_parse, originName, StringUtils.isNotEmpty(field.getDateFormat()) ? field.getDateFormat() : TrinoConstants.DEFAULT_DATE_FORMAT);
                    }
                    if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                        String cast = String.format(TrinoConstants.CAST, originName, "bigint");
                        whereName = String.format(TrinoConstants.FROM_UNIXTIME, cast);
                    }
                    if (field.getDeExtractType() == 1) {
                        whereName = originName;
                    }
                } else if (field.getDeType() == 2 || field.getDeType() == 3) {
                    if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                        whereName = String.format(TrinoConstants.CAST, originName, TrinoConstants.DEFAULT_FLOAT_FORMAT);
                    }
                    if (field.getDeExtractType() == 1) {
                        whereName = String.format(TrinoConstants.UNIX_TIMESTAMP, originName);
                    }
                    if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
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
                whereName = "CONCAT(" + StringUtils.join(whereNameList, ",',',") + ")";
            } else {
                whereName = whereNameList.get(0);
            }
            String whereTerm = transMysqlFilterTerm(request.getOperator());
            String whereValue = "";

            if (StringUtils.containsIgnoreCase(request.getOperator(), "in")) {
                if (numberValueFlag || StringUtils.equalsIgnoreCase(value.get(0), "null")) {
                    whereValue = "(" + StringUtils.join(value, ",") + ")";
                } else {
                    whereValue = "('" + StringUtils.join(value, "','") + "')";
                }
            } else if (StringUtils.containsIgnoreCase(request.getOperator(), "like")) {
                String keyword = value.get(0).toUpperCase();
                whereValue = "'%" + keyword + "%'";
                whereName = "upper(" + whereName + ")";
            } else if (StringUtils.containsIgnoreCase(request.getOperator(), "between")) {
                if (request.getDatasetTableField().getDeType() == 1) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String startTime = simpleDateFormat.format(new Date(Long.parseLong(value.get(0))));
                    String endTime = simpleDateFormat.format(new Date(Long.parseLong(value.get(1))));
                    whereValue = String.format(TrinoConstants.WHERE_TIME_BETWEEN, startTime, endTime);
                } else {
                    whereValue = String.format(TrinoConstants.WHERE_BETWEEN, value.get(0), value.get(1));
                }
            } else {
                if (numberValueFlag || StringUtils.equalsIgnoreCase(value.get(0), "null")) {
                    whereValue = String.format(TrinoConstants.WHERE_NUMBER_VALUE, value.get(0));
                } else {
                    whereValue = String.format(TrinoConstants.WHERE_VALUE_VALUE, value.get(0));
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
        if (sql.lastIndexOf(";") == sql.length() - 1) {
            sql = sql.substring(0, sql.length() - 1);
        }

        return sql;
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
            return "yyyy-MM-dd HH:mm:ss";
        } else {
            switch (dateStyle) {
                case "y":
                    return "yyyy";
                case "y_M":
                    return "yyyy" + split + "MM";
                case "y_M_d":
                    return "yyyy" + split + "MM" + split + "dd";
                case "H_m_s":
                    return "hh:mi:ss";
                case "y_M_d_H_m":
                    return "yyyy" + split + "MM" + split + "dd HH:mm";
                case "y_M_d_H_m_s":
                    return "yyyy" + split + "MM" + split + "dd HH:mm:ss";
                default:
                    return "yyyy-MM-dd HH:mm:s";
            }
        }
    }


    private SQLObj getXFields(ChartViewFieldDTO x, String originField, String fieldAlias) {
        String fieldName = "";
        if (x.getDeExtractType() == DeTypeConstants.DE_TIME) {
            if (x.getDeType() == 2 || x.getDeType() == 3) {
                fieldName = String.format(TrinoConstants.UNIX_TIMESTAMP, originField);
            } else if (x.getDeType() == DeTypeConstants.DE_TIME) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                fieldName = String.format(TrinoConstants.FORMAT_DATETIME, originField, format);
            } else {
                fieldName = originField;
            }
        } else {
            if (x.getDeType() == DeTypeConstants.DE_TIME) {
                String format = transDateFormat(x.getDateStyle(), x.getDatePattern());
                if (x.getDeExtractType() == DeTypeConstants.DE_STRING) {
                    fieldName = String.format(TrinoConstants.FORMAT_DATETIME, String.format(TrinoConstants.date_parse, originField, StringUtils.isNotEmpty(x.getDateFormat()) ? x.getDateFormat() : TrinoConstants.DEFAULT_DATE_FORMAT), format);
                } else {
                    String from_unixtime = String.format(TrinoConstants.FROM_UNIXTIME, originField + "/1000");
                    fieldName = String.format(TrinoConstants.FORMAT_DATETIME, from_unixtime, format);
                }
            } else {
                if (x.getDeType() == DeTypeConstants.DE_INT) {
                    fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_INT_FORMAT);
                } else if (x.getDeType() == DeTypeConstants.DE_FLOAT) {
                    fieldName = String.format(TrinoConstants.CAST, originField, TrinoConstants.DEFAULT_FLOAT_FORMAT);
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

    private SQLObj getYFields(ChartViewFieldDTO y, String originField, String fieldAlias) {
        String fieldName = "";
        if (StringUtils.equalsIgnoreCase(y.getOriginName(), "*")) {
            fieldName = TrinoConstants.AGG_COUNT;
        } else if (SQLConstants.DIMENSION_TYPE.contains(y.getDeType())) {
            fieldName = String.format(TrinoConstants.AGG_FIELD, y.getSummary(), originField);
        } else {
            if (StringUtils.equalsIgnoreCase(y.getSummary(), "avg") || StringUtils.containsIgnoreCase(y.getSummary(), "pop")) {
                String cast = String.format(TrinoConstants.CAST, originField, y.getDeType() == DeTypeConstants.DE_INT ? TrinoConstants.DEFAULT_INT_FORMAT : TrinoConstants.DEFAULT_FLOAT_FORMAT);
                String agg = String.format(TrinoConstants.AGG_FIELD, y.getSummary(), cast);
                fieldName = String.format(TrinoConstants.CAST, agg, TrinoConstants.DEFAULT_FLOAT_FORMAT);
            } else {
                String cast = String.format(TrinoConstants.CAST, originField, y.getDeType() == DeTypeConstants.DE_INT ? TrinoConstants.DEFAULT_INT_FORMAT : TrinoConstants.DEFAULT_FLOAT_FORMAT);
                fieldName = String.format(TrinoConstants.AGG_FIELD, y.getSummary(), cast);
            }
        }
        return SQLObj.builder()
                .fieldName(fieldName)
                .fieldAlias(fieldAlias)
                .build();
    }

    private String getYWheres(ChartViewFieldDTO y, String originField, String fieldAlias) {
        List<SQLObj> list = new ArrayList();
        if (CollectionUtils.isNotEmpty(y.getFilter()) && y.getFilter().size() > 0) {
            y.getFilter().forEach((f) -> {
                String whereTerm = this.transMysqlFilterTerm(f.getTerm());
                String whereValue = "";
                if (StringUtils.equalsIgnoreCase(f.getTerm(), "null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_null")) {
                    whereValue = "";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "empty")) {
                    whereValue = "''";
                } else if (StringUtils.equalsIgnoreCase(f.getTerm(), "not_empty")) {
                    whereValue = "''";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "in")) {
                    whereValue = "('" + StringUtils.join(new String[]{f.getValue(), "','"}) + "')";
                } else if (StringUtils.containsIgnoreCase(f.getTerm(), "like")) {
                    whereValue = "'%" + f.getValue() + "%'";
                } else {
                    whereValue = String.format("'%s'", f.getValue());
                }

                list.add(SQLObj.builder().whereField(fieldAlias).whereAlias(fieldAlias).whereTermAndValue(whereTerm + whereValue).build());
            });
        }

        List<String> strList = new ArrayList();
        list.forEach((ele) -> {
            strList.add(ele.getWhereField() + " " + ele.getWhereTermAndValue());
        });
        return CollectionUtils.isNotEmpty(list) ? "(" + String.join(" " + this.getLogic(y.getLogic()) + " ", strList) + ")" : null;
    }

    private String calcFieldRegex(String originField, SQLObj tableObj) {
        originField = originField.replaceAll("[\\t\\n\\r]]", "");
        String regex = "\\[(.*?)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(originField);
        Set<String> ids = new HashSet();

        while(matcher.find()) {
            String id = matcher.group(1);
            ids.add(id);
        }

        if (CollectionUtils.isEmpty(ids)) {
            return originField;
        } else {
            DatasetTableFieldExample datasetTableFieldExample = new DatasetTableFieldExample();
            datasetTableFieldExample.createCriteria().andIdIn(new ArrayList(ids));
            List<DatasetTableField> calcFields = this.datasetTableFieldMapper.selectByExample(datasetTableFieldExample);

            DatasetTableField ele;
            for(Iterator var9 = calcFields.iterator(); var9.hasNext(); originField = originField.replaceAll("\\[" + ele.getId() + "]", String.format("%s.%s", tableObj.getTableAlias(), ele.getOriginName()))) {
                ele = (DatasetTableField)var9.next();
            }

            return originField;
        }
    }

    private String sqlLimit(String sql, ChartViewWithBLOBs view) {
        return StringUtils.equalsIgnoreCase(view.getResultMode(), "custom") ? sql + " LIMIT " + view.getResultCount() : sql;
    }

    public void setSchema(SQLObj tableObj, Datasource ds) {
        if (ds != null && !tableObj.getTableName().startsWith("(") && !tableObj.getTableName().endsWith(")")) {
            TrinoConfig trinoConfig = (TrinoConfig)json.fromJson(ds.getConfiguration(), TrinoConfig.class);
            String schema = trinoConfig.getSchema();
            String database = trinoConfig.getDataBase();
            tableObj.setTableName(database + "." + schema + "." + tableObj.getTableName());
        }

    }

    public List<Dateformat> dateformat() {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Dateformat> dateformats = new ArrayList();

//        try {
//            dateformats = (List)objectMapper.readValue("[\n{\"dateformat\": \"yyyy-MM-dd\"},\n{\"dateformat\": \"yyyy/MM/dd\"},\n{\"dateformat\": \"YyyyyMMdd\"},\n{\"dateformat\": \"yyyy-MM-dd HH:mm:s\"},\n{\"dateformat\": \"yyyy-MM-dd HH:mm:s\"},\n{\"dateformat\": \"yyyy-MM-dd HH:mm:s\"}\n]", new TypeReference<List<Dateformat>>() {
//            });
//        } catch (Exception var4) {
//        }

        return (List)dateformats;
    }

    public String getResultCount(boolean isTable, String sql, List<ChartViewFieldDTO> xAxis, List<ChartFieldCustomFilterDTO> fieldCustomFilter, List<DataSetRowPermissionsTreeDTO> rowPermissionsTree, List<ChartExtFilterRequest> extFilterRequestList, Datasource ds, ChartViewWithBLOBs view) {
        return null;
    }
}
