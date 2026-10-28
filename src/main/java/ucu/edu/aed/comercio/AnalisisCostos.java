package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.ShortestPathAlgorithms;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

/**
 * Desafio 3: alternativas para calcular los costos minimos entre todos los pares de
 * nodos, y comparacion con la implementacion original.
 *
 * <h2>1. La operacion elegida</h2>
 * <p>{@link SistemaComercio#costosMinimosEntreTodos(Producto)}. Es la consulta mas cara
 * del sistema y la que la consultora repite mas: se usa para comparar escenarios (antes y
 * despues de una sancion, de un acuerdo o del cierre de un puerto), asi que se la llama
 * varias veces sobre una red que no cambio.</p>
 *
 * <h2>2. Por que es un cuello de botella</h2>
 * <p>La version original resuelve todo con una corrida de Floyd, O(V^3), y antes de eso
 * reconstruye el grafo del producto desde cero, O(V + E). Dos problemas distintos:</p>
 * <ul>
 *   <li><b>El algoritmo.</b> Floyd es O(V^3) sin importar cuantas aristas haya. La red de
 *       comercio es dispersa: cada ciudad o puerto tiene unos pocos tramos, asi que E se
 *       parece mas a V que a V^2. En ese regimen conviene correr V veces Dijkstra, que
 *       cuesta O(V*(V + E)*log V).</li>
 *   <li><b>La repeticion.</b> Dos llamadas seguidas con el mismo producto y la red sin
 *       cambios repiten todo el trabajo.</li>
 * </ul>
 *
 * <h2>3. Las alternativas implementadas</h2>
 * <p><b>Motor.</b> {@link Motor#FLOYD} reproduce la version original;
 * {@link Motor#DIJKSTRA} corre Dijkstra desde cada nodo. Los dos devuelven exactamente lo
 * mismo, incluido que los pares sin ruta no aparecen en el mapa.</p>
 *
 * <p><b>Cache con invalidacion por huella.</b> El resultado se guarda por producto. Para
 * saber si sigue valiendo se compara una huella de la red calculada en O(V + E + aranceles).
 * No se usa un contador de version incrementado por los metodos que modifican el sistema
 * porque no serviria: el modelo expone sus colecciones mutables a traves de los getters, y
 * tanto el cargador del dataset como las consultas de escenarios modifican embargos,
 * aranceles y restricciones directamente sobre esos mapas, sin pasar por ningun metodo del
 * sistema. Un contador se perderia esos cambios y el cache devolveria resultados viejos,
 * que es peor que no cachear. La huella los ve a todos porque los lee.</p>
 *
 * <p>El costo de validar queda entonces en O(V + E) contra O(V^3) de recalcular, que es la
 * comparacion que justifica el cache.</p>
 *
 * <h2>4. Como se comparan</h2>
 * <p>Ver {@code MedicionCostosMinimos}, que genera redes sinteticas de tamano creciente y
 * mide las tres variantes, y {@code AnalisisCostosTest}, que verifica que las tres dan el
 * mismo resultado que la implementacion original y que la invalidacion funciona.</p>
 */
public class AnalisisCostos {

    /** Algoritmo usado para resolver todos los pares. */
    public enum Motor {
        /** Una corrida de Floyd: O(V^3). Es lo que hace la version original. */
        FLOYD,
        /**
         * Una llamada a {@code dijkstra} por nodo: O(V*(V + E)*log V) en teoria, pero cada
         * llamada rehace lo que no depende del origen (resolver el criterio, validar los
         * pesos, construir el indice), asi que paga O(V*(V + E)) solo de preparacion.
         */
        DIJKSTRA,
        /**
         * Dijkstra desde cada nodo con un unico indice de adyacencias compartido, via
         * {@code dijkstraDesdeTodos}. Es la misma cuenta que {@link #DIJKSTRA} pero
         * pagando la preparacion una sola vez, O(V + E).
         */
        DIJKSTRA_INDICE_UNICO
    }

    private static final ShortestPathAlgorithms ALGORITMOS = new ShortestPathAlgorithms();

    private final SistemaComercio sistema;
    private final Motor motor;
    private final boolean cachear;

