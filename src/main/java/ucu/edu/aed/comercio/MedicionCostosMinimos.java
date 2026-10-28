package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Arnes de medicion del Desafio 3: compara las tres variantes de
 * {@code costosMinimosEntreTodos} sobre redes sinteticas de tamano creciente.
 *
 * <p>Se corre con {@code java ucu.edu.aed.comercio.MedicionCostosMinimos} y escribe una
 * tabla en la salida estandar para pasar al informe.</p>
 *
 * <p>Las redes se generan dispersas a proposito, con un grado promedio fijo, porque asi es
 * la red del dominio: una ciudad o un puerto tiene unos pocos tramos, no uno hacia cada
 * otro nodo. La comparacion entre Floyd y Dijkstra solo tiene sentido en ese regimen, y la
 * ultima columna de la tabla muestra que pasa cuando la red se vuelve densa.</p>
 */
public final class MedicionCostosMinimos {

    private static final int REPETICIONES = 3;

    private MedicionCostosMinimos() {
    }

    public static void main(String[] args) {
        System.out.println("Desafio 3 - costosMinimosEntreTodos: Floyd contra V x Dijkstra contra cache");
        System.out.println("Redes sinteticas dispersas, grado promedio 4, mejor de " + REPETICIONES + " corridas.");
        System.out.println();
        encabezado();
        for (int nodos : new int[] {25, 50, 100, 200, 400, 800}) {
            medir(nodos, 4);
        }

        System.out.println();
        System.out.println("Red densa (grado promedio = nodos/2), donde E se acerca a V^2:");
        encabezado();
        for (int nodos : new int[] {100, 200, 400}) {
            medir(nodos, nodos / 2);
        }
    }

    private static void encabezado() {
        System.out.printf("%7s %8s %11s %11s %13s %11s %9s%n",
                "nodos", "tramos", "Floyd", "V.Dijkstra", "Dijkstra.1idx", "cache", "mejor");
        System.out.printf("%7s %8s %11s %11s %13s %11s %9s%n",
                "", "", "(ms)", "(ms)", "(ms)", "(ms)", "");
        System.out.println("-".repeat(78));
    }

    private static void medir(int cantidadNodos, int gradoPromedio) {
        SistemaComercio sistema = generar(cantidadNodos, Math.max(2, cantidadNodos / 10), gradoPromedio, 42);
        Producto producto = sistema.getProductos().get("soja");

        calentar(sistema, producto);

        double floyd = mejorTiempo(sistema, producto, AnalisisCostos.Motor.FLOYD);
        double dijkstra = mejorTiempo(sistema, producto, AnalisisCostos.Motor.DIJKSTRA);
        double unIndice = mejorTiempo(sistema, producto, AnalisisCostos.Motor.DIJKSTRA_INDICE_UNICO);
        double cache = mejorTiempoConCache(sistema, producto);

        String mejor = floyd <= dijkstra && floyd <= unIndice ? "Floyd"
                : unIndice <= dijkstra ? "1 indice" : "V.Dijkstra";

        System.out.printf("%7d %8d %11.2f %11.2f %13.2f %11.3f %9s%n",
                cantidadNodos, sistema.getTramos().size(), floyd, dijkstra, unIndice, cache, mejor);
    }

    /** Deja que el JIT compile los dos motores antes de medir. */
    private static void calentar(SistemaComercio sistema, Producto producto) {
        for (int i = 0; i < 2; i++) {
            for (AnalisisCostos.Motor motor : AnalisisCostos.Motor.values()) {
                new AnalisisCostos(sistema, motor, false).costosMinimosEntreTodos(producto);
            }
        }
    }

    private static double mejorTiempo(SistemaComercio sistema, Producto producto, AnalisisCostos.Motor motor) {
        double mejor = Double.MAX_VALUE;
        for (int i = 0; i < REPETICIONES; i++) {
            AnalisisCostos analisis = new AnalisisCostos(sistema, motor, false);
            long inicio = System.nanoTime();
            analisis.costosMinimosEntreTodos(producto);
            mejor = Math.min(mejor, (System.nanoTime() - inicio) / 1e6);
        }
        return mejor;
    }

