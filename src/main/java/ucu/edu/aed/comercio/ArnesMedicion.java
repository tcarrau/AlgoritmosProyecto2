package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ucu.edu.aed.tda.grafo.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraph;
import ucu.edu.aed.tda.grafo.UndirectedGraphAlgorithm;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;

public class ArnesMedicion {

    public static class ResultadoExperimento {
        public final int cantidadNodos;
        public final int cantidadTramos;
        public final int consultas;
        public final double msSinOptimizar;
        public final double msOptimizadoCache;
        public final double msPuntosArticulacion;

        public ResultadoExperimento(int cantidadNodos, int cantidadTramos, int consultas,
                                    double msSinOptimizar, double msOptimizadoCache, double msPuntosArticulacion) {
            this.cantidadNodos = cantidadNodos;
            this.cantidadTramos = cantidadTramos;
            this.consultas = consultas;
            this.msSinOptimizar = msSinOptimizar;
            this.msOptimizadoCache = msOptimizadoCache;
            this.msPuntosArticulacion = msPuntosArticulacion;
        }
    }


    public static SistemaComercio generarRed(int cantidadNodos, long semilla) {
        Random rnd = new Random(semilla);
        SistemaComercio sistema = new SistemaComercio();

        Producto soja = new Producto();
        soja.setNombre("Soja");
        soja.setCategoria("Granos");
        soja.setTipoCarga("GRANEL");
        soja.setValorUnitario(BigDecimal.valueOf(100));
        sistema.registrarProducto(soja);

        int cantPaises = Math.max(2, cantidadNodos / 3);
        List<Pais> listaPaises = new ArrayList<>();
        for (int i = 0; i < cantPaises; i++) {
            Pais p = new Pais();
            p.setNombre("Pais_" + i);
            sistema.registrarPais(p);
            listaPaises.add(p);
        }

        for (Pais p1 : listaPaises) {
            for (Pais p2 : listaPaises) {
                if (!p1.equals(p2)) p1.getArancelesGenerales().put(p2, BigDecimal.ZERO);
            }
        }

        List<NodoLogistico> listaNodos = new ArrayList<>();
        for (int i = 0; i < cantidadNodos; i++) {
            NodoLogistico n = new NodoLogistico();
            n.setNombre("Nodo_" + i);
            n.setTipo(i % 2 == 0 ? "PUERTO" : "CIUDAD");
            n.setPais(listaPaises.get(i % cantPaises));
            n.setTiposCargaAdmitidos(Collections.singleton("GRANEL"));
            n.setActivo(true);
            sistema.registrarNodo(n);
            listaNodos.add(n);
        }

        for (int i = 0; i < cantidadNodos - 1; i++) {
            agregarTramo(sistema, listaNodos.get(i), listaNodos.get(i + 1), 10 + rnd.nextInt(90));
        }
        int tramosExtra = cantidadNodos * 2;
        for (int i = 0; i < tramosExtra; i++) {
            NodoLogistico u = listaNodos.get(rnd.nextInt(cantidadNodos));
            NodoLogistico v = listaNodos.get(rnd.nextInt(cantidadNodos));
            if (!u.equals(v)) {
                agregarTramo(sistema, u, v, 10 + rnd.nextInt(200));
            }
        }
        return sistema;
    }

    private static void agregarTramo(SistemaComercio sistema, NodoLogistico orig, NodoLogistico dest, double costo) {
        Tramo t = new Tramo();
        t.setOrigen(orig);
        t.setDestino(dest);
        t.setMedio("MARITIMO");
        t.setCostoUnitario(BigDecimal.valueOf(costo));
        t.setTiempo(BigDecimal.valueOf(costo / 2.0));
        t.setTiposCargaAdmitidos(Collections.singleton("GRANEL"));
        t.setActivo(true);
        sistema.registrarTramo(t);
    }

    public static ResultadoExperimento medirEscenario(int cantidadNodos, int cantidadConsultas) {
        SistemaComercio sistema = generarRed(cantidadNodos, 42L);
        Producto prod = sistema.getProductos().get("Soja");
        List<Pais> paises = new ArrayList<>(sistema.getPaises().values());
        Pais pOrigen = paises.get(0);
        Pais pDestino = paises.get(paises.size() - 1);


        long t0 = System.nanoTime();
        boolean esperado = false;
        for (int q = 0; q < cantidadConsultas; q++) {
            esperado = consultarSinCache(sistema, pOrigen, pDestino, prod);
        }
        double msSinOptimizar = (System.nanoTime() - t0) / 1_000_000.0;

        long t1 = System.nanoTime();
        for (int q = 0; q < cantidadConsultas; q++) {
            if (sistema.puedeEnviarProducto(pOrigen, pDestino, prod) != esperado) {
                throw new IllegalStateException("La versión optimizada cambió el resultado");
            }
        }
        double msOptimizado = (System.nanoTime() - t1) / 1_000_000.0;

        long t2 = System.nanoTime();
        sistema.obtenerNodosCriticos();
        double msArticulacion = (System.nanoTime() - t2) / 1_000_000.0;

        return new ResultadoExperimento(
                cantidadNodos,
                sistema.getTramos().size(),
                cantidadConsultas,
                msSinOptimizar,
                msOptimizado,
                msArticulacion
        );
    }

    private static boolean consultarSinCache(SistemaComercio sistema, Pais origen,
                                              Pais destino, Producto producto) {
        IDirectedIGraph<NodoLogistico, WeightedEdge> grafo = sistema.construirGrafoDirigido(producto);
        IFloydWarshallResult<NodoLogistico> cacheWarshall = new DirectedGraphAlgorithms().warshall(grafo);
        for (NodoLogistico nOrig : grafo.vertices()) {
            if (!origen.equals(nOrig.getPais())) continue;
            for (NodoLogistico nDest : grafo.vertices()) {
                if (!destino.equals(nDest.getPais())) continue;
                if (!nOrig.equals(nDest) && cacheWarshall.connected(nOrig, nDest)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int[] tamanos = {15, 30, 60, 90};
        int consultas = 50;
        System.out.println("=== DESAFIO 3: ARNES DE MEDICION (P3) ===");
        System.out.printf("%-8s %-8s %-10s %-18s %-18s %-15s%n",
                "Nodos(V)", "Tramos", "Consultas", "Sin Opt (ms)", "Con Cache (ms)", "Articulac (ms)");
        for (int v : tamanos) {
            for (int i = 0; i < 2; i++) medirEscenario(v, consultas);
            List<Double> sinCache = new ArrayList<>();
            List<Double> conCache = new ArrayList<>();
            List<Double> articulacion = new ArrayList<>();
            ResultadoExperimento r = null;
            for (int i = 0; i < 5; i++) {
                r = medirEscenario(v, consultas);
                sinCache.add(r.msSinOptimizar);
                conCache.add(r.msOptimizadoCache);
                articulacion.add(r.msPuntosArticulacion);
            }
            System.out.printf("%-8d %-8d %-10d %-18.3f %-18.3f %-15.3f%n",
                    r.cantidadNodos, r.cantidadTramos, r.consultas,
                    mediana(sinCache), mediana(conCache), mediana(articulacion));
        }
    }

    private static double mediana(List<Double> muestras) {
        Collections.sort(muestras);
        return muestras.get(muestras.size() / 2);
    }
}
