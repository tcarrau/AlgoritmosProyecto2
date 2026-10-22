package ucu.edu.aed.comercio;

import java.util.LinkedHashSet;
import java.util.Set;

public class NodoLogistico {
    public String nombre;
    public String tipo;
    public Pais pais;
    public Set<String> tiposCargaAdmitidos = new LinkedHashSet<>();
    public Boolean activo;

    public boolean admite(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
