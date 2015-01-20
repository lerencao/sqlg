package org.umlg.sqlg.process.graph.util;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.step.map.FlatMapStep;
import com.tinkerpop.gremlin.process.graph.step.map.VertexStep;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Vertex;
import org.apache.commons.lang3.tuple.Pair;
import org.umlg.sqlg.structure.SqlgVertex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Date: 2014/08/15
 * Time: 8:10 PM
 */
public class SqlgVertexStepCompiler<E extends Element> extends FlatMapStep<Vertex, E> implements Reversible {

    private List<Pair<VertexStep, List<HasContainer>>> replacedSteps = new ArrayList<>();

    public SqlgVertexStepCompiler(final Traversal traversal) {
        super(traversal);
        this.setFunction(traverser -> (Iterator<E>) ((SqlgVertex) traverser.get()).elements(this.replacedSteps));
    }

    public void addReplacedStep(Pair<VertexStep, List<HasContainer>> stepPair) {
        this.replacedSteps.add(stepPair);
    }

}
