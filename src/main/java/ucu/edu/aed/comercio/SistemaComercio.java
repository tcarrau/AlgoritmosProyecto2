package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraphAlgorithm;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

public class SistemaComercio {

    private Map<String, Pais> paises = new LinkedHashMap<>();
    private Map<String, Producto> productos = new LinkedHashMap<>();
    private Map<String, NodoLogistico> nodos = new LinkedHashMap<>();
    private List<Tramo> tramos = new ArrayList<>();

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
        if (pais != null && pais.getNombre() != null) {
            paises.put(pais.getNombre(), pais);
        }
    }

    public void registrarProducto(Producto producto) {
        if (producto != null && producto.getNombre() != null) {
            productos.put(producto.getNombre(), producto);
        }
    }

    public void registrarNodo(NodoLogistico nodo) {
        if (nodo != null && nodo.getNombre() != null) {
            if (nodo.getActivo() == null) {
                nodo.setActivo(true);
            }
            nodos.put(nodo.getNombre(), nodo);
        }
    }

    public void registrarTramo(Tramo tramo) {
        if (tramo != null) {
            if (tramo.getActivo() == null) {
                tramo.setActivo(true);
            }
            tramos.add(tramo);
        }
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