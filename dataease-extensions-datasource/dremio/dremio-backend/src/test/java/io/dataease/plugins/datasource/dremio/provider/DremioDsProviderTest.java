package io.dataease.plugins.datasource.dremio.provider;

//import com.alibaba.fastjson.JSONObject;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.dto.datasource.TableField;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

public class DremioDsProviderTest extends TestCase {

    public void testGetTables() {

    }

//    public void testGetTableFields() {
//        DremioDsProvider provider = new DremioDsProvider();
//
//        DatasourceRequest datasourceRequest = new DatasourceRequest();
//        datasourceRequest.setTable("article");
//        String datasourceStr = "{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"localhost\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1690290556932,\"id\":\"3a8d1a3b-1b6e-46ae-9a7f-b424e0639f5d\",\"name\":\"test\",\"status\":\"Success\",\"type\":\"dremio\",\"updateTime\":1690342338423}";
//        Datasource datasource = JSONObject.parseObject(datasourceStr, Datasource.class);
//        datasourceRequest.setDatasource(datasource);
//        datasourceRequest.setFetchSize(1000);
//        datasourceRequest.setPageable(false);
//        datasourceRequest.setTotalPageFlag(false);
//        try {
//            List<TableField> fields = provider.getTableFields(datasourceRequest);
//            System.out.println(JSONObject.toJSON(fields));
//        } catch (Exception exception) {
//            System.out.println(exception.getMessage());
//        }
//    }
//
//    public void test(){
//        DremioDsProvider provider = new DremioDsProvider();
//
//        String reqStr = "{\"datasource\":{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"localhost\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1690354363174,\"id\":\"975551ac-30c7-45c2-a7ec-fa5cc74ee3d9\",\"name\":\"test\",\"status\":\"Success\",\"type\":\"dremio\",\"updateTime\":1690354363174},\"fetchSize\":10000,\"pageable\":false,\"previewData\":false,\"query\":\"SELECT * FROM (SELECT * FROM article) DE_TMP  WHERE rownum <= 1000\",\"rEG_WITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\",\"table\":\"article\",\"totalPageFlag\":false,\"wITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\"}";
//        DatasourceRequest datasourceRequest = JSONObject.parseObject(reqStr, DatasourceRequest.class);
//        try {
//            Map<String, List> map = provider.fetchResultAndField(datasourceRequest);
//            System.out.println(JSONObject.toJSON(map));
//        } catch (Exception exception) {
//            System.out.println(exception.getMessage());
//        }
//
//    }
}
