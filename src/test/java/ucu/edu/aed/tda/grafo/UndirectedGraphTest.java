package ucu.edu.aed.tda.grafo;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.List;
import java.util.Set;

public class UndirectedGraphTest extends TestCase {
    private UndirectedGraph<String, Integer> g;

    @Override
    protected void setUp() {
        g = new UndirectedGraph<>();
    }

    private void vs(String... names) {
        for (String name : names) g.agregarVertice(name);
    }

    public void testEmptyAndSingleVertex() {
        assertEquals(0, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());

        vs("A");
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());
        assertFalse(g.agregarVertice("A"));
    }

    public void testAddUndirectedEdgeAndKeepItsData() {
        vs("A", "B", "C");

        assertTrue(g.agregarArista("A", "B", 7));
        assertFalse(g.agregarArista("B", "A", 9));
        assertEquals(1, g.cantidadDeAristas());
        assertTrue(g.existeArista(g.construirComparable("A"), g.construirComparable("B")));
        assertTrue(g.existeArista(g.construirComparable("B"), g.construirComparable("A")));
        assertEquals(Integer.valueOf(7), g.obtenerArista("A", "B").dato());
        assertEquals(Integer.valueOf(7), g.obtenerArista("B", "A").dato());
        assertFalse(g.obtenerArista("A", "B").directed());
        assertEquals(1, g.adyacencias(g.construirComparable("A")).size());
        assertEquals(1, g.adyacencias(g.construirComparable("B")).size());
    }

    public void testCannotAddEdgeWithMissingVertex() {
        vs("A");
        assertFalse(g.agregarArista("A", "X", 1));
        assertFalse(g.agregarArista("X", "A", 1));
        assertEquals(0, g.cantidadDeAristas());
    }

    public void testEdgesReturnsEachConnectionOnceAndIsUnmodifiable() {
        vs("A", "B", "C");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);

        Set<Edge<String, Integer>> edges = g.aristas();
        assertEquals(2, edges.size());
        assertTrue(edges.contains(g.obtenerArista("A", "B")));
        assertTrue(edges.contains(g.obtenerArista("B", "C")));
        try {
            edges.clear();
            fail("aristas() debe devolver un conjunto no modificable");
        } catch (UnsupportedOperationException expected) { }
    }

    public void testAdjacenciesReturnsCopy() {
        vs("A", "B");
        g.agregarArista("A", "B", 3);
        List<Edge<String, Integer>> adj = g.adyacencias(g.construirComparable("A"));
        assertEquals(1, adj.size());
        adj.clear();
        assertEquals(1, g.adyacencias(g.construirComparable("A")).size());
        assertTrue(g.adyacencias(g.construirComparable("X")).isEmpty());
    }

    public void testRemoveEdgeInEitherOrder() {
        vs("A", "B", "C");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);

        assertTrue(g.eliminarArista(g.construirComparable("B"), g.construirComparable("A")));
        assertFalse(g.existeArista(g.construirComparable("A"), g.construirComparable("B")));
        assertEquals(1, g.cantidadDeAristas());
        assertFalse(g.eliminarArista(g.construirComparable("A"), g.construirComparable("B")));
        assertTrue(g.eliminarArista(g.construirComparable("B"), g.construirComparable("C")));
        assertEquals(0, g.cantidadDeAristas());
    }

    public void testRemoveVertexAlsoRemovesIncidentEdges() {
        vs("A", "B", "C", "D");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);
        g.agregarArista("C", "D", 3);

        assertTrue(g.removerVertice(g.construirComparable("B")));
        assertFalse(g.existeVertice(g.construirComparable("B")));
        assertEquals(3, g.cantidadDeVertices());
        assertEquals(1, g.cantidadDeAristas());
        assertTrue(g.existeArista(g.construirComparable("C"), g.construirComparable("D")));
        assertFalse(g.removerVertice(g.construirComparable("B")));
    }

    public void testConnectivity() {
        vs("A", "B", "C", "D");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);
        assertFalse(g.esConexo());
        g.agregarArista("C", "D", 3);
        assertTrue(g.esConexo());
    }

    public void testCycleDetectionForTreeAndCycle() {
        vs("A", "B", "C", "D");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);
        g.agregarArista("B", "D", 3);
        assertFalse(g.tieneCiclos());

        g.agregarArista("C", "D", 4);
        assertTrue(g.tieneCiclos());
    }

    public void testSelfLoopIsCycleAndCanBeRemovedWithVertex() {
        vs("A");
        assertTrue(g.agregarArista("A", "A", 1));
        assertTrue(g.tieneCiclos());
        assertEquals(1, g.cantidadDeAristas());
        assertTrue(g.removerVertice(g.construirComparable("A")));
        assertEquals(0, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
    }

    public void testClear() {
        vs("A", "B");
        g.agregarArista("A", "B", 1);
        g.vaciar();
        assertEquals(0, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());
    }
}
