package io.dataease.plugins.datasource.dremio.provider;

import com.alibaba.fastjson.JSONObject;
import io.dataease.plugins.common.base.domain.Datasource;
import io.dataease.plugins.common.request.datasource.DatasourceRequest;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DremioDsProviderTest extends TestCase {

    @Test
    public void testGetTables() {
        DremioDsProvider dremioDsProvider = new DremioDsProvider();
        DatasourceRequest datasourceRequest = new DatasourceRequest();

        String ds = "{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"10.1.13.137\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1697607935105,\"id\":\"d572b7b5-8bb8-4601-9007-cce0ac748612\",\"name\":\"dremio\",\"status\":\"Error\",\"type\":\"dremio\",\"updateTime\":1697607935105}";

        datasourceRequest.setDatasource(JSONObject.parseObject(ds, Datasource.class));

        try {
            dremioDsProvider.getTables(datasourceRequest);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Test
    public void testCheckStatus() {
        DremioDsProvider dremioDsProvider = new DremioDsProvider();
        DatasourceRequest datasourceRequest = new DatasourceRequest();

        String ds = "{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"10.1.13.137\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1697607935105,\"id\":\"d572b7b5-8bb8-4601-9007-cce0ac748612\",\"name\":\"dremio\",\"status\":\"Error\",\"type\":\"dremio\",\"updateTime\":1697607935105}";

        datasourceRequest.setDatasource(JSONObject.parseObject(ds, Datasource.class));

        try {
            dremioDsProvider.checkStatus(datasourceRequest);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

        String test = "{\"datasource\":{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"10.1.13.137\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1698824378958,\"id\":\"7ae5ee76-f5a1-4d18-9c53-5d37648e2565\",\"name\":\"dremio\",\"status\":\"Success\",\"type\":\"dremio\",\"updateTime\":1698824378958},\"fetchSize\":10000,\"pageable\":false,\"previewData\":false,\"query\":\"SELECT * FROM (SELECT * from test) AS tmp  LIMIT 1000\",\"rEG_WITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\",\"totalPageFlag\":false,\"wITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\"}";
    }

    public void testGetData() {
        DremioDsProvider dremioDsProvider = new DremioDsProvider();


        String test = "{\"datasource\":{\"configuration\":\"{\\\"initialPoolSize\\\":5,\\\"extraParams\\\":\\\"\\\",\\\"minPoolSize\\\":5,\\\"maxPoolSize\\\":50,\\\"maxIdleTime\\\":30,\\\"acquireIncrement\\\":5,\\\"idleConnectionTestPeriod\\\":5,\\\"connectTimeout\\\":5,\\\"customDriver\\\":\\\"default\\\",\\\"queryTimeout\\\":30,\\\"username\\\":\\\"zq\\\",\\\"password\\\":\\\"Calong@2015\\\",\\\"host\\\":\\\"10.1.13.137\\\",\\\"port\\\":\\\"31010\\\",\\\"dataBase\\\":\\\"dataease\\\"}\",\"createBy\":\"admin\",\"createTime\":1698824378958,\"id\":\"7ae5ee76-f5a1-4d18-9c53-5d37648e2565\",\"name\":\"dremio\",\"status\":\"Success\",\"type\":\"dremio\",\"updateTime\":1698824378958},\"fetchSize\":1000,\"page\":1,\"pageSize\":1000,\"pageable\":true,\"permissionFields\":[{\"accuracy\":0,\"checked\":true,\"columnIndex\":0,\"dataeaseName\":\"C_b80bb7740288fda1f201890375a60c8f\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"3755f936-84a4-489e-a8a3-c43f52261f17\",\"lastSyncTime\":1700031962471,\"name\":\"id\",\"originName\":\"id\",\"size\":65536,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":1,\"dataeaseName\":\"C_99bea2cd698b56b1a3b8c1701bd51c67\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"055567ac-50ab-4f35-b859-787afcb14670\",\"lastSyncTime\":1700031962471,\"name\":\"pv\",\"originName\":\"pv\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":2,\"dataeaseName\":\"C_a3b34c0871dc2fd51eec5559b68f709d\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"dd91c52a-763e-4f67-a285-f13d8a2c3e39\",\"lastSyncTime\":1700031962471,\"name\":\"play\",\"originName\":\"play\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":3,\"dataeaseName\":\"C_a7dd12b1dab17d25467b0b0a4c8d4a92\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"fd83c950-e20d-449e-9195-9dcad9857058\",\"lastSyncTime\":1700031962471,\"name\":\"show\",\"originName\":\"show\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":4,\"dataeaseName\":\"C_1bc33012992b99a3b7fc01faaedd04e1\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"e22b4822-8bbb-4efa-8a93-2e77fa89f897\",\"lastSyncTime\":1700031962471,\"name\":\"upvote\",\"originName\":\"upvote\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":5,\"dataeaseName\":\"C_06d4cd63bde972fc66a0aed41d2f5c51\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"00c355f1-1042-4b41-86fd-75977f00b70f\",\"lastSyncTime\":1700031962471,\"name\":\"comment\",\"originName\":\"comment\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":6,\"dataeaseName\":\"C_be1ab1632e4285edc3733b142935c60b\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"19b52e88-fadf-421a-891a-4de6520a732a\",\"lastSyncTime\":1700031962471,\"name\":\"like\",\"originName\":\"like\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":7,\"dataeaseName\":\"C_0788a6922bd5f9f130e7ed8980193bab\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"ba137f8f-2445-4d5e-8a67-a976c942c83e\",\"lastSyncTime\":1700031962471,\"name\":\"collect\",\"originName\":\"collect\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":8,\"dataeaseName\":\"C_85e47ac07ac9d6416168a97e33fa969a\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"8e2c95a7-8122-4a77-8e8f-a5c9f17652e9\",\"lastSyncTime\":1700031962471,\"name\":\"share\",\"originName\":\"share\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":9,\"dataeaseName\":\"C_e0ab074e8104870583f417cbd8afa027\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"d7a2ab15-0788-4c45-b300-1f3723766eef\",\"lastSyncTime\":1700031962471,\"name\":\"reaction\",\"originName\":\"reaction\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":10,\"dataeaseName\":\"C_920b9115ef920ff411c979dd5a684be2\",\"deExtractType\":2,\"deType\":2,\"extField\":0,\"groupType\":\"q\",\"id\":\"95f36cb5-5b74-421e-9420-63e7bfe74740\",\"lastSyncTime\":1700031962471,\"name\":\"re_pin\",\"originName\":\"re_pin\",\"size\":11,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"INTEGER\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":11,\"dataeaseName\":\"C_d5d3db1765287eef77d7927cc956f50a\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"1fc5a358-63d3-4a66-8c16-6c4d7e77d752\",\"lastSyncTime\":1700031962471,\"name\":\"title\",\"originName\":\"title\",\"size\":65536,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":12,\"dataeaseName\":\"C_0faea619a3eb6dfca4ced9bbe5bf4c9a\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"77252afc-3805-4373-913a-dad401ee1b8f\",\"lastSyncTime\":1700031962471,\"name\":\"publish_time\",\"originName\":\"publish_time\",\"size\":65536,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":13,\"dataeaseName\":\"C_bb5855f0349346ae0b19eb381f00ab70\",\"deExtractType\":1,\"deType\":1,\"extField\":0,\"groupType\":\"d\",\"id\":\"cf98b7e4-f3b7-424e-a3aa-da44fdac6e40\",\"lastSyncTime\":1700031962471,\"name\":\"created_time\",\"originName\":\"created_time\",\"size\":23,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"TIMESTAMP\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":14,\"dataeaseName\":\"C_2a304a1348456ccd2234cd71a81bd338\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"b0ee7bed-7dd1-4520-93e0-459bb61b1c63\",\"lastSyncTime\":1700031962471,\"name\":\"link\",\"originName\":\"link\",\"size\":65536,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"CHARACTER VARYING\"},{\"accuracy\":0,\"checked\":true,\"columnIndex\":15,\"dataeaseName\":\"C_ee11cbb19052e40b07aac0ca060c23ee\",\"deExtractType\":0,\"deType\":0,\"extField\":0,\"groupType\":\"d\",\"id\":\"2514c9b7-e3f8-4d52-b4b4-51cd6cbd512a\",\"lastSyncTime\":1700031962471,\"name\":\"user\",\"originName\":\"user\",\"size\":65536,\"tableId\":\"c12b1f12-d428-43c7-824f-ce575c583456\",\"type\":\"CHARACTER VARYING\"}],\"previewData\":true,\"query\":\"SELECT\\n    t_a_0.\\\"id\\\" AS f_ax_0,\\n    CAST(t_a_0.\\\"pv\\\" AS DECIMAL(20,0)) AS f_ax_1,\\n    CAST(t_a_0.\\\"play\\\" AS DECIMAL(20,0)) AS f_ax_2,\\n    CAST(t_a_0.\\\"show\\\" AS DECIMAL(20,0)) AS f_ax_3,\\n    CAST(t_a_0.\\\"upvote\\\" AS DECIMAL(20,0)) AS f_ax_4,\\n    CAST(t_a_0.\\\"comment\\\" AS DECIMAL(20,0)) AS f_ax_5,\\n    CAST(t_a_0.\\\"like\\\" AS DECIMAL(20,0)) AS f_ax_6,\\n    CAST(t_a_0.\\\"collect\\\" AS DECIMAL(20,0)) AS f_ax_7,\\n    CAST(t_a_0.\\\"share\\\" AS DECIMAL(20,0)) AS f_ax_8,\\n    CAST(t_a_0.\\\"reaction\\\" AS DECIMAL(20,0)) AS f_ax_9,\\n    CAST(t_a_0.\\\"re_pin\\\" AS DECIMAL(20,0)) AS f_ax_10,\\n    t_a_0.\\\"title\\\" AS f_ax_11,\\n    t_a_0.\\\"publish_time\\\" AS f_ax_12,\\n    TO_CHAR(t_a_0.\\\"created_time\\\",'yyyy-MM-dd hh:mm:ss') AS f_ax_13,\\n    t_a_0.\\\"link\\\" AS f_ax_14,\\n    t_a_0.\\\"user\\\" AS f_ax_15\\nFROM\\n    (SELECT * from t_zhihu_answer)   t_a_0\\n LIMIT 1000 OFFSET 0\",\"rEG_WITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\",\"realSize\":1000,\"totalPageFlag\":false,\"wITH_SQL_FRAGMENT\":\"((?i)WITH[\\\\s\\\\S]+(?i)AS?\\\\s*\\\\([\\\\s\\\\S]+\\\\))\\\\s*(?i)SELECT\"}";
        DatasourceRequest datasourceRequest = JSONObject.parseObject(test, DatasourceRequest.class);

        List<String[]> data = new ArrayList<>();

        try {
            data = dremioDsProvider.getData(datasourceRequest);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

        System.out.println(JSONObject.toJSON(data));
    }
}
