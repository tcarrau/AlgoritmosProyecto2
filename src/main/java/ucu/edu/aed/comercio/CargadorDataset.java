package ucu.edu.aed.comercio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CargadorDataset {
    private final SistemaComercio sistema = new SistemaComercio();

    public static SistemaComercio cargarPredeterminado() throws IOException {
        try (Reader r = new InputStreamReader(CargadorDataset.class.getResourceAsStream("/dataset.txt"), "UTF-8")) {
            return cargar(r);
        }
    }

    public static SistemaComercio cargar(Reader reader) throws IOException {
        CargadorDataset c = new CargadorDataset();
        BufferedReader br = new BufferedReader(reader);
        int n = 0;
        for (String linea = br.readLine(); linea != null; linea = br.readLine()) {
            n++;
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            try {
                c.procesar(linea.split("\\|", -1));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Línea " + n + ": " + e.getMessage(), e);
            }
        }
        return c.sistema;
    }

    private void procesar(String[] f) {
        switch (f[0]) {
            case "PAIS" -> {
                Pais p = new Pais();
                p.setNombre(f[1]);
                sistema.registrarPais(p);
            }
            case "PRODUCTO" -> {
                Producto p = new Producto();
                p.setNombre(f[1]);
                p.setCategoria(f[2]);
                p.setTipoCarga(f[3]);
                p.setValorUnitario(new BigDecimal(f[4]));
                sistema.registrarProducto(p);
            }
            case "PRODUCE" -> pais(f[1]).getProductosProducidos().add(producto(f[2]));
            case "DEPENDENCIA" -> sistema.registrarDependenciaProductiva(producto(f[1]), producto(f[2]));
            case "NODO" -> {
                NodoLogistico n = new NodoLogistico();
                n.setNombre(f[1]);
                n.setTipo(f[2]);
                n.setPais(pais(f[3]));
                n.setTiposCargaAdmitidos(lista(f[4]));
                n.setActivo(true);
                sistema.registrarNodo(n);
            }
            case "TRAMO" -> {
                tramo(nodo(f[1]), nodo(f[2]), f);
                if (f[7].equals("S")) {
                    tramo(nodo(f[2]), nodo(f[1]), f);
                }
            }
            case "ACUERDO" -> {
                arancel(pais(f[1]), pais(f[2]), f[3], f[4]);
                arancel(pais(f[2]), pais(f[1]), f[3], f[4]);
            }
            case "ARANCEL" -> arancel(pais(f[1]), pais(f[2]), f[3], f[4]);
            case "ARANCELPROD" -> pais(f[1]).getArancelesPorProducto()
                    .computeIfAbsent(pais(f[2]), k -> new LinkedHashMap<>())
                    .put(producto(f[3]), new BigDecimal(f[4]));
            case "EMBARGO" -> pais(f[1]).getDestinosEmbargados().add(pais(f[2]));
            case "RESTRICCION" -> pais(f[1]).getProductosRestringidos()
                    .computeIfAbsent(pais(f[2]), k -> new LinkedHashSet<>()).add(producto(f[3]));
            default -> throw new IllegalArgumentException("Registro desconocido: " + f[0]);
        }
    }

    private void tramo(NodoLogistico origen, NodoLogistico destino, String[] f) {
        Tramo t = new Tramo();
        t.setOrigen(origen);
        t.setDestino(destino);
        t.setMedio(f[3]);
        t.setCostoUnitario(new BigDecimal(f[4]));
        t.setTiempo(new BigDecimal(f[5]));
        t.setTiposCargaAdmitidos(lista(f[6]));
        t.setActivo(true);
        sistema.registrarTramo(t);
    }

    private void arancel(Pais origen, Pais destino, String tipo, String valor) {
        origen.getTiposAcuerdo().put(destino, tipo);
        origen.getArancelesGenerales().put(destino, new BigDecimal(valor));
    }

    private static Set<String> lista(String s) {
        return new LinkedHashSet<>(Arrays.asList(s.split(";")));
    }

    private Pais pais(String nombre) {
        return buscar(sistema.getPaises(), nombre, "país");
    }

    private Producto producto(String nombre) {
        return buscar(sistema.getProductos(), nombre, "producto");
    }

    private NodoLogistico nodo(String nombre) {
        return buscar(sistema.getNodos(), nombre, "nodo");
    }

    private static <T> T buscar(Map<String, T> m, String nombre, String que) {
        T t = m.get(nombre);
        if (t == null) {
            throw new IllegalArgumentException(que + " inexistente: " + nombre);
        }
        return t;
    }
}
