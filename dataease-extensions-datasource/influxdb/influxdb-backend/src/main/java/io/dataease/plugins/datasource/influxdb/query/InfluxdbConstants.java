package io.dataease.plugins.datasource.influxdb.query;



import io.dataease.plugins.common.constants.datasource.SQLConstants;

import java.util.HashMap;
import java.util.Map;

import static io.dataease.plugins.common.constants.DatasourceTypes.oracle;

public class InfluxdbConstants extends SQLConstants {

    public static final String KEYWORD_TABLE = "%s";

    public static final String KEYWORD_FIX = "%s";

    public static final String ALIAS_FIX = "%s";

    public static final String UNIX_TIMESTAMP = "UNIX_TIMESTAMP(%s)";

    public static final String DATE_FORMAT = "to_timestamp(%s,'%s')";

    public static final String GROUP_TIME_FORMAT = "%s%s";

    public static final String FROM_UNIXTIME = "FROM_UNIXTIME(%s,'%s')";

    public static final String CAST = "%s::%s";

    public static final String DEFAULT_DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";

    public static final String DEFAULT_GROUP_TIME = "(60m)";

    public static final String DEFAULT_INT_FORMAT = "integer";

    public static final String DEFAULT_STRING_FORMAT = "string";

    public static final String DEFAULT_BOOLEAN_FORMAT = "boolean";

    public static final String DEFAULT_FLOAT_FORMAT = "float";

    public static final String WHERE_VALUE_NULL = "(NULL,'')";

    public static final String WHERE_VALUE_VALUE = "'%s'";

    public static final String WHERE_NUMBER_VALUE = "%s";

    public static final String AGG_COUNT = "COUNT(%s)";

    public static final String AGG_FIELD = "%s(%s)";

    public static final String WHERE_BETWEEN = "'%s' AND '%s'";

    public static final String BRACKETS = "(%s)";

    public static final String TO_NUMBER = "TO_NUMBER(%s)";

    public static final String BOTTOM_VALUE = "bottom(%s, %s) as %s";

    public static final String TOP_VALUE = "top(%s, %s) as %s";

    public static final String TO_DATE = "%s";

    public static final String NAME = "influxdb";

    public static final Map replaceMap = new HashMap<String, String>();

    public static final String AVG_FUNC = "mean";

    static {
        replaceMap.put("\n", " ");
        replaceMap.put("name","\"name\"");
        replaceMap.put("NAME","\"NAME\"");
    }

    public static final String INFLUXDB_SQL_TEMPLATE = "/thirdpart/influxdbDriver/infludbPluginSqltemplate.stg";

}
