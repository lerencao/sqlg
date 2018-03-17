package org.umlg.sqlg.test.topology;

import org.apache.tinkerpop.gremlin.structure.T;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.umlg.sqlg.structure.PropertyType;
import org.umlg.sqlg.structure.topology.VertexLabel;
import org.umlg.sqlg.test.BaseTest;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

/**
 * @author Pieter Martin (https://github.com/pietermartin)
 * Date: 2018/03/05
 */
public class TestSharding extends BaseTest {

    @Before
    public void before() throws Exception {
        super.before();
        Assume.assumeTrue(this.sqlgGraph.getSqlDialect().supportsPartitioning());
    }

    @Test
    public void prepareCitusPOC() {
        Random random = new Random();
        LinkedHashMap<String, Object> counters = new LinkedHashMap<>(100);
        for (int i = 1; i <= 100; i++) {
            counters.put("counter_" + i, random.nextDouble());
        }
        this.sqlgGraph.tx().streamingBatchModeOn();
        for (int i = 0; i < 50_000_000; i++) {
            this.sqlgGraph.streamVertex("HUAWEI_GSM_Object1", counters);
            if (i % 100_000 == 0) {
                this.sqlgGraph.tx().commit();
                this.sqlgGraph.tx().streamingBatchModeOn();
            }
        }
        this.sqlgGraph.tx().commit();
    }

//    @Test
    public void testShardingA() throws SQLException {

        Connection connection = this.sqlgGraph.tx().getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET citus.shard_count = 2");
        }
        VertexLabel gsm = this.sqlgGraph.getTopology().getPublicSchema().ensureVertexLabelExist(
                "GSM",
                new HashMap<String, PropertyType>() {{
                    put("name", PropertyType.STRING);
                }},
                "name", null);
        VertexLabel umts = this.sqlgGraph.getTopology().getPublicSchema().ensureVertexLabelExist(
                "UMTS",
                new HashMap<String, PropertyType>() {{
                    put("name", PropertyType.STRING);
                }},
                "name",
                null);
//        VertexLabel lte = this.sqlgGraph.getTopology().getPublicSchema().ensureVertexLabelExist(
//                "LTE",
//                new HashMap<String, PropertyType>() {{
//                    put("name", PropertyType.STRING);
//                }},
//                "name");
        this.sqlgGraph.tx().commit();

        this.sqlgGraph.tx().streamingBatchModeOn();
        this.sqlgGraph.streamVertex(T.label, "GSM", "name", "UMTS");
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.tx().streamingBatchModeOn();
        this.sqlgGraph.streamVertex(T.label, "GSM", "name", "b");
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.tx().streamingBatchModeOn();
        this.sqlgGraph.streamVertex(T.label, "UMTS", "name", "UMTS");
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.tx().streamingBatchModeOn();
        this.sqlgGraph.streamVertex(T.label, "UMTS", "name", "b");
        this.sqlgGraph.tx().commit();
//        this.sqlgGraph.tx().streamingBatchModeOn();
//        for (int j = 1; j <= 10; j++) {
//            this.sqlgGraph.streamVertex(T.label, "LTE", "name", "LTE");
//        }
//        this.sqlgGraph.tx().commit();

    }

}
