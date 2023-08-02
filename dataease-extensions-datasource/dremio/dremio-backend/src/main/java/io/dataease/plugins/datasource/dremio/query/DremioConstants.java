package io.dataease.plugins.datasource.dremio.query;

import io.dataease.plugins.common.constants.datasource.SQLConstants;

public class DremioConstants extends SQLConstants {
    public static final String KEYWORD_TABLE = "%s";
    public static final String KEYWORD_FIX= "%s.\"%s\"";
    public static final String ALIAS_FIX = "%s";
    public static final String UNIX_TIMESTAMP = "UNIX_TIMESTAMP(%s)";
    public static final String DATE_FORMAT = "TO_CHAR(%s,'%s')";
    public static final String DATE_FORMAT_TO_CHAR = "TO_CHAR(%s,'%s')";
    public static final String FROM_UNIXTIME = "FROM_UNIXTIME(%s,'%s')";
//    public static final String STR_TO_DATE = "STR_TO_DATE(%s,'%s')";
    public static final String STR_TO_DATE = "TO_DATE(%s,'%s')";
    public static final String CAST = "CAST(%s AS %s)";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    public static final String DEFAULT_INT_FORMAT = "DECIMAL(20,0)";
    public static final String DEFAULT_FLOAT_FORMAT = "DECIMAL(27,8)";
    public static final String WHERE_VALUE_NULL = "(NULL,'')";
    public static final String WHERE_VALUE_VALUE = "'%s'";
    public static final String AGG_COUNT = "COUNT(*)";
    public static final String AGG_FIELD = "%s(%s)";
    public static final String WHERE_BETWEEN = "'%s' AND '%s'";
    public static final String BRACKETS = "(%s)";
    public static final String NAME = "mysql";
    public static final String GROUP_CONCAT = "group_concat(%s)";
    public static final String QUARTER = "quarter(%s)";

    public DremioConstants() {
    }

    static {
//        String var10000 = DatasourceTypes.mysql.getKeywordPrefix();
//        KEYWORD_TABLE = var10000 + "%s" + DatasourceTypes.mysql.getKeywordSuffix();
//        var10000 = DatasourceTypes.mysql.getKeywordPrefix();
//        KEYWORD_FIX = "%s." + var10000 + "%s" + DatasourceTypes.mysql.getKeywordSuffix();
//        var10000 = DatasourceTypes.mysql.getAliasPrefix();
//        ALIAS_FIX = var10000 + "%s" + DatasourceTypes.mysql.getAliasSuffix();
    }

}
