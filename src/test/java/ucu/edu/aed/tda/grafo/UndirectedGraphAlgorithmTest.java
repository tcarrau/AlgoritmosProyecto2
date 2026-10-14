package ucu.edu.aed.tda.grafo;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UndirectedGraphAlgorithmTest extends TestCase {
    private UndirectedGraphAlgorithm algorithms;

    @Override
    protected void setUp() {
        algorithms = new UndirectedGraphAlgorithm();
    }

    private UndirectedGraph<String, WeightedEdge> newGraph(String... vertices) {
        UndirectedGraph<String, WeightedEdge> graph = new UndirectedGraph<>();
        for (String vertex : vertices) graph.agregarVertice(vertex);
        return graph;
    }

    private void addEdge(UndirectedGraph<String, WeightedEdge> graph,
                        String source, String target, double weight) {
        assertTrue(graph.agregarArista(source, target, new WeightedEdge(weight)));
    }

    private double totalWeight(IUndirectedGraph<String, WeightedEdge> graph) {
        return graph.aristas().stream()
                .mapToDouble(edge -> edge.dato().getWeight())
                .sum();
    }

    public void testKruskalBuildsMinimumSpanningTree() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B", "C", "D");
        addEdge(graph, "A", "B", 10);
        addEdge(graph, "A", "C", 1);
        addEdge(graph, "C", "B", 2);
        addEdge(graph, "B", "D", 3);
        addEdge(graph, "C", "D", 8);

        IUndirectedGraph<String, WeightedEdge> result = algorithms.kruskal(graph);

        assertEquals(4, result.cantidadDeVertices());
        assertEquals(3, result.cantidadDeAristas());
        assertEquals(6.0, totalWeight(result), 0.0001);
        assertFalse(result.tieneCiclos());
        assertTrue(result.esConexo());
    }

    public void testKruskalKeepsIsolatedVertices() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B", "C");
        addEdge(graph, "A", "B", 4);

        IUndirectedGraph<String, WeightedEdge> result = algorithms.kruskal(graph);

        assertEquals(3, result.cantidadDeVertices());
        assertEquals(1, result.cantidadDeAristas());
        assertFalse(result.esConexo());
    }

    public void testPrimFromGivenSourceBuildsMinimumSpanningTree() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B", "C", "D");
        addEdge(graph, "A", "B", 10);
        addEdge(graph, "A", "C", 1);
        addEdge(graph, "C", "B", 2);
        addEdge(graph, "B", "D", 3);
        addEdge(graph, "C", "D", 8);

        IUndirectedGraph<String, WeightedEdge> result =
                algorithms.prim(graph, graph.construirComparable("A"));

        assertEquals(4, result.cantidadDeVertices());
        assertEquals(3, result.cantidadDeAristas());
        assertEquals(6.0, totalWeight(result), 0.0001);
        assertFalse(result.tieneCiclos());
        assertTrue(result.esConexo());
    }

    public void testPrimWithoutSourceUsesFirstVertex() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B", "C");
        addEdge(graph, "A", "B", 5);
        addEdge(graph, "B", "C", 1);
        addEdge(graph, "A", "C", 9);

        IUndirectedGraph<String, WeightedEdge> result = algorithms.prim(graph);

        assertEquals(3, result.cantidadDeVertices());
        assertEquals(2, result.cantidadDeAristas());
        assertEquals(6.0, totalWeight(result), 0.0001);
    }

    public void testPrimReturnsNullForMissingOrNullSource() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B");

        assertNull(algorithms.prim(graph, graph.construirComparable("X")));
        assertNull(algorithms.prim(graph, null));
        assertNull(algorithms.prim(newGraph()));
    }

    public void testPrimOnDisconnectedGraphStopsAndReturnsReachableComponent() {
        UndirectedGraph<String, WeightedEdge> graph = newGraph("A", "B", "C");
        addEdge(graph, "A", "B", 2);

        IUndirectedGraph<String, WeightedEdge> result =
                algorithms.prim(graph, graph.construirComparable("A"));

        assertEquals(2, result.cantidadDeVertices());
        assertEquals(1, result.cantidadDeAristas());
        assertTrue(result.existeVertice(result.construirComparable("A")));
        assertTrue(result.existeVertice(result.construirComparable("B")));
        assertFalse(result.existeVertice(result.construirComparable("C")));
    }

    public void testBeaVisitsInBreadthFirstOrderAndAcrossComponents() {
        UndirectedGraph<String, Integer> graph = new UndirectedGraph<>();
        graph.agregarVertices(Arrays.asList("A", "B", "C", "D", "E"));
        graph.agregarArista("A", "C", 1);
        graph.agregarArista("A", "B", 1);
        graph.agregarArista("C", "D", 1);

        List<String> visited = new ArrayList<>();
        algorithms.bea(graph, visited::add);

        assertEquals(Arrays.asList("A", "C", "B", "D", "E"), visited);
    }

    public void testBeaVisitsEachVertexOnceInGraphWithCycle() {
        UndirectedGraph<String, Integer> graph = new UndirectedGraph<>();
        graph.agregarVertices(Arrays.asList("A", "B", "C"));
        graph.agregarArista("A", "B", 1);
        graph.agregarArista("B", "C", 1);
        graph.agregarArista("C", "A", 1);

        List<String> visited = new ArrayList<>();
        algorithms.bea(graph, visited::add);

        assertEquals(3, visited.size());
        assertEquals(3, new java.util.HashSet<>(visited).size());
    }
}
