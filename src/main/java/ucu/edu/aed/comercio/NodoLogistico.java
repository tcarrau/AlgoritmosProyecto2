package ucu.edu.aed.comercio;

import java.util.LinkedHashSet;
import java.util.Set;

public class NodoLogistico {
    private String nombre;
    private String tipo;
    private Pais pais;
    private Set<String> tiposCargaAdmitidos = new LinkedHashSet<>();
    private Boolean activo;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Pais getPais() { return pais; }
    public void setPais(Pais pais) { this.pais = pais; }

    public Set<String> getTiposCargaAdmitidos() { return tiposCargaAdmitidos; }
    public void setTiposCargaAdmitidos(Set<String> tiposCargaAdmitidos) { this.tiposCargaAdmitidos = tiposCargaAdmitidos; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean admite(Producto producto) {
        if (Boolean.FALSE.equals(activo)) {
            return false;
        }
        if (producto == null || producto.getTipoCarga() == null) {
            return true;
        }
        if (tiposCargaAdmitidos == null || tiposCargaAdmitidos.isEmpty()) {
            return true;
        }
        return tiposCargaAdmitidos.contains(producto.getTipoCarga());
    }
}
