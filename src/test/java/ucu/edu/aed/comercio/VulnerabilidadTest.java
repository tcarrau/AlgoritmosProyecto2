package ucu.edu.aed.comercio;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.Set;

public class VulnerabilidadTest extends TestCase {
    private SistemaComercio s;
    private Pais uy, ar, br;
    private Producto soja;
    private NodoLogistico u1, u2, a1, b1;

    private Pais pais(String nombre) {
        Pais p = new Pais();
        p.setNombre(nombre);
        s.registrarPais(p);
        return p;
    }

    private NodoLogistico nodo(String nombre, Pais pais) {
        NodoLogistico n = new NodoLogistico();
        n.setNombre(nombre);
        n.setPais(pais);
        n.setTiposCargaAdmitidos(Set.of("granel"));
        n.setActivo(true);
        s.registrarNodo(n);
        return n;
    }

    private void camino(NodoLogistico x, NodoLogistico y) {
        for (NodoLogistico[] par : new NodoLogistico[][]{{x, y}, {y, x}}) {
            Tramo t = new Tramo();
            t.setOrigen(par[0]);
            t.setDestino(par[1]);
            t.setCostoUnitario(BigDecimal.ONE);
            t.setTiempo(BigDecimal.ONE);
            t.setTiposCargaAdmitidos(Set.of("granel"));
            t.setActivo(true);
            s.registrarTramo(t);
        }
    }

    // u2 - u1 - a1 - b1
    @Override
    protected void setUp() {
        s = new SistemaComercio();
        uy = pais("UY");
        ar = pais("AR");
        br = pais("BR");
        for (Pais[] par : new Pais[][]{{uy, ar}, {ar, uy}, {ar, br}, {br, ar}}) {
            par[0].getArancelesGenerales().put(par[1], BigDecimal.ZERO);
        }
        soja = new Producto();
        soja.setNombre("soja");
        soja.setTipoCarga("granel");
        soja.setValorUnitario(BigDecimal.TEN);
        s.registrarProducto(soja);
        u2 = nodo("u2", uy);
        u1 = nodo("u1", uy);
        a1 = nodo("a1", ar);
        b1 = nodo("b1", br);
        camino(u2, u1);
        camino(u1, a1);
        camino(a1, b1);
    }

    public void testNodosDeUruguaySinSalidaSiFallaUnCritico() {
        SistemaComercio.Vulnerabilidad v = s.vulnerabilidad(uy, soja);
        assertEquals(Set.of(u1, a1), v.dependientes().keySet());
        assertEquals(Set.of(u2), v.dependientes().get(u1));
        assertEquals(Set.of(u1, u2), v.dependientes().get(a1));
        assertEquals(2, v.afectados().size());
        assertEquals(2.0, v.excentricidad(), 1e-9);
    }

    public void testPaisCentralNoDependeDeNadie() {
        SistemaComercio.Vulnerabilidad v = s.vulnerabilidad(ar, soja);
        assertTrue(v.afectados().isEmpty());
        assertEquals(2.0, v.excentricidad(), 1e-9);
    }

    public void testNodoHojaDependeDelVecino() {
        SistemaComercio.Vulnerabilidad v = s.vulnerabilidad(br, soja);
        assertEquals(Set.of(a1), v.dependientes().keySet());
        assertEquals(Set.of(b1), v.afectados());
    }

    public void testPaisSinNodosEnLaRed() {
        Pais cl = pais("CL");
        SistemaComercio.Vulnerabilidad v = s.vulnerabilidad(cl, soja);
        assertTrue(v.afectados().isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, v.excentricidad());
    }

    public void testSobreElDataset() throws Exception {
        SistemaComercio d = CargadorDataset.cargarPredeterminado();
        SistemaComercio.Vulnerabilidad v = d.vulnerabilidad(d.getPaises().get("AR"), d.getProductos().get("vino"));
        assertEquals(Set.of(d.getNodos().get("ROSARIO")), v.afectados());
        assertEquals(d.getNodos().get("BSAS"), v.dependientes().keySet().iterator().next());
    }
}
