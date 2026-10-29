# Comparación del PR #20

Ejecutar desde la raíz del repositorio:

```bash
bash benchmarks/comparar_pr20.sh
```

Requiere `git`, `javac` y `java` (Java 16 o superior). El script extrae solo
`src/main/java` de `2400bc8` (antes) y `0aa9ed2` (después) a un directorio
temporal, compila exactamente el mismo benchmark en ambos y muestra CSV.

- `obtenerArista`: 200 000 consultas sobre grafos de 100, 500 y 1000 vértices;
  mitad de las consultas encuentra una arista. Usa vértices sin `Comparable`
  para llamar a `obtenerArista(V,V)` en ambas versiones.
- `construirGrafo`: crea redes deterministas con semilla 42 y mide la construcción
  del grafo de costo para 100, 300 y 600 nodos.

Cada caso tiene tres calentamientos y siete mediciones; se informa la mediana.
La columna `comprobacion` debe coincidir entre versiones. El script falla si
no coincide. Las cifras dependen de la máquina, la JVM y su carga; repetir la
ejecución para confirmar diferencias pequeñas.
