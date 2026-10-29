package ucu.edu.aed.tda.grafo;

import ucu.edu.aed.tda.grafo.model.edge.DirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

/**
 * Grafo dirigido con matriz de adyacencia: m[i][j] guarda la arista del vértice i al j.
 * Misma semántica que DirectedGraph; cambia el costo: aristas en O(1) pero listar sucesores es O(V)
 * y la memoria es O(V^2) aunque la red sea dispersa.
 */
public class DirectedMatrixGraph<V, D> implements IDirectedIGraph<V, D> {
    private final List<V> vs = new ArrayList<>();
    private final Map<V, Integer> indice = new HashMap<>();
    private DirectedEdge<V, D>[][] m = nueva(4);

    @SuppressWarnings("unchecked")
    private static <V, D> DirectedEdge<V, D>[][] nueva(int n) {
        return new DirectedEdge[n][n];
    }

    private int resolver(Comparable<V> criterio) {
        for (int i = 0; i < vs.size(); i++) if (criterio.compareTo(vs.get(i)) == 0) return i;
        return -1;
    }

    @Override
    public boolean agregarVertice(V vertex) {
        Objects.requireNonNull(vertex);
        if (indice.containsKey(vertex)) return false;
        if (vs.size() == m.length) {
            DirectedEdge<V, D>[][] grande = nueva(m.length * 2);
            for (int i = 0; i < vs.size(); i++) System.arraycopy(m[i], 0, grande[i], 0, vs.size());
            m = grande;
        }
        indice.put(vertex, vs.size());
        vs.add(vertex);
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        int i = resolver(criterio);
        return i < 0 ? null : vs.get(i);
    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        Integer i = indice.get(source), j = indice.get(target);
        if (i == null || j == null || m[i][j] != null) return false;
        m[i][j] = new DirectedEdge<>(source, target, dato);
        return true;
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        int i = resolver(source), j = resolver(target);
        if (i < 0 || j < 0 || m[i][j] == null) return false;
        m[i][j] = null;
        return true;
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        int i = resolver(criteria);
        if (i < 0) return false;
        int ultimo = vs.size() - 1;
        indice.remove(vs.get(i));
        if (i != ultimo) {
            // el último vértice ocupa el lugar del removido
            for (int k = 0; k <= ultimo; k++) m[i][k] = m[ultimo][k];
            for (int k = 0; k <= ultimo; k++) m[k][i] = m[k][ultimo];
            vs.set(i, vs.get(ultimo));
            indice.put(vs.get(i), i);
        }
        for (int k = 0; k <= ultimo; k++) m[ultimo][k] = m[k][ultimo] = null;
        vs.remove(ultimo);
        return true;
    }

    @Override
    public Set<V> vertices() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(vs));
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> r = new LinkedHashSet<>();
        for (int i = 0; i < vs.size(); i++)
            for (int j = 0; j < vs.size(); j++) if (m[i][j] != null) r.add(m[i][j]);
        return Collections.unmodifiableSet(r);
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        int i = resolver(sourceCriteria), j = resolver(targetCriteria);
        return i < 0 || j < 0 ? null : m[i][j];
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        List<Edge<V, D>> r = new ArrayList<>();
        int i = resolver(verticeCriteria);
        if (i >= 0) for (int j = 0; j < vs.size(); j++) if (m[i][j] != null) r.add(m[i][j]);
        return r;
    }

    @Override
    public Set<V> successors(Comparable<V> criteria) {
        Set<V> r = new LinkedHashSet<>();
        int i = resolver(criteria);
        if (i >= 0) for (int j = 0; j < vs.size(); j++) if (m[i][j] != null) r.add(vs.get(j));
        return r;
    }

    @Override
    public Set<V> predecessors(Comparable<V> criteria) {
        Set<V> r = new LinkedHashSet<>();
        int j = resolver(criteria);
        if (j >= 0) for (int i = 0; i < vs.size(); i++) if (m[i][j] != null) r.add(vs.get(i));
        return r;
    }

    @Override
    public boolean esConexo() {
        int n = vs.size();
        if (n <= 1) return true;
        boolean[] visto = new boolean[n];
        Deque<Integer> pila = new ArrayDeque<>();
        visto[0] = true;
        pila.push(0);
        int cuenta = 1;
        while (!pila.isEmpty()) {
            int i = pila.pop();
            for (int j = 0; j < n; j++) {
                if (!visto[j] && (m[i][j] != null || m[j][i] != null)) {
                    visto[j] = true;
                    cuenta++;
                    pila.push(j);
                }
            }
        }
        return cuenta == n;
    }

    @Override
    public void vaciar() {
        vs.clear();
        indice.clear();
        m = nueva(4);
    }

    @Override
    public boolean tieneCiclos() {
        int n = vs.size();
        int[] estado = new int[n];   // 0 sin ver, 1 en el camino, 2 terminado
        int[] sig = new int[n];      // próxima columna a explorar por vértice
        for (int raiz = 0; raiz < n; raiz++) {
            if (estado[raiz] != 0) continue;
            Deque<Integer> camino = new ArrayDeque<>();
            camino.push(raiz);
            estado[raiz] = 1;
            while (!camino.isEmpty()) {
                int i = camino.peek();
                if (sig[i] == n) {
                    estado[i] = 2;
                    camino.pop();
                    continue;
                }
                int j = sig[i]++;
                if (m[i][j] == null) continue;
                if (estado[j] == 1) return true;
                if (estado[j] == 0) {
                    estado[j] = 1;
                    camino.push(j);
                }
            }
        }
        return false;
    }
}
