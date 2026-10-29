package ucu.edu.aed.tda.grafo;

import junit.framework.TestCase;

import java.util.Set;

public class DirectedGraphTest extends TestCase {
    protected IDirectedIGraph<String, Integer> g;

    protected IDirectedIGraph<String, Integer> crear() {
        return new DirectedGraph<>();
    }

    @Override
    protected void setUp() {
        g = crear();
    }

    private void vs(String... names) {
        for (String n : names) g.agregarVertice(n);
    }

    public void testVacio() {
        assertEquals(0, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());
    }

    public void testUnVertice() {
        vs("A");
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());
        assertFalse(g.agregarVertice("A"));
    }

    public void testVerticeInexistente() {
        vs("A");
        assertFalse(g.agregarArista("A", "X", 1));
        assertFalse(g.agregarArista("X", "A", 1));
        assertFalse(g.removerVertice(g.construirComparable("X")));
        assertFalse(g.eliminarArista(g.construirComparable("A"), g.construirComparable("X")));
        assertNull(g.buscarVertice(g.construirComparable("X")));
        assertNull(g.obtenerArista("A", "X"));
        assertTrue(g.successors(g.construirComparable("X")).isEmpty());
    }

    public void testDireccionSucesoresPredecesores() {
        vs("A", "B", "C");
        assertTrue(g.agregarArista("A", "B", 1));
        assertTrue(g.agregarArista("A", "C", 2));
        assertFalse(g.agregarArista("A", "B", 9)); // duplicada
        assertTrue(g.existeArista(g.construirComparable("A"), g.construirComparable("B")));
        assertFalse(g.existeArista(g.construirComparable("B"), g.construirComparable("A")));
        assertEquals(Set.of("B", "C"), g.successors(g.construirComparable("A")));
        assertEquals(Set.of("A"), g.predecessors(g.construirComparable("B")));
        assertEquals(2, g.gradoDeSalida(g.construirComparable("A")));
        assertEquals(1, g.gradoDeEntrada(g.construirComparable("C")));
        assertEquals(2, g.adyacencias(g.construirComparable("A")).size());
        assertEquals(0, g.adyacencias(g.construirComparable("B")).size());
        assertEquals(Integer.valueOf(1), g.obtenerArista("A", "B").dato());
    }

    public void testBidireccionalSonDosAristas() {
        vs("A", "B");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "A", 2);
        assertEquals(2, g.cantidadDeAristas());
    }

    public void testConexidadDebil() {
        vs("A", "B", "C");
        g.agregarArista("A", "B", 1);
        assertFalse(g.esConexo());
        g.agregarArista("C", "B", 1); // C->B: conexo ignorando dirección
        assertTrue(g.esConexo());
    }

    public void testCiclos() {
        vs("A", "B", "C");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("A", "C", 1); // diamante sin ciclo
        assertFalse(g.tieneCiclos());
        g.agregarArista("C", "A", 1);
        assertTrue(g.tieneCiclos());
        g.eliminarArista(g.construirComparable("C"), g.construirComparable("A"));
        assertFalse(g.tieneCiclos());
    }

    public void testAutociclo() {
        vs("A");
        g.agregarArista("A", "A", 1);
        assertTrue(g.tieneCiclos());
    }

    public void testRemoverVerticeLimpiaAristas() {
        vs("A", "B", "C");
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("C", "B", 1);
        assertTrue(g.removerVertice(g.construirComparable("B")));
        assertEquals(2, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
        assertTrue(g.successors(g.construirComparable("A")).isEmpty());
        assertTrue(g.predecessors(g.construirComparable("C")).isEmpty());
    }

    public void testVaciar() {
        vs("A", "B");
        g.agregarArista("A", "B", 1);
        g.vaciar();
        assertEquals(0, g.cantidadDeVertices());
        assertEquals(0, g.cantidadDeAristas());
    }
}
