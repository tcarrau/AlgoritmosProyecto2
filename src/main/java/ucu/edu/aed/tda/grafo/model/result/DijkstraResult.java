package ucu.edu.aed.tda.grafo.model.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultado inmutable de una corrida de Dijkstra desde un vértice origen.
 *
 * <p>Convenciones adoptadas (documentadas porque la interfaz no las fija):</p>
 * <ul>
 *   <li>{@code getCost(origen)} es 0.</li>
 *   <li>{@code getCost(v)} es {@link Double#POSITIVE_INFINITY} si v no es alcanzable
 *       desde el origen o si v no pertenece al grafo.</li>
 *   <li>{@code getPath(origen)} es la lista con el solo origen.</li>
 *   <li>{@code getPath(v)} es la lista vacía si v no es alcanzable o no pertenece al grafo,
 *       de modo que un camino inexistente nunca se confunde con un camino de largo 1.</li>
 * </ul>
 */
public class DijkstraResult<V> implements IDijkstraResult<V> {

    private final V origen;
    private final Map<V, Double> costos;
    private final Map<V, V> previos;

    /**
     * @param origen  vértice desde el que se corrió el algoritmo
     * @param costos  costo mínimo conocido para cada vértice del grafo
     * @param previos predecesor de cada vértice en el árbol de caminos mínimos
     */
    public DijkstraResult(V origen, Map<V, Double> costos, Map<V, V> previos) {
        this.origen = Objects.requireNonNull(origen, "origen");
        this.costos = Map.copyOf(Objects.requireNonNull(costos, "costos"));
        this.previos = Map.copyOf(Objects.requireNonNull(previos, "previos"));
    }

    /**
     * Vértice origen de esta corrida.
     */
    public V getOrigen() {
        return origen;
    }

    @Override
    public double getCost(V otherVertex) {
        if (otherVertex == null) return Double.POSITIVE_INFINITY;
        Double costo = costos.get(otherVertex);
        return costo == null ? Double.POSITIVE_INFINITY : costo;
    }

    @Override
    public List<V> getPath(V otherVertex) {
        if (otherVertex == null) return List.of();
        if (Double.isInfinite(getCost(otherVertex))) return List.of();
        if (origen.equals(otherVertex)) return List.of(origen);

        List<V> camino = new ArrayList<>();
        V actual = otherVertex;
        while (actual != null) {
            camino.add(actual);
            if (origen.equals(actual)) break;
            actual = previos.get(actual);
        }
        Collections.reverse(camino);
        return Collections.unmodifiableList(camino);
    }
}
