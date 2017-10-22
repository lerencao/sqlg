package org.umlg.sqlg.strategy;

import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.optimization.MessagePassingReductionStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.branch.OptionalStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.*;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.umlg.sqlg.step.SqlgOptionalStepBarrier;
import org.umlg.sqlg.structure.SqlgGraph;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pieter Martin (https://github.com/pietermartin)
 *         Date: 2014/08/15
 */
public class SqlgOptionalStepStrategy<S> extends AbstractTraversalStrategy<TraversalStrategy.OptimizationStrategy> implements TraversalStrategy.OptimizationStrategy {

    public SqlgOptionalStepStrategy() {
        super();
    }

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        //Only optimize SqlgGraph. StarGraph also passes through here.
        //noinspection OptionalGetWithoutIsPresent
        if (!(traversal.getGraph().get() instanceof SqlgGraph)) {
            return;
        }
        List<OptionalStep> optionalSteps = TraversalHelper.getStepsOfAssignableClass(OptionalStep.class, traversal);
        for (OptionalStep<S> optionalStep : optionalSteps) {

            //The predicate branch step is a local traversal.
            //As such if it contains a ReducingBarrierStep the SqlgBranchStepBarrier will not work.
            Traversal.Admin<S, S> optionalTraversal = optionalStep.getLocalChildren().get(0);

            List<ReducingBarrierStep> reducingBarrierSteps = TraversalHelper.getStepsOfAssignableClass(ReducingBarrierStep.class, optionalTraversal);
            if (!reducingBarrierSteps.isEmpty()) {
                continue;
            }

            SqlgOptionalStepBarrier sqlgOptionalStepBarrier = new SqlgOptionalStepBarrier<>(
                    traversal,
                    optionalTraversal
            );
            for (String label : optionalStep.getLabels()) {
                sqlgOptionalStepBarrier.addLabel(label);
            }
            TraversalHelper.replaceStep(
                    optionalStep,
                    sqlgOptionalStepBarrier,
                    optionalStep.getTraversal()
            );
        }
    }

    @Override
    public Set<Class<? extends OptimizationStrategy>> applyPost() {
        return Stream.of(
                MatchPredicateStrategy.class,
                RepeatUnrollStrategy.class,
                PathRetractionStrategy.class,
//                InlineFilterStrategy.class,
                MessagePassingReductionStrategy.class,
                IncidentToAdjacentStrategy.class
        ).collect(Collectors.toSet());
    }

    @Override
    public Set<Class<? extends OptimizationStrategy>> applyPrior() {
        return Stream.of(
                SqlgVertexStepStrategy.class
        ).collect(Collectors.toSet());
    }

}