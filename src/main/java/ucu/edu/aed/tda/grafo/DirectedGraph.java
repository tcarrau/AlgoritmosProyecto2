package ucu.edu.aed.tda.grafo;

import ucu.edu.aed.tda.grafo.model.edge.DirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

/**
 * Grafo dirigido con listas de adyacencia (mapas): salientes y entrantes por vértice.
 * Los vértices se identifican con equals/hashCode; los criterios (Comparable) se resuelven recorriendo los vértices.
 * esConexo() es conexidad débil (ignora la dirección); vacío o de un vértice es conexo.
 */
public class DirectedGraph<V, D> implements IDirectedIGraph<V, D> {
    private final Map<V, Map<V, DirectedEdge<V, D>>> out = new LinkedHashMap<>();
    private final Map<V, Set<V>> in = new HashMap<>();

    // ponytail: O(V) por criterio; indexar si la carga de datos lo exige
    private V resolver(Comparable<V> criterio) {
        for (V v : out.keySet()) if (criterio.compareTo(v) == 0) return v;
        return null;
    }

    @Override
    public boolean agregarVertice(V vertex) {
        Objects.requireNonNull(vertex);
        if (out.containsKey(vertex)) return false;
        out.put(vertex, new LinkedHashMap<>());
        in.put(vertex, new LinkedHashSet<>());
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        return resolver(criterio);
    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (!out.containsKey(source) || !out.containsKey(target)) return false;
        if (out.get(source).containsKey(target)) return false;
        out.get(source).put(target, new DirectedEdge<>(source, target, dato));
        in.get(target).add(source);
        return true;
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        V s = resolver(source), t = resolver(target);
        return s != null && t != null && eliminarArista(s, t);
    }

    /** Acceso directo por hash: O(1) cuando ya se tienen los vértices. */
    public boolean eliminarArista(V source, V target) {
        Map<V, DirectedEdge<V, D>> salientes = out.get(source);
        if (salientes == null || salientes.remove(target) == null) return false;
        in.get(target).remove(source);
        return true;
    }

    @Override
    public Edge<V, D> obtenerArista(V source, V target) {
        Map<V, DirectedEdge<V, D>> salientes = out.get(source);
        return salientes == null ? null : salientes.get(target);
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        V v = resolver(criteria);
        if (v == null) return false;
        for (V t : out.get(v).keySet()) in.get(t).remove(v);
        for (V s : in.get(v)) out.get(s).remove(v);
        out.remove(v);
        in.remove(v);
        return true;
    }

    @Override
    public Set<V> vertices() {
        return Collections.unmodifiableSet(out.keySet());
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> r = new LinkedHashSet<>();
        out.values().forEach(m -> r.addAll(m.values()));
        return Collections.unmodifiableSet(r);
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        V s = resolver(sourceCriteria), t = resolver(targetCriteria);
        return s == null || t == null ? null : out.get(s).get(t);
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V v = resolver(verticeCriteria);
        return v == null ? List.of() : new ArrayList<>(out.get(v).values());
    }

    @Override
    public Set<V> successors(Comparable<V> criteria) {
        V v = resolver(criteria);
        return v == null ? Set.of() : Collections.unmodifiableSet(out.get(v).keySet());
    }

    @Override
    public Set<V> predecessors(Comparable<V> criteria) {
        V v = resolver(criteria);
        return v == null ? Set.of() : Collections.unmodifiableSet(in.get(v));
    }

    @Override
    public boolean esConexo() {
        if (out.size() <= 1) return true;
        Set<V> seen = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();
        V start = out.keySet().iterator().next();
        stack.push(start);
        seen.add(start);
        while (!stack.isEmpty()) {
            V v = stack.pop();
            for (V n : out.get(v).keySet()) if (seen.add(n)) stack.push(n);
            for (V n : in.get(v)) if (seen.add(n)) stack.push(n);
        }
        return seen.size() == out.size();
    }

    @Override
    public void vaciar() {
        out.clear();
        in.clear();
    }

    @Override
    public boolean tieneCiclos() {
        // DFS iterativo con colores: gris = en el camino actual, negro = terminado
        Set<V> gris = new HashSet<>(), negro = new HashSet<>();
        for (V raiz : out.keySet()) {
            if (negro.contains(raiz)) continue;
            Deque<Iterator<V>> iters = new ArrayDeque<>();
            Deque<V> camino = new ArrayDeque<>();
            gris.add(raiz);
            camino.push(raiz);
            iters.push(out.get(raiz).keySet().iterator());
            while (!iters.isEmpty()) {
                Iterator<V> it = iters.peek();
                if (it.hasNext()) {
                    V n = it.next();
                    if (gris.contains(n)) return true;
                    if (negro.contains(n)) continue;
                    gris.add(n);
                    camino.push(n);
                    iters.push(out.get(n).keySet().iterator());
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
