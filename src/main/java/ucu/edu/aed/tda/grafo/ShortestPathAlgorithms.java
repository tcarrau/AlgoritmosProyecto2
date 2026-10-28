package ucu.edu.aed.tda.grafo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import ucu.edu.aed.tda.grafo.model.IGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.DijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.FloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.Path;

/**
 * Algoritmos de caminos mínimos y métricas de distancia sobre grafos:
 * Dijkstra, Floyd, excentricidad, centro del grafo y todos los caminos entre dos vértices.
 *
 * <p>La clase no tiene estado: todas las operaciones son funciones del grafo recibido,
 * de modo que una misma instancia puede compartirse sin sincronización.</p>
 *
 * <h2>Decisión de diseño: índice propio de adyacencias</h2>
 * <p>El TDA identifica vértices mediante un {@code Comparable} usado como criterio de
 * búsqueda, por lo que resolver un criterio cuesta O(V) en las implementaciones del grupo
 * (recorren los vértices comparando). Si cada relajación de Dijkstra llamara a
 * {@code adyacencias(criterio)}, el costo de resolver se multiplicaría por cada arista
 * visitada y Dijkstra degradaría a O(V*E).</p>
 *
 * <p>Por eso cada algoritmo construye una única vez, en O(V + E), un índice de adyacencias
 * a partir de {@code vertices()} y {@code aristas()}, y desde ahí accede a las adyacencias
 * en O(1) amortizado. El índice es una vista de solo lectura: no modifica el grafo ni
 * sustituye su representación.</p>
 *
 * <h2>Grafos no dirigidos</h2>
 * <p>{@code obtenerTodosLosCaminos} recibe un {@link IGraph} y por lo tanto puede recibir
 * un grafo no dirigido. En ese caso cada arista se indexa en ambos sentidos y el vecino se
 * calcula con {@code otroExtremo}, sin asumir que {@code source()} sea el vértice
 * consultado. Esto es necesario porque {@code UndirectedEdge} conserva el orden con que
 * fue creada: para una arista entre a y b, {@code source()} devuelve a tanto si se pide la
 * adyacencia de a como la de b.</p>
 */
public class ShortestPathAlgorithms {

    // =======================================================================
    // Dijkstra
    // =======================================================================

    /**
     * Calcula los caminos de costo mínimo desde {@code source} al resto de los vértices.
     *
     * <p>Usa una cola de prioridad con borrado diferido: en lugar de reordenar la cola
     * cuando mejora una distancia, encola una entrada nueva y descarta las obsoletas al
     * extraerlas. Ante costos iguales desempata por el orden de inserción del vértice en
     * el grafo, de modo que el resultado es reproducible cuando hay caminos alternativos
     * del mismo costo.</p>
     *
     * <p><b>Complejidad:</b> O(V + E) para construir el índice, O(E) para validar los pesos
     * y O((V + E)*log V) para el recorrido, es decir <b>O((V + E)*log V)</b> en tiempo y
     * O(V + E) en espacio. El log V viene del tamaño de la cola, que puede llegar a tener
     * una entrada por arista.</p>
     *
     * @throws IllegalArgumentException si el vértice origen no pertenece al grafo,
     *                                 o si alguna arista tiene peso negativo
     * @throws IllegalStateException    si alguna arista no tiene peso (dato nulo)
     */
    public <V, D extends WeightedEdge> IDijkstraResult<V> dijkstra(Comparable<V> source, IDirectedIGraph<V, D> grafo) {
        Objects.requireNonNull(grafo, "grafo");
        V origen = resolver(grafo, source);
        verificarPesosNoNegativos(grafo);

        Map<V, List<Edge<V, D>>> salientes = indexarSalientes(grafo);
        Map<V, Integer> orden = indexarOrden(grafo.vertices());

        return nucleoDijkstra(origen, grafo.vertices(), salientes, orden);
    }

