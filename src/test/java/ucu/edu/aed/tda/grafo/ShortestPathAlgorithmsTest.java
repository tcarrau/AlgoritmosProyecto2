package ucu.edu.aed.tda.grafo;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.Path;

/**
 * Pruebas de Dijkstra, Floyd, excentricidad, centro del grafo y todos los caminos.
 *
 * <p>Cubre los casos borde que pide la consigna (grafo vacío, un único vértice, grafos no
 * conexos, con y sin ciclos, vértices inexistentes, pesos iguales con caminos alternativos
 * del mismo costo) y además los casos propios de estos algoritmos: pesos negativos, ciclos
 * de costo negativo, bucles, aristas sin peso y grafos no dirigidos.</p>
 */
public class ShortestPathAlgorithmsTest extends TestCase {

    private ShortestPathAlgorithms algoritmos;
    private DirectedGraph<String, WeightedEdge> g;

    @Override
    protected void setUp() {
        algoritmos = new ShortestPathAlgorithms();
        g = new DirectedGraph<>();
    }

    // ---------- utilidades ----------

    private void vs(String... nombres) {
        for (String nombre : nombres) assertTrue(g.agregarVertice(nombre));
    }

    private void arista(String origen, String destino, double peso) {
        assertTrue(g.agregarArista(origen, destino, new WeightedEdge(peso)));
    }

    private Comparable<String> c(String vertice) {
        return g.construirComparable(vertice);
    }

    private List<Double> costosOrdenados(List<Path<String>> caminos) {
        List<Double> costos = new ArrayList<>();
        for (Path<String> camino : caminos) costos.add(camino.getCost());
        costos.sort(null);
        return costos;
    }

    // =======================================================================
    // Grafo vacío y de un único vértice
    // =======================================================================

    public void testGrafoVacioNoTieneCentro() {
        assertNull(algoritmos.obtenerCentroGrafo(g));
    }

    public void testGrafoVacioFloydNoConectaNada() {
        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertFalse(d.connected("A", "B"));
        assertTrue(Double.isInfinite(d.getCost("A", "B")));
        assertTrue(d.getPath("A", "B").isEmpty());
    }

    public void testGrafoVacioDijkstraFallaPorqueNoHayOrigen() {
        try {
            algoritmos.dijkstra(c("A"), g);
            fail("esperaba IllegalArgumentException: el origen no pertenece al grafo");
        } catch (IllegalArgumentException esperada) {
            // ok
        }
    }

    public void testUnSoloVertice() {
        vs("A");

        IDijkstraResult<String> desdeA = algoritmos.dijkstra(c("A"), g);
        assertEquals(0.0, desdeA.getCost("A"), 0.0);
        assertEquals(List.of("A"), desdeA.getPath("A"));

        assertEquals(0.0, algoritmos.obtenerExcentricidad(g, c("A")), 0.0);
        assertEquals("A", algoritmos.obtenerCentroGrafo(g));

        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertEquals(0.0, d.getCost("A", "A"), 0.0);
        assertTrue(d.connected("A", "A"));
        assertEquals(List.of("A"), d.getPath("A", "A"));

        List<Path<String>> caminos = algoritmos.obtenerTodosLosCaminos(c("A"), c("A"), g);
        assertEquals(1, caminos.size());
        assertEquals(List.of("A"), caminos.get(0).getPath());
        assertEquals(0.0, caminos.get(0).getCost(), 0.0);
    }

    // =======================================================================
    // Dijkstra
    // =======================================================================

