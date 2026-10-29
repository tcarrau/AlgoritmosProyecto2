package ucu.edu.aed.tda.grafo;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.Set;

public class DirectedGraphAccesoDirectoTest extends TestCase {
    // No implementa Comparable: obliga a llamar a las sobrecargas (V,V).
    private record Vertice(String nombre) {}

    private DirectedGraph<Vertice, Integer> g;
    private Vertice a, b, c;

    @Override
    protected void setUp() {
        g = new DirectedGraph<>();
        a = new Vertice("A");
        b = new Vertice("B");
        c = new Vertice("C");
        g.agregarVertice(a);
        g.agregarVertice(b);
        g.agregarVertice(c);
        g.agregarArista(a, b, 7);
    }

    public void testConsultaDirectaConAristaExistente() {
        Edge<Vertice, Integer> arista = g.obtenerArista(a, b);
        assertNotNull(arista);
        assertSame(a, arista.source());
        assertSame(b, arista.target());
        assertEquals(Integer.valueOf(7), arista.dato());
    }

    public void testConsultaDirectaConVerticesAisladosInexistentesYNulos() {
        Vertice ausente = new Vertice("X");
        assertNull(g.obtenerArista(b, a));
        assertNull(g.obtenerArista(c, b));
        assertNull(g.obtenerArista(a, ausente));
        assertNull(g.obtenerArista(ausente, b));
        assertNull(g.obtenerArista(null, b));
        assertNull(g.obtenerArista(a, null));
    }

    public void testConsultaDirectaConObjetosIgualesPeroDistintos() {
        Edge<Vertice, Integer> arista = g.obtenerArista(new Vertice("A"), new Vertice("B"));
        assertNotNull(arista);
        assertSame(a, arista.source());
        assertSame(b, arista.target());
    }

    public void testEliminacionDirectaActualizaAmbasAdyacencias() {
        assertTrue(g.eliminarArista(a, b));
        assertNull(g.obtenerArista(a, b));
        assertEquals(0, g.cantidadDeAristas());
        assertTrue(g.successors(g.construirComparable(a)).isEmpty());
        assertTrue(g.predecessors(g.construirComparable(b)).isEmpty());
        assertFalse(g.eliminarArista(a, b));
        assertTrue(g.agregarArista(a, b, 8));
        assertEquals(Integer.valueOf(8), g.obtenerArista(a, b).dato());
    }

    public void testEliminacionDirectaConVerticesAusentesNulosYAristaAusente() {
        Vertice ausente = new Vertice("X");
        assertFalse(g.eliminarArista(b, a));
        assertFalse(g.eliminarArista(ausente, b));
        assertFalse(g.eliminarArista(a, ausente));
        assertFalse(g.eliminarArista(null, b));
        assertFalse(g.eliminarArista(a, null));
        assertNotNull(g.obtenerArista(a, b));
    }

    public void testEliminacionDirectaConObjetosIgualesPeroDistintos() {
        assertTrue(g.eliminarArista(new Vertice("A"), new Vertice("B")));
        assertNull(g.obtenerArista(a, b));
        assertTrue(g.predecessors(g.construirComparable(b)).isEmpty());
    }

    public void testEliminacionNoBorraOtrosPredecesores() {
        assertTrue(g.agregarArista(c, b, 9));
        assertTrue(g.eliminarArista(a, b));
        assertEquals(Set.of(c), g.predecessors(g.construirComparable(b)));
        assertEquals(Integer.valueOf(9), g.obtenerArista(c, b).dato());
    }

    public void testBucleSobreElMismoVertice() {
        assertTrue(g.agregarArista(c, c, 4));
        assertEquals(Integer.valueOf(4), g.obtenerArista(c, c).dato());
        assertTrue(g.eliminarArista(c, c));
        assertNull(g.obtenerArista(c, c));
        assertTrue(g.predecessors(g.construirComparable(c)).isEmpty());
    }

    public void testSobrecargasConComparableSiguenFuncionando() {
        Comparable<Vertice> criterioA = g.construirComparable(a);
        Comparable<Vertice> criterioB = g.construirComparable(b);
        assertEquals(Integer.valueOf(7), g.obtenerArista(criterioA, criterioB).dato());
        assertTrue(g.eliminarArista(criterioA, criterioB));
        assertNull(g.obtenerArista(a, b));
    }
}
