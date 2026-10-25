package ucu.edu.aed.comercio;

import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class ArcoTramo extends WeightedEdge {
    private final Tramo tramo;

    public ArcoTramo(double peso, Tramo tramo) {
        super(peso);
        this.tramo = tramo;
    }

    public Tramo getTramo() {
        return tramo;
    }
}
