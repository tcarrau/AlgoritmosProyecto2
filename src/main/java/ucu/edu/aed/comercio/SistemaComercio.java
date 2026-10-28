package ucu.edu.aed.comercio;

import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;
import ucu.edu.aed.tda.grafo.UndirectedGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraphAlgorithm;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SistemaComercio {

    private Map<String, Pais> paises = new LinkedHashMap<>();
    private Map<String, Producto> productos = new LinkedHashMap<>();
    private Map<String, NodoLogistico> nodos = new LinkedHashMap<>();
    private List<Tramo> tramos = new ArrayList<>();
    private final Map<Producto, Set<Producto>> dependenciasProductivas = new LinkedHashMap<>();

    private final DirectedGraphAlgorithms algoritmosDirigidos = new DirectedGraphAlgorithms();
    private final UndirectedGraphAlgorithm algoritmosNoDirigidos = new UndirectedGraphAlgorithm();

    public Map<String, Pais> getPaises() { return paises; }
    public void setPaises(Map<String, Pais> paises) { this.paises = paises; }

    public Map<String, Producto> getProductos() { return productos; }
    public void setProductos(Map<String, Producto> productos) { this.productos = productos; }

    public Map<String, NodoLogistico> getNodos() { return nodos; }
    public void setNodos(Map<String, NodoLogistico> nodos) { this.nodos = nodos; }

    public List<Tramo> getTramos() { return tramos; }
    public void setTramos(List<Tramo> tramos) { this.tramos = tramos; }

    // =======================================================================
    // Registro de la red (Base para operar el grafo)
    // =======================================================================

    public void registrarPais(Pais pais) {
        paises.put(pais.getNombre(), pais);
    }

    public void registrarProducto(Producto producto) {
        productos.put(producto.getNombre(), producto);
    }

    public void registrarNodo(NodoLogistico nodo) {
        nodos.put(nodo.getNombre(), nodo);
    }

    public void registrarTramo(Tramo tramo) {
        tramos.add(tramo);
    }

    /** Grafo de los nodos y tramos que sirven para el producto; criterio "COSTO" o "TIEMPO". */
    public DirectedGraph<NodoLogistico, ArcoTramo> construirGrafo(Producto producto, String criterio) {
        return construirGrafoSinTramo(producto, criterio, null);
    }

    private DirectedGraph<NodoLogistico, ArcoTramo> construirGrafoSinTramo(
            Producto producto, String criterio, Tramo excluido) {
        Objects.requireNonNull(producto, "producto");
        Objects.requireNonNull(criterio, "criterio");
        boolean porCosto = criterio.equalsIgnoreCase("COSTO");
        if (!porCosto && !criterio.equalsIgnoreCase("TIEMPO")) {
            throw new IllegalArgumentException("Criterio desconocido: " + criterio);
        }
        DirectedGraph<NodoLogistico, ArcoTramo> g = new DirectedGraph<>();
        for (NodoLogistico n : nodos.values()) {
            if (n.admite(producto)) {
                g.agregarVertice(n);
            }
        }
        for (Tramo t : tramos) {
            if (t == excluido || !g.vertices().contains(t.getOrigen()) || !g.vertices().contains(t.getDestino())
                    || !t.esUtilizable(producto)) {
                continue;
            }
            double peso = (porCosto ? t.calcularCosto(producto) : t.getTiempo()).doubleValue();
            Edge<NodoLogistico, ArcoTramo> previa = g.obtenerArista(t.getOrigen(), t.getDestino());
            if (previa != null && previa.dato().getWeight() <= peso) {
                continue;
            }
            g.eliminarArista(g.construirComparable(t.getOrigen()), g.construirComparable(t.getDestino()));
            g.agregarArista(t.getOrigen(), t.getDestino(), new ArcoTramo(peso, t));
        }
        return g;
    }

    // =======================================================================
    // Cinco consultas externas: cada resultado se calcula sobre un grafo.
    // =======================================================================

    /**
     * Consulta 1 - Tramos críticos para un producto.
     * Un tramo es crítico cuando, al quitarlo, ya no se puede viajar desde su
     * origen hasta su destino. Esto permite identificar conexiones cuyo cierre
     * interrumpe envíos; se analizan aristas, no los nodos críticos ya requeridos.
     * Los tramos paralelos se vuelven a considerar al reconstruir el grafo.
     */
    public Set<Tramo> tramosCriticos(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> base = construirGrafo(producto, "COSTO");
        Set<Tramo> resultado = new LinkedHashSet<>();
        for (Edge<NodoLogistico, ArcoTramo> arista : base.aristas()) {
            Tramo tramo = arista.dato().getTramo();
            DirectedGraph<NodoLogistico, ArcoTramo> sinTramo =
                    construirGrafoSinTramo(producto, "COSTO", tramo);
            IFloydWarshallResult<NodoLogistico> alcance = algoritmosDirigidos.warshall(sinTramo);
            if (!alcance.connected(arista.source(), arista.target())) {
                resultado.add(tramo);
            }
        }
        return resultado;
    }

    /**
     * Consulta 2 - Tramos directos redundantes para un producto.
     * Devuelve tramos utilizables cuyo costo directo supera estrictamente el
     * costo mínimo entre los mismos extremos, calculado con Floyd. Sirve para
     * detectar conexiones que no convienen económicamente frente a otra ruta.
     */
    public Set<Tramo> tramosDirectosRedundantes(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, "COSTO");
        IFloydWarshallResult<NodoLogistico> minimos = algoritmosDirigidos.floyd(grafo);
        Set<Tramo> resultado = new LinkedHashSet<>();
        for (Tramo tramo : tramos) {
            if (grafo.vertices().contains(tramo.getOrigen())
                    && grafo.vertices().contains(tramo.getDestino())
                    && tramo.esUtilizable(producto)
                    && minimos.getCost(tramo.getOrigen(), tramo.getDestino())
                       < tramo.calcularCosto(producto).doubleValue() - 1e-9) {
                resultado.add(tramo);
            }
        }
        return resultado;
    }

    /**
     * Consulta 3 - Bloques de comercio recíproco para un producto.
     * Agrupa nodos que pueden alcanzarse mutuamente mediante rutas dirigidas.
     * Identifica regiones con intercambio de ida y vuelta, no solo envíos en un
     * sentido. Los nodos aislados aparecen como bloques de un elemento.
     */
    public List<Set<NodoLogistico>> bloquesComercioReciproco(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, "COSTO");
        IFloydWarshallResult<NodoLogistico> alcance = algoritmosDirigidos.warshall(grafo);
        Set<NodoLogistico> pendientes = new LinkedHashSet<>(grafo.vertices());
        List<Set<NodoLogistico>> bloques = new ArrayList<>();
        for (NodoLogistico origen : grafo.vertices()) {
            if (!pendientes.contains(origen)) continue;
            Set<NodoLogistico> bloque = new LinkedHashSet<>();
            for (NodoLogistico destino : pendientes) {
                if (alcance.connected(origen, destino) && alcance.connected(destino, origen)) {
                    bloque.add(destino);
                }
            }
            pendientes.removeAll(bloque);
            bloques.add(bloque);
        }
        return bloques;
    }

    /**
     * Registra que un insumo debe estar disponible antes de producir otro producto.
     * Esta relación crea la arista insumo -> producto final del grafo productivo.
     */
    public void registrarDependenciaProductiva(Producto insumo, Producto productoFinal) {
        if (insumo == null || productoFinal == null
                || !productos.containsValue(insumo) || !productos.containsValue(productoFinal)) {
            throw new IllegalArgumentException("Ambos productos deben estar registrados");
        }
        dependenciasProductivas.computeIfAbsent(insumo, k -> new LinkedHashSet<>()).add(productoFinal);
    }

    /**
     * Consulta 4 - Orden de la cadena productiva registrada.
     * Construye el grafo de dependencias y aplica clasificación topológica.
     * Ayuda a planificar la secuencia de abastecimiento y producción. Si hay un
     * ciclo, no existe un orden válido y se informa mediante una excepción.
     */
    public List<Producto> consultarOrdenCadenaProductiva() {
        DirectedGraph<Producto, WeightedEdge> grafo = new DirectedGraph<>();
        for (Producto producto : productos.values()) grafo.agregarVertice(producto);
        for (Map.Entry<Producto, Set<Producto>> entrada : dependenciasProductivas.entrySet()) {
            for (Producto destino : entrada.getValue()) {
                if (!grafo.agregarArista(entrada.getKey(), destino, new WeightedEdge(1))) {
                    throw new IllegalStateException("Dependencia productiva inválida o duplicada");
                }
            }
        }
        List<Producto> orden = algoritmosDirigidos.calcularClasificacionTopologica(grafo);
        if (orden == null) throw new IllegalStateException("La cadena productiva contiene un ciclo");
        return orden;
    }

    /**
     * Consulta 5 - Red física mínima para un tipo de producto.
     * Proyecta los tramos físicos activos a un grafo no dirigido y usa Kruskal
     * para obtener un bosque de costo mínimo. Sirve para planificar qué enlaces
     * de infraestructura sostener; no representa permisos comerciales ni rutas
     * dirigidas. En una red desconectada devuelve un bosque, no un solo árbol.
     */
    public IUndirectedGraph<NodoLogistico, ArcoTramo> redFisicaMinima(Producto producto) {
        Objects.requireNonNull(producto, "producto");
        UndirectedGraph<NodoLogistico, ArcoTramo> fisico = new UndirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (nodo.admite(producto)) fisico.agregarVertice(nodo);
        }
        for (Tramo tramo : tramos) {
            if (!Boolean.TRUE.equals(tramo.getActivo())
                    || tramo.getCostoUnitario() == null
                    || tramo.getTiposCargaAdmitidos() == null
                    || !tramo.getTiposCargaAdmitidos().contains(producto.getTipoCarga())
                    || !fisico.vertices().contains(tramo.getOrigen())
                    || !fisico.vertices().contains(tramo.getDestino())) continue;
            Edge<NodoLogistico, ArcoTramo> previa = fisico.obtenerArista(tramo.getOrigen(), tramo.getDestino());
            double costo = tramo.getCostoUnitario().doubleValue();
            if (previa != null && previa.dato().getWeight() <= costo) continue;
            if (previa != null) fisico.eliminarArista(fisico.construirComparable(tramo.getOrigen()),
                    fisico.construirComparable(tramo.getDestino()));
            fisico.agregarArista(tramo.getOrigen(), tramo.getDestino(), new ArcoTramo(costo, tramo));
        }
        return algoritmosNoDirigidos.kruskal(fisico);
    }

    /**
     * dependientes: por cada punto de articulación de la red, los nodos del país que quedan
     * sin salida al exterior si ese nodo falla; afectados: unión de todos ellos;
     * excentricidad: la menor entre los nodos del país (infinita si no tiene nodos en la red
     * o ninguno alcanza al resto).
     */
    public record Vulnerabilidad(Map<NodoLogistico, Set<NodoLogistico>> dependientes,
                                 Set<NodoLogistico> afectados, double excentricidad) {}

    public Vulnerabilidad vulnerabilidad(Pais pais, Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> g = construirGrafo(producto, "TIEMPO");
        UndirectedGraph<NodoLogistico, ArcoTramo> subyacente = new UndirectedGraph<>();
        Map<NodoLogistico, Set<NodoLogistico>> vecinos = new LinkedHashMap<>();
        for (NodoLogistico n : g.vertices()) {
            subyacente.agregarVertice(n);
            vecinos.put(n, new LinkedHashSet<>());
        }
        for (Edge<NodoLogistico, ArcoTramo> e : g.aristas()) {
            subyacente.agregarArista(e.source(), e.target(), e.dato());
            vecinos.get(e.source()).add(e.target());
            vecinos.get(e.target()).add(e.source());
        }

        Map<NodoLogistico, Set<NodoLogistico>> dependientes = new LinkedHashMap<>();
        Set<NodoLogistico> afectados = new LinkedHashSet<>();
        for (NodoLogistico critico : new UndirectedGraphAlgorithm().puntosDeArticulacion(subyacente)) {
            Set<NodoLogistico> sinSalida = new LinkedHashSet<>();
            Set<NodoLogistico> visto = new HashSet<>(Set.of(critico));
            for (NodoLogistico inicio : vecinos.keySet()) {
                if (!visto.add(inicio)) {
                    continue;
                }
                List<NodoLogistico> componente = new ArrayList<>(List.of(inicio));
                Deque<NodoLogistico> pila = new ArrayDeque<>(componente);
                while (!pila.isEmpty()) {
                    for (NodoLogistico v : vecinos.get(pila.pop())) {
                        if (visto.add(v)) {
                            componente.add(v);
                            pila.push(v);
                        }
                    }
                }
                if (componente.stream().allMatch(n -> pais.equals(n.getPais()))) {
                    sinSalida.addAll(componente);
                }
            }
            if (!sinSalida.isEmpty()) {
                dependientes.put(critico, sinSalida);
                afectados.addAll(sinSalida);
            }
        }

        ShortestPathAlgorithms caminos = new ShortestPathAlgorithms();
        double excentricidad = Double.POSITIVE_INFINITY;
        for (NodoLogistico n : g.vertices()) {
            if (pais.equals(n.getPais())) {
                excentricidad = Math.min(excentricidad, caminos.obtenerExcentricidad(g, g.construirComparable(n)));
            }
        }
        return new Vulnerabilidad(dependientes, afectados, excentricidad);
    }

    public void registrarIntercambio(Pais origen, Pais destino, Producto producto, BigDecimal cantidad) {
        if (origen == null || destino == null || producto == null) return;
        origen.getExportaciones()
              .computeIfAbsent(destino, k -> new LinkedHashSet<>())
              .add(producto);
    }

    public void configurarRelacionComercial(Pais origen, Pais destino, String tipoAcuerdo) {
        if (origen == null || destino == null || tipoAcuerdo == null) return;
        origen.getTiposAcuerdo().put(destino, tipoAcuerdo);
        if ("EMBARGO".equalsIgnoreCase(tipoAcuerdo) || "SANCION".equalsIgnoreCase(tipoAcuerdo)) {
            origen.getDestinosEmbargados().add(destino);
        } else {
            origen.getDestinosEmbargados().remove(destino);
        }
    }

    public List<Tramo> rutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar por P2");
    }

    public List<Tramo> rutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar por P2");
    }

    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar por P2");
    }

    public NodoLogistico obtenerCentro() {
        throw new UnsupportedOperationException("Pendiente de implementar por P2");
    }

    public IDirectedIGraph<NodoLogistico, WeightedEdge> construirGrafoDirigido(Producto producto) {
        DirectedGraph<NodoLogistico, WeightedEdge> grafo = new DirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (nodo.admite(producto)) {
                grafo.agregarVertice(nodo);
            }
        }
        for (Tramo tramo : tramos) {
            if (tramo.esUtilizable(producto)) {
                double peso = tramo.calcularCosto(producto).doubleValue();
                grafo.agregarArista(tramo.getOrigen(), tramo.getDestino(), new WeightedEdge(peso));
            }
        }
        return grafo;
    }

    public boolean puedeEnviarProducto(Pais origen, Pais destino, Producto producto) {
        if (origen == null || destino == null || producto == null) {
            return false;
        }
        IDirectedIGraph<NodoLogistico, WeightedEdge> grafo = construirGrafoDirigido(producto);
        IFloydWarshallResult<NodoLogistico> warshallResult = algoritmosDirigidos.warshall(grafo);

        for (NodoLogistico nOrigen : grafo.vertices()) {
            if (!origen.equals(nOrigen.getPais())) continue;
            for (NodoLogistico nDestino : grafo.vertices()) {
                if (!destino.equals(nDestino.getPais())) continue;
                if (!nOrigen.equals(nDestino) && warshallResult.connected(nOrigen, nDestino)) {
                    return true;
                }
            }
        }
        return false;
    }
    public Set<NodoLogistico> obtenerNodosCriticos() {
        UndirectedGraph<NodoLogistico, WeightedEdge> grafoSubyacente = new UndirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (!Boolean.FALSE.equals(nodo.getActivo())) {
                grafoSubyacente.agregarVertice(nodo);
            }
        }
        for (Tramo tramo : tramos) {
            if (tramo.esUtilizable(null)) {
                double peso = tramo.getCostoUnitario() != null ? tramo.getCostoUnitario().doubleValue() : 1.0;
                grafoSubyacente.agregarArista(tramo.getOrigen(), tramo.getDestino(), new WeightedEdge(peso));
            }
        }
        return algoritmosNoDirigidos.puntosDeArticulacion(grafoSubyacente);
    }

    /**
     * Analiza el estado actual de la red evaluando nodos críticos y conectividad general.
     */
    public void analizarCambio() {
        obtenerNodosCriticos();
        IDirectedIGraph<NodoLogistico, WeightedEdge> grafo = construirGrafoDirigido(null);
        algoritmosDirigidos.warshall(grafo);
    }


    public Set<String> analizarImpactoCierreNodo(NodoLogistico nodoACerrar, Producto producto) {
        Set<String> paresAfectados = new LinkedHashSet<>();
        if (nodoACerrar == null || producto == null) return paresAfectados;

        Boolean estadoOriginal = nodoACerrar.getActivo();
        try {
            // Conectividad antes del cierre
            nodoACerrar.setActivo(true);
            List<Pais> listaPaises = new ArrayList<>(paises.values());
            boolean[][] antes = new boolean[listaPaises.size()][listaPaises.size()];
            for (int i = 0; i < listaPaises.size(); i++) {
                for (int j = 0; j < listaPaises.size(); j++) {
                    if (i != j) {
                        antes[i][j] = puedeEnviarProducto(listaPaises.get(i), listaPaises.get(j), producto);
                    }
                }
            }

            // Conectividad después del cierre
            nodoACerrar.setActivo(false);
            for (int i = 0; i < listaPaises.size(); i++) {
                for (int j = 0; j < listaPaises.size(); j++) {
                    if (i != j && antes[i][j] && !puedeEnviarProducto(listaPaises.get(i), listaPaises.get(j), producto)) {
                        paresAfectados.add(listaPaises.get(i).getNombre() + " -> " + listaPaises.get(j).getNombre());
                    }
                }
            }
        } finally {
            nodoACerrar.setActivo(estadoOriginal);
        }
        return paresAfectados;
    }

    public List<Producto> ordenarCadenaProductiva(IDirectedIGraph<Producto, WeightedEdge> grafoDependencias) {
        if (grafoDependencias == null) return null;
        return algoritmosDirigidos.calcularClasificacionTopologica(grafoDependencias);
    }
}
