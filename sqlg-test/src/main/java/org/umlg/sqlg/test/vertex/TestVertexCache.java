package org.umlg.sqlg.test.vertex;

import com.tinkerpop.gremlin.process.T;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Test;
import org.umlg.sqlg.test.BaseTest;

/**
 * Date: 2014/10/04
 * Time: 2:03 PM
 */
public class TestVertexCache extends BaseTest {

//    @Test
//    public void testVertexTransactionalCache() {
//        Vertex v1 = this.sqlG.addVertex(T.label, "Person");
//        Vertex v2 = this.sqlG.addVertex(T.label, "Person");
//        Vertex v3 = this.sqlG.addVertex(T.label, "Person");
//        v1.addEdge("friend", v2);
//        Assert.assertEquals(1, v1.out("friend").count().next().intValue());
//        Vertex tmpV1 = this.sqlG.v(v1.id());
//        tmpV1.addEdge("foe", v3);
//        //this should fail as v1's out edges will not be updated
//        Assert.assertEquals(1, tmpV1.out("foe").count().next().intValue());
//        Assert.assertEquals(1, v1.out("foe").count().next().intValue());
//        this.sqlG.tx().rollback();
//    }
//
//    @Test
//    public void testVertexTransactionalCache2() {
//        Vertex v1 = this.sqlG.addVertex(T.label, "Person");
//        Vertex v2 = this.sqlG.addVertex(T.label, "Person");
//        Vertex v3 = this.sqlG.addVertex(T.label, "Person");
//        Edge e1 = v1.addEdge("friend", v2);
//        Assert.assertEquals(1, v1.out("friend").count().next().intValue());
//
//        Vertex tmpV1 = this.sqlG.e(e1.id()).outV().next();
//        tmpV1.addEdge("foe", v3);
//        //this should fail as v1's out edges will not be updated
//        Assert.assertEquals(1, tmpV1.out("foe").count().next().intValue());
//        Assert.assertEquals(1, v1.out("foe").count().next().intValue());
//        this.sqlG.tx().rollback();
//    }

    @Test
    public void testVertexTransactionalCache3() {
        Vertex v1 = this.sqlG.addVertex(T.label, "Person");
        Vertex v2 = this.sqlG.addVertex(T.label, "Person");
        Vertex v3 = this.sqlG.addVertex(T.label, "Person");
        Edge e1 = v1.addEdge("friend", v2);
        Assert.assertEquals(1, v1.out("friend").count().next().intValue());

        Vertex tmpV1 = v1.outE("friend").inV().next();
        tmpV1.addEdge("foe", v3);
        //this should fail as v1's out edges will not be updated
        Assert.assertEquals(1, tmpV1.out("foe").count().next().intValue());
        Assert.assertEquals(1, v1.out("foe").count().next().intValue());
        this.sqlG.tx().rollback();
    }

//    @Test
//    public void testMultipleReferencesToSameVertex() {
//        Vertex v1 = this.sqlG.addVertex(T.label, "Person", "name", "john");
//        this.sqlG.tx().commit();
//        Assert.assertEquals("john", v1.value("name"));
//        //_v1 is in the transaction cache
//        //v1 is not
//        Vertex _v1 = this.sqlG.v(v1.id());
//        Assert.assertEquals("john", _v1.value("name"));
//        v1.property("name", "john1");
//        Assert.assertEquals("john1", v1.value("name"));
//        Assert.assertEquals("john1", _v1.value("name"));
//    }
//
//    @Test
//    public void testMultipleReferencesToSameVertex2Instances() {
//        Vertex v1 = this.sqlG.addVertex(T.label, "Person", "name", "john");
//        this.sqlG.tx().commit();
//        //_v1 is in the transaction cache
//        //v1 is not
//        Vertex _v1 = this.sqlG.v(v1.id());
//        Assert.assertEquals("john", v1.value("name"));
//        Assert.assertEquals("john", _v1.value("name"));
//        v1.property("name", "john1");
//        Assert.assertEquals("john1", v1.value("name"));
//        Assert.assertEquals("john1", _v1.value("name"));
//    }

    //TODO test multiple threads accessing same vertex changing properties
    //properties are not thread scoped at the moment
//    @Test
//    public void testMultipleThreadsAccessSameVertexInstance() throws InterruptedException {
//        final Vertex v1 = this.sqlG.addVertex(T.label, "Person", "name", "john");
//        this.sqlG.tx().commit();
//        Thread t = new Thread(new Runnable() {
//            public void run() {
//                Vertex b = sqlG.addVertex(T.label, "Person");
//                v1.property("name", "john1");
//                sqlG.tx().commit();
//            }
//        });
//        t.start();
//        t.join();
//        Assert.assertEquals(1, v1.out("relation").count().next().intValue());
//    }
}
