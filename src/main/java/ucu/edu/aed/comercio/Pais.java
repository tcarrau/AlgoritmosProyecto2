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
        if (destino == null || producto == null)
        {
            return new BigDecimal(-1);
        }

        if (permiteComercioCon(destino, producto)) 
        {
            if (arancelesPorProducto.containsKey(destino)) 
            {
                if (arancelesPorProducto.get(destino).containsKey(producto))
                {
                    return arancelesPorProducto.get(destino).get(producto);
                }
            }
            else 
            {
                if (arancelesGenerales.containsKey(destino)) 
                {
                    return arancelesGenerales.get(destino);   
                }
            }
        }

        return new BigDecimal(-1);

    }

    public BigDecimal obtenerArancel(Pais destino, String categoria) {
        if (destino == null || categoria == null) {
            return new BigDecimal(-1);
        }

        if (permiteComercioCon(destino)) {
            if (arancelesPorCategoria.containsKey(destino)) {
                if (arancelesPorCategoria.get(destino).containsKey(categoria)){
                    return arancelesPorCategoria.get(destino).get(categoria);
                }
            }
            else {
                if (arancelesGenerales.containsKey(destino)) {
                    return arancelesGenerales.get(destino);   
                }
            }
        }

        return new BigDecimal(-1);
    }
}
