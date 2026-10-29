package ucu.edu.aed.tda.grafo.model.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultado de Floyd (y reutilizable por Warshall) sobre un grafo de V vértices.
 *
 * <p>Guarda la matriz de costos mínimos entre todos los pares y la matriz de
 * vértices intermedios que permite reconstruir cada camino. Ocupa O(V²) en espacio.</p>
 *
 * <p>Convenciones adoptadas:</p>
 * <ul>
 *   <li>{@code getCost(v, v)} es 0 y {@code connected(v, v)} es {@code true}
 *       (clausura reflexiva): todo vértice se alcanza a sí mismo con el camino trivial.</li>
 *   <li>{@code getCost} es {@link Double#POSITIVE_INFINITY} si no hay camino o si
 *       alguno de los vértices no pertenece al grafo.</li>
 *   <li>{@code getPath} es la lista vacía si no hay camino, e incluye ambos extremos
 *       cuando sí lo hay.</li>
 * </ul>
 */
public class FloydWarshallResult<V> implements IFloydWarshallResult<V> {

    /** Marca de "sin vértice intermedio" en la matriz de reconstrucción. */
    public static final int SIN_INTERMEDIO = -1;

    private final List<V> vertices;
    private final Map<V, Integer> indices;
    private final double[][] costos;
    private final int[][] intermedios;

    /**
     * @param vertices    vértices en el orden en que indexan las matrices
     * @param costos      costos[i][j] = costo mínimo de vertices[i] a vertices[j]
     * @param intermedios intermedios[i][j] = índice del vértice intermedio usado, o {@link #SIN_INTERMEDIO}
     */
    public FloydWarshallResult(List<V> vertices, double[][] costos, int[][] intermedios) {
        this.vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        this.costos = Objects.requireNonNull(costos, "costos");
        this.intermedios = Objects.requireNonNull(intermedios, "intermedios");

        Map<V, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < this.vertices.size(); i++) mapa.put(this.vertices.get(i), i);
        this.indices = Collections.unmodifiableMap(mapa);
    }

    /**
     * Vértices en el mismo orden en que indexan las matrices de costos.
     */
    public List<V> getVertices() {
        return vertices;
    }

    @Override
    public double getCost(V source, V target) {
        Integer i = indice(source), j = indice(target);
        return (i == null || j == null) ? Double.POSITIVE_INFINITY : costos[i][j];
    }

    @Override
    public boolean connected(V source, V target) {
        return !Double.isInfinite(getCost(source, target));
    }

    @Override
    public List<V> getPath(V source, V target) {
        Integer i = indice(source), j = indice(target);
        if (i == null || j == null || Double.isInfinite(costos[i][j])) return List.of();
        if (i.equals(j)) return List.of(vertices.get(i));

        List<V> camino = new ArrayList<>();
        camino.add(vertices.get(i));
        agregarIntermedios(i, j, camino);
        camino.add(vertices.get(j));
        return Collections.unmodifiableList(camino);
    }

    private Integer indice(V vertice) {
        return vertice == null ? null : indices.get(vertice);
    }

    /**
     * Inserta en {@code acc} los vértices que quedan estrictamente entre i y j,
     * recorriendo la matriz de intermedios en orden.
     */
    private void agregarIntermedios(int i, int j, List<V> acc) {
        int k = intermedios[i][j];
        if (k == SIN_INTERMEDIO) return;
        agregarIntermedios(i, k, acc);
        acc.add(vertices.get(k));
        agregarIntermedios(k, j, acc);
    }
}
