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
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private final Map<Producto, CacheAlcance> cacheAlcance = new IdentityHashMap<>();

    private record EstadoNodo(String nombre, Pais pais, boolean disponible) {}
    private record EstadoTramo(String origen, String destino, boolean utilizable) {}
    private record EstadoRed(List<EstadoNodo> nodos, List<EstadoTramo> tramos) {}
    private record CacheAlcance(EstadoRed estado, IDirectedIGraph<NodoLogistico, WeightedEdge> grafo,
                                IFloydWarshallResult<NodoLogistico> cierre) {}

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
     * Consulta 2 - Nodos que participan en circuitos logísticos dirigidos.
     * Detecta nodos que pertenecen a algún recorrido cerrado utilizable por el
     * producto. Sirve para reconocer circuitos de transporte de ida y vuelta,
     * independientemente de cuál sea la ruta más barata o más rápida.
     */
    public Set<NodoLogistico> nodosEnCircuitosLogisticos(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, "COSTO");
        IFloydWarshallResult<NodoLogistico> alcance = algoritmosDirigidos.warshall(grafo);
        Set<NodoLogistico> circuitos = new LinkedHashSet<>();
        for (Edge<NodoLogistico, ArcoTramo> arista : grafo.aristas()) {
            if (alcance.connected(arista.target(), arista.source())) {
                circuitos.add(arista.source());
                circuitos.add(arista.target());
            }
        }
        return circuitos;
    }

    /** Resumen de cobertura dirigida de un medio de transporte para un producto. */
    public record CoberturaMedio(int nodosConServicio, int alcanceMaximo) {}

    /**
     * Consulta 3 - Cobertura de cada medio de transporte para un producto.
     * Construye un subgrafo dirigido por medio y usa BEA desde cada nodo para
     * medir su mayor alcance. Permite comparar la cobertura de barco, camión o
     * avión sin pedir una ruta entre dos puntos concretos.
     */
    public Map<String, CoberturaMedio> coberturaPorMedio(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> elegibles = construirGrafo(producto, "COSTO");
        Map<String, CoberturaMedio> resultado = new LinkedHashMap<>();
        Set<String> medios = new LinkedHashSet<>();
        for (Tramo tramo : tramos) {
            if (tramo.getMedio() != null
                    && elegibles.vertices().contains(tramo.getOrigen())
                    && elegibles.vertices().contains(tramo.getDestino())
                    && tramo.esUtilizable(producto)) medios.add(tramo.getMedio());
        }
        for (String medio : medios) {
            DirectedGraph<NodoLogistico, ArcoTramo> grafo = new DirectedGraph<>();
            for (NodoLogistico nodo : elegibles.vertices()) grafo.agregarVertice(nodo);
            Set<NodoLogistico> conServicio = new LinkedHashSet<>();
            for (Tramo tramo : tramos) {
                if (medio.equals(tramo.getMedio())
                        && grafo.vertices().contains(tramo.getOrigen())
                        && grafo.vertices().contains(tramo.getDestino())
                        && tramo.esUtilizable(producto)) {
                    grafo.agregarArista(tramo.getOrigen(), tramo.getDestino(),
                            new ArcoTramo(tramo.calcularCosto(producto).doubleValue(), tramo));
                    conServicio.add(tramo.getOrigen());
                    conServicio.add(tramo.getDestino());
                }
            }
            int alcanceMaximo = 0;
            for (NodoLogistico origen : conServicio) {
                Set<NodoLogistico> visitados = new LinkedHashSet<>();
                algoritmosDirigidos.recorridoEnAmplitud(grafo, grafo.construirComparable(origen), visitados::add);
                alcanceMaximo = Math.max(alcanceMaximo, visitados.size());
            }
            resultado.put(medio, new CoberturaMedio(conServicio.size(), alcanceMaximo));
        }
        return resultado;
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

    /** Productos que un país exporta a, e importa de, un socio comercial. */
    public record Intercambio(Set<Producto> exportados, Set<Producto> importados) {}

    /** Incluye todos los demás países registrados, incluso si aún no hay intercambio. */
    public Map<Pais, Intercambio> intercambioComercial(Pais pais) {
        if (pais == null || !paises.containsValue(pais)) {
            throw new IllegalArgumentException("País no registrado");
        }
        Map<Pais, Intercambio> resultado = new LinkedHashMap<>();
        for (Pais socio : paises.values()) {
            if (pais == socio) continue;
            Set<Producto> exportados = pais.getExportaciones().getOrDefault(socio, Set.of());
            Set<Producto> importados = socio.getExportaciones().getOrDefault(pais, Set.of());
            resultado.put(socio, new Intercambio(Set.copyOf(exportados), Set.copyOf(importados)));
        }
        return java.util.Collections.unmodifiableMap(resultado);
    }

    public void configurarRelacionComercial(Pais origen, Pais destino, String tipoAcuerdo) {
        if (origen == null || destino == null || tipoAcuerdo == null) return;
        origen.getTiposAcuerdo().put(destino, tipoAcuerdo);
        if ("EMBARGO".equalsIgnoreCase(tipoAcuerdo) || "SANCION".equalsIgnoreCase(tipoAcuerdo)
                || "SANCIÓN".equalsIgnoreCase(tipoAcuerdo)) {
            origen.getDestinosEmbargados().add(destino);
        } else {
            origen.getDestinosEmbargados().remove(destino);
        }
    }

    public record ParPaises(Pais origen, Pais destino) {}

    /** La ausencia de un par en un mapa indica que no existe ruta comercial para el producto. */
    public record ImpactoComercial(Map<ParPaises, BigDecimal> antes,
                                   Map<ParPaises, BigDecimal> despues) {}

    /**
     * Simula una relación dirigida y compara el menor costo entre cada par de países.
     * Restablece el acuerdo, el arancel y el embargo originales antes de devolver.
     */
    public ImpactoComercial analizarCambioRelacionComercial(Pais origen, Pais destino,
            Producto producto, String nuevoAcuerdo, BigDecimal nuevoArancel) {
        if (origen == null || destino == null || origen == destino
                || !paises.containsValue(origen) || !paises.containsValue(destino)
                || producto == null || !productos.containsValue(producto)
                || nuevoAcuerdo == null || (nuevoArancel != null && nuevoArancel.signum() < 0)) {
            throw new IllegalArgumentException("Cambio comercial inválido");
        }
        Map<ParPaises, BigDecimal> antes = costosMinimosPorPais(producto);
        String acuerdoAnterior = origen.getTiposAcuerdo().get(destino);
        BigDecimal arancelAnterior = origen.getArancelesGenerales().get(destino);
        boolean embargadoAnteriormente = origen.getDestinosEmbargados().contains(destino);
        try {
            configurarRelacionComercial(origen, destino, nuevoAcuerdo);
            if (nuevoArancel != null) origen.getArancelesGenerales().put(destino, nuevoArancel);
            return new ImpactoComercial(antes, costosMinimosPorPais(producto));
        } finally {
            if (acuerdoAnterior == null) origen.getTiposAcuerdo().remove(destino);
            else origen.getTiposAcuerdo().put(destino, acuerdoAnterior);
            if (arancelAnterior == null) origen.getArancelesGenerales().remove(destino);
            else origen.getArancelesGenerales().put(destino, arancelAnterior);
            if (embargadoAnteriormente) origen.getDestinosEmbargados().add(destino);
            else origen.getDestinosEmbargados().remove(destino);
        }
    }

    private Map<ParPaises, BigDecimal> costosMinimosPorPais(Producto producto) {
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> porNodo = costosMinimosEntreTodos(producto);
        Map<ParPaises, BigDecimal> porPais = new LinkedHashMap<>();
        for (Map.Entry<NodoLogistico, Map<NodoLogistico, BigDecimal>> origen : porNodo.entrySet()) {
            for (Map.Entry<NodoLogistico, BigDecimal> destino : origen.getValue().entrySet()) {
                Pais paisOrigen = origen.getKey().getPais();
                Pais paisDestino = destino.getKey().getPais();
                if (paisOrigen == paisDestino) continue;
                porPais.merge(new ParPaises(paisOrigen, paisDestino), destino.getValue(), BigDecimal::min);
            }
        }
        return java.util.Collections.unmodifiableMap(porPais);
    }

    public List<Tramo> rutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return rutaMinima(origen, destino, producto, "COSTO");
    }

    public List<Tramo> rutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return rutaMinima(origen, destino, producto, "TIEMPO");
    }

    public record DetalleRuta(boolean alcanzable, List<Tramo> tramos, List<String> medios,
                              List<Pais> paises, BigDecimal costoTotal, BigDecimal tiempoTotal) {}

    public DetalleRuta detalleRutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return detallarRuta(origen, destino, producto, rutaMenorCosto(origen, destino, producto));
    }

    public DetalleRuta detalleRutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return detallarRuta(origen, destino, producto, rutaMenorTiempo(origen, destino, producto));
    }

    private DetalleRuta detallarRuta(NodoLogistico origen, NodoLogistico destino,
                                    Producto producto, List<Tramo> tramosRuta) {
        boolean alcanzable = origen.equals(destino) || !tramosRuta.isEmpty();
        if (!alcanzable) return new DetalleRuta(false, List.of(), List.of(), List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO);
        List<String> medios = new ArrayList<>();
        List<Pais> paisesRuta = new ArrayList<>();
        paisesRuta.add(origen.getPais());
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal tiempo = BigDecimal.ZERO;
        for (Tramo tramo : tramosRuta) {
            medios.add(tramo.getMedio());
            Pais paisDestino = tramo.getDestino().getPais();
            if (paisesRuta.get(paisesRuta.size() - 1) != paisDestino) paisesRuta.add(paisDestino);
            costo = costo.add(tramo.calcularCosto(producto));
            tiempo = tiempo.add(tramo.getTiempo());
        }
        return new DetalleRuta(true, tramosRuta, List.copyOf(medios), List.copyOf(paisesRuta),
                costo, tiempo);
    }

    /** Devuelve los tramos en orden; cada tramo identifica su medio y los países de sus extremos. */
    private List<Tramo> rutaMinima(NodoLogistico origen, NodoLogistico destino,
                                    Producto producto, String criterio) {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, criterio);
        if (!grafo.vertices().contains(origen) || !grafo.vertices().contains(destino)) {
            throw new IllegalArgumentException("Origen o destino no disponible para el producto");
        }
        IDijkstraResult<NodoLogistico> resultado = algoritmosDirigidos.dijkstra(
                grafo.construirComparable(origen), grafo);
        List<NodoLogistico> camino = resultado.getPath(destino);
        List<Tramo> ruta = new ArrayList<>();
        for (int i = 1; i < camino.size(); i++) {
            ruta.add(grafo.obtenerArista(camino.get(i - 1), camino.get(i)).dato().getTramo());
        }
        return List.copyOf(ruta);
    }

    /** Un destino inalcanzable se omite del mapa interno; cada nodo incluye costo 0 a sí mismo. */
    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, "COSTO");
        IFloydWarshallResult<NodoLogistico> resultado = algoritmosDirigidos.floyd(grafo);
        Map<NodoLogistico, Map<NodoLogistico, Tramo>> tramosPorPar = new LinkedHashMap<>();
        for (Edge<NodoLogistico, ArcoTramo> arista : grafo.aristas()) {
            tramosPorPar.computeIfAbsent(arista.source(), k -> new LinkedHashMap<>())
                    .put(arista.target(), arista.dato().getTramo());
        }
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = new LinkedHashMap<>();
        for (NodoLogistico origen : grafo.vertices()) {
            Map<NodoLogistico, BigDecimal> desdeOrigen = new LinkedHashMap<>();
            for (NodoLogistico destino : grafo.vertices()) {
                if (!resultado.connected(origen, destino)) continue;
                BigDecimal costo = BigDecimal.ZERO;
                List<NodoLogistico> camino = resultado.getPath(origen, destino);
                for (int i = 1; i < camino.size(); i++) {
                    Tramo tramo = tramosPorPar.get(camino.get(i - 1)).get(camino.get(i));
                    costo = costo.add(tramo.calcularCosto(producto));
                }
                desdeOrigen.put(destino, costo);
            }
            costos.put(origen, java.util.Collections.unmodifiableMap(desdeOrigen));
        }
        return java.util.Collections.unmodifiableMap(costos);
    }

    /** Centro de la red física activa según el tiempo de traslado, sin filtrar por producto. */
    public NodoLogistico obtenerCentro() {
        DirectedGraph<NodoLogistico, WeightedEdge> grafo = new DirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (Boolean.TRUE.equals(nodo.getActivo())) grafo.agregarVertice(nodo);
        }
        for (Tramo tramo : tramos) {
            if (!Boolean.TRUE.equals(tramo.getActivo()) || tramo.getTiempo() == null
                    || tramo.getTiempo().signum() < 0
                    || !grafo.vertices().contains(tramo.getOrigen())
                    || !grafo.vertices().contains(tramo.getDestino())) continue;
            Edge<NodoLogistico, WeightedEdge> anterior = grafo.obtenerArista(tramo.getOrigen(), tramo.getDestino());
            double tiempo = tramo.getTiempo().doubleValue();
            if (anterior != null && anterior.dato().getWeight() <= tiempo) continue;
            if (anterior != null) grafo.eliminarArista(grafo.construirComparable(tramo.getOrigen()),
                    grafo.construirComparable(tramo.getDestino()));
            grafo.agregarArista(tramo.getOrigen(), tramo.getDestino(), new WeightedEdge(tiempo));
        }
        return algoritmosDirigidos.obtenerCentroGrafo(grafo);
    }

    /** Centro de la red comercial utilizable por un producto, medido por tiempo. */
    public NodoLogistico obtenerCentro(Producto producto) {
        return algoritmosDirigidos.obtenerCentroGrafo(construirGrafo(producto, "TIEMPO"));
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
        EstadoRed estado = estadoRed(producto);
        CacheAlcance cache = cacheAlcance.get(producto);
        if (cache == null || !cache.estado().equals(estado)) {
            IDirectedIGraph<NodoLogistico, WeightedEdge> grafo = construirGrafoDirigido(producto);
            cache = new CacheAlcance(estado, grafo, algoritmosDirigidos.warshall(grafo));
            cacheAlcance.put(producto, cache);
        }
        IDirectedIGraph<NodoLogistico, WeightedEdge> grafo = cache.grafo();
        IFloydWarshallResult<NodoLogistico> warshallResult = cache.cierre();

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

    /** Captura solo los cambios que afectan la alcanzabilidad del producto. */
    private EstadoRed estadoRed(Producto producto) {
        List<EstadoNodo> estadoNodos = new ArrayList<>();
        for (NodoLogistico nodo : nodos.values()) {
            estadoNodos.add(new EstadoNodo(nodo.getNombre(), nodo.getPais(), nodo.admite(producto)));
        }
        List<EstadoTramo> estadoTramos = new ArrayList<>();
        for (Tramo tramo : tramos) {
            estadoTramos.add(new EstadoTramo(tramo.getOrigen().getNombre(),
                    tramo.getDestino().getNombre(), tramo.esUtilizable(producto)));
        }
        return new EstadoRed(estadoNodos, estadoTramos);
    }
    public Set<NodoLogistico> obtenerNodosCriticos() {
        UndirectedGraph<NodoLogistico, WeightedEdge> grafoSubyacente = new UndirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (Boolean.TRUE.equals(nodo.getActivo())) {
                grafoSubyacente.agregarVertice(nodo);
            }
        }
        for (Tramo tramo : tramos) {
            if (Boolean.TRUE.equals(tramo.getActivo())
                    && grafoSubyacente.vertices().contains(tramo.getOrigen())
                    && grafoSubyacente.vertices().contains(tramo.getDestino())) {
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
        for (Producto p : productos.values()) {
            algoritmosDirigidos.warshall(construirGrafo(p, "COSTO"));
        }
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