    /**
     * Mide la segunda consulta y las siguientes, que es el caso que el cache mejora: la
     * primera paga el calculo completo igual.
     */
    private static double mejorTiempoConCache(SistemaComercio sistema, Producto producto) {
        AnalisisCostos analisis = new AnalisisCostos(sistema, AnalisisCostos.Motor.DIJKSTRA, true);
        analisis.costosMinimosEntreTodos(producto); // llena el cache

        double mejor = Double.MAX_VALUE;
        for (int i = 0; i < REPETICIONES; i++) {
            long inicio = System.nanoTime();
            analisis.costosMinimosEntreTodos(producto);
            mejor = Math.min(mejor, (System.nanoTime() - inicio) / 1e6);
        }
        if (analisis.getRecalculos() != 1) {
            throw new IllegalStateException("el cache deberia haber calculado una sola vez, calculo "
                    + analisis.getRecalculos() + " veces");
        }
        return mejor;
    }

    // =======================================================================
    // Generador de redes sinteticas
    // =======================================================================

    /**
     * Arma una red de {@code cantidadNodos} nodos repartidos en {@code cantidadPaises}
     * paises, con un anillo que garantiza que todos se alcancen y aristas extra al azar
     * hasta llegar al grado promedio pedido.
     *
     * <p>Se registran aranceles generales entre todos los pares de paises para que los
     * tramos que cruzan frontera sean utilizables; de otro modo
     * {@code Pais.obtenerArancel} devuelve -1 para un par sin datos y
     * {@code Tramo.esUtilizable} descarta el tramo, lo que dejaria la red casi vacia y las
     * mediciones no dirian nada.</p>
     */
    static SistemaComercio generar(int cantidadNodos, int cantidadPaises, int gradoPromedio, long semilla) {
        Random azar = new Random(semilla);
        SistemaComercio sistema = new SistemaComercio();

        Producto soja = new Producto();
        soja.setNombre("soja");
        soja.setCategoria("granos");
        soja.setTipoCarga("granel");
        soja.setValorUnitario(BigDecimal.valueOf(100));
        sistema.registrarProducto(soja);

        List<Pais> paises = new ArrayList<>();
        for (int i = 0; i < cantidadPaises; i++) {
            Pais pais = new Pais();
            pais.setNombre("P" + i);
            sistema.registrarPais(pais);
            paises.add(pais);
        }
        for (Pais origen : paises) {
            for (Pais destino : paises) {
                if (origen != destino) {
                    origen.getArancelesGenerales().put(destino, BigDecimal.valueOf(0.05));
                }
            }
        }

        Set<String> cargas = new LinkedHashSet<>(List.of("granel"));
        List<NodoLogistico> nodos = new ArrayList<>();
        for (int i = 0; i < cantidadNodos; i++) {
            NodoLogistico nodo = new NodoLogistico();
            nodo.setNombre("N" + i);
            nodo.setTipo(i % 3 == 0 ? "puerto" : "ciudad");
            nodo.setPais(paises.get(i % cantidadPaises));
            nodo.setTiposCargaAdmitidos(cargas);
            nodo.setActivo(true);
            sistema.registrarNodo(nodo);
            nodos.add(nodo);
        }

        // Anillo: asegura que la red sea fuertemente conexa.
        for (int i = 0; i < cantidadNodos; i++) {
            agregarTramo(sistema, nodos.get(i), nodos.get((i + 1) % cantidadNodos), cargas, azar);
        }

        // Aristas extra hasta alcanzar el grado promedio pedido.
        int extra = Math.max(0, cantidadNodos * (gradoPromedio - 1));
        for (int i = 0; i < extra; i++) {
            NodoLogistico origen = nodos.get(azar.nextInt(cantidadNodos));
            NodoLogistico destino = nodos.get(azar.nextInt(cantidadNodos));
            if (origen != destino) {
                agregarTramo(sistema, origen, destino, cargas, azar);
            }
        }

        return sistema;
    }

    private static void agregarTramo(SistemaComercio sistema, NodoLogistico origen, NodoLogistico destino,
                                     Set<String> cargas, Random azar) {
        Tramo tramo = new Tramo();
        tramo.setOrigen(origen);
        tramo.setDestino(destino);
        tramo.setMedio("barco");
        tramo.setCostoUnitario(BigDecimal.valueOf(1 + azar.nextInt(100)));
        tramo.setTiempo(BigDecimal.valueOf(1 + azar.nextInt(100)));
        tramo.setTiposCargaAdmitidos(cargas);
        tramo.setActivo(true);
        sistema.registrarTramo(tramo);
    }
}
