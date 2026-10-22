package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class Tramo {
    public NodoLogistico origen;
    public NodoLogistico destino;
    public String medio;
    public BigDecimal costoUnitario;
    public BigDecimal tiempo;
    public Set<String> tiposCargaAdmitidos = new LinkedHashSet<>();
    public Boolean activo;

    public boolean esUtilizable(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public BigDecimal calcularCosto(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
