package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Pais {
    public String nombre;
    public Set<Producto> productosProducidos = new LinkedHashSet<>();
    public Map<Pais, Set<Producto>> exportaciones = new LinkedHashMap<>();
    public Map<Pais, String> tiposAcuerdo = new LinkedHashMap<>();
    public Map<Pais, BigDecimal> arancelesGenerales = new LinkedHashMap<>();
    public Map<Pais, Map<Producto, BigDecimal>> arancelesPorProducto = new LinkedHashMap<>();
    public Map<Pais, Map<String, BigDecimal>> arancelesPorCategoria = new LinkedHashMap<>();
    public Set<Pais> destinosEmbargados = new LinkedHashSet<>();
    public Map<Pais, Set<Producto>> productosRestringidos = new LinkedHashMap<>();

    public boolean permiteComercioCon(Pais destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public BigDecimal obtenerArancel(Pais destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