    /**
     * Corre Dijkstra desde cada vértice reutilizando un único índice de adyacencias.
     *
     * <p>Es la misma cuenta que llamar a {@link #dijkstra} una vez por vértice, pero sin
     * repetir por cada llamada lo que no depende del origen: resolver el criterio O(V),
     * validar los pesos O(E) y construir el índice O(V + E). Esas tres cosas pasan de
     * pagarse V veces a pagarse una sola.</p>
     *
     * <p><b>Complejidad:</b> O(V + E) de preparación más O(V*(V + E)*log V) de las V
     * corridas, contra O(V*(V + E)) solo de preparación en la versión que llama V veces a
     * {@code dijkstra}. La diferencia no cambia el orden asintótico, pero en la práctica es
     * la que decide si conviene frente a Floyd: ver las mediciones del Desafío 3.</p>
     *
     * @return para cada vértice, el resultado de Dijkstra tomándolo como origen
     */
    public <V, D extends WeightedEdge> Map<V, IDijkstraResult<V>> dijkstraDesdeTodos(IDirectedIGraph<V, D> grafo) {
        Objects.requireNonNull(grafo, "grafo");
        verificarPesosNoNegativos(grafo);

        Map<V, List<Edge<V, D>>> salientes = indexarSalientes(grafo);
        Map<V, Integer> orden = indexarOrden(grafo.vertices());

        Map<V, IDijkstraResult<V>> resultados = new LinkedHashMap<>();
        for (V origen : grafo.vertices()) {
            resultados.put(origen, nucleoDijkstra(origen, grafo.vertices(), salientes, orden));
        }
        return resultados;
    }

    /**
     * El algoritmo propiamente dicho, una vez que ya están resueltos el origen y el índice.
     * Lo comparten {@link #dijkstra} y {@link #dijkstraDesdeTodos} para que no haya dos
     * versiones del mismo recorrido que puedan divergir.
     */
    private static <V, D extends WeightedEdge> IDijkstraResult<V> nucleoDijkstra(
            V origen, Collection<V> vertices, Map<V, List<Edge<V, D>>> salientes, Map<V, Integer> orden) {

        Map<V, Double> costos = new LinkedHashMap<>();
        for (V v : vertices) costos.put(v, Double.POSITIVE_INFINITY);
        costos.put(origen, 0.0);

        Map<V, V> previos = new LinkedHashMap<>();
        Set<V> cerrados = new HashSet<>();

        PriorityQueue<Candidato<V>> cola = new PriorityQueue<>(
                Comparator.<Candidato<V>>comparingDouble(c -> c.costo).thenComparingInt(c -> c.orden));
        cola.add(new Candidato<>(origen, 0.0, orden.getOrDefault(origen, Integer.MAX_VALUE)));

        while (!cola.isEmpty()) {
            Candidato<V> actual = cola.poll();
            if (!cerrados.add(actual.vertice)) continue; // entrada obsoleta de la cola

            for (Edge<V, D> arista : salientes.getOrDefault(actual.vertice, List.of())) {
                V vecino = otroExtremo(arista, actual.vertice);
                if (cerrados.contains(vecino)) continue;

                double candidato = actual.costo + peso(arista);
                if (candidato < costos.getOrDefault(vecino, Double.POSITIVE_INFINITY)) {
                    costos.put(vecino, candidato);
                    previos.put(vecino, actual.vertice);
                    cola.add(new Candidato<>(vecino, candidato, orden.getOrDefault(vecino, Integer.MAX_VALUE)));
                }
            }
        }

        return new DijkstraResult<>(origen, costos, previos);
    }

    // =======================================================================
    // Floyd
    // =======================================================================

    /**
     * Calcula el costo mínimo entre todos los pares de vértices y la información
     * necesaria para reconstruir cada camino.
     *
     * <p>A diferencia de Dijkstra, admite pesos negativos (no así ciclos de costo
     * negativo, que hacen que los caminos mínimos no estén definidos y se reportan como
     * error). La diagonal arranca en 0, por lo que un bucle de peso positivo nunca mejora
     * un camino y uno de peso negativo queda detectado como ciclo negativo.</p>
     *
     * <p><b>Complejidad:</b> <b>O(V^3)</b> en tiempo por el triple ciclo, y O(V^2) en
     * espacio por las matrices de costos y de vértices intermedios. Conviene frente a
     * correr V veces Dijkstra (O(V*(V+E)*log V)) cuando el grafo es denso, es decir cuando
     * E se acerca a V^2.</p>
     *
     * @throws IllegalStateException si alguna arista no tiene peso, o si el grafo tiene
     *                               un ciclo de costo negativo
     */
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> floyd(IDirectedIGraph<V, D> grafo) {
        Objects.requireNonNull(grafo, "grafo");

        List<V> vertices = new ArrayList<>(grafo.vertices());
        int n = vertices.size();
        Map<V, Integer> indices = indexarOrden(vertices);

        double[][] costos = new double[n][n];
        int[][] intermedios = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(costos[i], Double.POSITIVE_INFINITY);
            Arrays.fill(intermedios[i], FloydWarshallResult.SIN_INTERMEDIO);
            costos[i][i] = 0.0;
        }

