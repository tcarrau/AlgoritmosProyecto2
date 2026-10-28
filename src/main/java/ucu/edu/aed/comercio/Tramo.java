package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
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

    private boolean cruzaFrontera() {
        return !Objects.equals(origen.getPais(), destino.getPais());
    }

    public boolean esUtilizable(Producto producto) {
        if (!Boolean.TRUE.equals(activo) || !tiposCargaAdmitidos.contains(producto.getTipoCarga())) {
            return false;
        }
        return !cruzaFrontera() || origen.getPais().obtenerArancel(destino.getPais(), producto).signum() >= 0;
    }

    // el arancel se toma como fracción del valor unitario (0.10 = 10%)
    public BigDecimal calcularCosto(Producto producto) {
        if (!cruzaFrontera()) {
            return costoUnitario;
        }
        BigDecimal arancel = origen.getPais().obtenerArancel(destino.getPais(), producto);
        if (arancel.signum() < 0) {
            throw new IllegalStateException("El tramo no admite comercio para el producto");
        }
        return costoUnitario.add(producto.getValorUnitario().multiply(arancel));
    }
}
