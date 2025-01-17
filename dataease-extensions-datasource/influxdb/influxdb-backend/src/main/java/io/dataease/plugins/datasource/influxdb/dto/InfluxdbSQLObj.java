package io.dataease.plugins.datasource.influxdb.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfluxdbSQLObj {
    private String sort;
    private int sortCount;
    private String tableName;
    private String tableAlias;

    private String fieldName;
    private String fieldAlias;

    private String groupField;
    private String groupAlias;

    private String orderField;
    private String orderAlias;
    private String orderDirection;

    private String whereField;
    private String whereAlias;
    private String whereTermAndValue;

    private String limitFiled;
}
