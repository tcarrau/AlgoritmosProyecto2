package ucu.edu.aed.comercio;

import java.math.BigDecimal;

public class Producto {
    private String nombre;
    private String categoria;
    private String tipoCarga;
    private BigDecimal valorUnitario;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
}
