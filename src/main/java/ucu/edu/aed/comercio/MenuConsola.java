package ucu.edu.aed.comercio;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import ucu.edu.aed.tda.grafo.DirectedGraph;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

/** Menú de consola para explorar las consultas principales de SistemaComercio. */
public final class MenuConsola {
    private final Scanner entrada;
    private final PrintStream salida;
    private final Path archivoDataset;
    private SistemaComercio sistema;

    private MenuConsola(SistemaComercio sistema, Scanner entrada, PrintStream salida, Path archivoDataset) {
        this.sistema = sistema;
        this.entrada = entrada;
        this.salida = salida;
        this.archivoDataset = archivoDataset;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Uso: bash menu.sh [archivo-dataset]");
            return;
        }
        Path archivo = args.length == 1 ? Path.of(args[0]) : null;
        try {
            SistemaComercio sistema = cargar(archivo);
            new MenuConsola(sistema, new Scanner(System.in, StandardCharsets.UTF_8), System.out, archivo)
                    .ejecutar();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo cargar el dataset: " + e.getMessage());
        }
    }

    private static SistemaComercio cargar(Path archivo) throws IOException {
        if (archivo == null) return CargadorDataset.cargarPredeterminado();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return CargadorDataset.cargar(lector);
        }
    }

    private void ejecutar() {
        salida.println("Red comercial cargada: " + sistema.getPaises().size() + " países, "
                + sistema.getProductos().size() + " productos, " + sistema.getNodos().size() + " nodos y "
                + sistema.getTramos().size() + " tramos.");
        while (true) {
            mostrarOpciones();
            if (!entrada.hasNextLine()) return;
            String opcion = entrada.nextLine().trim();
            try {
                switch (opcion) {
                    case "1" -> listarDatos();
                    case "2" -> mostrarGrafo();
                    case "3" -> mostrarRuta(true);
                    case "4" -> mostrarRuta(false);
                    case "5" -> consultarEnvio();
                    case "6" -> mostrarCentro();
                    case "7" -> mostrarNodosCriticos();
                    case "8" -> cambiarEstadoNodo();
                    case "9" -> recargarDataset();
                    case "10" -> consultasAvanzadas();
                    case "0" -> { salida.println("Hasta luego."); return; }
                    default -> salida.println("Opción inválida. Ingresá un número del 0 al 10.");
                }
            } catch (NoSuchElementException e) {
                return; // Fin de la entrada, también cuando se usa un archivo o pipe.
            } catch (IllegalArgumentException | IllegalStateException e) {
                salida.println("No se pudo realizar la consulta: " + e.getMessage());
            }
        }
    }

    private void mostrarOpciones() {
        salida.println("\n=== MENÚ DE COMERCIO INTERNACIONAL ===");
        salida.println("1. Listar países, productos y nodos");
        salida.println("2. Ver grafo de un producto");
        salida.println("3. Ruta de menor costo");
        salida.println("4. Ruta de menor tiempo");
        salida.println("5. Consultar si un país puede enviar un producto a otro");
        salida.println("6. Centro de la red para un producto");
        salida.println("7. Nodos críticos de la red física");
        salida.println("8. Activar o desactivar un nodo");
        salida.println("9. Recargar el dataset");
        salida.println("10. Consultas avanzadas y registros");
        salida.println("0. Salir");
        salida.print("Opción: ");
    }

    private void consultasAvanzadas() {
        while (true) {
            salida.println("\n=== CONSULTAS AVANZADAS ===");
            salida.println("1. Tramos críticos por producto");
            salida.println("2. Impacto de cerrar un nodo");
            salida.println("3. Nodos en circuitos logísticos");
            salida.println("4. Cobertura por medio de transporte");
            salida.println("5. Red física mínima por producto");
            salida.println("6. Orden de la cadena productiva");
            salida.println("7. Registrar dependencia productiva");
            salida.println("8. Vulnerabilidad de un país");
            salida.println("9. Intercambio comercial de un país");
            salida.println("10. Registrar intercambio comercial");
            salida.println("11. Simular cambio de relación comercial");
            salida.println("0. Volver al menú principal");
            String opcion = leer("Opción: ");
            try {
                switch (opcion) {
                    case "1" -> mostrarTramosCriticos();
                    case "2" -> mostrarImpactoCierre();
                    case "3" -> mostrarCircuitos();
                    case "4" -> mostrarCobertura();
                    case "5" -> mostrarRedMinima();
                    case "6" -> mostrarCadenaProductiva();
                    case "7" -> registrarDependencia();
                    case "8" -> mostrarVulnerabilidad();
                    case "9" -> mostrarIntercambio();
                    case "10" -> registrarIntercambio();
                    case "11" -> simularRelacion();
                    case "0" -> { return; }
                    default -> salida.println("Opción inválida. Ingresá un número del 0 al 11.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                salida.println("No se pudo realizar la consulta: " + e.getMessage());
            }
        }
    }

    private void listarDatos() {
        salida.println("Países: " + String.join(", ", sistema.getPaises().keySet()));
        salida.println("Productos: " + String.join(", ", sistema.getProductos().keySet()));
        salida.println("Nodos (nombre, país, estado):");
        for (NodoLogistico nodo : sistema.getNodos().values()) {
            salida.printf("  %s — %s — %s%n", nodo.getNombre(), nodo.getPais().getNombre(),
                    Boolean.TRUE.equals(nodo.getActivo()) ? "activo" : "inactivo");
        }
        salida.println("Tramos registrados: " + sistema.getTramos().size());
    }

    private void mostrarGrafo() {
        Producto producto = pedir("Producto", sistema.getProductos());
        String criterio = leer("Criterio (COSTO/TIEMPO): ").toUpperCase();
        if (!criterio.equals("COSTO") && !criterio.equals("TIEMPO")) {
            throw new IllegalArgumentException("el criterio debe ser COSTO o TIEMPO");
        }
        DirectedGraph<NodoLogistico, ArcoTramo> grafo = sistema.construirGrafo(producto, criterio);
        Set<Edge<NodoLogistico, ArcoTramo>> aristas = grafo.aristas();
        salida.printf("Grafo de %s por %s: %d nodos y %d aristas.%n", producto.getNombre(),
                criterio, grafo.vertices().size(), aristas.size());
        for (Edge<NodoLogistico, ArcoTramo> arista : aristas) {
            salida.printf("  %s -> %s | %s | peso %.2f%n", arista.source(), arista.target(),
                    arista.dato().getTramo().getMedio(), arista.dato().getWeight());
        }
    }

    private void mostrarRuta(boolean porCosto) {
        Producto producto = pedir("Producto", sistema.getProductos());
        NodoLogistico origen = pedir("Nodo de origen", sistema.getNodos());
        NodoLogistico destino = pedir("Nodo de destino", sistema.getNodos());
        SistemaComercio.DetalleRuta ruta = porCosto
                ? sistema.detalleRutaMenorCosto(origen, destino, producto)
                : sistema.detalleRutaMenorTiempo(origen, destino, producto);
        if (!ruta.alcanzable()) {
            salida.println("No existe una ruta utilizable para ese producto.");
            return;
        }
        salida.println("Ruta: " + origen.getNombre());
        for (Tramo tramo : ruta.tramos()) {
            salida.printf("  -> %s (%s)%n", tramo.getDestino().getNombre(), tramo.getMedio());
        }
        salida.println("Costo total: " + ruta.costoTotal());
        salida.println("Tiempo total: " + ruta.tiempoTotal());
    }

    private void consultarEnvio() {
        Producto producto = pedir("Producto", sistema.getProductos());
        Pais origen = pedir("País de origen", sistema.getPaises());
        Pais destino = pedir("País de destino", sistema.getPaises());
        boolean posible = sistema.puedeEnviarProducto(origen, destino, producto);
        salida.println(posible ? "Sí, existe un envío posible." : "No existe un envío posible.");
    }

    private void mostrarCentro() {
        Producto producto = pedir("Producto", sistema.getProductos());
        NodoLogistico centro = sistema.obtenerCentro(producto);
        salida.println(centro == null ? "No hay centro para esa red."
                : "Centro de la red: " + centro.getNombre());
    }

    private void mostrarNodosCriticos() {
        Set<NodoLogistico> criticos = sistema.obtenerNodosCriticos();
        salida.println(criticos.isEmpty() ? "No hay nodos críticos."
                : "Nodos críticos: " + criticos);
    }

    private void cambiarEstadoNodo() {
        NodoLogistico nodo = pedir("Nodo", sistema.getNodos());
        boolean activo = !Boolean.TRUE.equals(nodo.getActivo());
        nodo.setActivo(activo);
        salida.println(nodo.getNombre() + " ahora está " + (activo ? "activo" : "inactivo") + ".");
    }

    private void recargarDataset() {
        try {
            sistema = cargar(archivoDataset);
            salida.println("Dataset recargado; los cambios de esta sesión fueron descartados.");
        } catch (IOException | IllegalArgumentException e) {
            salida.println("No se pudo recargar el dataset: " + e.getMessage());
        }
    }

    private void mostrarTramosCriticos() {
        Producto producto = pedir("Producto", sistema.getProductos());
        Set<Tramo> criticos = sistema.tramosCriticos(producto);
        if (criticos.isEmpty()) {
            salida.println("No hay tramos críticos para " + producto.getNombre() + ".");
            return;
        }
        salida.println("Tramos críticos para " + producto.getNombre() + ":");
        for (Tramo tramo : criticos) {
            salida.printf("  %s -> %s (%s)%n", tramo.getOrigen(), tramo.getDestino(), tramo.getMedio());
        }
    }

    private void mostrarImpactoCierre() {
        Producto producto = pedir("Producto", sistema.getProductos());
        NodoLogistico nodo = pedir("Nodo a cerrar", sistema.getNodos());
        Set<String> afectados = sistema.analizarImpactoCierreNodo(nodo, producto);
        salida.println(afectados.isEmpty() ? "No se pierden conexiones entre países."
                : "Conexiones entre países afectadas: " + afectados);
        salida.println("El estado del nodo fue restablecido.");
    }

    private void mostrarCircuitos() {
        Producto producto = pedir("Producto", sistema.getProductos());
        Set<NodoLogistico> nodos = sistema.nodosEnCircuitosLogisticos(producto);
        salida.println(nodos.isEmpty() ? "No hay nodos en circuitos logísticos."
                : "Nodos en circuitos logísticos: " + nodos);
    }

    private void mostrarCobertura() {
        Producto producto = pedir("Producto", sistema.getProductos());
        Map<String, SistemaComercio.CoberturaMedio> cobertura = sistema.coberturaPorMedio(producto);
        if (cobertura.isEmpty()) {
            salida.println("No hay medios con servicio para ese producto.");
            return;
        }
        for (Map.Entry<String, SistemaComercio.CoberturaMedio> entrada : cobertura.entrySet()) {
            SistemaComercio.CoberturaMedio dato = entrada.getValue();
            salida.printf("%s: %d nodos con servicio; alcance máximo %d nodos.%n",
                    entrada.getKey(), dato.nodosConServicio(), dato.alcanceMaximo());
        }
    }

    private void mostrarRedMinima() {
        Producto producto = pedir("Producto", sistema.getProductos());
        IUndirectedGraph<NodoLogistico, ArcoTramo> red = sistema.redFisicaMinima(producto);
        salida.printf("Red física mínima: %d nodos y %d enlaces.%n",
                red.vertices().size(), red.aristas().size());
        double costoTotal = 0;
        for (Edge<NodoLogistico, ArcoTramo> arista : red.aristas()) {
            double costo = arista.dato().getWeight();
            costoTotal += costo;
            salida.printf("  %s -- %s | costo físico %.2f%n", arista.source(), arista.target(), costo);
        }
        salida.printf("Costo físico total: %.2f%n", costoTotal);
    }

    private void mostrarCadenaProductiva() {
        List<Producto> orden = sistema.consultarOrdenCadenaProductiva();
        salida.println(orden.isEmpty() ? "No hay productos registrados."
                : "Orden productivo: " + String.join(" -> ", orden.stream().map(Producto::getNombre).toList()));
    }

    private void registrarDependencia() {
        Producto insumo = pedir("Producto insumo", sistema.getProductos());
        Producto finalProducto = pedir("Producto final", sistema.getProductos());
        sistema.registrarDependenciaProductiva(insumo, finalProducto);
        salida.println("Dependencia registrada: " + insumo.getNombre() + " -> " + finalProducto.getNombre());
    }

    private void mostrarVulnerabilidad() {
        Pais pais = pedir("País", sistema.getPaises());
        Producto producto = pedir("Producto", sistema.getProductos());
        SistemaComercio.Vulnerabilidad dato = sistema.vulnerabilidad(pais, producto);
        salida.println("Nodos afectados: " + dato.afectados());
        if (dato.dependientes().isEmpty()) salida.println("No se detectaron nodos críticos con dependientes.");
        for (Map.Entry<NodoLogistico, Set<NodoLogistico>> entrada : dato.dependientes().entrySet()) {
            salida.println("  Si falla " + entrada.getKey() + ": " + entrada.getValue());
        }
        salida.println("Excentricidad mínima: " + (Double.isInfinite(dato.excentricidad())
                ? "infinita" : dato.excentricidad()));
    }

    private void mostrarIntercambio() {
        Pais pais = pedir("País", sistema.getPaises());
        for (Map.Entry<Pais, SistemaComercio.Intercambio> entrada : sistema.intercambioComercial(pais).entrySet()) {
            SistemaComercio.Intercambio dato = entrada.getValue();
            salida.printf("%s: exporta [%s]; importa [%s].%n", entrada.getKey().getNombre(),
                    nombres(dato.exportados()), nombres(dato.importados()));
        }
    }

    private void registrarIntercambio() {
        Pais origen = pedir("País exportador", sistema.getPaises());
        Pais destino = pedir("País importador", sistema.getPaises());
        Producto producto = pedir("Producto", sistema.getProductos());
        if (origen == destino) throw new IllegalArgumentException("los países deben ser distintos");
        sistema.registrarIntercambio(origen, destino, producto, BigDecimal.ONE);
        salida.println("Intercambio registrado: " + origen.getNombre() + " -> "
                + destino.getNombre() + " (" + producto.getNombre() + ").");
    }

    private void simularRelacion() {
        Pais origen = pedir("País de origen", sistema.getPaises());
        Pais destino = pedir("País de destino", sistema.getPaises());
        Producto producto = pedir("Producto", sistema.getProductos());
        String acuerdo = leer("Nuevo acuerdo (por ejemplo, TLC o EMBARGO): ");
        if (acuerdo.isEmpty()) throw new IllegalArgumentException("el acuerdo no puede estar vacío");
        String textoArancel = leer("Nuevo arancel, o vacío para mantener el actual: ");
        BigDecimal arancel = textoArancel.isEmpty() ? null : new BigDecimal(textoArancel);
        SistemaComercio.ImpactoComercial impacto = sistema.analizarCambioRelacionComercial(
                origen, destino, producto, acuerdo, arancel);
        Set<SistemaComercio.ParPaises> pares = new LinkedHashSet<>(impacto.antes().keySet());
        pares.addAll(impacto.despues().keySet());
        int cambios = 0;
        for (SistemaComercio.ParPaises par : pares) {
            BigDecimal antes = impacto.antes().get(par);
            BigDecimal despues = impacto.despues().get(par);
            if (mismoCosto(antes, despues)) continue;
            cambios++;
            salida.printf("  %s -> %s: %s -> %s%n", par.origen().getNombre(),
                    par.destino().getNombre(), costo(antes), costo(despues));
        }
        if (cambios == 0) salida.println("No cambian los costos mínimos entre países.");
        salida.println("Se compararon " + cambios + " pares afectados; la relación original fue restablecida.");
    }

    private static String costo(BigDecimal valor) {
        return valor == null ? "sin ruta" : valor.toPlainString();
    }

    private static boolean mismoCosto(BigDecimal primero, BigDecimal segundo) {
        return primero == null ? segundo == null : segundo != null && primero.compareTo(segundo) == 0;
    }

    private static String nombres(Set<Producto> productos) {
        return String.join(", ", productos.stream().map(Producto::getNombre).toList());
    }

    private <T> T pedir(String etiqueta, Map<String, T> opciones) {
        String nombre = leer(etiqueta + " [" + String.join(", ", opciones.keySet()) + "]: ");
        T valor = opciones.get(nombre);
        if (valor != null) return valor;
        for (Map.Entry<String, T> opcion : opciones.entrySet()) {
            if (opcion.getKey().equalsIgnoreCase(nombre)) return opcion.getValue();
        }
        throw new IllegalArgumentException(etiqueta.toLowerCase() + " inexistente: " + nombre);
    }

    private String leer(String mensaje) {
        salida.print(mensaje);
        if (!entrada.hasNextLine()) throw new NoSuchElementException();
        return entrada.nextLine().trim();
    }
}
