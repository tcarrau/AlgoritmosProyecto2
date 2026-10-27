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
        if (Boolean.FALSE.equals(activo)) {
            return false;
        }
        if (origen == null || destino == null) {
            return false;
        }
        if (!origen.admite(producto) || !destino.admite(producto)) {
            return false;
        }
        if (producto != null && producto.getTipoCarga() != null
                && tiposCargaAdmitidos != null && !tiposCargaAdmitidos.isEmpty()
                && !tiposCargaAdmitidos.contains(producto.getTipoCarga())) {
            return false;
        }
        Pais paisOrigen = origen.getPais();
        Pais paisDestino = destino.getPais();
        if (paisOrigen != null && paisDestino != null && !paisOrigen.equals(paisDestino)) {
            if (producto != null) {
                return paisOrigen.permiteComercioCon(paisDestino, producto);
            } else {
                return paisOrigen.permiteComercioCon(paisDestino);
            }
        }
        return true;
    }

    public BigDecimal calcularCosto(Producto producto) {
        BigDecimal base = costoUnitario != null ? costoUnitario : BigDecimal.ZERO;
        if (origen != null && destino != null
                && origen.getPais() != null && destino.getPais() != null
                && !origen.getPais().equals(destino.getPais()) && producto != null) {
            BigDecimal arancel = origen.getPais().obtenerArancel(destino.getPais(), producto);
            if (arancel != null && arancel.compareTo(BigDecimal.ZERO) > 0) {
                return base.add(arancel);
            }
        }
        return base;
    }
}
