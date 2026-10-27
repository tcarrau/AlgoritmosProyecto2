package ucu.edu.aed.comercio;

import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;
import ucu.edu.aed.tda.grafo.UndirectedGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraphAlgorithm;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SistemaComercio {
    private Map<String, Pais> paises = new LinkedHashMap<>();
    private Map<String, Producto> productos = new LinkedHashMap<>();
    private Map<String, NodoLogistico> nodos = new LinkedHashMap<>();
    private List<Tramo> tramos = new ArrayList<>();

    public Map<String, Pais> getPaises() { return paises; }
    public void setPaises(Map<String, Pais> paises) { this.paises = paises; }

    public Map<String, Producto> getProductos() { return productos; }
    public void setProductos(Map<String, Producto> productos) { this.productos = productos; }

    public Map<String, NodoLogistico> getNodos() { return nodos; }
    public void setNodos(Map<String, NodoLogistico> nodos) { this.nodos = nodos; }

    public List<Tramo> getTramos() { return tramos; }
    public void setTramos(List<Tramo> tramos) { this.tramos = tramos; }

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
            if (!g.vertices().contains(t.getOrigen()) || !g.vertices().contains(t.getDestino())
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
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarRelacionComercial(Pais origen, Pais destino, String tipoAcuerdo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Tramo> rutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Tramo> rutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean puedeEnviarProducto(Pais origen, Pais destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public NodoLogistico obtenerCentro() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Set<NodoLogistico> obtenerNodosCriticos() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void analizarCambio() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
