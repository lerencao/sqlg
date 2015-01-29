package org.umlg.sqlg.structure;

import com.tinkerpop.gremlin.process.graph.step.map.VertexStep;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Date: 2015/01/03
 * Time: 1:06 PM
 */
public class GremlinParser {

    private SqlgGraph sqlgGraph;
    private SchemaManager schemaManager;

    public GremlinParser(SqlgGraph sqlgGraph) {
        this.sqlgGraph = sqlgGraph;
        this.schemaManager = sqlgGraph.getSchemaManager();
    }

    /**
     * This will create SqlgVertex from the resultSet.
     * Note that the nature of inner join queries on multiple tables creates duplicates.
     * This method filters out duplicates.
     *
     * @param resultSet
     * @param schemaTable
     * @param elements
     * @throws SQLException
     */
    public void loadElements(ResultSet resultSet, SchemaTable schemaTable, List<Element> elements) throws SQLException {
        List<Object> keyValues = new ArrayList<>();
        String idProperty = schemaTable.getSchema() + "." + schemaTable.getTable() + "." + SchemaManager.ID;
        Long id = resultSet.getLong(idProperty);
        Map<String, PropertyType> propertyTypeMap = this.sqlgGraph.getSchemaManager().getLocalTables().get(schemaTable.toString());
        for (String propertyName : propertyTypeMap.keySet()) {
            String property = schemaTable.getSchema() + "." + schemaTable.getTable() + "." + propertyName;
            keyValues.add(propertyName);
            keyValues.add(resultSet.getObject(property));
        }
        Map<String, Object> keyValueMap = SqlgUtil.transformToInsertValues(keyValues.toArray());
        SqlgElement sqlgElement;
        if (schemaTable.getTable().startsWith(SchemaManager.VERTEX_PREFIX)) {
            sqlgElement = SqlgVertex.of(this.sqlgGraph, id, schemaTable.getSchema(), schemaTable.getTable());
        } else {
            sqlgElement = new SqlgEdge(this.sqlgGraph, id, schemaTable.getSchema(), schemaTable.getTable());
        }
        sqlgElement.properties.clear();
        sqlgElement.properties.putAll(keyValueMap);
        elements.add(sqlgElement);
    }

    /**
     * Constructs the label paths from the given schemaTable to the leaf vertex labels for the gremlin query.
     * For each path Sqlg will execute a sql query. The union of the queries is the result the gremlin query.
     * The vertex labels can be calculated from the steps.
     *
     * @param schemaTable
     * @param replacedSteps
     * @return a List of paths. Each path is itself a list of SchemaTables.
     */
    public SchemaTableTree parse(SchemaTable schemaTable, List<Pair<VertexStep, List<HasContainer>>> replacedSteps) {
        Set<SchemaTableTree> schemaTableTrees = new HashSet<>();
        SchemaTableTree rootSchemaTableTree = new SchemaTableTree(this.sqlgGraph, schemaTable, 0);
        schemaTableTrees.add(rootSchemaTableTree);
        int depth = 1;
        for (Pair<VertexStep, List<HasContainer>> replacedStep : replacedSteps) {
            schemaTableTrees = calculatePathForVertexStep(schemaTableTrees, replacedStep, depth++);
        }
        System.out.println(rootSchemaTableTree.toTreeString());
        rootSchemaTableTree.removeAllButDeepestLeafNodes(replacedSteps.size());
        rootSchemaTableTree.removeNodesInvalidatedByHas();
        System.out.println(rootSchemaTableTree.toTreeString());
        return rootSchemaTableTree;
    }

