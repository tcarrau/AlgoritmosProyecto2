package ucu.edu.aed.tda.grafo;

public class DirectedMatrixGraphTest extends DirectedGraphTest {
    @Override
    protected IDirectedIGraph<String, Integer> crear() {
        return new DirectedMatrixGraph<>();
    }

    public void testCrecePorEncimaDeLaCapacidadInicial() {
        for (int i = 0; i < 20; i++) g.agregarVertice("v" + i);
        for (int i = 0; i < 19; i++) g.agregarArista("v" + i, "v" + (i + 1), i);
        assertEquals(19, g.cantidadDeAristas());
        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());
    }

    public void testRemoverConservaLasAristasDelVerticeQueSeMueve() {
        for (String v : new String[]{"A", "B", "C", "D"}) g.agregarVertice(v);
        g.agregarArista("D", "A", 1);
        g.agregarArista("C", "D", 2);
        g.agregarArista("D", "D", 3);
        g.removerVertice(g.construirComparable("A"));
        assertEquals(3, g.cantidadDeVertices());
        assertEquals(2, g.cantidadDeAristas());
        assertTrue(g.existeArista(g.construirComparable("C"), g.construirComparable("D")));
        assertTrue(g.existeArista(g.construirComparable("D"), g.construirComparable("D")));
        assertFalse(g.existeArista(g.construirComparable("D"), g.construirComparable("A")));
    }
}
