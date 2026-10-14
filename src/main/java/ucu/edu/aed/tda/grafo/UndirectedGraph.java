package ucu.edu.aed.tda.grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.UndirectedEdge;

public class UndirectedGraph<V, D> implements IUndirectedGraph<V, D> {
    private final Map<V, List<Edge<V, D>>> listaAdyacencia = new LinkedHashMap<>();

    private V resolver(Comparable<V> criterio) {
        for (V v : listaAdyacencia.keySet())
            if (criterio.compareTo(v) == 0)
                return v;
        return null;
    }

    @Override
    public boolean agregarVertice(V vertex) {
        Objects.requireNonNull(vertex);
        if (listaAdyacencia.containsKey(vertex))
            return false;
        listaAdyacencia.put(vertex, new ArrayList<>());
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        return resolver(criterio);
    }

    public Edge<V, D> conectaAmbos(V source, V target) {
        List<Edge<V, D>> aristas = listaAdyacencia.get(source);
        if (aristas == null)
            return null;
        for (Edge<V, D> arista : aristas) {
            boolean conecta = (arista.source().equals(source) && arista.target().equals(target))
                    || (arista.source().equals(target) && arista.target().equals(source));
            if (conecta)
                return arista;
        }
        return null;

    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (!listaAdyacencia.containsKey(source) || !listaAdyacencia.containsKey(target))
            return false;
        for (Edge<V, D> arista : listaAdyacencia.get(source)) {
            if ((arista.source().equals(source) && arista.target().equals(target))
                    || (arista.target().equals(source) && arista.source().equals(target))) {
                return false;
            }
        }
        for (Edge<V, D> arista : listaAdyacencia.get(target)) {
            if ((arista.source().equals(source) && arista.target().equals(target))
                    || (arista.target().equals(source) && arista.source().equals(target))) {
                return false;
            }
        }
        UndirectedEdge<V, D> arista = new UndirectedEdge<>(source, target, dato);
        listaAdyacencia.get(source).add(arista);
        listaAdyacencia.get(target).add(arista);
        return true;
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        V s = resolver(source), t = resolver(target);
        if (s == null || t == null)
            return false;

        Edge<V, D> aristaRemoverT = conectaAmbos(s, t);

        if (aristaRemoverT == null)
            return false;
        listaAdyacencia.get(t).remove(aristaRemoverT);
        listaAdyacencia.get(s).remove(aristaRemoverT);
        return true;
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        V vertice = resolver(criteria);
        if (vertice == null)
            return false;
        List<Edge<V, D>> incidentes = new ArrayList<>(listaAdyacencia.get(vertice));
        for (Edge<V, D> arista : incidentes) {
            V verticeABorrar = arista.source().equals(vertice) ? arista.target() : arista.source();
            listaAdyacencia.get(verticeABorrar).remove(arista);
        }
        listaAdyacencia.remove(vertice);
        return true;
    }

    @Override
    public Set<V> vertices() {
        return Collections.unmodifiableSet(listaAdyacencia.keySet());
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> resultado = new LinkedHashSet<>();
        listaAdyacencia.values().forEach(resultado::addAll);

        return Collections.unmodifiableSet(resultado);
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        V s = resolver(sourceCriteria), t = resolver(targetCriteria);
        return s == null || t == null ? null : conectaAmbos(s, t);
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V v = resolver(verticeCriteria);
        return v == null ? List.of() : new ArrayList<>(listaAdyacencia.get(v));
    }

    @Override
    public boolean esConexo() {
        if (listaAdyacencia.size() <= 1)
            return true;

        Set<V> visitados = new HashSet<>();
        Deque<V> pendientes = new ArrayDeque<>();

        V inicio = listaAdyacencia.keySet().iterator().next();
        visitados.add(inicio);
        pendientes.push(inicio);

        while (!pendientes.isEmpty()) {
            V actual = pendientes.pop();

            for (Edge<V, D> arista : listaAdyacencia.get(actual)) {
                V vecino = arista.source().equals(actual)
                        ? arista.target()
                        : arista.source();

                if (visitados.add(vecino)) {
                    pendientes.push(vecino);
                }
            }
        }

        return visitados.size() == listaAdyacencia.size();
    }

    @Override
    public void vaciar() {
        listaAdyacencia.clear();
    }

    private boolean tieneCicloDesde(V actual, V padre, Set<V> visitados) {
        visitados.add(actual);

        for (Edge<V, D> arista : listaAdyacencia.get(actual)) {
            V vecino = arista.source().equals(actual)
                    ? arista.target()
                    : arista.source();

            if (!visitados.contains(vecino)) {
                if (tieneCicloDesde(vecino, actual, visitados))
                    return true;
            } else if (!Objects.equals(vecino, padre)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean tieneCiclos() {
        Set<V> visitados = new HashSet<>();

        for (V vertice : listaAdyacencia.keySet()) {
            if (!visitados.contains(vertice)
                    && tieneCicloDesde(vertice, null, visitados)) {
                return true;
            }
        }

        return false;
    }
}