    private Set<SchemaTableTree> calculatePathForVertexStep(Set<SchemaTableTree> schemaTableTrees, Pair<VertexStep, List<HasContainer>> replacedStep, int depth) {
        Set<SchemaTableTree> result = new HashSet<>();
        for (SchemaTableTree schemaTableTree : schemaTableTrees) {
            String[] edgeLabels = replacedStep.getLeft().getEdgeLabels();
            Direction direction = replacedStep.getLeft().getDirection();
            Class<? extends Element> elementClass = replacedStep.getLeft().getReturnClass();
            List<HasContainer> hasContainers = replacedStep.getRight();

            Pair<Set<SchemaTable>, Set<SchemaTable>> labels = this.schemaManager.getLocalTableLabels().get(schemaTableTree.getSchemaTable());
            Set<SchemaTable> inLabels = labels.getLeft();
            Set<SchemaTable> outLabels = labels.getRight();
            Set<SchemaTable> inLabelsToTraversers = new HashSet<>();
            Set<SchemaTable> outLabelsToTraversers = new HashSet<>();
            switch (direction) {
                case IN:
                    inLabelsToTraversers = edgeLabels.length > 0 ? filter(inLabels, edgeLabels) : inLabels;
                    break;
                case OUT:
                    outLabelsToTraversers = edgeLabels.length > 0 ? filter(outLabels, edgeLabels) : outLabels;
                    break;
                case BOTH:
                    inLabelsToTraversers = edgeLabels.length > 0 ? filter(inLabels, edgeLabels) : inLabels;
                    outLabelsToTraversers = edgeLabels.length > 0 ? filter(outLabels, edgeLabels) : outLabels;
                    break;
                default:
                    throw new IllegalStateException("Unknown direction " + direction.name());
            }
            //Each labelToTravers more than the first one forms a new distinct path
            for (SchemaTable inLabelsToTravers : inLabelsToTraversers) {
                SchemaTableTree schemaTableTreeChild = schemaTableTree.addChild(inLabelsToTravers, direction, elementClass, hasContainers, depth);
                if (elementClass.isAssignableFrom(Edge.class)) {
                    result.add(schemaTableTreeChild);
                } else {
                    result.addAll(calculatePathFromEdgeToVertex(schemaTableTreeChild, inLabelsToTravers, Direction.IN, elementClass, hasContainers, depth));
                }
            }
            for (SchemaTable outLabelsToTravers : outLabelsToTraversers) {
                SchemaTableTree schemaTableTreeChild = schemaTableTree.addChild(outLabelsToTravers, direction, elementClass, hasContainers, depth);
                if (elementClass.isAssignableFrom(Edge.class)) {
                    result.add(schemaTableTreeChild);
                } else {
                    result.addAll(calculatePathFromEdgeToVertex(schemaTableTreeChild, outLabelsToTravers, Direction.OUT, elementClass, hasContainers, depth));
                }
            }
        }
        return result;
    }

    private Set<SchemaTableTree> calculatePathFromEdgeToVertex(SchemaTableTree schemaTableTree, SchemaTable labelToTravers, Direction direction, Class<? extends Element> elementClass, List<HasContainer> hasContainers, int depth) {
        Set<SchemaTableTree> result = new HashSet<>();
        Map<String, Set<String>> edgeForeignKeys = this.schemaManager.getEdgeForeignKeys();
        //join from the edge table to the incoming vertex table
        Set<String> foreignKeys = edgeForeignKeys.get(labelToTravers.toString());
        //Every foreignKey for the given direction must be joined on
        for (String foreignKey : foreignKeys) {
            String foreignKeySchema = foreignKey.split("\\.")[0];
            String foreignKeyTable = foreignKey.split("\\.")[1];
            if (direction == Direction.IN && foreignKey.endsWith(SchemaManager.OUT_VERTEX_COLUMN_END)) {
                SchemaTableTree schemaTableTree1 = schemaTableTree.addChild(
                        SchemaTable.of(foreignKeySchema, SchemaManager.VERTEX_PREFIX + foreignKeyTable.replace(SchemaManager.OUT_VERTEX_COLUMN_END, "")),
                        direction,
                        elementClass,
                        hasContainers,
                        depth);
                result.add(schemaTableTree1);
            } else if (direction == Direction.OUT && foreignKey.endsWith(SchemaManager.IN_VERTEX_COLUMN_END)) {
                SchemaTableTree schemaTableTree1 = schemaTableTree.addChild(
                        SchemaTable.of(foreignKeySchema, SchemaManager.VERTEX_PREFIX + foreignKeyTable.replace(SchemaManager.IN_VERTEX_COLUMN_END, "")),
                        direction,
                        elementClass,
                        hasContainers,
                        depth);
                result.add(schemaTableTree1);
            }
        }
        return result;
    }

    private Set<SchemaTable> filter(Set<SchemaTable> labels, String[] edgeLabels) {
        Set<SchemaTable> result = new HashSet<>();
        List<String> edges = Arrays.asList(edgeLabels);
        for (SchemaTable label : labels) {
            String rawLabel = label.getTable().replace(SchemaManager.EDGE_PREFIX, "");
            if (edges.contains(rawLabel)) {
                result.add(label);
            }
        }
        return result;
    }

}