    private final Map<String, Map<NodoLogistico, Map<NodoLogistico, BigDecimal>>> cache = new LinkedHashMap<>();
    private long huellaDelCache;
    private boolean cacheIniciado;
    private int recalculos;
    private int invalidaciones;

    public AnalisisCostos(SistemaComercio sistema, Motor motor) {
        this(sistema, motor, true);
    }

    public AnalisisCostos(SistemaComercio sistema, Motor motor, boolean cachear) {
        this.sistema = Objects.requireNonNull(sistema, "sistema");
        this.motor = Objects.requireNonNull(motor, "motor");
        this.cachear = cachear;
    }

    /**
     * Costos minimos entre todos los pares de nodos que sirven para el producto, con el
     * mismo contrato que {@link SistemaComercio#costosMinimosEntreTodos(Producto)}: los
     * pares sin ruta no aparecen y la diagonal vale 0.
     */
    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        Objects.requireNonNull(producto, "producto");

        if (!cachear) {
            recalculos++;
            return calcular(producto);
        }

        long huellaActual = huellaDe(sistema);
        if (!cacheIniciado || huellaActual != huellaDelCache) {
            if (cacheIniciado) {
                invalidaciones++;
            }
            cache.clear();
            huellaDelCache = huellaActual;
            cacheIniciado = true;
        }

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> guardado = cache.get(producto.getNombre());
        if (guardado != null) {
            return guardado;
        }

