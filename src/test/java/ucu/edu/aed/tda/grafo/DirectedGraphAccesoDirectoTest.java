package ucu.edu.aed.tda.grafo;

import junit.framework.TestCase;

public class DirectedGraphAccesoDirectoTest extends TestCase {
    private final DirectedGraph<String, Integer> g = new DirectedGraph<>();

    @Override
    protected void setUp() {
        g.agregarVertice("A");
        g.agregarVertice("B");
        g.agregarArista("A", "B", 7);
    }

    public void testObtenerAristaPorVertice() {
        assertEquals(Integer.valueOf(7), g.obtenerArista("A", "B").dato());
        assertNull(g.obtenerArista("B", "A"));
        assertNull(g.obtenerArista("A", "X"));
        assertNull(g.obtenerArista("X", "A"));
    }

    public void testEliminarAristaPorVertice() {
        assertFalse(g.eliminarArista("B", "A"));
        assertFalse(g.eliminarArista("X", "A"));
        assertTrue(g.eliminarArista("A", "B"));
        assertNull(g.obtenerArista("A", "B"));
        assertTrue(g.predecessors(g.construirComparable("B")).isEmpty());
        assertTrue(g.agregarArista("A", "B", 8));
    }
}
