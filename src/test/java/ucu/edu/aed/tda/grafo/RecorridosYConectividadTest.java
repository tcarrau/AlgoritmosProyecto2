package ucu.edu.aed.tda.grafo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import ucu.edu.aed.comercio.ArnesMedicion;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

public class RecorridosYConectividadTest extends TestCase {

    private DirectedGraphAlgorithms algDirigidos;
    private UndirectedGraphAlgorithm algNoDirigidos;
    private DirectedGraph<String, WeightedEdge> gDir;
    private UndirectedGraph<String, WeightedEdge> gNoDir;

    @Override
    protected void setUp() {
        algDirigidos = new DirectedGraphAlgorithms();
        algNoDirigidos = new UndirectedGraphAlgorithm();
        gDir = new DirectedGraph<>();
        gNoDir = new UndirectedGraph<>();
    }

    public void testCasosBordeGrafoVacioYUnVertice() {
        // Grafo vacío
        List<String> visitados = new ArrayList<>();
        algDirigidos.recorridoEnProfundidad(gDir, gDir.construirComparable("A"), visitados::add);
        algDirigidos.recorridoEnAmplitud(gDir, gDir.construirComparable("A"), visitados::add);
        assertTrue(visitados.isEmpty());

        IFloydWarshallResult<String> wVacio = algDirigidos.warshall(gDir);
        assertFalse(wVacio.connected("A", "B"));
        assertTrue(algDirigidos.calcularClasificacionTopologica(gDir).isEmpty());
        assertTrue(algNoDirigidos.puntosDeArticulacion(gNoDir).isEmpty());

        // Un único vértice
        gDir.agregarVertice("A");
        gNoDir.agregarVertice("A");

        algDirigidos.recorridoEnProfundidad(gDir, gDir.construirComparable("A"), visitados::add);
        assertEquals(List.of("A"), visitados);

        assertTrue(algDirigidos.warshall(gDir).connected("A", "A"));
        assertEquals(List.of("A"), algDirigidos.calcularClasificacionTopologica(gDir));
        assertTrue(algNoDirigidos.puntosDeArticulacion(gNoDir).isEmpty());
    }

    public void testRecorridosDFSyBFSConCiclosYVerticeInexistente() {
        gDir.agregarVertices(List.of("A", "B", "C"));
        gDir.agregarArista("A", "B", new WeightedEdge(1));
        gDir.agregarArista("B", "C", new WeightedEdge(1));
        gDir.agregarArista("C", "A", new WeightedEdge(1)); // Ciclo

        List<String> dfsList = new ArrayList<>();
        algDirigidos.recorridoEnProfundidad(gDir, gDir.construirComparable("A"), dfsList::add);
        assertEquals(List.of("A", "B", "C"), dfsList);

        List<String> bfsList = new ArrayList<>();
        algDirigidos.recorridoEnAmplitud(gDir, gDir.construirComparable("A"), bfsList::add);
        assertEquals(List.of("A", "B", "C"), bfsList);

        // Vértice inexistente no agrega nada
        List<String> inexistente = new ArrayList<>();
        algDirigidos.recorridoEnProfundidad(gDir, gDir.construirComparable("Z"), inexistente::add);
        assertTrue(inexistente.isEmpty());
    }

    public void testWarshallConectividadTransitivaYGrafoNoConexo() {
        gDir.agregarVertices(List.of("A", "B", "C", "D"));
        gDir.agregarArista("A", "B", new WeightedEdge(1));
        gDir.agregarArista("B", "C", new WeightedEdge(1));
        // D queda desconectado

        IFloydWarshallResult<String> res = algDirigidos.warshall(gDir);
        assertTrue(res.connected("A", "C"));
        assertFalse(res.connected("C", "A"));
        assertFalse(res.connected("A", "D"));
    }

    public void testClasificacionTopologicaSinCiclosYConCiclos() {
        gDir.agregarVertices(List.of("Soja", "Harina", "Alimento"));
        gDir.agregarArista("Soja", "Harina", new WeightedEdge(1));
        gDir.agregarArista("Harina", "Alimento", new WeightedEdge(1));

        List<String> orden = algDirigidos.calcularClasificacionTopologica(gDir);
        assertEquals(List.of("Soja", "Harina", "Alimento"), orden);

        // Si agregamos un ciclo, devuelve null
        gDir.agregarArista("Alimento", "Soja", new WeightedEdge(1));
        assertNull(algDirigidos.calcularClasificacionTopologica(gDir));
    }

    public void testPuntosDeArticulacionEnCadenaEnCicloYGrafoNoConexo() {
        // Cadena A - B - C (B es punto de articulación) + componente separada D - E - F (E es articulación)
        gNoDir.agregarVertices(List.of("A", "B", "C", "D", "E", "F"));
        gNoDir.agregarArista("A", "B", new WeightedEdge(1));
        gNoDir.agregarArista("B", "C", new WeightedEdge(1));
        gNoDir.agregarArista("D", "E", new WeightedEdge(1));
        gNoDir.agregarArista("E", "F", new WeightedEdge(1));

        assertEquals(Set.of("B", "E"), algNoDirigidos.puntosDeArticulacion(gNoDir));

        // Si cerramos el triángulo A-B-C con A-C, B deja de ser punto de articulación
        gNoDir.agregarArista("A", "C", new WeightedEdge(1));
        assertEquals(Set.of("E"), algNoDirigidos.puntosDeArticulacion(gNoDir));
    }

    public void testArnesMedicionDesafio3() {
        ArnesMedicion.ResultadoExperimento r = ArnesMedicion.medirEscenario(25, 20);
        assertEquals(25, r.cantidadNodos);
        assertTrue(r.cantidadTramos >= 24);
        assertTrue(r.msSinOptimizar >= 0);
        assertTrue(r.msOptimizadoCache >= 0);
    }
}