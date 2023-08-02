package io.dataease.plugins.datasource.dremio.provider;


import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DremioConfig extends JdbcConfiguration {

    private String driver = "com.dremio.jdbc.Driver";
    private String extraParams;


    public String getJdbc() {
        return "jdbc:dremio:direct=HOST:PORT;schema=DATABASE"
                .replace("HOST", getHost().trim())
                .replace("PORT", getPort().toString())
                .replace("DATABASE", getDataBase().trim());
    }
}
