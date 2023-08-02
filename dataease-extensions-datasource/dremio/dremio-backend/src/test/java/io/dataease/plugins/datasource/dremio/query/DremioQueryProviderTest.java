package io.dataease.plugins.datasource.dremio.query;

//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
import io.dataease.plugins.common.base.domain.DatasetTableField;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.dto.chart.ChartFieldCustomFilterDTO;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import io.dataease.plugins.common.request.permission.DataSetRowPermissionsTreeDTO;
import io.dataease.plugins.datasource.dremio.provider.DremioDsProvider;
import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DremioQueryProviderTest extends TestCase {

    public void testTransFieldType() {
        List<Datasource> items = null;
        items.add(null);
        Datasource item1 = new Datasource();
//        item1.setId(null);
//        items.add(item1);
        Datasource item2 = new Datasource();
        item2.setId("ewew");
//        items.add(item2);
        if (CollectionUtils.isNotEmpty(items) && items.stream().map(Datasource::getId).collect(Collectors.toList()).contains("ewew")) {
            System.out.println("111");
        }

//        DremioDsProvider dremioDsProvider = new DremioDsProvider();

//        String datasourceStr = "{\"apiConfiguration\":[],\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"localhost\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"space.folder\\\"}\",\"configurationEncryption\":true,\"name\":\"zq\",\"type\":\"dremio\"}";
//        Datasource datasource = JSONObject.parseObject(datasourceStr, Datasource.class);
//        DatasourceRequest datasourceRequest = new DatasourceRequest();
//        datasourceRequest.setDatasource(datasource);
//
//        try {
//            String res = dremioDsProvider.checkStatus(datasourceRequest);
//            System.out.println(res);
//        } catch (Exception exception) {
//            System.out.println(exception.getMessage());
//        }
    }
//
//
//    public void testCreateQueryTableWithPage() {
//        DremioQueryProvider provider = new DremioQueryProvider();
//
//        String table = "testzq";
////        String fieldsStr = "[{\"accuracy\":0,\"checked\":true,\"columnIndex\":0,\"dataeaseName\":\"C_b80bb7740288fda1f201890375a60c8f\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"96441638-0dfc-42b6-bd8b-87dc6a014931\",\"lastSyncTime\":1690428810320,\"name\":\"id\",\"originName\":\"id\",\"size\":65536,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":1,\"dataeaseName\":\"C_99bea2cd698b56b1a3b8c1701bd51c67\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"30ba71ea-3854-4ee1-a6f6-6bc0db8fcd01\",\"lastSyncTime\":1690428810320,\"name\":\"pv\",\"originName\":\"pv\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":2,\"dataeaseName\":\"C_a3b34c0871dc2fd51eec5559b68f709d\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"017fb8ad-8f7e-4da2-80f0-106dfcd0697a\",\"lastSyncTime\":1690428810320,\"name\":\"play\",\"originName\":\"play\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":3,\"dataeaseName\":\"C_a7dd12b1dab17d25467b0b0a4c8d4a92\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"d585b6ba-9258-4e2f-ab5c-c37d50c5e153\",\"lastSyncTime\":1690428810320,\"name\":\"show\",\"originName\":\"show\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":4,\"dataeaseName\":\"C_1bc33012992b99a3b7fc01faaedd04e1\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"64066bc7-6119-42fa-9052-6f487f052851\",\"lastSyncTime\":1690428810320,\"name\":\"upvote\",\"originName\":\"upvote\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":5,\"dataeaseName\":\"C_06d4cd63bde972fc66a0aed41d2f5c51\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"c5dca60f-807f-48fc-a4ea-11863ea80d32\",\"lastSyncTime\":1690428810320,\"name\":\"comment\",\"originName\":\"comment\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":6,\"dataeaseName\":\"C_be1ab1632e4285edc3733b142935c60b\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"58186c44-a643-43c2-ae4e-270aebe771fe\",\"lastSyncTime\":1690428810320,\"name\":\"like\",\"originName\":\"like\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":7,\"dataeaseName\":\"C_0788a6922bd5f9f130e7ed8980193bab\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"8ec4612a-0e34-4133-8925-e3477751b67a\",\"lastSyncTime\":1690428810320,\"name\":\"collect\",\"originName\":\"collect\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":8,\"dataeaseName\":\"C_85e47ac07ac9d6416168a97e33fa969a\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"9de95cbe-d980-4452-ba2a-cc80a8c0fc5b\",\"lastSyncTime\":1690428810320,\"name\":\"share\",\"originName\":\"share\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":9,\"dataeaseName\":\"C_e0ab074e8104870583f417cbd8afa027\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"7eb9f4bf-7539-4c00-ad7e-b3fe4d19f254\",\"lastSyncTime\":1690428810320,\"name\":\"reaction\",\"originName\":\"reaction\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":10,\"dataeaseName\":\"C_920b9115ef920ff411c979dd5a684be2\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"dd2be540-64da-49ef-9073-0891cb7ec6c6\",\"lastSyncTime\":1690428810320,\"name\":\"re_pin\",\"originName\":\"re_pin\",\"size\":32,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":11,\"dataeaseName\":\"C_d5d3db1765287eef77d7927cc956f50a\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"108aefc7-d21a-4958-83d4-91bd79280746\",\"lastSyncTime\":1690428810320,\"name\":\"title\",\"originName\":\"title\",\"size\":65536,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":12,\"dataeaseName\":\"C_0faea619a3eb6dfca4ced9bbe5bf4c9a\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"115d1017-65b2-4947-a197-c6489e696e1d\",\"lastSyncTime\":1690428810320,\"name\":\"publish_time\",\"originName\":\"publish_time\",\"size\":65536,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":13,\"dataeaseName\":\"C_bb5855f0349346ae0b19eb381f00ab70\",\"deExtractType\":1,\"deType\":1,\"extField\":0,\"groupType\":\"d\",\"id\":\"edc3137b-0812-4b88-af54-544152da1f84\",\"lastSyncTime\":1690428810320,\"name\":\"created_time\",\"originName\":\"created_time\",\"size\":23,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"TIMESTAMP\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":14,\"dataeaseName\":\"C_2a304a1348456ccd2234cd71a81bd338\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"41f205aa-8368-45a8-8d98-14dbb4b178bd\",\"lastSyncTime\":1690428810320,\"name\":\"link\",\"originName\":\"link\",\"size\":65536,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":15,\"dataeaseName\":\"C_ee11cbb19052e40b07aac0ca060c23ee\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"aec06dc3-059d-4d75-bb5b-2290e53f94ee\",\"lastSyncTime\":1690428810320,\"name\":\"user\",\"originName\":\"user\",\"size\":65536,\"tableId\":\"cc71d92a-7c7f-420b-b0b7-f8ad842b813e\",\"type\":\"CHARACTER VARYING\"}]";
//        String fieldsStr = "[{\"accuracy\":0,\"checked\":true,\"columnIndex\":12,\"dataeaseName\":\"C_0faea619a3eb6dfca4ced9bbe5bf4c9a\",\"dateFormat\":\"yyyy-MM-dd\",\"dateFormatType\":\"yyyy-MM-dd\",\"deExtractType\":0,\"deType\":1,\"extField\":0,\"groupType\":\"d\",\"id\":\"05e44d6b-9e28-45ab-85b4-e52a92a6afda\",\"lastSyncTime\":1690442910315,\"name\":\"publish_time\",\"originName\":\"publish_time\",\"size\":65536,\"tableId\":\"4a235907-4062-4195-978a-7b2ac824505a\",\"type\":\"CHARACTER VARYING\"}]";
//        List<DatasetTableField> fields = JSONArray.parseArray(fieldsStr, DatasetTableField.class);
//        Integer page = 1;
//        Integer pageSize = 1;
//        Integer realSize = 1;
//
//        String datasourceStr = "{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"localhost\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1690429796582,\"id\":\"d797e5ea-78d9-4011-8aee-ce25c62dcae4\",\"name\":\"local\",\"status\":\"Success\",\"type\":\"dremio\",\"updateTime\":1690429796582}";
//        Datasource datasource = JSONObject.parseObject(datasourceStr, Datasource.class);
//        List<DataSetRowPermissionsTreeDTO> rowPermissionsTree = null;
//
//        String sql = provider.createQueryTableWithPage(table, fields, page, pageSize, realSize, false, datasource, null, rowPermissionsTree);
//        System.out.println(sql);
//    }
}