        recalculos++;
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> calculado = calcular(producto);
        cache.put(producto.getNombre(), calculado);
        return calculado;
    }

    /** Cuantas veces se calculo de verdad, sin contar los aciertos del cache. */
    public int getRecalculos() {
        return recalculos;
    }

    /** Cuantas veces la huella cambio y hubo que descartar lo guardado. */
    public int getInvalidaciones() {
        return invalidaciones;
    }

    // =======================================================================
    // Motores
    // =======================================================================

    private Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> calcular(Producto producto) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = sistema.construirGrafo(producto, "COSTO");
        switch (motor) {
            case FLOYD:
                return porFloyd(grafo);
            case DIJKSTRA:
                return porDijkstra(grafo);
            case DIJKSTRA_INDICE_UNICO:
                return porDijkstraConIndiceUnico(grafo);
            default:
                throw new IllegalStateException("motor desconocido: " + motor);
        }
    }

    /**
     * Igual que {@link #porDijkstra} pero con un unico indice de adyacencias para las V
     * corridas, en lugar de reconstruirlo en cada llamada.
     */
    private Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> porDijkstraConIndiceUnico(
            DirectedGraph<NodoLogistico, ArcoTramo> grafo) {

        Map<NodoLogistico, IDijkstraResult<NodoLogistico>> resultados = ALGORITMOS.dijkstraDesdeTodos(grafo);

        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = new LinkedHashMap<>();
        for (Map.Entry<NodoLogistico, IDijkstraResult<NodoLogistico>> entrada : resultados.entrySet()) {
            Map<NodoLogistico, BigDecimal> fila = new LinkedHashMap<>();
            for (NodoLogistico hasta : grafo.vertices()) {
                double costo = entrada.getValue().getCost(hasta);
                if (!Double.isInfinite(costo)) {
                    fila.put(hasta, BigDecimal.valueOf(costo));
                }
            }
            costos.put(entrada.getKey(), fila);
        }
        return costos;
    }

    /** O(V^3): una sola corrida de Floyd llena la matriz completa. */
    private Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> porFloyd(DirectedGraph<NodoLogistico, ArcoTramo> grafo) {
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
     * O(V*(V + E)*log V): una corrida de Dijkstra por nodo. Cada corrida recorre solo las
     * aristas que existen, asi que en una red dispersa hace mucho menos trabajo que el
     * triple ciclo de Floyd, que paga V^3 incluso sobre una matriz casi vacia.
     */
    private Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> porDijkstra(DirectedGraph<NodoLogistico, ArcoTramo> grafo) {
        Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costos = new LinkedHashMap<>();
        for (NodoLogistico desde : grafo.vertices()) {
            IDijkstraResult<NodoLogistico> desdeNodo = ALGORITMOS.dijkstra(grafo.construirComparable(desde), grafo);
            Map<NodoLogistico, BigDecimal> fila = new LinkedHashMap<>();
            for (NodoLogistico hasta : grafo.vertices()) {
                double costo = desdeNodo.getCost(hasta);
                if (!Double.isInfinite(costo)) {
                    fila.put(hasta, BigDecimal.valueOf(costo));
                }
            }
            costos.put(desde, fila);
        }
        return costos;
    }

    // =======================================================================
    // Huella de la red
    // =======================================================================

    /**
     * Huella de todo lo que puede cambiar el grafo de un producto: los nodos con su pais,
     * sus cargas y si estan activos; los tramos con sus costos, tiempos, cargas y si estan
     * activos; y las relaciones comerciales de cada pais, que son las que definen si un
     * tramo que cruza frontera se puede usar y cuanto cuesta.
     *
     * <p><b>Complejidad:</b> O(V + E + cantidad de relaciones comerciales), que en esta red
     * es del mismo orden que armar el grafo y mucho menos que O(V^3).</p>
     */
    public static long huellaDe(SistemaComercio sistema) {
        long h = 17;

        for (NodoLogistico nodo : sistema.getNodos().values()) {
            h = mezclar(h, nodo.getNombre());
            h = mezclar(h, nodo.getActivo());
            h = mezclar(h, nodo.getPais() == null ? null : nodo.getPais().getNombre());
            h = mezclarTextos(h, nodo.getTiposCargaAdmitidos());
        }

        for (Tramo tramo : sistema.getTramos()) {
            h = mezclar(h, tramo.getOrigen() == null ? null : tramo.getOrigen().getNombre());
            h = mezclar(h, tramo.getDestino() == null ? null : tramo.getDestino().getNombre());
            h = mezclar(h, tramo.getMedio());
            h = mezclar(h, tramo.getCostoUnitario());
            h = mezclar(h, tramo.getTiempo());
            h = mezclar(h, tramo.getActivo());
            h = mezclarTextos(h, tramo.getTiposCargaAdmitidos());
        }

        for (Pais pais : sistema.getPaises().values()) {
            h = mezclar(h, pais.getNombre());
            for (Pais embargado : pais.getDestinosEmbargados()) {
                h = mezclar(h, embargado.getNombre());
            }
            for (Map.Entry<Pais, BigDecimal> e : pais.getArancelesGenerales().entrySet()) {
                h = mezclar(h, e.getKey().getNombre());
                h = mezclar(h, e.getValue());
            }
            for (Map.Entry<Pais, Map<Producto, BigDecimal>> e : pais.getArancelesPorProducto().entrySet()) {
                h = mezclar(h, e.getKey().getNombre());
                for (Map.Entry<Producto, BigDecimal> p : e.getValue().entrySet()) {
                    h = mezclar(h, p.getKey().getNombre());
                    h = mezclar(h, p.getValue());
                }
            }
            for (Map.Entry<Pais, Map<String, BigDecimal>> e : pais.getArancelesPorCategoria().entrySet()) {
                h = mezclar(h, e.getKey().getNombre());
                for (Map.Entry<String, BigDecimal> c : e.getValue().entrySet()) {
                    h = mezclar(h, c.getKey());
                    h = mezclar(h, c.getValue());
                }
            }
            for (Map.Entry<Pais, Set<Producto>> e : pais.getProductosRestringidos().entrySet()) {
                h = mezclar(h, e.getKey().getNombre());
                for (Producto restringido : e.getValue()) {
                    h = mezclar(h, restringido.getNombre());
                }
            }
        }

        return h;
    }

    private static long mezclar(long h, Object valor) {
        return h * 31 + (valor == null ? 0 : valor.hashCode());
    }

    /**
     * Los tipos de carga se mezclan sumando, no multiplicando, para que la huella no
     * dependa del orden en que el conjunto los itera.
     */
    private static long mezclarTextos(long h, Set<String> textos) {
        long suma = 0;
        if (textos != null) {
            for (String texto : textos) {
                suma += texto == null ? 0 : texto.hashCode();
            }
        }
        return h * 31 + suma;
    }
}
