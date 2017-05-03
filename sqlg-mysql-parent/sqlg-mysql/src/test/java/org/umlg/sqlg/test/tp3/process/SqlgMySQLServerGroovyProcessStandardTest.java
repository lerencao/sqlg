package org.umlg.sqlg.test.tp3.process;

import org.apache.tinkerpop.gremlin.GraphProviderClass;
import org.apache.tinkerpop.gremlin.process.GroovyProcessStandardSuite;
import org.junit.runner.RunWith;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.test.tp3.SqlgMySQLProvider;


/**
 * Executes the Standard Gremlin Structure Test Suite using SqlgGraph on postgresql.
 */
@RunWith(GroovyProcessStandardSuite.class)
@GraphProviderClass(provider = SqlgMySQLProvider.class, graph = SqlgGraph.class)
public class SqlgMySQLServerGroovyProcessStandardTest {
}