        for (Edge<V, D> arista : grafo.aristas()) {
            Integer i = indices.get(arista.source());
            Integer j = indices.get(arista.target());
            if (i == null || j == null) continue;
            double w = peso(arista);
            if (w < costos[i][j]) costos[i][j] = w;
            if (!arista.directed() && w < costos[j][i]) costos[j][i] = w;
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (Double.isInfinite(costos[i][k])) continue; // k no aporta nada desde i
                for (int j = 0; j < n; j++) {
                    if (Double.isInfinite(costos[k][j])) continue;
                    double via = costos[i][k] + costos[k][j];
                    if (via < costos[i][j]) {
                        costos[i][j] = via;
                        intermedios[i][j] = k;
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            if (costos[i][i] < 0) {
                throw new IllegalStateException("El grafo tiene un ciclo de costo negativo que pasa por "
                        + vertices.get(i) + ": los caminos mínimos no están definidos");
            }
        }

        return new FloydWarshallResult<>(vertices, costos, intermedios);
    }

    // =======================================================================
    // Excentricidad y centro
    // =======================================================================

    /**
     * Excentricidad de un vértice: la mayor de sus distancias mínimas al resto del grafo.
     *
     * <p>Se usa la definición estricta: si algún vértice no es alcanzable desde
     * {@code vertexCriteria}, la excentricidad es {@link Double#POSITIVE_INFINITY}. Un
     * grafo de un solo vértice tiene excentricidad 0.</p>
     *
     * <p>Se resuelve con una sola corrida de Dijkstra en lugar de Floyd, porque solo hacen
     * falta las distancias desde un vértice.</p>
     *
     * <p><b>Complejidad:</b> <b>O((V + E)*log V)</b>, la de Dijkstra, más O(V) para tomar
     * el máximo.</p>
     *
     * @throws IllegalArgumentException si el vértice no pertenece al grafo o hay pesos
     *                                 negativos
     */
    public <V, D extends WeightedEdge> double obtenerExcentricidad(IDirectedIGraph<V, D> grafo, Comparable<V> vertexCriteria) {
        Objects.requireNonNull(grafo, "grafo");
        resolver(grafo, vertexCriteria);

        IDijkstraResult<V> desde = dijkstra(vertexCriteria, grafo);
        double excentricidad = 0.0;
        for (V otro : grafo.vertices()) {
            double costo = desde.getCost(otro);
            if (costo > excentricidad) excentricidad = costo;
        }
        return excentricidad;
    }

    /**
     * Centro del grafo: el vértice mejor posicionado respecto del resto.
     *
     * <p>En un grafo fuertemente conexo coincide con la definición clásica, el vértice de
     * menor excentricidad. Pero si el grafo no es fuertemente conexo la excentricidad
     * estricta es infinita para todos los vértices y la definición clásica no distingue
     * ninguno, cuando justamente ahí interesa saber quién está mejor ubicado. Por eso el
     * criterio es, en orden:</p>
     * <ol>
     *   <li>mayor cobertura: cantidad de vértices alcanzables desde el candidato;</li>
     *   <li>a igual cobertura, menor excentricidad restringida a los vértices que alcanza;</li>
     *   <li>a igualdad de ambas, el primero en el orden de inserción del grafo, para que
     *       el resultado sea determinístico.</li>
     * </ol>
     * <p>Cuando el grafo es fuertemente conexo todas las coberturas valen V-1 y el criterio
     * se reduce al clásico. Notar que esta excentricidad "por alcanzables" no es la misma
     * que devuelve {@link #obtenerExcentricidad}, que es la estricta.</p>
     *
     * <p><b>Complejidad:</b> <b>O(V^3)</b>, dominada por la única corrida de Floyd; la
     * selección posterior recorre la matriz una vez más, O(V^2).</p>
     *
     * @return el centro, o {@code null} si el grafo no tiene vértices
     */
    public <V, D extends WeightedEdge> V obtenerCentroGrafo(IDirectedIGraph<V, D> grafo) {
        Objects.requireNonNull(grafo, "grafo");
        if (grafo.vertices().isEmpty()) return null;

        IFloydWarshallResult<V> distancias = floyd(grafo);

        V mejor = null;
        int mejorCobertura = -1;
        double mejorExcentricidad = Double.POSITIVE_INFINITY;

        for (V candidato : grafo.vertices()) {
            int cobertura = 0;
            double excentricidad = 0.0;
            for (V otro : grafo.vertices()) {
                if (candidato.equals(otro)) continue;
                double costo = distancias.getCost(candidato, otro);
                if (Double.isInfinite(costo)) continue;
                cobertura++;
                if (costo > excentricidad) excentricidad = costo;
            }

            boolean mejora = cobertura > mejorCobertura
                    || (cobertura == mejorCobertura && excentricidad < mejorExcentricidad);
            if (mejora) {
                mejor = candidato;
                mejorCobertura = cobertura;
                mejorExcentricidad = excentricidad;
            }
        }

        return mejor;
    }

    // =======================================================================
    // Todos los caminos
    // =======================================================================

    /**
     * Devuelve todos los caminos <b>simples</b> (sin vértices repetidos) de
     * {@code source} a {@code target}, cada uno con su costo total.
     *
     * <p>Se restringe a caminos simples porque si se permitieran ciclos la cantidad de
     * caminos sería infinita. Si origen y destino coinciden, el resultado es el camino
     * trivial de un vértice y costo 0. El destino no se atraviesa: un camino termina al
     * llegar a él.</p>
     *
     * <p>Funciona sobre grafos dirigidos y no dirigidos; en los no dirigidos cada arista
     * se recorre en ambos sentidos.</p>
     *
     * <p><b>Complejidad:</b> exponencial por naturaleza del problema, <b>O(V!)</b> en el
     * peor caso (grafo completo), o más precisamente O(V * cantidad de caminos) ya que cada
     * camino encontrado se copia. El espacio es O(V) de pila de recursión más el tamaño del
     * resultado. Por eso no sirve como operación interactiva sobre una red grande: para el
     * dominio conviene acotarla (por ejemplo, solo caminos hasta un X% por encima del
     * óptimo de Dijkstra).</p>
     *
     * @throws IllegalArgumentException si el origen o el destino no pertenecen al grafo
     */
    public <V, D extends WeightedEdge> List<Path<V>> obtenerTodosLosCaminos(Comparable<V> source, Comparable<V> target, IGraph<V, D> grafo) {
        Objects.requireNonNull(grafo, "grafo");
        V origen = resolver(grafo, source);
        V destino = resolver(grafo, target);

        Map<V, List<Edge<V, D>>> salientes = indexarSalientes(grafo);

        List<Path<V>> caminos = new ArrayList<>();
        List<V> parcial = new ArrayList<>();
        Set<V> enCamino = new HashSet<>();
        parcial.add(origen);
        enCamino.add(origen);

        buscarCaminos(origen, destino, salientes, parcial, enCamino, 0.0, caminos);
        return List.copyOf(caminos);
    }

    /**
     * Backtracking: extiende {@code parcial} por cada arista saliente cuyo destino no esté
     * ya en el camino, y al llegar al destino guarda una copia inmodificable del camino
     * actual.
     */
    private static <V, D extends WeightedEdge> void buscarCaminos(
            V actual, V destino, Map<V, List<Edge<V, D>>> salientes,
            List<V> parcial, Set<V> enCamino, double costo, List<Path<V>> acumulado) {

        if (actual.equals(destino)) {
            acumulado.add(new Path<>(List.copyOf(parcial), costo));
            return;
        }

        for (Edge<V, D> arista : salientes.getOrDefault(actual, List.of())) {
            V vecino = otroExtremo(arista, actual);
            if (!enCamino.add(vecino)) continue; // ya está en el camino: evitamos el ciclo

            parcial.add(vecino);
            buscarCaminos(vecino, destino, salientes, parcial, enCamino, costo + peso(arista), acumulado);
            parcial.remove(parcial.size() - 1);
            enCamino.remove(vecino);
        }
    }

    // =======================================================================
    // Auxiliares
    // =======================================================================

    /**
     * Resuelve un criterio de búsqueda a un vértice concreto del grafo.
     *
     * <p>Un criterio que no corresponde a ningún vértice se trata como error de uso y no
     * como "todo infinito": de otro modo un vértice mal escrito sería indistinguible de un
     * vértice aislado, que es un resultado legítimo.</p>
     *
     * <p><b>Complejidad:</b> O(V), porque el criterio es un predicado y hay que probarlo
     * contra cada vértice. Se llama una sola vez por operación.</p>
     */
    private static <V, D> V resolver(IGraph<V, D> grafo, Comparable<V> criterio) {
        Objects.requireNonNull(criterio, "criterio");
        V vertice = grafo.buscarVertice(criterio);
        if (vertice == null) {
            throw new IllegalArgumentException("El vértice indicado no pertenece al grafo");
        }
        return vertice;
    }

    /**
     * Índice de adyacencias salientes construido en O(V + E) a partir del TDA.
     * Las aristas no dirigidas se indexan en los dos extremos.
     */
    private static <V, D> Map<V, List<Edge<V, D>>> indexarSalientes(IGraph<V, D> grafo) {
        Map<V, List<Edge<V, D>>> salientes = new LinkedHashMap<>();
        for (V vertice : grafo.vertices()) salientes.put(vertice, new ArrayList<>());

        for (Edge<V, D> arista : grafo.aristas()) {
            List<Edge<V, D>> desdeOrigen = salientes.get(arista.source());
            if (desdeOrigen != null) desdeOrigen.add(arista);

            if (!arista.directed()) {
                List<Edge<V, D>> desdeDestino = salientes.get(arista.target());
                if (desdeDestino != null) desdeDestino.add(arista);
            }
        }
        return salientes;
    }

    /**
     * Asigna a cada vértice su posición en el orden de iteración del grafo.
     * Sirve para desempatar costos iguales de forma determinística y para indexar matrices.
     */
    private static <V> Map<V, Integer> indexarOrden(Collection<V> vertices) {
        Map<V, Integer> orden = new LinkedHashMap<>();
        int i = 0;
        for (V vertice : vertices) orden.put(vertice, i++);
        return orden;
    }

    /**
     * Extremo de la arista opuesto a {@code desde}. En una arista dirigida siempre es el
     * destino; en una no dirigida depende de por cuál extremo se la esté recorriendo.
     */
    private static <V, D> V otroExtremo(Edge<V, D> arista, V desde) {
        return arista.source().equals(desde) ? arista.target() : arista.source();
    }

    /**
     * Peso de una arista, con error explícito si la arista no lo tiene.
     */
    private static double peso(Edge<?, ? extends WeightedEdge> arista) {
        WeightedEdge dato = arista.dato();
        if (dato == null) {
            throw new IllegalStateException("La arista " + arista + " no tiene peso asociado (dato nulo)");
        }
        return dato.getWeight();
    }

    /**
     * Dijkstra supone pesos no negativos: con un peso negativo puede cerrar un vértice
     * antes de encontrar su camino más corto. Lo validamos en O(E) y damos un error claro
     * en lugar de devolver un resultado silenciosamente incorrecto.
     */
    private static <V, D extends WeightedEdge> void verificarPesosNoNegativos(IGraph<V, D> grafo) {
        for (Edge<V, D> arista : grafo.aristas()) {
            if (peso(arista) < 0) {
                throw new IllegalArgumentException("Dijkstra no admite pesos negativos y la arista "
                        + arista + " tiene peso negativo; para ese caso usar floyd");
            }
        }
    }

    /**
     * Entrada de la cola de prioridad de Dijkstra. Guarda el costo con el que se encoló
     * para poder descartar entradas obsoletas, y el orden del vértice para desempatar.
     */
    private static final class Candidato<V> {
        private final V vertice;
        private final double costo;
        private final int orden;

        private Candidato(V vertice, double costo, int orden) {
            this.vertice = vertice;
            this.costo = costo;
            this.orden = orden;
        }
    }
}
