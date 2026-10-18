package ucu.edu.aed.tda.grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import ucu.edu.aed.tda.grafo.model.IGraph;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.FloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.Path;
import java.util.Set;
import java.util.Stack;
import java.util.LinkedHashSet;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

/**
 * Implementación de {@link IDirectedGraphAlgorithms}.
 *
 * <p>
 * La interfaz del proyecto base agrupa en un solo contrato dos familias de
 * algoritmos
 * bastante distintas: las que calculan distancias (Dijkstra, Floyd, centro,
 * excentricidad,
 * todos los caminos) y las de recorrido y conectividad (Warshall, DFS, BEA,
 * clasificación
 * topológica). Esta clase cumple el contrato completo pero no implementa nada
 * por sí misma:
 * delega en una clase por familia.
 * </p>
 *
 * <p>
 * La razón es organizativa antes que técnica. Cada familia tiene un responsable
 * distinto
 * en el equipo y la defensa es individual, así que conviene que cada uno tenga
 * su propia
 * clase, con sus propios tests, en lugar de compartir un archivo que las dos
 * personas
 * editan al mismo tiempo. Esta fachada es el único punto donde se juntan, y
 * cambiar un
 * delegado es cambiar una línea.
 * </p>
 */
public class DirectedGraphAlgorithms implements IDirectedGraphAlgorithms {

    private final ShortestPathAlgorithms caminosMinimos = new ShortestPathAlgorithms();

    // =======================================================================
    // Caminos mínimos y métricas de distancia
    // =======================================================================

    @Override
    public <V, D extends WeightedEdge> IDijkstraResult<V> dijkstra(Comparable<V> source, IDirectedIGraph<V, D> grafo) {
        return caminosMinimos.dijkstra(source, grafo);
    }

    @Override
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> floyd(IDirectedIGraph<V, D> grafo) {
        return caminosMinimos.floyd(grafo);
    }

    @Override
    public <V, D extends WeightedEdge> V obtenerCentroGrafo(IDirectedIGraph<V, D> grafo) {
        return caminosMinimos.obtenerCentroGrafo(grafo);
    }

    @Override
    public <V, D extends WeightedEdge> double obtenerExcentricidad(IDirectedIGraph<V, D> grafo,
            Comparable<V> vertexCriteria) {
        return caminosMinimos.obtenerExcentricidad(grafo, vertexCriteria);
    }

    @Override
    public <V, D extends WeightedEdge> List<Path<V>> obtenerTodosLosCaminos(Comparable<V> source, Comparable<V> target,
            IGraph<V, D> grafo) {
        return caminosMinimos.obtenerTodosLosCaminos(source, target, grafo);
    }

    // =======================================================================
    // Recorridos y conectividad: pendientes de implementar
    // =======================================================================

    @Override
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> warshall(IDirectedIGraph<V, D> grafo) {
        if (grafo == null)
            throw new NullPointerException("grafo");

        List<V> vertices = new ArrayList<>(grafo.vertices());
        int n = vertices.size();
        Map<V, Integer> indices = new HashMap<>();
        for (int i = 0; i < n; i++)
            indices.put(vertices.get(i), i);

        boolean[][] alcanzable = new boolean[n][n];
        double[][] costos = new double[n][n];
        int[][] intermedios = new int[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(costos[i], Double.POSITIVE_INFINITY);
            Arrays.fill(intermedios[i], FloydWarshallResult.SIN_INTERMEDIO);
            alcanzable[i][i] = true;
            costos[i][i] = 0.0;
        }

        // Las aristas directas forman los casos base de la clausura transitiva.
        for (V origen : vertices) {
            int i = indices.get(origen);
            for (V destino : grafo.successors(grafo.construirComparable(origen))) {
                Integer j = indices.get(destino);
                if (j != null) {
                    alcanzable[i][j] = true;
                    costos[i][j] = 0.0;
                }
            }
        }

        // Permite caminos que pasan por el vértice k.
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (!alcanzable[i][k])
                    continue;
                for (int j = 0; j < n; j++) {
                    if (!alcanzable[i][j] && alcanzable[k][j]) {
                        alcanzable[i][j] = true;
                        costos[i][j] = 0.0;
                        intermedios[i][j] = k;
                    }
                }
            }
        }

        return new FloydWarshallResult<>(vertices, costos, intermedios);
    }

    @Override
    public <V, D> void recorridoEnProfundidad(IGraph<V, D> grafo, Comparable<V> sourceCriteria, Consumer<V> consumer) {
        List<V> visitiados = new LinkedList<>();
        if (grafo.buscarVertice(sourceCriteria) == null)
            return;

        dfs(grafo, sourceCriteria, consumer, visitiados);

    }

    private <V, D> void dfs(IGraph<V, D> grafo, Comparable<V> sourceCriteria, Consumer<V> consumer,
            List<V> listaVisitados) {
        if (grafo.buscarVertice(sourceCriteria) == null || listaVisitados.contains(grafo.buscarVertice(sourceCriteria)))
            return;
        listaVisitados.add(grafo.buscarVertice(sourceCriteria));

        List<Edge<V, D>> aristas = new LinkedList<>();
        consumer.accept(grafo.buscarVertice(sourceCriteria));
        aristas = grafo.adyacencias(sourceCriteria);

        for (Edge<V, D> arista : aristas) {
            dfs(grafo, grafo.construirComparable(arista.target()), consumer, listaVisitados);
        }

    }

    @Override
    public <V, D> void recorridoEnAmplitud(IGraph<V, D> grafo, Comparable<V> sourceCriteria, Consumer<V> consumer) {
        Queue<V> cola = new LinkedList<>();
        Set<V> visitados = new HashSet<>();
        if (grafo.buscarVertice(sourceCriteria) == null)
            return;
        V verticeInicial = grafo.buscarVertice(sourceCriteria);
        visitados.add(verticeInicial);
        cola.add(verticeInicial);

        consumer.accept(verticeInicial);

        while (!cola.isEmpty()) {
            V actual = cola.poll();

            for (Edge<V, D> arista : grafo.adyacencias(grafo.construirComparable(actual))) {

                V vecino;

                if (arista.directed()) {
                    if (!actual.equals(arista.source()))
                        continue;
                    vecino = arista.target();
                } else {
                    vecino = actual.equals(arista.source())
                            ? arista.target()
                            : arista.source();
                }

                if (visitados.add(vecino)) {
                    consumer.accept(vecino);
                    cola.add(vecino);
                }
            }
        }
    }

    @Override
    public <V, D> List<V> calcularClasificacionTopologica(
            IDirectedIGraph<V, D> grafo) {

        if (grafo == null || grafo.tieneCiclos())
            return null;

        Set<V> visitados = new HashSet<>();
        Deque<V> pila = new ArrayDeque<>();

        for (V vertice : grafo.vertices()) {
            if (!visitados.contains(vertice)) {
                dfsTopologico(grafo, vertice, visitados, pila);
            }
        }

        List<V> resultado = new ArrayList<>();
        while (!pila.isEmpty()) {
            resultado.add(pila.pop());
        }

        return resultado;
    }

    private <V, D> void dfsTopologico(
            IDirectedIGraph<V, D> grafo,
            V actual,
            Set<V> visitados,
            Deque<V> pila) {

        visitados.add(actual);

        for (V sucesor : grafo.successors(grafo.construirComparable(actual))) {
            if (!visitados.contains(sucesor)) {
                dfsTopologico(grafo, sucesor, visitados, pila);
            }
        }

        // Se apila al terminar de visitar los sucesores.
        pila.push(actual);
    }
}
