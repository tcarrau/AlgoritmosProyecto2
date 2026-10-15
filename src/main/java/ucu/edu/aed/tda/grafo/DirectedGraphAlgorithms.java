package ucu.edu.aed.tda.grafo;

import java.util.List;
import java.util.function.Consumer;

import ucu.edu.aed.tda.grafo.model.IGraph;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.Path;

/**
 * Implementación de {@link IDirectedGraphAlgorithms}.
 *
 * <p>La interfaz del proyecto base agrupa en un solo contrato dos familias de algoritmos
 * bastante distintas: las que calculan distancias (Dijkstra, Floyd, centro, excentricidad,
 * todos los caminos) y las de recorrido y conectividad (Warshall, DFS, BEA, clasificación
 * topológica). Esta clase cumple el contrato completo pero no implementa nada por sí misma:
 * delega en una clase por familia.</p>
 *
 * <p>La razón es organizativa antes que técnica. Cada familia tiene un responsable distinto
 * en el equipo y la defensa es individual, así que conviene que cada uno tenga su propia
 * clase, con sus propios tests, en lugar de compartir un archivo que las dos personas
 * editan al mismo tiempo. Esta fachada es el único punto donde se juntan, y cambiar un
 * delegado es cambiar una línea.</p>
 *
 * <p>Las operaciones de recorrido y conectividad todavía no están implementadas: cuando la
 * clase correspondiente exista, hay que reemplazar los cuatro métodos marcados más abajo
 * por una delegación análoga a la de {@link ShortestPathAlgorithms}.</p>
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
    public <V, D extends WeightedEdge> double obtenerExcentricidad(IDirectedIGraph<V, D> grafo, Comparable<V> vertexCriteria) {
        return caminosMinimos.obtenerExcentricidad(grafo, vertexCriteria);
    }

    @Override
    public <V, D extends WeightedEdge> List<Path<V>> obtenerTodosLosCaminos(Comparable<V> source, Comparable<V> target, IGraph<V, D> grafo) {
        return caminosMinimos.obtenerTodosLosCaminos(source, target, grafo);
    }

    // =======================================================================
    // Recorridos y conectividad: pendientes de implementar
    // =======================================================================

    @Override
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> warshall(IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("warshall todavía no está implementado");
    }

    @Override
    public <V, D> void recorridoEnProfundidad(IGraph<V, D> grafo, Comparable<V> sourceCriteria, Consumer<V> consumer) {
        throw new UnsupportedOperationException("recorridoEnProfundidad todavía no está implementado");
    }

    @Override
    public <V, D> void recorridoEnAmplitud(IGraph<V, D> grafo, Comparable<V> sourceCriteria, Consumer<V> consumer) {
        throw new UnsupportedOperationException("recorridoEnAmplitud todavía no está implementado");
    }

    @Override
    public <V, D> List<V> calcularClasificacionTopologica(IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("calcularClasificacionTopologica todavía no está implementado");
    }
}
