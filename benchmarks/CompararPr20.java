import java.util.Arrays;
import java.util.Locale;
import ucu.edu.aed.comercio.ArcoTramo;
import ucu.edu.aed.comercio.ArnesMedicion;
import ucu.edu.aed.comercio.NodoLogistico;
import ucu.edu.aed.comercio.Producto;
import ucu.edu.aed.comercio.SistemaComercio;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

/** Misma carga para los commits anterior y posterior al PR #20. */
public final class CompararPr20 {
    private record Vertice(int id) {}
    private static volatile long resultado;

    public static void main(String[] args) {
        System.out.println("prueba,nodos,operaciones,mediana_ms,comprobacion");
        for (int nodos : new int[] {100, 500, 1000}) medirAcceso(nodos);
        for (int nodos : new int[] {100, 300, 600}) medirConstruccion(nodos);
    }

    private static void medirAcceso(int n) {
        DirectedGraph<Vertice, Integer> grafo = new DirectedGraph<>();
        Vertice[] vertices = new Vertice[n];
        for (int i = 0; i < n; i++) {
            vertices[i] = new Vertice(i);
            grafo.agregarVertice(vertices[i]);
        }
        for (int i = 0; i < n; i++) grafo.agregarArista(vertices[i], vertices[(i + 1) % n], i);

        int operaciones = 200_000;
        long esperado = 0;
        for (int i = 0; i < operaciones; i++) {
            if ((i & 1) == 0) esperado += (i * 37) % n + 1;
        }
        // Los vértices no implementan Comparable: se fuerza la sobrecarga (V,V).
        Runnable prueba = () -> {
            long suma = 0;
            for (int i = 0; i < operaciones; i++) {
                int origen = (i * 37) % n;
                int destino = (origen + ((i & 1) == 0 ? 1 : 2)) % n;
                Edge<Vertice, Integer> arista = grafo.obtenerArista(vertices[origen], vertices[destino]);
                if (arista != null) suma += arista.dato() + 1;
            }
            resultado = suma;
        };
        prueba.run();
        long comprobacion = resultado;
        if (comprobacion != esperado) throw new AssertionError("Consulta inválida");
        double ms = mediana(prueba, 3, 7);
        System.out.printf(Locale.US, "obtenerArista,%d,%d,%.3f,%d%n", n, operaciones, ms, comprobacion);
    }

    private static void medirConstruccion(int n) {
        SistemaComercio sistema = ArnesMedicion.generarRed(n, 42L);
        Producto producto = sistema.getProductos().get("Soja");
        Runnable prueba = () -> {
            DirectedGraph<NodoLogistico, ArcoTramo> grafo = sistema.construirGrafo(producto, "COSTO");
            long suma = grafo.vertices().size() * 1_000_000L + grafo.aristas().size();
            for (Edge<NodoLogistico, ArcoTramo> arista : grafo.aristas()) {
                suma += Math.round(arista.dato().getWeight());
            }
            resultado = suma;
        };
        prueba.run();
        long comprobacion = resultado;
        if (comprobacion <= 0) throw new AssertionError("Grafo vacío");
        double ms = mediana(prueba, 3, 7);
        System.out.printf(Locale.US, "construirGrafo,%d,%d,%.3f,%d%n", n, sistema.getTramos().size(), ms, comprobacion);
    }

    private static double mediana(Runnable prueba, int calentamientos, int muestras) {
        for (int i = 0; i < calentamientos; i++) prueba.run();
        double[] tiempos = new double[muestras];
        for (int i = 0; i < muestras; i++) {
            long inicio = System.nanoTime();
            prueba.run();
            tiempos[i] = (System.nanoTime() - inicio) / 1_000_000.0;
        }
        Arrays.sort(tiempos);
        return tiempos[muestras / 2];
    }
}
