package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class Tramo {
    private NodoLogistico origen;
    private NodoLogistico destino;
    private String medio;
    private BigDecimal costoUnitario;
    private BigDecimal tiempo;
    private Set<String> tiposCargaAdmitidos = new LinkedHashSet<>();
    private Boolean activo;

    public NodoLogistico getOrigen() { return origen; }
    public void setOrigen(NodoLogistico origen) { this.origen = origen; }

    public NodoLogistico getDestino() { return destino; }
    public void setDestino(NodoLogistico destino) { this.destino = destino; }

    public String getMedio() { return medio; }
    public void setMedio(String medio) { this.medio = medio; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public BigDecimal getTiempo() { return tiempo; }
    public void setTiempo(BigDecimal tiempo) { this.tiempo = tiempo; }

    public Set<String> getTiposCargaAdmitidos() { return tiposCargaAdmitidos; }
    public void setTiposCargaAdmitidos(Set<String> tiposCargaAdmitidos) { this.tiposCargaAdmitidos = tiposCargaAdmitidos; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean esUtilizable(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public BigDecimal calcularCosto(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
