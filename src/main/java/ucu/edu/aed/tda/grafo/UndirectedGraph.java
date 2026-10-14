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

import ucu.edu.aed.tda.grafo.model.edge.DirectedEdge;
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
        listaAdyacencia.put(vertex, new ArrayList<>() {
            
        });
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        return resolver(criterio);
    }

    public Edge<V, D> conectaAmbos(V source, V target) {
        List<Edge<V, D>> aristas = listaAdyacencia.get(source);
        if (aristas == null) return null;
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
        if (s == null || t == null) return false;

        Edge<V, D> aristaRemoverT = conectaAmbos(s, t);

        if(aristaRemoverT == null) return false;
        listaAdyacencia.get(t).remove(aristaRemoverT);
        listaAdyacencia.get(s).remove(aristaRemoverT);
        return true;
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        V vertice = resolver(criteria);
        if (vertice == null)
            return false;
        for (Edge<V, D> arista : listaAdyacencia.get(vertice)) {
            V verticeABorrar = arista.source().equals(vertice) ? arista.target() : arista.source();
            listaAdyacencia.get(verticeABorrar).remove(conectaAmbos(arista.source(), arista.target()));
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
        List<V> vertices = new ArrayList<>(listaAdyacencia.keySet());

        for (int i = 0; i < vertices.size(); i++) {
            V origen = vertices.get(i);

            for (V destino : listaAdyacencia.get(origen)) {
                if (i < vertices.indexOf(destino)) {
                    resultado.add(new UndirectedEdge<>(origen, destino, null));
                }
            }
        }

        return Collections.unmodifiableSet(resultado);
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        V s = resolver(sourceCriteria), t = resolver(targetCriteria);
        return s == null || t == null ? null : listaAdyacencia.get(s).get(t);
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V v = resolver(verticeCriteria);
        return v == null ? List.of() : new ArrayList<>(listaAdyacencia.get(v).values());
    }

    @Override
    public boolean esConexo() {
        if (listaAdyacencia.size() <= 1)
            return true;
        Set<V> seen = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();
        V start = listaAdyacencia.keySet().iterator().next();
        stack.push(start);
        seen.add(start);
        while (!stack.isEmpty()) {
            V v = stack.pop();
            for (V n : listaAdyacencia.get(v).keySet())
                if (seen.add(n))
                    stack.push(n);
            for (V n : in.get(v))
                if (seen.add(n))
                    stack.push(n);
        }
        return seen.size() == listaAdyacencia.size();
    }

    @Override
    public void vaciar() {
        listaAdyacencia.clear();
        in.clear();
    }

    @Override
    public boolean tieneCiclos() {
        // DFS iterativo con colores: gris = en el camino actual, negro = terminado
        Set<V> gris = new HashSet<>(), negro = new HashSet<>();
        for (V raiz : listaAdyacencia.keySet()) {
            if (negro.contains(raiz))
                continue;
            Deque<Iterator<V>> iters = new ArrayDeque<>();
            Deque<V> camino = new ArrayDeque<>();
            gris.add(raiz);
            camino.push(raiz);
            iters.push(listaAdyacencia.get(raiz).keySet().iterator());
            while (!iters.isEmpty()) {
                Iterator<V> it = iters.peek();
                if (it.hasNext()) {
                    V n = it.next();
                    if (gris.contains(n))
                        return true;
                    if (negro.contains(n))
                        continue;
                    gris.add(n);
                    camino.push(n);
                    iters.push(listaAdyacencia.get(n).keySet().iterator());
                } else {
                    iters.pop();
                    V v = camino.pop();
                    gris.remove(v);
                    negro.add(v);
                }
            }
        }
        return false;
    }
}
