package ucu.edu.aed.comercio;

import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Seis consultas breves sobre el dataset incluido en el proyecto. */
public class DemoConsola {

    public static void main(String[] args) throws IOException {
        SistemaComercio sistema = CargadorDataset.cargarPredeterminado();
        Producto soja = sistema.getProductos().get("soja");
        Producto carne = sistema.getProductos().get("carne");
        Producto maquinaria = sistema.getProductos().get("maquinaria");
        NodoLogistico mvd = sistema.getNodos().get("MVD");
        NodoLogistico shanghai = sistema.getNodos().get("SHANGHAI");

        System.out.println("=== Demo del sistema de comercio ===");
        mostrarMatriz(sistema, soja, "Matriz de adyacencia inicial");

        SistemaComercio.DetalleRuta costo = sistema.detalleRutaMenorCosto(mvd, shanghai, soja);
        consulta(1, "sistema.detalleRutaMenorCosto(MVD, SHANGHAI, soja)",
                ruta(costo), "9 (recargar dataset original) -> 3");

        SistemaComercio.DetalleRuta tiempo = sistema.detalleRutaMenorTiempo(mvd, shanghai, carne);
        consulta(2, "sistema.detalleRutaMenorTiempo(MVD, SHANGHAI, carne)",
                ruta(tiempo), "4");

        Pais us = sistema.getPaises().get("US");
        Pais cn = sistema.getPaises().get("CN");
        boolean puedeEnviar = sistema.puedeEnviarProducto(us, cn, maquinaria);
        consulta(3, "sistema.puedeEnviarProducto(US, CN, maquinaria)",
                puedeEnviar ? "Sí, existe una ruta indirecta" : "No hay ruta disponible", "5");

        NodoLogistico nuevo = new NodoLogistico();
        nuevo.setNombre("PUERTO_NUEVO");
        nuevo.setTipo("puerto");
        nuevo.setPais(sistema.getPaises().get("UY"));
        nuevo.setTiposCargaAdmitidos(Set.of("granel"));
        nuevo.setActivo(true);
        sistema.registrarNodo(nuevo);
        agregarTramo(sistema, mvd, nuevo, "camion", 5, 2);
        agregarTramo(sistema, nuevo, shanghai, "barco", 40, 300);
        System.out.println("\nCambio: se agrega PUERTO_NUEVO (UY) y los tramos "
                + "MVD -> PUERTO_NUEVO (costo 5), PUERTO_NUEVO -> SHANGHAI "
                + "(costo base 40 + arancel).");

        consulta(4, "sistema.detalleRutaMenorCosto(MVD, SHANGHAI, soja)",
                ruta(sistema.detalleRutaMenorCosto(mvd, shanghai, soja)), "3");

        consulta(5, "sistema.coberturaPorMedio(soja)",
                sistema.coberturaPorMedio(soja).entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue().nodosConServicio()
                                + " nodos con servicio, alcance máximo " + e.getValue().alcanceMaximo())
                        .collect(Collectors.joining("; ")), "10 -> 4");

        NodoLogistico centro = sistema.obtenerCentro(carne);
        consulta(6, "sistema.obtenerCentro(carne)",
                centro == null ? "No hay centro" : centro.getNombre(), "6");

        MenuConsola.ejecutar(sistema);
    }

    private static void mostrarMatriz(SistemaComercio sistema, Producto producto, String titulo) {
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = sistema.construirGrafo(producto, "COSTO");
        List<NodoLogistico> nodos = new ArrayList<>(grafo.vertices());
        System.out.println("\n" + titulo + " para " + producto.getNombre()
                + " (costo directo con arancel):");
        System.out.println("- = sin tramo utilizable; filas = origen, columnas = destino");
        for (int i = 0; i < nodos.size(); i++) {
            System.out.print((i + 1) + "=" + nodos.get(i) + "  ");
        }
        System.out.println();
        System.out.printf("%4s", "");
        for (int i = 0; i < nodos.size(); i++) System.out.printf("%7d", i + 1);
        System.out.println();
        for (int i = 0; i < nodos.size(); i++) {
            System.out.printf("%4d", i + 1);
            for (NodoLogistico destino : nodos) {
                Edge<NodoLogistico, ArcoTramo> arista = grafo.obtenerArista(nodos.get(i), destino);
                String valor = arista == null ? "-" : arista.dato().getTramo()
                        .calcularCosto(producto).stripTrailingZeros().toPlainString();
                System.out.printf("%7s", valor);
            }
            System.out.println();
        }
    }

    private static void agregarTramo(SistemaComercio sistema, NodoLogistico origen,
                                     NodoLogistico destino, String medio, int costo, int tiempo) {
        Tramo tramo = new Tramo();
        tramo.setOrigen(origen);
        tramo.setDestino(destino);
        tramo.setMedio(medio);
        tramo.setCostoUnitario(BigDecimal.valueOf(costo));
        tramo.setTiempo(BigDecimal.valueOf(tiempo));
        tramo.setTiposCargaAdmitidos(Set.of("granel"));
        tramo.setActivo(true);
        sistema.registrarTramo(tramo);
    }

    private static String ruta(SistemaComercio.DetalleRuta detalle) {
        if (!detalle.alcanzable()) return "No hay ruta";
        String recorrido = detalle.tramos().stream()
                .map(t -> t.getOrigen() + " -" + t.getMedio() + "-> " + t.getDestino())
                .collect(Collectors.joining("; "));
        return recorrido + " | costo total: "
                + detalle.costoTotal().stripTrailingZeros().toPlainString()
                + " | tiempo total: " + detalle.tiempoTotal().stripTrailingZeros().toPlainString();
    }

    private static void consulta(int numero, String funcion, String respuesta, String opcionesMenu) {
        System.out.println("\n" + numero + ". Función: " + funcion);
        System.out.println("   Respuesta: " + respuesta);
        System.out.println("   En MenuConsola: " + opcionesMenu);
    }

}
