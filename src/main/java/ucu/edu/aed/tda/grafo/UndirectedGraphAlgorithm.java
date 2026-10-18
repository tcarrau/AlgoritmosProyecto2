package ucu.edu.aed.tda.grafo;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.function.Consumer;

import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class UndirectedGraphAlgorithm implements IUndirectedGraphAlgorithm {
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> kruskal(IUndirectedGraph<V, D> graph) {
        Set<Edge<V, D>> aristas = new LinkedHashSet<>();
        aristas = graph.aristas();
        List<Edge<V, D>> ordenadas = new ArrayList<>(aristas);

        ordenadas.sort(
                Comparator.comparingDouble(e -> e.dato().getWeight()));

        IUndirectedGraph<V, D> grafo = new UndirectedGraph<>();

        for (V v : graph.vertices()) {
            grafo.agregarVertice(v);
        }

        for (Edge<V, D> arista : ordenadas) {
            grafo.agregarArista(arista.source(), arista.target(), arista.dato());
            if (grafo.tieneCiclos()) {
                grafo.eliminarArista(grafo.construirComparable(arista.source()),
                        grafo.construirComparable(arista.target()));
            }
        }

        return grafo;
    }

    /**
     * ejecuta el algoritmo Prim sobre el grafo
     */
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(IUndirectedGraph<V, D> graph, Comparable<V> source) {
        if (source == null || graph == null)
            return null;

        Set<Edge<V, D>> aristas = new LinkedHashSet<>();
        aristas = graph.aristas();
        List<Edge<V, D>> ordenadas = new ArrayList<>(aristas);

        ordenadas.sort(
                Comparator.comparingDouble(e -> e.dato().getWeight()));

        IUndirectedGraph<V, D> grafo = new UndirectedGraph<>();
        V origen = graph.buscarVertice(source);
        if (origen == null)
            return null;
        grafo.agregarVertice(origen);

        ArrayList<V> verticesNoAgregados = new ArrayList<>();

        for (V v : graph.vertices()) {
            if (!origen.equals(v)) {
                verticesNoAgregados.add(v);
            }
        }
        while (!verticesNoAgregados.isEmpty()) {
            boolean agregoArista = false;
            for (Edge<V, D> arista : ordenadas) {
                if ((grafo.existeVertice(grafo.construirComparable(arista.source()))
                        || grafo.existeVertice(grafo.construirComparable(arista.target())))
                        && !(grafo.existeVertice(grafo.construirComparable(arista.source()))
                                && grafo.existeVertice(grafo.construirComparable(arista.target())))) {

                    if (grafo.existeVertice(grafo.construirComparable(arista.source()))) {
                        verticesNoAgregados.remove((arista.target()));
                        grafo.agregarVertice(arista.target());
                    } else {
                        verticesNoAgregados.remove((arista.source()));
                        grafo.agregarVertice(arista.source());

                    }

                    agregoArista = grafo.agregarArista(arista.source(), arista.target(), arista.dato());
                    break;

                }
            }
            if (!agregoArista)
                break;
        }
        return grafo;

    }

    /**
     * Ejecuta prim sin un vértice origen
     */
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(IUndirectedGraph<V, D> graph) {

        for (V vertex : graph.vertices()) {
            return prim(graph, graph.construirComparable(vertex));
        }

        return null;
    }

    /**
     * Retorna la mínima arista (u,v) del grafo "graph", tal que u está en U, y v
     * está en V.
     * Este método es útil para implementar "Prim"
     */
    public <V, D extends WeightedEdge> Edge<V, D> searchMinEdge(IUndirectedGraph<V, D> graph, Collection<V> U, Collection<V> V) {
        if (U == null || graph == null || null == V)
            return null;

        Set<Edge<V, D>> aristas = new LinkedHashSet<>();
        aristas = graph.aristas();
        List<Edge<V, D>> ordenadas = new ArrayList<>(aristas);

        ordenadas.sort(
                Comparator.comparingDouble(e -> e.dato().getWeight()));

        for (Edge<V, D> arista : ordenadas) {
            if ((graph.existeVertice(graph.construirComparable(arista.source()))
                    || graph.existeVertice(graph.construirComparable(arista.target())))
                    && !(graph.existeVertice(graph.construirComparable(arista.source()))
                            && graph.existeVertice(graph.construirComparable(arista.target())))) {

                if (graph.existeVertice(graph.construirComparable(arista.source()))) {
                    V.remove((arista.target()));
                    graph.agregarVertice(arista.target());
                } else {
                    V.remove((arista.source()));
                    graph.agregarVertice(arista.source());

                }
                return arista;

            }
        }
        return null;

    }

    /**
     * Implementa el algoritmo de búsqueda en amplitud
     */
    public <V, D> void bea(IUndirectedGraph<V, D> graph, Consumer<V> consumer) {

        Set<V> visitados = new HashSet<>();
        Deque<V> pendientes = new ArrayDeque<>();

        for (V inicio : graph.vertices()) {
            if (!visitados.add(inicio)) continue;

            pendientes.addLast(inicio);
            while (!pendientes.isEmpty()) {
                V actual = pendientes.removeFirst();
                consumer.accept(actual);

                for (Edge<V, D> arista : graph.adyacencias(graph.construirComparable(actual))) {
                    V vecino = Objects.equals(arista.source(), actual)
                            ? arista.target()
                            : arista.source();

                    if (visitados.add(vecino)) {
                        pendientes.addLast(vecino);
                    }
                }
            }
        }
    }


    @Override
    public <V, D> Set<V> puntosDeArticulacion(IUndirectedGraph<V, D> graph) {
        Objects.requireNonNull(graph, "graph");

        Map<V, Integer> descubrimiento = new HashMap<>();
        Map<V, Integer> low = new HashMap<>();
        Set<V> articulaciones = new LinkedHashSet<>();
        int[] tiempo = {0};

        // Procesar cada componente, incluso si el grafo es desconectado.
        for (V vertice : graph.vertices()) {
            if (!descubrimiento.containsKey(vertice)) {
                dfsArticulaciones(graph, vertice, null, descubrimiento, low,
                        articulaciones, tiempo);
            }
        }

        return Collections.unmodifiableSet(articulaciones);
    }

    private <V, D> void dfsArticulaciones(
            IUndirectedGraph<V, D> graph,
            V actual,
            V padre,
            Map<V, Integer> descubrimiento,
            Map<V, Integer> low,
            Set<V> articulaciones,
            int[] tiempo) {

        int tiempoActual = tiempo[0]++;
        descubrimiento.put(actual, tiempoActual);
        low.put(actual, tiempoActual);
        int hijos = 0;

        for (Edge<V, D> arista : graph.adyacencias(graph.construirComparable(actual))) {
            V vecino = Objects.equals(arista.source(), actual)
                    ? arista.target()
                    : arista.source();

            if (Objects.equals(vecino, padre)) continue;

            if (!descubrimiento.containsKey(vecino)) {
                hijos++;
                dfsArticulaciones(graph, vecino, actual, descubrimiento, low,
                        articulaciones, tiempo);

                low.put(actual, Math.min(low.get(actual), low.get(vecino)));

                // La raíz se evalúa aparte: debe tener más de un hijo DFS.
                if (padre != null && low.get(vecino) >= descubrimiento.get(actual)) {
                    articulaciones.add(actual);
                }
            } else {
                low.put(actual, Math.min(low.get(actual), descubrimiento.get(vecino)));
            }
        }

        if (padre == null && hijos > 1) {
            articulaciones.add(actual);
        }
    }
}
