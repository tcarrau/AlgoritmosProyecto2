package ucu.edu.aed.comercio;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.math.BigDecimal;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsultasExternasTest extends TestCase {
    private SistemaComercio sistema;
    private Pais pais;
    private Producto carga;

    @Override
    protected void setUp() {
        sistema = new SistemaComercio();
        pais = new Pais();
        pais.setNombre("UY");
        sistema.registrarPais(pais);
        carga = producto("soja");
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setCategoria("granos");
        producto.setTipoCarga("granel");
        producto.setValorUnitario(BigDecimal.valueOf(100));
        sistema.registrarProducto(producto);
        return producto;
    }

    private NodoLogistico nodo(String nombre, Pais ubicacion) {
        NodoLogistico nodo = new NodoLogistico();
        nodo.setNombre(nombre);
        nodo.setPais(ubicacion);
        nodo.setActivo(true);
        nodo.setTiposCargaAdmitidos(Set.of("granel"));
        sistema.registrarNodo(nodo);
        return nodo;
    }

    private Tramo tramo(NodoLogistico origen, NodoLogistico destino, int costo) {
        Tramo tramo = new Tramo();
        tramo.setOrigen(origen);
        tramo.setDestino(destino);
        tramo.setActivo(true);
        tramo.setCostoUnitario(BigDecimal.valueOf(costo));
        tramo.setTiempo(BigDecimal.ONE);
        tramo.setTiposCargaAdmitidos(Set.of("granel"));
        sistema.registrarTramo(tramo);
        return tramo;
    }

    public void testTramosCriticosConsideraRutasAlternativasYParalelos() {
        NodoLogistico a = nodo("A", pais);
        NodoLogistico b = nodo("B", pais);
        NodoLogistico c = nodo("C", pais);
        tramo(a, b, 2);
        tramo(a, b, 5);
        Tramo indispensable = tramo(b, c, 3);
        tramo(a, c, 9);

        assertEquals(Set.of(indispensable), sistema.tramosCriticos(carga));
    }

    public void testTramosRedundantesUsaCostoMinimoYExcluyeEmpates() {
        NodoLogistico a = nodo("A", pais);
        NodoLogistico b = nodo("B", pais);
        NodoLogistico c = nodo("C", pais);
        tramo(a, b, 2);
        tramo(b, c, 3);
        Tramo caro = tramo(a, c, 10);
        Tramo paralelo = tramo(a, c, 8);
        tramo(a, c, 5);

        assertEquals(Set.of(caro, paralelo), sistema.tramosDirectosRedundantes(carga));
    }

    public void testBloquesReciprocosIncluyeComponentesYAislados() {
        NodoLogistico a = nodo("A", pais);
        NodoLogistico b = nodo("B", pais);
        NodoLogistico c = nodo("C", pais);
        NodoLogistico d = nodo("D", pais);
        NodoLogistico e = nodo("E", pais);
        tramo(a, b, 1);
        tramo(b, a, 1);
        tramo(b, c, 1);
        tramo(c, d, 1);
        tramo(d, c, 1);

        List<Set<NodoLogistico>> bloques = sistema.bloquesComercioReciproco(carga);
        assertEquals(3, bloques.size());
        assertTrue(bloques.contains(Set.of(a, b)));
        assertTrue(bloques.contains(Set.of(c, d)));
        assertTrue(bloques.contains(Set.of(e)));
    }

    public void testOrdenProductivoRespetaDependenciasYDetectaCiclos() {
        Producto aceite = producto("aceite");
        Producto alimento = producto("alimento");
        sistema.registrarDependenciaProductiva(carga, aceite);
        sistema.registrarDependenciaProductiva(aceite, alimento);

        List<Producto> orden = sistema.consultarOrdenCadenaProductiva();
        assertTrue(orden.indexOf(carga) < orden.indexOf(aceite));
        assertTrue(orden.indexOf(aceite) < orden.indexOf(alimento));

        sistema.registrarDependenciaProductiva(alimento, carga);
        try {
            sistema.consultarOrdenCadenaProductiva();
            fail("Una dependencia cíclica no tiene orden topológico");
        } catch (IllegalStateException esperado) {
            assertTrue(esperado.getMessage().contains("ciclo"));
        }
    }

    public void testDependenciasProductivasPuedenCargarseDesdeDataset() throws Exception {
        SistemaComercio cargado = CargadorDataset.cargar(new StringReader(
                "PRODUCTO|soja|granos|granel|100\n"
              + "PRODUCTO|harina|alimentos|granel|150\n"
              + "DEPENDENCIA|soja|harina\n"));
        List<Producto> orden = cargado.consultarOrdenCadenaProductiva();
        assertEquals("soja", orden.get(0).getNombre());
        assertEquals("harina", orden.get(1).getNombre());
    }

    public void testRedFisicaMinimaConservaElBosqueMasBaratoSinExigirArancel() {
        Pais exterior = new Pais();
        exterior.setNombre("AR");
        sistema.registrarPais(exterior);
        NodoLogistico a = nodo("A", pais);
        NodoLogistico b = nodo("B", exterior);
        NodoLogistico c = nodo("C", exterior);
        NodoLogistico aislado = nodo("D", exterior);
        Tramo ab = tramo(a, b, 5);
        Tramo bc = tramo(b, c, 4);
        tramo(a, c, 20);

        IUndirectedGraph<NodoLogistico, ArcoTramo> minimo = sistema.redFisicaMinima(carga);
        assertEquals(Set.of(a, b, c, aislado), minimo.vertices());
        assertEquals(2, minimo.cantidadDeAristas());
        assertEquals(Set.of(ab, bc), minimo.aristas().stream()
                .map(Edge::dato).map(ArcoTramo::getTramo).collect(Collectors.toSet()));
    }
}
