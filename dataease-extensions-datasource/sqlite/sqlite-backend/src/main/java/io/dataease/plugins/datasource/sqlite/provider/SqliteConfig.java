package io.dataease.plugins.datasource.sqlite.provider;

import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import io.dataease.plugins.datasource.provider.ExtendedJdbcClassLoader;
import lombok.Getter;
import lombok.Setter;

import java.sql.*;
import java.util.Properties;

@Getter
@Setter
public class SqliteConfig extends JdbcConfiguration {

    private String driver = "org.sqlite.JDBC";
    private String extraParams;
    private String journalMode;

    public String getJdbc() {
        // jdbc:sqlite:test.db
        return "jdbc:sqlite:DATABASE"
//                .replace("HOST", getHost().trim())
//                .replace("PORT", getPort().toString())
                .replace("DATABASE", getDataBase().trim());
    }
}
