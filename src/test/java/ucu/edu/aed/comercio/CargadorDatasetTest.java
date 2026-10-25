package ucu.edu.aed.comercio;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;

import java.io.StringReader;

public class CargadorDatasetTest extends TestCase {
    private SistemaComercio s;

    @Override
    protected void setUp() throws Exception {
        s = CargadorDataset.cargarPredeterminado();
    }

    public void testCantidades() {
        assertEquals(7, s.getPaises().size());
        assertEquals(5, s.getProductos().size());
        assertEquals(14, s.getNodos().size());
        assertEquals(42, s.getTramos().size());
    }

    public void testEmbargoEsAsimetrico() {
        Producto maq = s.getProductos().get("maquinaria");
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(maq, "COSTO");
        NodoLogistico sh = s.getNodos().get("SHANGHAI"), hou = s.getNodos().get("HOUSTON");
        assertNull(g.obtenerArista(hou, sh));
        assertNotNull(g.obtenerArista(sh, hou));
    }

    public void testFiltroPorCarga() {
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(s.getProductos().get("carne"), "TIEMPO");
        assertFalse(g.vertices().contains(s.getNodos().get("ROSARIO")));
        assertTrue(g.vertices().contains(s.getNodos().get("MENDOZA")));
    }

    public void testRutaMultimodalDeUruguayAChina() {
        Producto carne = s.getProductos().get("carne");
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(carne, "TIEMPO");
        NodoLogistico mvd = s.getNodos().get("MVD");
        double horas = new ShortestPathAlgorithms()
                .dijkstra(g.construirComparable(mvd), g).getCost(s.getNodos().get("SHANGHAI"));
        assertEquals(30.0, horas, 1e-9);
    }

    public void testRestriccionDeProducto() {
        Producto carne = s.getProductos().get("carne");
        NodoLogistico stos = s.getNodos().get("SANTOS"), ham = s.getNodos().get("HAMBURGO");
        assertNull(s.construirGrafo(carne, "COSTO").obtenerArista(stos, ham));
        Producto soja = s.getProductos().get("soja");
        assertNotNull(s.construirGrafo(soja, "COSTO").obtenerArista(stos, ham));
    }

    public void testReferenciaInexistente() throws Exception {
        try {
            CargadorDataset.cargar(new StringReader("NODO|X|puerto|ZZ|granel"));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Línea 1"));
        }
    }
}
