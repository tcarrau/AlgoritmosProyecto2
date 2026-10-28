package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.Map;

import junit.framework.TestCase;
import ucu.edu.aed.comercio.AnalisisCostos.Motor;

/**
 * Pruebas del Desafio 3. Lo que hay que demostrar es que la optimizacion no cambia el
 * resultado: los dos motores y la version cacheada tienen que coincidir con la
 * implementacion original de {@link SistemaComercio#costosMinimosEntreTodos(Producto)}.
 *
 * <p>La otra mitad prueba la invalidacion, que es la parte riesgosa de cachear: un cache
 * que devuelve un resultado viejo despues de una sancion es peor que no tener cache.</p>
 */
public class AnalisisCostosTest extends TestCase {

    private SistemaComercio sistema;
    private Producto soja;

    @Override
    protected void setUp() throws Exception {
        sistema = CargadorDataset.cargarPredeterminado();
        soja = sistema.getProductos().get("soja");
    }

    /** Compara dos resultados par por par, incluyendo que falten los mismos pares. */
    private static void assertMismosCostos(String mensaje,
                                           Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> esperado,
                                           Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> obtenido) {
        assertEquals(mensaje + ": distinta cantidad de filas", esperado.size(), obtenido.size());
        for (NodoLogistico desde : esperado.keySet()) {
            Map<NodoLogistico, BigDecimal> filaEsperada = esperado.get(desde);
            Map<NodoLogistico, BigDecimal> filaObtenida = obtenido.get(desde);
            assertNotNull(mensaje + ": falta la fila de " + desde, filaObtenida);
            assertEquals(mensaje + ": distinta cantidad de destinos desde " + desde,
                    filaEsperada.size(), filaObtenida.size());
            for (NodoLogistico hasta : filaEsperada.keySet()) {
                BigDecimal unoEsperado = filaEsperada.get(hasta);
                BigDecimal unoObtenido = filaObtenida.get(hasta);
                assertNotNull(mensaje + ": falta " + desde + "->" + hasta, unoObtenido);
                assertEquals(mensaje + ": " + desde + "->" + hasta, 0, unoEsperado.compareTo(unoObtenido));
            }
        }
    }

    // =======================================================================
    // Equivalencia con la implementacion original
    // =======================================================================

    public void testFloydCoincideConLaImplementacionOriginal() {
        assertMismosCostos("floyd",
                sistema.costosMinimosEntreTodos(soja),
                new AnalisisCostos(sistema, Motor.FLOYD, false).costosMinimosEntreTodos(soja));
    }

    public void testDijkstraCoincideConLaImplementacionOriginal() {
        assertMismosCostos("dijkstra",
                sistema.costosMinimosEntreTodos(soja),
                new AnalisisCostos(sistema, Motor.DIJKSTRA, false).costosMinimosEntreTodos(soja));
    }

    public void testDijkstraConIndiceUnicoCoincideConLaImplementacionOriginal() {
        assertMismosCostos("dijkstra con un solo indice",
                sistema.costosMinimosEntreTodos(soja),
                new AnalisisCostos(sistema, Motor.DIJKSTRA_INDICE_UNICO, false).costosMinimosEntreTodos(soja));
    }

    public void testTodosLosMotoresCoincidenParaTodosLosProductos() {
        for (Producto producto : sistema.getProductos().values()) {
            Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> referencia =
                    sistema.costosMinimosEntreTodos(producto);
            for (Motor motor : Motor.values()) {
                assertMismosCostos(motor + " con " + producto.getNombre(), referencia,
                        new AnalisisCostos(sistema, motor, false).costosMinimosEntreTodos(producto));
            }
        }
    }

    public void testTodosLosMotoresCoincidenSobreUnaRedSintetica() {
        SistemaComercio sintetica = MedicionCostosMinimos.generar(60, 6, 4, 7);
        Producto producto = sintetica.getProductos().get("soja");
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> referencia =
                sintetica.costosMinimosEntreTodos(producto);
        for (Motor motor : Motor.values()) {
            assertMismosCostos("sintetica con " + motor, referencia,
                    new AnalisisCostos(sintetica, motor, false).costosMinimosEntreTodos(producto));
        }
    }

    // =======================================================================
    // Cache
    // =======================================================================

    public void testElCacheNoRecalculaSiLaRedNoCambio() {
        AnalisisCostos analisis = new AnalisisCostos(sistema, Motor.DIJKSTRA);

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> primera = analisis.costosMinimosEntreTodos(soja);
        for (int i = 0; i < 5; i++) {
            assertMismosCostos("consulta repetida", primera, analisis.costosMinimosEntreTodos(soja));
        }

        assertEquals("deberia haber calculado una sola vez", 1, analisis.getRecalculos());
        assertEquals(0, analisis.getInvalidaciones());
    }

    public void testElCacheGuardaUnResultadoPorProducto() {
        AnalisisCostos analisis = new AnalisisCostos(sistema, Motor.DIJKSTRA);
        analisis.costosMinimosEntreTodos(soja);
        analisis.costosMinimosEntreTodos(sistema.getProductos().get("vino"));
        analisis.costosMinimosEntreTodos(soja);

        assertEquals(2, analisis.getRecalculos());
        assertEquals("un producto nuevo no invalida lo guardado", 0, analisis.getInvalidaciones());
    }

