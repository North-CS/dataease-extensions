package io.dataease.plugins.datasource.influxdb.provider;

import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfluxdbConfig extends JdbcConfiguration {

    private String driver = "influxdb.jdbc.driver.InfluxdbDriver";
    private String extraParams;


    public String getJdbc() {
        return "jdbc:influxdb://HOST:PORT/DATABASE"
                .replace("HOST", getHost().trim())
                .replace("PORT", getPort().toString())
                .replace("DATABASE", getDataBase().trim());
    }
}
