package io.dataease.plugins.datasource.dm.service;

import io.dataease.plugins.common.constants.DatabaseClassification;
import io.dataease.plugins.common.constants.DatasourceCalculationMode;
import io.dataease.plugins.common.dto.StaticResource;
import io.dataease.plugins.common.dto.datasource.DataSourceType;
import io.dataease.plugins.datasource.service.DatasourceService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrinoService extends DatasourceService {

    public TrinoService() {
    }

    public List<String> components() {
        List<String> result = new ArrayList();
        result.add("trino");
        return result;
    }

    protected InputStream readContent(String s) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("static/" + s);
        return resourceAsStream;
    }

    public List<StaticResource> staticResources() {
        List<StaticResource> results = new ArrayList();
        StaticResource staticResource = new StaticResource();
        staticResource.setName("trino");
        staticResource.setSuffix("jpg");
        results.add(staticResource);
        results.add(this.pluginSvg());
        return results;
    }

    public DataSourceType getDataSourceType() {
        DataSourceType dataSourceType = new DataSourceType("trino", "Trino", true, "", DatasourceCalculationMode.DIRECT, true);
        dataSourceType.setDatabaseClassification(DatabaseClassification.OLAP);
        return dataSourceType;
    }

    private StaticResource pluginSvg() {
        StaticResource staticResource = new StaticResource();
        staticResource.setName("trino-backend");
        staticResource.setSuffix("svg");
        return staticResource;
    }
}