    public void testElCacheSeInvalidaAlCerrarUnNodo() {
        AnalisisCostos analisis = new AnalisisCostos(sistema, Motor.DIJKSTRA);
        NodoLogistico santos = sistema.getNodos().get("SANTOS");
        NodoLogistico houston = sistema.getNodos().get("HOUSTON");
        NodoLogistico shanghai = sistema.getNodos().get("SHANGHAI");

        // Houston solo llega a Shanghai triangulando por Santos, por el embargo de US a CN
        assertNotNull(analisis.costosMinimosEntreTodos(soja).get(houston).get(shanghai));

        santos.setActivo(false);

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> despues = analisis.costosMinimosEntreTodos(soja);
        assertEquals("cerrar un puerto tiene que invalidar el cache", 1, analisis.getInvalidaciones());
        assertEquals(2, analisis.getRecalculos());
        assertNull("sin Santos, Houston ya no llega a Shanghai", despues.get(houston).get(shanghai));
        assertMismosCostos("recalculado contra original", sistema.costosMinimosEntreTodos(soja), despues);
    }

    /**
     * El caso que justifica invalidar por huella y no por un contador de version: el embargo
     * se agrega directamente sobre el conjunto que devuelve el getter, sin pasar por ningun
     * metodo de {@link SistemaComercio}, asi que un contador no se enteraria.
     */
    public void testElCacheSeInvalidaAlAgregarUnEmbargoPorElGetter() {
        AnalisisCostos analisis = new AnalisisCostos(sistema, Motor.DIJKSTRA);
        NodoLogistico mvd = sistema.getNodos().get("MVD");
        NodoLogistico bsas = sistema.getNodos().get("BSAS");

        BigDecimal antes = analisis.costosMinimosEntreTodos(soja).get(mvd).get(bsas);
        assertEquals("el barco directo, sin arancel por el MERCOSUR", 0, BigDecimal.valueOf(30).compareTo(antes));

        sistema.getPaises().get("UY").getDestinosEmbargados().add(sistema.getPaises().get("AR"));

        BigDecimal despues = analisis.costosMinimosEntreTodos(soja).get(mvd).get(bsas);
        assertEquals("el embargo tiene que invalidar el cache", 1, analisis.getInvalidaciones());
        // el embargo corta el tramo directo, pero la carga todavia llega triangulando por
        // Hamburgo, que comercia con Argentina: el costo sube de 30 a 265
        assertTrue("sin el tramo directo la ruta tiene que encarecerse",
                despues.compareTo(antes) > 0);
        assertEquals(0, BigDecimal.valueOf(265).compareTo(despues));
    }

    public void testElCacheSeInvalidaAlCambiarUnArancel() {
        AnalisisCostos analisis = new AnalisisCostos(sistema, Motor.DIJKSTRA);
        NodoLogistico mvd = sistema.getNodos().get("MVD");
        NodoLogistico bsas = sistema.getNodos().get("BSAS");

        BigDecimal antes = analisis.costosMinimosEntreTodos(soja).get(mvd).get(bsas);

        sistema.getPaises().get("UY").getArancelesGenerales()
                .put(sistema.getPaises().get("AR"), BigDecimal.valueOf(0.5));

        BigDecimal despues = analisis.costosMinimosEntreTodos(soja).get(mvd).get(bsas);
        assertEquals(1, analisis.getInvalidaciones());
        assertTrue("subir el arancel tiene que encarecer la ruta", despues.compareTo(antes) > 0);
    }

    // =======================================================================
    // Huella
    // =======================================================================

    public void testLaHuellaNoCambiaSiLaRedNoCambia() {
        assertEquals(AnalisisCostos.huellaDe(sistema), AnalisisCostos.huellaDe(sistema));
    }

    public void testLaHuellaCambiaConCadaTipoDeModificacion() {
        long inicial = AnalisisCostos.huellaDe(sistema);

        sistema.getNodos().get("MVD").setActivo(false);
        long trasCerrarNodo = AnalisisCostos.huellaDe(sistema);
        assertTrue("cerrar un nodo", trasCerrarNodo != inicial);

        sistema.getTramos().get(0).setActivo(false);
        long trasCerrarTramo = AnalisisCostos.huellaDe(sistema);
        assertTrue("cerrar un tramo", trasCerrarTramo != trasCerrarNodo);

        sistema.getPaises().get("UY").getProductosRestringidos()
                .computeIfAbsent(sistema.getPaises().get("AR"), k -> new java.util.LinkedHashSet<>())
                .add(soja);
        long trasRestriccion = AnalisisCostos.huellaDe(sistema);
        assertTrue("agregar una restriccion", trasRestriccion != trasCerrarTramo);

        sistema.getPaises().get("UY").getArancelesPorProducto()
                .computeIfAbsent(sistema.getPaises().get("BR"), k -> new java.util.LinkedHashMap<>())
                .put(soja, BigDecimal.valueOf(0.42));
        assertTrue("agregar un arancel por producto", AnalisisCostos.huellaDe(sistema) != trasRestriccion);
    }
}
