package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Pais {
    private String nombre;
    private Set<Producto> productosProducidos = new LinkedHashSet<>();
    private Map<Pais, Set<Producto>> exportaciones = new LinkedHashMap<>();
    private Map<Pais, String> tiposAcuerdo = new LinkedHashMap<>();
    private Map<Pais, BigDecimal> arancelesGenerales = new LinkedHashMap<>();
    private Map<Pais, Map<Producto, BigDecimal>> arancelesPorProducto = new LinkedHashMap<>();
    private Map<Pais, Map<String, BigDecimal>> arancelesPorCategoria = new LinkedHashMap<>();
    private Set<Pais> destinosEmbargados = new LinkedHashSet<>();
    private Map<Pais, Set<Producto>> productosRestringidos = new LinkedHashMap<>();

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<Producto> getProductosProducidos() {
        return productosProducidos;
    }

    public void setProductosProducidos(Set<Producto> productosProducidos) {
        this.productosProducidos = productosProducidos;
    }

    public Map<Pais, Set<Producto>> getExportaciones() {
        return exportaciones;
    }

    public void setExportaciones(Map<Pais, Set<Producto>> exportaciones) {
        this.exportaciones = exportaciones;
    }

    public Map<Pais, String> getTiposAcuerdo() {
        return tiposAcuerdo;
    }

    public void setTiposAcuerdo(Map<Pais, String> tiposAcuerdo) {
        this.tiposAcuerdo = tiposAcuerdo;
    }

    public Map<Pais, BigDecimal> getArancelesGenerales() {
        return arancelesGenerales;
    }

    public void setArancelesGenerales(Map<Pais, BigDecimal> arancelesGenerales) {
        this.arancelesGenerales = arancelesGenerales;
    }

    public Map<Pais, Map<Producto, BigDecimal>> getArancelesPorProducto() {
        return arancelesPorProducto;
    }

    public void setArancelesPorProducto(Map<Pais, Map<Producto, BigDecimal>> arancelesPorProducto) {
        this.arancelesPorProducto = arancelesPorProducto;
    }

    public Map<Pais, Map<String, BigDecimal>> getArancelesPorCategoria() {
        return arancelesPorCategoria;
    }

    public void setArancelesPorCategoria(Map<Pais, Map<String, BigDecimal>> arancelesPorCategoria) {
        this.arancelesPorCategoria = arancelesPorCategoria;
    }

    public Set<Pais> getDestinosEmbargados() {
        return destinosEmbargados;
    }

    public void setDestinosEmbargados(Set<Pais> destinosEmbargados) {
        this.destinosEmbargados = destinosEmbargados;
    }

    public Map<Pais, Set<Producto>> getProductosRestringidos() {
        return productosRestringidos;
    }

    public void setProductosRestringidos(Map<Pais, Set<Producto>> productosRestringidos) {
        this.productosRestringidos = productosRestringidos;
    }

    public boolean permiteComercioCon(Pais destino, Producto producto) {
        if (destino == null || producto == null) {
            return false;
        }
    
        if (destinosEmbargados.contains(destino)) {
            return false;
        }
    
        Set<Producto> restringidos = productosRestringidos.get(destino);
    
        return restringidos == null || !restringidos.contains(producto);

    }

    public boolean permiteComercioCon(Pais destino) {
        if (destino == null) {
            return false;
        }
    
        return !destinosEmbargados.contains(destino);

    }

    public BigDecimal obtenerArancel(Pais destino, Producto producto) {
        if (!permiteComercioCon(destino, producto)) return BigDecimal.valueOf(-1);
        Map<Producto, BigDecimal> porProducto = arancelesPorProducto.get(destino);
        if (porProducto != null && porProducto.containsKey(producto)) {
            return porProducto.get(producto);
        }
        Map<String, BigDecimal> porCategoria = arancelesPorCategoria.get(destino);
        if (porCategoria != null && porCategoria.containsKey(producto.getCategoria())) {
            return porCategoria.get(producto.getCategoria());
        }
        return arancelesGenerales.getOrDefault(destino, BigDecimal.valueOf(-1));
    }

    public BigDecimal obtenerArancel(Pais destino, String categoria) {
        if (categoria == null || !permiteComercioCon(destino)) return BigDecimal.valueOf(-1);
        Map<String, BigDecimal> porCategoria = arancelesPorCategoria.get(destino);
        if (porCategoria != null && porCategoria.containsKey(categoria)) {
            return porCategoria.get(categoria);
        }
        return arancelesGenerales.getOrDefault(destino, BigDecimal.valueOf(-1));
    }
}
