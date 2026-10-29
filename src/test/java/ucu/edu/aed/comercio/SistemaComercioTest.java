package ucu.edu.aed.comercio;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SistemaComercioTest extends TestCase {
    private SistemaComercio s;
    private Pais uy, ar, br;
    private Producto soja, hielo;
    private NodoLogistico mvd, bsas, rio;

    private static BigDecimal bd(double d) {
        return BigDecimal.valueOf(d);
    }

    private Pais pais(String nombre) {
        Pais p = new Pais();
        p.setNombre(nombre);
        s.registrarPais(p);
        return p;
    }

    private Producto producto(String nombre, String carga) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setTipoCarga(carga);
        p.setValorUnitario(bd(100));
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

    @Override
    protected void setUp() {
        s = new SistemaComercio();
        uy = pais("UY");
        ar = pais("AR");
        br = pais("BR");
        uy.getArancelesGenerales().put(ar, bd(0.1));
        uy.getArancelesGenerales().put(br, bd(0.2));
        ar.getArancelesGenerales().put(br, bd(0));
        soja = producto("soja", "granel");
        hielo = producto("hielo", "frio");
        mvd = nodo("MVD", uy, "granel", "frio");
        bsas = nodo("BSAS", ar, "granel");
        rio = nodo("RIO", br, "granel", "frio");
    }

    public void testRegistros() {
        assertEquals(3, s.getPaises().size());
        assertEquals(2, s.getProductos().size());
        assertEquals(3, s.getNodos().size());
    }

    public void testFiltraNodosPorCarga() {
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(hielo, "COSTO");
        assertEquals(Set.of(mvd, rio), g.vertices());
    }

    public void testNodoInactivoQuedaFuera() {
        bsas.setActivo(false);
        assertFalse(s.construirGrafo(soja, "COSTO").vertices().contains(bsas));
    }

    public void testCostoIncluyeArancelSoloSiCruzaFrontera() {
        Tramo local = tramo(mvd, nodo("PDE", uy, "granel"), 5, 1, "granel");
        Tramo ext = tramo(mvd, bsas, 5, 1, "granel");
        assertEquals(0, bd(5).compareTo(local.calcularCosto(soja)));
        assertEquals(0, bd(15).compareTo(ext.calcularCosto(soja)));
    }

    public void testParaleloConservaElMasBarato() {
        tramo(mvd, bsas, 50, 1, "granel");
        Tramo barato = tramo(mvd, bsas, 10, 9, "granel");
        tramo(mvd, bsas, 30, 1, "granel");
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(soja, "COSTO");
        assertEquals(1, g.cantidadDeAristas());
        assertSame(barato, g.obtenerArista(mvd, bsas).dato().getTramo());
        assertEquals(1.0, s.construirGrafo(soja, "TIEMPO").obtenerArista(mvd, bsas).dato().getWeight());
    }

    public void testEmbargoYTramoInactivoOCargaIncompatible() {
        tramo(mvd, bsas, 10, 1, "granel");
        tramo(bsas, rio, 10, 1, "frio");
        Tramo off = tramo(mvd, rio, 10, 1, "granel");
        off.setActivo(false);
        uy.getDestinosEmbargados().add(ar);
        assertEquals(0, s.construirGrafo(soja, "COSTO").cantidadDeAristas());
    }

    public void testDijkstraSobreElGrafoDelProducto() {
        tramo(mvd, bsas, 10, 1, "granel");
        tramo(bsas, rio, 10, 1, "granel");
        tramo(mvd, rio, 100, 1, "granel");
        DirectedGraph<NodoLogistico, ArcoTramo> g = s.construirGrafo(soja, "COSTO");
        double directo = 100 + 100 * 0.2;
        double viaBsas = (10 + 100 * 0.1) + (10 + 100 * 0);
        assertEquals(Math.min(directo, viaBsas),
                new ShortestPathAlgorithms().dijkstra(g.construirComparable(mvd), g).getCost(rio), 1e-9);
    }

    public void testCriterioInvalido() {
        try {
            s.construirGrafo(soja, "X");
            fail();
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    public void testRutasEligenCostoOTiempoEIndicanTramosYMedios() {
        Tramo primerTramo = tramo(mvd, bsas, 10, 8, "granel");
        Tramo segundoTramo = tramo(bsas, rio, 10, 8, "granel");
        Tramo directo = tramo(mvd, rio, 100, 1, "granel");
        directo.setMedio("avion");

        assertEquals(List.of(primerTramo, segundoTramo), s.rutaMenorCosto(mvd, rio, soja));
        assertEquals(List.of(directo), s.rutaMenorTiempo(mvd, rio, soja));
        assertEquals("camion", primerTramo.getMedio());
        assertEquals(ar, primerTramo.getDestino().getPais());
        SistemaComercio.DetalleRuta detalle = s.detalleRutaMenorCosto(mvd, rio, soja);
        assertTrue(detalle.alcanzable());
        assertEquals(List.of("camion", "camion"), detalle.medios());
        assertEquals(List.of(uy, ar, br), detalle.paises());
        assertEquals(0, bd(30).compareTo(detalle.costoTotal()));
        assertEquals(0, bd(16).compareTo(detalle.tiempoTotal()));
        assertEquals(List.of(), s.rutaMenorCosto(rio, mvd, soja));
        assertFalse(s.detalleRutaMenorCosto(rio, mvd, soja).alcanzable());
        assertEquals(List.of(), s.rutaMenorCosto(mvd, mvd, soja));
    }

    public void testCostosMinimosIncluyenArancelesYOmitenInalcanzables() {
        tramo(mvd, bsas, 10, 8, "granel");
        tramo(bsas, rio, 10, 8, "granel");
        tramo(mvd, rio, 100, 1, "granel");

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = s.costosMinimosEntreTodos(soja);
        assertEquals(0, bd(30).compareTo(costos.get(mvd).get(rio)));
        assertEquals(0, BigDecimal.ZERO.compareTo(costos.get(mvd).get(mvd)));
        assertFalse(costos.get(rio).containsKey(mvd));
    }

    public void testCentroPriorizaCoberturaYTiempoDeRedActiva() {
        tramo(mvd, bsas, 1, 4, "granel");
        tramo(bsas, rio, 1, 5, "granel");
        tramo(mvd, rio, 1, 20, "granel");
        assertEquals(mvd, s.obtenerCentro());
        mvd.setActivo(false);
        assertEquals(bsas, s.obtenerCentro());
    }

    public void testCentroPorProductoRespetaFiltrosComerciales() {
        tramo(mvd, bsas, 1, 2, "granel");
        tramo(bsas, rio, 1, 2, "granel");
        tramo(mvd, rio, 1, 1, "frio");
        assertEquals(mvd, s.obtenerCentro(soja));
        assertEquals(mvd, s.obtenerCentro(hielo));
        uy.getDestinosEmbargados().add(ar);
        assertEquals(bsas, s.obtenerCentro(soja));
    }

    public void testIntercambioDistingueExportacionesEImportacionesPorSocio() {
        s.registrarIntercambio(uy, ar, soja, bd(10));
        s.registrarIntercambio(ar, uy, hielo, bd(5));
        s.registrarIntercambio(br, uy, soja, bd(2));

        Map<Pais, SistemaComercio.Intercambio> intercambio = s.intercambioComercial(uy);
        assertEquals(Set.of(ar, br), intercambio.keySet());
        assertEquals(Set.of(soja), intercambio.get(ar).exportados());
        assertEquals(Set.of(hielo), intercambio.get(ar).importados());
        assertEquals(Set.of(), intercambio.get(br).exportados());
        assertEquals(Set.of(soja), intercambio.get(br).importados());
    }

    public void testArancelPriorizaProductoLuegoCategoriaYLuegoGeneral() {
        soja.setCategoria("granos");
        Producto maiz = producto("maiz", "granel");
        maiz.setCategoria("granos");
        hielo.setCategoria("otros");
        uy.getArancelesPorCategoria().computeIfAbsent(ar, k -> new java.util.LinkedHashMap<>())
                .put("granos", bd(0.05));
        Tramo cruce = tramo(mvd, bsas, 10, 2, "granel", "frio");

        assertEquals(0, bd(15).compareTo(cruce.calcularCosto(maiz)));
        assertEquals(0, bd(20).compareTo(cruce.calcularCosto(hielo)));
        uy.getArancelesPorProducto().computeIfAbsent(ar, k -> new java.util.LinkedHashMap<>())
                .put(soja, bd(0.02));
        assertEquals(0, bd(12).compareTo(cruce.calcularCosto(soja)));
        assertEquals(0, bd(15).compareTo(cruce.calcularCosto(maiz)));
    }

    public void testImpactoEmbargoYAcuerdoComparaConectividadYCostosSinMutarLaRed() {
        tramo(mvd, bsas, 5, 2, "granel");
        tramo(bsas, rio, 5, 2, "granel");
        SistemaComercio.ParPaises uyBr = new SistemaComercio.ParPaises(uy, br);

        SistemaComercio.ImpactoComercial embargo = s.analizarCambioRelacionComercial(
                uy, ar, soja, "EMBARGO", null);
        assertEquals(0, bd(20).compareTo(embargo.antes().get(uyBr)));
        assertFalse(embargo.despues().containsKey(uyBr));
        assertFalse(uy.getDestinosEmbargados().contains(ar));

        SistemaComercio.ImpactoComercial acuerdo = s.analizarCambioRelacionComercial(
                uy, ar, soja, "TLC", BigDecimal.ZERO);
        assertEquals(0, bd(20).compareTo(acuerdo.antes().get(uyBr)));
        assertEquals(0, bd(10).compareTo(acuerdo.despues().get(uyBr)));
        assertEquals(0, bd(0.1).compareTo(uy.getArancelesGenerales().get(ar)));
        assertTrue(uy.getTiposAcuerdo().get(ar) == null);

        SistemaComercio.ImpactoComercial sancion = s.analizarCambioRelacionComercial(
                uy, ar, soja, "SANCION", null);
        assertFalse(sancion.despues().containsKey(uyBr));
    }

    public void testCacheDeAccesibilidadDetectaCambiosDeTramosYEmbargos() {
        assertFalse(s.puedeEnviarProducto(uy, ar, soja));
        Tramo enlace = tramo(mvd, bsas, 5, 1, "granel");
        assertTrue(s.puedeEnviarProducto(uy, ar, soja));
        enlace.setActivo(false);
        assertFalse(s.puedeEnviarProducto(uy, ar, soja));
        enlace.setActivo(true);
        uy.getDestinosEmbargados().add(ar);
        assertFalse(s.puedeEnviarProducto(uy, ar, soja));
        uy.getDestinosEmbargados().remove(ar);
        assertTrue(s.puedeEnviarProducto(uy, ar, soja));
    }
}
