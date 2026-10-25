package ucu.edu.aed.comercio;

import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;

import java.math.BigDecimal;
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
}
