package ucu.edu.aed.comercio;

import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public void registrarIntercambio(Pais origen, Pais destino, Producto producto, BigDecimal cantidad) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarRelacionComercial(Pais origen, Pais destino, String tipoAcuerdo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    // =======================================================================
    // Rutas y costos (P2)
    //
    // Los algoritmos no tienen estado, asi que una sola instancia compartida
    // alcanza para todas las consultas.
    // =======================================================================

    private static final ShortestPathAlgorithms ALGORITMOS = new ShortestPathAlgorithms();

    /**
     * Ruta de menor costo total para llevar el producto de un nodo a otro, contando
     * el transporte de cada tramo mas los aranceles de los tramos que cruzan frontera.
     *
     * <p>Devuelve los tramos en orden, de los que se obtienen tambien los medios de
     * transporte y los paises atravesados. La lista vacia significa que no hay ruta
     * posible, o que origen y destino son el mismo nodo.</p>
     *
     * <p><b>Complejidad:</b> O(V + E) para armar el grafo del producto mas
     * O((V + E)*log V) de Dijkstra.</p>
     */
    public List<Tramo> rutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return rutaMasConveniente(origen, destino, producto, "COSTO");
    }

    /**
     * Ruta de menor tiempo de transito. No tiene por que coincidir con la de menor
     * costo: un tramo aereo suele ser mucho mas caro y mucho mas rapido que el maritimo.
     *
     * <p><b>Complejidad:</b> igual que {@link #rutaMenorCosto}.</p>
     */
    public List<Tramo> rutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        return rutaMasConveniente(origen, destino, producto, "TIEMPO");
    }

    /**
     * Costo minimo entre todos los pares de nodos que sirven para el producto.
     *
     * <p>Los pares sin ruta no aparecen en el mapa interno, de modo que un
     * {@code get} devuelve {@code null} en lugar de un valor centinela: BigDecimal no
     * tiene infinito y usar -1 lo confundiria con un costo real. La diagonal vale 0.
     * Los valores salen de un {@code double}, asi que conviene compararlos con
     * {@code compareTo} y no con {@code equals}.</p>
     *
     * <p><b>Complejidad:</b> O(V^3) por Floyd, mas O(V^2) para pasar la matriz al mapa.
     * Sobre una red dispersa como esta (pocos tramos por nodo) correr V veces Dijkstra
     * seria asintoticamente mejor, O(V*(V+E)*log V); se usa Floyd porque resuelve de una
     * vez el pedido de "todos los pares" y porque la red de ejemplo es chica. Es una de
     * las operaciones candidatas a optimizar en el Desafio 3.</p>
     */
    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, "COSTO");
        IFloydWarshallResult<NodoLogistico> distancias = ALGORITMOS.floyd(grafo);

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = new LinkedHashMap<>();
        for (NodoLogistico desde : grafo.vertices()) {
            Map<NodoLogistico, BigDecimal> fila = new LinkedHashMap<>();
            for (NodoLogistico hasta : grafo.vertices()) {
                double costo = distancias.getCost(desde, hasta);
                if (!Double.isInfinite(costo)) {
                    fila.put(hasta, BigDecimal.valueOf(costo));
                }
            }
            costos.put(desde, fila);
        }
        return costos;
    }

    /**
     * Costo total de recorrer una ruta con un producto, sumando el costo de cada tramo
     * con sus aranceles. Sirve para mostrar el total de lo que devuelve
     * {@link #rutaMenorCosto}.
     *
     * <p><b>Complejidad:</b> O(largo de la ruta).</p>
     */
    public BigDecimal costoTotal(List<Tramo> ruta, Producto producto) {
        BigDecimal total = BigDecimal.ZERO;
        for (Tramo tramo : ruta) {
            total = total.add(tramo.calcularCosto(producto));
        }
        return total;
    }

    /**
     * Resuelve la ruta sobre el grafo del producto segun el criterio pedido y traduce
     * el camino de nodos que devuelve Dijkstra a la lista de tramos que lo componen.
     */
    private List<Tramo> rutaMasConveniente(NodoLogistico origen, NodoLogistico destino,
                                           Producto producto, String criterio) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = construirGrafo(producto, criterio);
        if (!grafo.vertices().contains(origen) || !grafo.vertices().contains(destino)) {
            return List.of(); // alguno de los dos nodos no sirve para este producto
        }

        IDijkstraResult<NodoLogistico> desdeOrigen = ALGORITMOS.dijkstra(grafo.construirComparable(origen), grafo);
        return tramosDelCamino(grafo, desdeOrigen.getPath(destino));
    }

    /**
     * Cada par de nodos consecutivos del camino corresponde a la arista que los une, y
     * esa arista guarda el tramo concreto que el grafo eligio para ese par.
     */
    private List<Tramo> tramosDelCamino(DirectedGraph<NodoLogistico, ArcoTramo> grafo, List<NodoLogistico> camino) {
        List<Tramo> ruta = new ArrayList<>();
        for (int i = 0; i + 1 < camino.size(); i++) {
            ruta.add(grafo.obtenerArista(camino.get(i), camino.get(i + 1)).dato().getTramo());
        }
        return ruta;
    }

    public boolean puedeEnviarProducto(Pais origen, Pais destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    /**
     * Nodo mejor posicionado como centro de distribucion de la red fisica.
     *
     * <p>La firma no recibe producto, asi que la pregunta se responde sobre la red de
     * transporte completa: todos los nodos y tramos activos, pesados por el costo de
     * transporte. No se pueden sumar aranceles porque el arancel depende del producto
     * que se envia, y sin producto no hay arancel que calcular. Para la version por
     * producto esta {@link #obtenerCentro(Producto)}, que si los incluye.</p>
     *
     * <p>El criterio de centro es el de {@code ShortestPathAlgorithms}: primero la
     * cantidad de nodos alcanzables y despues la menor excentricidad entre esos nodos.
     * Importa en este dominio porque la red no es fuertemente conexa (hay embargos y
     * tramos de un solo sentido) y la definicion clasica de menor excentricidad
     * premiaria a un nodo que no alcanza a nadie.</p>
     *
     * <p><b>Complejidad:</b> O(V + E) para armar la red mas O(V^3) de Floyd dentro del
     * calculo del centro.</p>
     *
     * @return el centro, o {@code null} si no hay ningun nodo activo
     */
    public NodoLogistico obtenerCentro() {
        return ALGORITMOS.obtenerCentroGrafo(construirRedFisica());
    }

    /**
     * Centro de distribucion considerando solo los nodos y tramos que sirven para el
     * producto, con los aranceles ya incluidos en el costo de cruzar cada frontera.
     *
     * <p><b>Complejidad:</b> O(V + E) para armar el grafo mas O(V^3) de Floyd.</p>
     */
    public NodoLogistico obtenerCentro(Producto producto) {
        return ALGORITMOS.obtenerCentroGrafo(construirGrafo(producto, "COSTO"));
    }

    /**
     * Red de transporte completa: nodos y tramos activos, pesados por el costo de
     * transporte, sin filtrar por producto ni sumar aranceles.
     *
     * <p>Comparte la forma con {@link #construirGrafo}, incluido el criterio de quedarse
     * con el tramo mas barato cuando hay varios paralelos entre los mismos nodos. Vale
     * unificar las dos en un solo metodo parametrizado una vez que se integren las ramas.</p>
     */
    private DirectedGraph<NodoLogistico, ArcoTramo> construirRedFisica() {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = new DirectedGraph<>();
        for (NodoLogistico nodo : nodos.values()) {
            if (Boolean.TRUE.equals(nodo.getActivo())) {
                grafo.agregarVertice(nodo);
            }
        }
        for (Tramo tramo : tramos) {
            if (!Boolean.TRUE.equals(tramo.getActivo())
                    || !grafo.vertices().contains(tramo.getOrigen())
                    || !grafo.vertices().contains(tramo.getDestino())) {
                continue;
            }
            double peso = tramo.getCostoUnitario().doubleValue();
            Edge<NodoLogistico, ArcoTramo> previa = grafo.obtenerArista(tramo.getOrigen(), tramo.getDestino());
            if (previa != null && previa.dato().getWeight() <= peso) {
                continue;
            }
            grafo.eliminarArista(grafo.construirComparable(tramo.getOrigen()),
                    grafo.construirComparable(tramo.getDestino()));
            grafo.agregarArista(tramo.getOrigen(), tramo.getDestino(), new ArcoTramo(peso, tramo));
        }
        return grafo;
    }

    public Set<NodoLogistico> obtenerNodosCriticos() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void analizarCambio() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
