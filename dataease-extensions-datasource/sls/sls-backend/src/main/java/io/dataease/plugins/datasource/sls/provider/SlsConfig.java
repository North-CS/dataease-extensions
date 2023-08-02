package io.dataease.plugins.datasource.sls.provider;

import io.dataease.plugins.datasource.entity.JdbcConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class SlsConfig extends JdbcConfiguration {

    private String driver = "com.aliyun.odps.jdbc.OdpsDriver";
    private String accessId;
    private String accessKey;
    private String project;
    private String logStore;
    private String topic;
    private String extraParams;
    private String defaultTimeRange;


    public String getJdbc() {
        return "jdbc:odps:END_POINT?project=PROJECT_NAME";
    }
}
