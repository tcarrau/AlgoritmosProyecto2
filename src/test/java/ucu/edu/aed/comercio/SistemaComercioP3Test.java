package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class SistemaComercioP3Test extends TestCase {

    private SistemaComercio sistema;
    private Pais uruguay, argentina, brasil, espana;
    private NodoLogistico montevideo, buenosAires, santos, madrid;
    private Producto soja, carne;

    @Override
    protected void setUp() {
        sistema = new SistemaComercio();

        uruguay = crearPais("Uruguay");
        argentina = crearPais("Argentina");
        brasil = crearPais("Brasil");
        espana = crearPais("Espana");

        soja = crearProducto("Soja", "Granos", "GRANEL");
        carne = crearProducto("Carne", "Alimentos", "REFRIGERADO");

        montevideo = crearNodo("Puerto Montevideo", "PUERTO", uruguay, Set.of("GRANEL", "REFRIGERADO"));
        buenosAires = crearNodo("Puerto Buenos Aires", "PUERTO", argentina, Set.of("GRANEL", "REFRIGERADO"));
        santos = crearNodo("Puerto Santos", "PUERTO", brasil, Set.of("GRANEL", "REFRIGERADO"));
        madrid = crearNodo("Terminal Madrid", "CIUDAD", espana, Set.of("GRANEL", "REFRIGERADO"));

        crearTramo(buenosAires, montevideo, "MARITIMO", 100, 10, Set.of("GRANEL", "REFRIGERADO"));
        crearTramo(montevideo, santos, "MARITIMO", 200, 20, Set.of("GRANEL", "REFRIGERADO"));
        crearTramo(santos, madrid, "MARITIMO", 500, 50, Set.of("GRANEL", "REFRIGERADO"));

        argentina.getArancelesGenerales().put(uruguay, BigDecimal.ZERO);
        argentina.getArancelesGenerales().put(espana, BigDecimal.ZERO);
        uruguay.getArancelesGenerales().put(brasil, BigDecimal.ZERO);
        brasil.getArancelesGenerales().put(espana, BigDecimal.ZERO);
    }

    private Pais crearPais(String nombre) {
        Pais p = new Pais();
        p.setNombre(nombre);
        sistema.registrarPais(p);
        return p;
    }

    private Producto crearProducto(String nombre, String categoria, String tipoCarga) {
        Producto pr = new Producto();
        pr.setNombre(nombre);
        pr.setCategoria(categoria);
        pr.setTipoCarga(tipoCarga);
        pr.setValorUnitario(BigDecimal.valueOf(100));
        sistema.registrarProducto(pr);
        return pr;
    }

    private NodoLogistico crearNodo(String nombre, String tipo, Pais pais, Set<String> cargas) {
        NodoLogistico n = new NodoLogistico();
        n.setNombre(nombre);
        n.setTipo(tipo);
        n.setPais(pais);
        n.setTiposCargaAdmitidos(cargas);
        n.setActivo(true);
        sistema.registrarNodo(n);
        return n;
    }

    private Tramo crearTramo(NodoLogistico orig, NodoLogistico dest, String medio, double costo, double tiempo, Set<String> cargas) {
        Tramo t = new Tramo();
        t.setOrigen(orig);
        t.setDestino(dest);
        t.setMedio(medio);
        t.setCostoUnitario(BigDecimal.valueOf(costo));
        t.setTiempo(BigDecimal.valueOf(tiempo));
        t.setTiposCargaAdmitidos(cargas);
        t.setActivo(true);
        sistema.registrarTramo(t);
        return t;
    }

    public void testAccesibilidadYTriangulacionAnteEmbargo() {
      
        assertTrue(sistema.puedeEnviarProducto(argentina, espana, soja));

      
        crearTramo(buenosAires, madrid, "MARITIMO", 600, 60, Set.of("GRANEL"));
        sistema.configurarRelacionComercial(argentina, espana, "EMBARGO");
        assertTrue(sistema.puedeEnviarProducto(argentina, espana, soja));

        sistema.configurarRelacionComercial(argentina, uruguay, "EMBARGO");
        assertFalse(sistema.puedeEnviarProducto(argentina, espana, soja));
    }

    public void testNodosCriticosConPuntosDeArticulacion() {
     
        Set<NodoLogistico> criticos = sistema.obtenerNodosCriticos();
        assertEquals(Set.of(montevideo, santos), criticos);
    }

    public void testImpactoCierreDePuerto() {
        Set<String> afectados = sistema.analizarImpactoCierreNodo(montevideo, soja);
        assertTrue(afectados.contains("Argentina -> Brasil"));
        assertTrue(afectados.contains("Argentina -> Espana"));
       
        assertTrue(montevideo.getActivo());
    }

    public void testCadenaProductivaClasificacionTopologica() {
        Producto harina = crearProducto("Harina de Soja", "Insumos", "GRANEL");
        Producto alimento = crearProducto("Alimento Balanceado", "Alimentos", "GRANEL");

        DirectedGraph<Producto, WeightedEdge> cadena = new DirectedGraph<>();
        cadena.agregarVertice(soja);
        cadena.agregarVertice(harina);
        cadena.agregarVertice(alimento);
        cadena.agregarArista(soja, harina, new WeightedEdge(1.0));
        cadena.agregarArista(harina, alimento, new WeightedEdge(1.0));

        List<Producto> orden = sistema.ordenarCadenaProductiva(cadena);
        assertNotNull(orden);
        assertEquals(List.of(soja, harina, alimento), orden);
    }
}