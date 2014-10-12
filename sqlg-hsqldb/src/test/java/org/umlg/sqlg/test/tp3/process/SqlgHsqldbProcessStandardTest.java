package org.umlg.sqlg.test.tp3.process;

import com.tinkerpop.gremlin.process.ProcessStandardSuite;
import org.junit.runner.RunWith;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.test.tp3.SqlgHsqldbProvider;


/**
 * Executes the Standard Gremlin Structure Test Suite using Hsqldb.
 */
@RunWith(ProcessStandardSuite.class)
@ProcessStandardSuite.GraphProviderClass(provider = SqlgHsqldbProvider.class, graph = SqlgGraph.class)
public class SqlgHsqldbProcessStandardTest {
}