    public void testDijkstraEligeElCaminoMasBaratoNoElMasCorto() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "C", 2);
        arista("A", "C", 5); // directo pero más caro que A -> B -> C

        IDijkstraResult<String> desdeA = algoritmos.dijkstra(c("A"), g);
        assertEquals(0.0, desdeA.getCost("A"), 0.0);
        assertEquals(1.0, desdeA.getCost("B"), 0.0);
        assertEquals(3.0, desdeA.getCost("C"), 0.0);
        assertEquals(List.of("A", "B", "C"), desdeA.getPath("C"));
    }

    public void testDijkstraRespetaLaDireccionDeLasAristas() {
        vs("A", "B");
        arista("A", "B", 1);

        IDijkstraResult<String> desdeB = algoritmos.dijkstra(c("B"), g);
        assertTrue(Double.isInfinite(desdeB.getCost("A")));
        assertTrue("un camino inexistente debe ser lista vacía", desdeB.getPath("A").isEmpty());
    }

    public void testDijkstraSobreGrafoNoConexo() {
        vs("A", "B", "C");
        arista("A", "B", 1); // C queda aislado

        IDijkstraResult<String> desdeA = algoritmos.dijkstra(c("A"), g);
        assertEquals(1.0, desdeA.getCost("B"), 0.0);
        assertTrue(Double.isInfinite(desdeA.getCost("C")));
        assertTrue(desdeA.getPath("C").isEmpty());
    }

    public void testDijkstraConCiclo() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "C", 1);
        arista("C", "A", 1); // el ciclo no debe hacerlo colgar ni empeorar los costos

        IDijkstraResult<String> desdeA = algoritmos.dijkstra(c("A"), g);
        assertEquals(0.0, desdeA.getCost("A"), 0.0);
        assertEquals(1.0, desdeA.getCost("B"), 0.0);
        assertEquals(2.0, desdeA.getCost("C"), 0.0);
    }

    /**
     * Con dos caminos alternativos del mismo costo el resultado debe ser siempre el mismo:
     * se desempata por el orden de inserción de los vértices.
     */
    public void testDijkstraConPesosIgualesEsDeterministico() {
        vs("A", "B", "C", "D");
        arista("A", "B", 1);
        arista("A", "C", 1);
        arista("B", "D", 1);
        arista("C", "D", 1);

        IDijkstraResult<String> primera = algoritmos.dijkstra(c("A"), g);
        IDijkstraResult<String> segunda = algoritmos.dijkstra(c("A"), g);

        assertEquals(2.0, primera.getCost("D"), 0.0);
        assertEquals(List.of("A", "B", "D"), primera.getPath("D"));
        assertEquals(primera.getPath("D"), segunda.getPath("D"));
    }

    public void testDijkstraVerticeInexistente() {
        vs("A");
        try {
            algoritmos.dijkstra(c("X"), g);
            fail("esperaba IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            // ok
        }
    }

    public void testDijkstraRechazaPesosNegativos() {
        vs("A", "B");
        arista("A", "B", -1);
        try {
            algoritmos.dijkstra(c("A"), g);
            fail("esperaba IllegalArgumentException: Dijkstra no admite pesos negativos");
        } catch (IllegalArgumentException esperada) {
            // ok
        }
    }

    public void testDijkstraAvisaSiUnaAristaNoTienePeso() {
        vs("A", "B");
        assertTrue(g.agregarArista("A", "B", null));
        try {
            algoritmos.dijkstra(c("A"), g);
            fail("esperaba IllegalStateException: la arista no tiene peso");
        } catch (IllegalStateException esperada) {
            // ok
        }
    }

    // =======================================================================
    // Floyd
    // =======================================================================

    public void testFloydCostosEntreTodosLosPares() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "C", 2);
        arista("A", "C", 10);

        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertEquals(3.0, d.getCost("A", "C"), 0.0);
        assertEquals(List.of("A", "B", "C"), d.getPath("A", "C"));
        assertTrue(d.connected("A", "C"));

        assertFalse("las aristas son dirigidas", d.connected("C", "A"));
        assertTrue(Double.isInfinite(d.getCost("C", "A")));
        assertTrue(d.getPath("C", "A").isEmpty());

        assertEquals(0.0, d.getCost("B", "B"), 0.0);
        assertTrue("todo vértice se alcanza a sí mismo con el camino trivial", d.connected("B", "B"));
    }

    public void testFloydReconstruyeCaminosConVariosIntermedios() {
        vs("A", "B", "C", "D", "E");
        arista("A", "B", 1);
        arista("B", "C", 1);
        arista("C", "D", 1);
        arista("D", "E", 1);
        arista("A", "E", 10);

        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertEquals(4.0, d.getCost("A", "E"), 0.0);
        assertEquals(List.of("A", "B", "C", "D", "E"), d.getPath("A", "E"));
    }

    public void testFloydAdmitePesosNegativosSinCicloNegativo() {
        vs("A", "B", "C");
        arista("A", "B", 2);
        arista("B", "C", -1);

        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertEquals(1.0, d.getCost("A", "C"), 0.0);
        assertEquals(List.of("A", "B", "C"), d.getPath("A", "C"));
    }

    public void testFloydDetectaCicloDeCostoNegativo() {
        vs("A", "B");
        arista("A", "B", 1);
        arista("B", "A", -5);
        try {
            algoritmos.floyd(g);
            fail("esperaba IllegalStateException: hay un ciclo de costo negativo");
        } catch (IllegalStateException esperada) {
            // ok
        }
    }

    public void testFloydIgnoraBucleDePesoPositivo() {
        vs("A");
        arista("A", "A", 5);

        IFloydWarshallResult<String> d = algoritmos.floyd(g);
        assertEquals("quedarse quieto cuesta 0, no 5", 0.0, d.getCost("A", "A"), 0.0);
    }

    // =======================================================================
    // Excentricidad y centro
    // =======================================================================

    public void testExcentricidadEsLaMayorDistanciaMinima() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "C", 2);

        assertEquals(3.0, algoritmos.obtenerExcentricidad(g, c("A")), 0.0);
    }

    public void testExcentricidadEsInfinitaSiNoAlcanzaATodos() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "C", 2); // desde B no se llega a A

        assertTrue(Double.isInfinite(algoritmos.obtenerExcentricidad(g, c("B"))));
    }

    public void testExcentricidadVerticeInexistente() {
        vs("A");
        try {
            algoritmos.obtenerExcentricidad(g, c("X"));
            fail("esperaba IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            // ok
        }
    }

    public void testCentroEnGrafoFuertementeConexoEsElDeMenorExcentricidad() {
        vs("A", "B", "C");
        arista("A", "B", 1);
        arista("B", "A", 1);
        arista("B", "C", 1);
        arista("C", "B", 1);
        arista("A", "C", 5);
        arista("C", "A", 5);

        // excentricidades: A = 2, B = 1, C = 2
        assertEquals("B", algoritmos.obtenerCentroGrafo(g));
    }

    /**
     * Caso que rompe la definición clásica: si el grafo no es fuertemente conexo, un vértice
     * que no alcanza a nadie tiene excentricidad 0 y ganaría por menor excentricidad, lo
     * cual no tiene sentido como centro de distribución. Gana el de mayor cobertura.
     */
    public void testCentroPrefiereElQueAlcanzaAMasVertices() {
        vs("A", "B", "C", "D");
        arista("A", "B", 1);
        arista("A", "C", 1); // B, C y D no alcanzan a nadie

        assertEquals("A", algoritmos.obtenerCentroGrafo(g));
    }

    // =======================================================================
    // Todos los caminos
    // =======================================================================

    public void testTodosLosCaminosEncuentraTodasLasAlternativas() {
        vs("A", "B", "C", "D");
        arista("A", "B", 1);
        arista("A", "C", 2);
        arista("B", "C", 1);
        arista("B", "D", 3);
        arista("C", "D", 1);

        List<Path<String>> caminos = algoritmos.obtenerTodosLosCaminos(c("A"), c("D"), g);

        // A-B-D = 4, A-B-C-D = 3, A-C-D = 3
        assertEquals(3, caminos.size());
        assertEquals(List.of(3.0, 3.0, 4.0), costosOrdenados(caminos));

        // el más barato debe coincidir con lo que dice Dijkstra
        assertEquals(algoritmos.dijkstra(c("A"), g).getCost("D"), costosOrdenados(caminos).get(0), 0.0);
    }

    public void testTodosLosCaminosNoRepiteVerticesAunqueHayaCiclo() {
        vs("A", "B", "C", "D");
        arista("A", "B", 1);
        arista("B", "C", 1);
        arista("C", "A", 1); // ciclo A -> B -> C -> A
        arista("C", "D", 1);

        List<Path<String>> caminos = algoritmos.obtenerTodosLosCaminos(c("A"), c("D"), g);
        assertEquals(1, caminos.size());
        assertEquals(List.of("A", "B", "C", "D"), caminos.get(0).getPath());
        assertEquals(3.0, caminos.get(0).getCost(), 0.0);
    }

    public void testTodosLosCaminosCuandoNoHayNinguno() {
        vs("A", "B", "C");
        arista("A", "B", 1);

        assertTrue(algoritmos.obtenerTodosLosCaminos(c("A"), c("C"), g).isEmpty());
    }

    public void testTodosLosCaminosVerticeInexistente() {
        vs("A");
        try {
            algoritmos.obtenerTodosLosCaminos(c("A"), c("X"), g);
            fail("esperaba IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            // ok
        }
    }

    /**
     * Sobre un grafo no dirigido las aristas se recorren en los dos sentidos. Importa porque
     * {@code UndirectedEdge} guarda el orden con que fue creada, así que no se puede asumir
     * que {@code source()} sea el vértice desde el que se está avanzando.
     */
    public void testTodosLosCaminosEnGrafoNoDirigido() {
        UndirectedGraph<String, WeightedEdge> ng = new UndirectedGraph<>();
        ng.agregarVertice("A");
        ng.agregarVertice("B");
        ng.agregarVertice("C");
        assertTrue(ng.agregarArista("A", "B", new WeightedEdge(1)));
        assertTrue(ng.agregarArista("B", "C", new WeightedEdge(1)));
        assertTrue(ng.agregarArista("A", "C", new WeightedEdge(5)));

        List<Path<String>> caminos = algoritmos.obtenerTodosLosCaminos(
                ng.construirComparable("A"), ng.construirComparable("C"), ng);

        // A-C = 5 y A-B-C = 2
        assertEquals(2, caminos.size());
        assertEquals(List.of(2.0, 5.0), costosOrdenados(caminos));
    }
}
