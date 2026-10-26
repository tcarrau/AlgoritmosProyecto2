package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Pruebas de las operaciones de rutas y costos: ruta de menor costo, ruta de menor
 * tiempo, costos minimos entre todos los pares y centro de distribucion.
 *
 * <p>La primera mitad usa una red armada a mano, chica, donde la aritmetica de los
 * aranceles se puede verificar en papel. La segunda mitad corre sobre el dataset de
 * ejemplo, para comprobar que las decisiones de modelado se sostienen en una red
 * realista: productos que no pueden entrar a un nodo, restricciones que obligan a
 * triangular por un tercer pais y embargos que valen en un solo sentido.</p>
 */
public class SistemaComercioRutasTest extends TestCase {

    private SistemaComercio s;
    private Pais uy, ar, br, pe;
    private Producto soja, hielo;
    private NodoLogistico mvd, bsas, rio, lima;

    private static BigDecimal bd(double d) {
        return BigDecimal.valueOf(d);
    }

    private Pais pais(String nombre) {
        Pais p = new Pais();
        p.setNombre(nombre);
        s.registrarPais(p);
        return p;
    }

    private Producto producto(String nombre, String carga, double valor) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setTipoCarga(carga);
        p.setValorUnitario(bd(valor));
        s.registrarProducto(p);
        return p;
    }

    private NodoLogistico nodo(String nombre, Pais pais, String... cargas) {
        NodoLogistico n = new NodoLogistico();
        n.setNombre(nombre);
        n.setPais(pais);
        n.setTiposCargaAdmitidos(Set.of(cargas));
        n.setActivo(true);
        s.registrarNodo(n);
        return n;
    }

    private Tramo tramo(NodoLogistico o, NodoLogistico d, double costo, double tiempo, String... cargas) {
        Tramo t = new Tramo();
        t.setOrigen(o);
        t.setDestino(d);
        t.setMedio("camion");
        t.setCostoUnitario(bd(costo));
        t.setTiempo(bd(tiempo));
        t.setTiposCargaAdmitidos(Set.of(cargas));
        t.setActivo(true);
        s.registrarTramo(t);
        return t;
    }

    /**
     * Red de prueba. Con soja (valor 100) los costos quedan asi:
     * MVD->BSAS = 10 + 100*0.10 = 20 ; BSAS->RIO = 10 + 100*0 = 10 ; MVD->RIO = 100 + 100*0.20 = 120.
     * Entonces el camino barato es MVD->BSAS->RIO (30) y el rapido es MVD->RIO (1 contra 2).
     */
    @Override
    protected void setUp() {
        s = new SistemaComercio();
        uy = pais("UY");
        ar = pais("AR");
        br = pais("BR");
        pe = pais("PE");
        uy.getArancelesGenerales().put(ar, bd(0.1));
        uy.getArancelesGenerales().put(br, bd(0.2));
        ar.getArancelesGenerales().put(br, bd(0));

        soja = producto("soja", "granel", 100);
        hielo = producto("hielo", "frio", 100);

        mvd = nodo("MVD", uy, "granel", "frio");
        bsas = nodo("BSAS", ar, "granel");
        rio = nodo("RIO", br, "granel");
        lima = nodo("LIMA", pe, "granel"); // sin tramos: inalcanzable

        tramo(mvd, bsas, 10, 1, "granel");
        tramo(bsas, rio, 10, 1, "granel");
        tramo(mvd, rio, 100, 1, "granel");
    }

    private static List<String> nombres(List<Tramo> ruta) {
        if (ruta.isEmpty()) {
            return List.of();
        }
        List<String> paso = new ArrayList<>();
        paso.add(ruta.get(0).getOrigen().getNombre());
        for (Tramo t : ruta) {
            paso.add(t.getDestino().getNombre());
        }
        return paso;
    }

    // =======================================================================
    // Red armada a mano
    // =======================================================================

    public void testRutaMenorCostoIncluyeLosAranceles() {
        List<Tramo> ruta = s.rutaMenorCosto(mvd, rio, soja);

        // el directo es mas corto en tramos pero mas caro, y el arancel desempata
        assertEquals(List.of("MVD", "BSAS", "RIO"), nombres(ruta));
        assertEquals(0, bd(30).compareTo(s.costoTotal(ruta, soja)));
    }

    public void testRutaMenorTiempoPuedeNoSerLaDeMenorCosto() {
        assertEquals(List.of("MVD", "RIO"), nombres(s.rutaMenorTiempo(mvd, rio, soja)));
        assertEquals(List.of("MVD", "BSAS", "RIO"), nombres(s.rutaMenorCosto(mvd, rio, soja)));
    }

    public void testSinRutaDevuelveListaVacia() {
        assertTrue(s.rutaMenorCosto(mvd, lima, soja).isEmpty());
        assertTrue(s.rutaMenorTiempo(mvd, lima, soja).isEmpty());
    }

    public void testRutaHaciaAtrasNoExistePorqueLosTramosSonDirigidos() {
        assertTrue(s.rutaMenorCosto(rio, mvd, soja).isEmpty());
    }

    public void testOrigenIgualDestinoNoTieneTramos() {
        assertTrue(s.rutaMenorCosto(mvd, mvd, soja).isEmpty());
    }

    public void testNodoQueNoAdmiteLaCargaQuedaFueraDeLaRuta() {
        // BSAS solo admite granel, asi que con hielo no hay por donde pasar
        assertTrue(s.rutaMenorCosto(mvd, bsas, hielo).isEmpty());
    }

    public void testCostoTotalSumaCadaTramoConSuArancel() {
        assertEquals(0, bd(0).compareTo(s.costoTotal(List.of(), soja)));
        assertEquals(0, bd(30).compareTo(s.costoTotal(s.rutaMenorCosto(mvd, rio, soja), soja)));
    }

    public void testCostosMinimosEntreTodosLosPares() {
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = s.costosMinimosEntreTodos(soja);

        assertEquals(0, bd(0).compareTo(costos.get(mvd).get(mvd)));
        assertEquals("debe elegir el camino barato, no el directo",
                0, bd(30).compareTo(costos.get(mvd).get(rio)));
        assertEquals(0, bd(20).compareTo(costos.get(mvd).get(bsas)));
        assertNull("un par sin ruta no aparece en el mapa", costos.get(mvd).get(lima));
        assertNull("los tramos son dirigidos", costos.get(rio).get(mvd));
    }

    public void testCostosMinimosCoincidenConLaRutaDevuelta() {
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = s.costosMinimosEntreTodos(soja);
        for (NodoLogistico origen : costos.keySet()) {
            for (NodoLogistico destino : costos.keySet()) {
                BigDecimal porFloyd = costos.get(origen).get(destino);
                List<Tramo> ruta = s.rutaMenorCosto(origen, destino, soja);
                if (porFloyd == null) {
                    assertTrue(origen + "->" + destino + " sin costo pero con ruta", ruta.isEmpty());
                } else {
                    assertEquals(origen + "->" + destino,
                            0, porFloyd.compareTo(s.costoTotal(ruta, soja)));
                }
            }
        }
    }

    public void testCentroEsElNodoQueAlcanzaAMasNodos() {
        // MVD alcanza 2, BSAS 1, RIO y LIMA ninguno
        assertEquals(mvd, s.obtenerCentro());
    }

    public void testCentroIgnoraNodosInactivos() {
        mvd.setActivo(false);
        assertEquals(bsas, s.obtenerCentro());
    }

    public void testCentroSinNodosActivosEsNull() {
        assertNull(new SistemaComercio().obtenerCentro());
    }

    // =======================================================================
    // Dataset de ejemplo
    // =======================================================================

    private SistemaComercio dataset() throws Exception {
        return CargadorDataset.cargarPredeterminado();
    }

    public void testDatasetRutaMenorCostoDelVinoEsElBarcoDirecto() throws Exception {
        SistemaComercio d = dataset();
        List<Tramo> ruta = d.rutaMenorCosto(d.getNodos().get("MVD"), d.getNodos().get("HAMBURGO"),
                d.getProductos().get("vino"));

        assertEquals(List.of("MVD", "HAMBURGO"), nombres(ruta));
        assertEquals("barco", ruta.get(0).getMedio());
        // 115 de transporte + 4% de arancel sobre un vino de 3000
        assertEquals(0, bd(235).compareTo(d.costoTotal(ruta, d.getProductos().get("vino"))));
    }

    public void testDatasetRutaMenorTiempoDelVinoUsaElAvionYSaleMasCara() throws Exception {
        SistemaComercio d = dataset();
        Producto vino = d.getProductos().get("vino");
        NodoLogistico mvdD = d.getNodos().get("MVD"), hamburgo = d.getNodos().get("HAMBURGO");

        List<Tramo> porTiempo = d.rutaMenorTiempo(mvdD, hamburgo, vino);
        List<Tramo> porCosto = d.rutaMenorCosto(mvdD, hamburgo, vino);

        assertTrue("la ruta rapida deberia usar el tramo aereo",
                porTiempo.stream().anyMatch(t -> t.getMedio().equals("avion")));
        assertTrue("y deberia ser mas caro que ir por barco",
                d.costoTotal(porTiempo, vino).compareTo(d.costoTotal(porCosto, vino)) > 0);

        BigDecimal tiempoRapido = BigDecimal.ZERO, tiempoBarato = BigDecimal.ZERO;
        for (Tramo t : porTiempo) tiempoRapido = tiempoRapido.add(t.getTiempo());
        for (Tramo t : porCosto) tiempoBarato = tiempoBarato.add(t.getTiempo());
        assertTrue("y mas rapido", tiempoRapido.compareTo(tiempoBarato) < 0);
    }

    public void testDatasetProductoQueElDestinoNoAdmiteNoTieneRuta() throws Exception {
        SistemaComercio d = dataset();
        // BERLIN solo admite contenedor y la soja viaja a granel
        assertTrue(d.rutaMenorCosto(d.getNodos().get("MVD"), d.getNodos().get("BERLIN"),
                d.getProductos().get("soja")).isEmpty());
    }

    public void testDatasetUnaRestriccionObligaATriangular() throws Exception {
        SistemaComercio d = dataset();
        // Brasil tiene restringida la carne hacia Alemania, asi que no puede ir directo
        List<Tramo> ruta = d.rutaMenorCosto(d.getNodos().get("SANTOS"), d.getNodos().get("HAMBURGO"),
                d.getProductos().get("carne"));

        assertEquals(List.of("SANTOS", "MVD", "HAMBURGO"), nombres(ruta));
        assertEquals(0, bd(360).compareTo(d.costoTotal(ruta, d.getProductos().get("carne"))));
    }

    public void testDatasetElEmbargoValeEnUnSoloSentido() throws Exception {
        SistemaComercio d = dataset();
        Producto soja = d.getProductos().get("soja");
        NodoLogistico houston = d.getNodos().get("HOUSTON"), shanghai = d.getNodos().get("SHANGHAI");

        // Estados Unidos tiene embargo hacia China: el tramo directo no se puede usar
        assertEquals(List.of("HOUSTON", "SANTOS", "SHANGHAI"), nombres(d.rutaMenorCosto(houston, shanghai, soja)));
        // pero en el sentido contrario el tramo directo sigue disponible
        assertEquals(List.of("SHANGHAI", "HOUSTON"), nombres(d.rutaMenorCosto(shanghai, houston, soja)));
    }

    public void testDatasetCostosMinimosCoincidenConLasRutas() throws Exception {
        SistemaComercio d = dataset();
        Producto vino = d.getProductos().get("vino");
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = d.costosMinimosEntreTodos(vino);

        int pares = 0;
        for (NodoLogistico origen : costos.keySet()) {
            for (NodoLogistico destino : costos.keySet()) {
                pares++;
                BigDecimal porFloyd = costos.get(origen).get(destino);
                List<Tramo> ruta = d.rutaMenorCosto(origen, destino, vino);
                if (porFloyd == null) {
                    assertTrue(origen + "->" + destino, ruta.isEmpty());
                } else {
                    assertEquals(origen + "->" + destino, 0, porFloyd.compareTo(d.costoTotal(ruta, vino)));
                }
            }
        }
        assertTrue("deberia haber comparado varios pares", pares > 100);
    }

    public void testDatasetCentroDeLaRedFisicaYPorProducto() throws Exception {
        SistemaComercio d = dataset();
        // sobre la red completa, sin aranceles, gana MVD por la cantidad de salidas directas
        assertEquals("MVD", d.obtenerCentro().getNombre());
        // con los aranceles y los filtros de carga del producto, gana SANTOS
        assertEquals("SANTOS", d.obtenerCentro(d.getProductos().get("soja")).getNombre());
    }
}
