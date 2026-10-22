package ucu.edu.aed.comercio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SistemaComercio {
    private Map<String, Pais> paises = new LinkedHashMap<>();
    private Map<String, Producto> productos = new LinkedHashMap<>();
    private Map<String, NodoLogistico> nodos = new LinkedHashMap<>();
    private List<Tramo> tramos = new ArrayList<>();

    public Map<String, Pais> getPaises() { return paises; }
    public void setPaises(Map<String, Pais> paises) { this.paises = paises; }

    public Map<String, Producto> getProductos() { return productos; }
    public void setProductos(Map<String, Producto> productos) { this.productos = productos; }

    public Map<String, NodoLogistico> getNodos() { return nodos; }
    public void setNodos(Map<String, NodoLogistico> nodos) { this.nodos = nodos; }

    public List<Tramo> getTramos() { return tramos; }
    public void setTramos(List<Tramo> tramos) { this.tramos = tramos; }

    public void registrarPais(Pais pais) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void registrarProducto(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void registrarNodo(NodoLogistico nodo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void registrarTramo(Tramo tramo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void registrarIntercambio(Pais origen, Pais destino, Producto producto, BigDecimal cantidad) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarRelacionComercial(Pais origen, Pais destino, String tipoAcuerdo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Tramo> rutaMenorCosto(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Tramo> rutaMenorTiempo(NodoLogistico origen, NodoLogistico destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Map<NodoLogistico, Map<NodoLogistico, BigDecimal>> costosMinimosEntreTodos(Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean puedeEnviarProducto(Pais origen, Pais destino, Producto producto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public NodoLogistico obtenerCentro() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Set<NodoLogistico> obtenerNodosCriticos() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void analizarCambio() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
