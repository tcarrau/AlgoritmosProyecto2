# Desafío 3 — Optimización de `costosMinimosEntreTodos` (P2)

## 1. La operación elegida

`SistemaComercio.costosMinimosEntreTodos(Producto)`: el costo mínimo de transporte más
aranceles entre todos los pares de ciudades y puertos que sirven para un producto.

Es la consulta más cara del sistema y la que más se repite. La consultora la usa para
comparar escenarios: cuánto cambia la red si aparece una sanción, si se firma un acuerdo o
si se cierra un puerto. Ese uso implica llamarla varias veces, muchas de ellas sobre una red
que no cambió entre consulta y consulta.

## 2. Por qué puede ser un cuello de botella

La implementación original hace dos cosas caras:

| Paso | Costo |
|---|---|
| Reconstruir el grafo del producto desde cero | O(V + E) |
| Resolver todos los pares con una corrida de Floyd | O(V³) |

Hay dos problemas distintos, y conviene no confundirlos:

1. **El algoritmo.** Floyd cuesta O(V³) sin importar cuántas aristas haya. La red de
   comercio es dispersa: una ciudad o un puerto tiene unos pocos tramos, así que E se parece
   más a V que a V². En ese régimen, correr V veces Dijkstra cuesta O(V·(V + E)·log V), que
   para V = 800 y E ≈ 3200 es del orden de 31 millones de operaciones contra los 512
   millones de Floyd: una ventaja teórica de unas 16 veces.
2. **La repetición.** Dos consultas seguidas con el mismo producto y la red sin cambios
   repiten todo el trabajo.

## 3. Las alternativas implementadas

En `AnalisisCostos` (paquete `ucu.edu.aed.comercio`), con tres motores y cache opcional.

- **`FLOYD`** — reproduce la versión original. Es la línea de base.
- **`DIJKSTRA`** — una llamada a `ShortestPathAlgorithms.dijkstra` por nodo.
- **`DIJKSTRA_INDICE_UNICO`** — Dijkstra desde cada nodo compartiendo un único índice de
  adyacencias, vía el método `dijkstraDesdeTodos` que se agregó a `ShortestPathAlgorithms`.
  Existe para separar dos efectos: cuánto del costo es el algoritmo y cuánto es la
  preparación que `dijkstra` repite en cada llamada (resolver el criterio O(V), validar los
  pesos O(E) y construir el índice O(V + E), que llamándolo V veces se paga V veces).
- **Cache con invalidación por huella** — guarda el resultado por producto y lo descarta
  cuando la red cambió.

### Por qué la invalidación es por huella y no por un contador de versión

Un contador de versión incrementado por los métodos que modifican el sistema sería O(1),
pero **sería incorrecto en este modelo**. `Pais` y `SistemaComercio` exponen sus colecciones
mutables a través de los getters, y tanto el cargador del dataset como las consultas de
escenarios modifican embargos, aranceles y restricciones directamente sobre esos mapas, sin
pasar por ningún método del sistema:

```java
sistema.getPaises().get("UY").getDestinosEmbargados().add(argentina);
```

Un contador no vería ese cambio y el cache devolvería un resultado viejo: una ruta que el
embargo ya cortó seguiría apareciendo como disponible. Un cache que miente es peor que no
tener cache. La huella recorre y mezcla todo lo que puede cambiar el grafo (nodos con su
país, cargas y estado; tramos con costos, tiempos, cargas y estado; y las relaciones
comerciales de cada país), así que ve todos los cambios porque los lee.

El costo de validar queda en **O(V + E + relaciones) contra O(V³) de recalcular**, que es la
comparación que justifica el cache. Hay un test por cada forma de modificar la red.

## 4. Mediciones

Redes sintéticas generadas por `MedicionCostosMinimos`, con un anillo que garantiza
conectividad más aristas al azar hasta el grado promedio pedido. Mejor de 3 corridas, con
calentamiento previo del JIT. Java 20, Windows 11.

Para reproducir:

```bash
java -Xmx2g -cp <classes> ucu.edu.aed.comercio.MedicionCostosMinimos
```

### Red dispersa (grado promedio 4, como la del dominio)

| Nodos | Tramos | Floyd (ms) | V×Dijkstra (ms) | Dijkstra 1 índice (ms) | Cache (ms) |
|---:|---:|---:|---:|---:|---:|
| 25 | 94 | 1,19 | 3,00 | 1,06 | 0,068 |
| 50 | 195 | 1,60 | 4,36 | 2,63 | 0,094 |
| 100 | 394 | 4,96 | 15,34 | 8,20 | 0,187 |
| 200 | 795 | 12,12 | 42,71 | 23,81 | 0,385 |
| 400 | 1597 | 68,82 | 217,05 | 114,83 | 0,819 |
| 800 | 3195 | 493,23 | 983,17 | 493,26 | 2,039 |

### Red densa (grado promedio V/2, donde E se acerca a V²)

| Nodos | Tramos | Floyd (ms) | V×Dijkstra (ms) | Dijkstra 1 índice (ms) | Cache (ms) |
|---:|---:|---:|---:|---:|---:|
| 100 | 4938 | 7,25 | 77,22 | 21,90 | 0,449 |
| 200 | 19896 | 53,33 | 635,07 | 144,11 | 1,082 |
| 400 | 79794 | 382,42 | 12162,23 | 1712,20 | 2,507 |

## 5. Comparación y conclusiones

**La hipótesis sobre el algoritmo quedó refutada por la medición.** El análisis asintótico
predecía que en una red dispersa V×Dijkstra le ganaría a Floyd por un factor de unas 16
veces a V = 800. Medido, Floyd le gana en todos los tamaños probados, incluso con el índice
compartido.

La causa está en los factores constantes, y es la lección interesante del experimento:

- Floyd trabaja sobre `double[][]` primitivos, en un triple ciclo sin asignación de memoria
  y con muy buena localidad de caché. El JIT lo optimiza bien.
- Dijkstra, tal como lo pide el TDA del proyecto, trabaja sobre `LinkedHashMap<V, Double>`,
  `HashSet<V>` y una `PriorityQueue` de objetos: `Double` encajonados, una asignación por
  entrada de la cola y saltos de puntero en cada acceso. Cada operación cuesta más de un
  orden de magnitud que una del ciclo de Floyd, y eso se come la ventaja asintótica.

**Compartir el índice sí mejora, y mucho.** Es la parte del experimento que aísla el costo
de la interfaz del TDA: pasar de reconstruir el índice en cada llamada a construirlo una vez
reduce el tiempo **a la mitad en red dispersa y unas 7 veces en red densa** (12162 ms →
1712 ms a 400 nodos). Confirma que una porción grande del costo de `V×Dijkstra` no era el
algoritmo sino la preparación repetida. Aun así no alcanza para superar a Floyd.

**La optimización que sirve es el cache.** Es la que se queda:

| Nodos | Sin cache (Floyd) | Con cache | Mejora |
|---:|---:|---:|---:|
| 100 | 4,96 ms | 0,187 ms | 27× |
| 400 | 68,82 ms | 0,819 ms | 84× |
| 800 | 493,23 ms | 2,039 ms | **242×** |

La mejora crece con el tamaño, porque validar la huella es O(V + E) mientras recalcular es
O(V³). Y el caso de uso real del dominio —repetir la consulta y comparar escenarios— es
exactamente donde el cache paga.

### Qué queda

- Floyd se mantiene como motor por omisión: la medición no da ninguna razón para cambiarlo.
- `DIJKSTRA_INDICE_UNICO` queda implementado y testeado. Conviene para la consulta de un
  solo origen, donde no hace falta la matriz completa, y es el motor a reconsiderar si la
  red creciera mucho más de 800 nodos, donde la ventaja asintótica eventualmente se impone.
- Si hiciera falta exprimir más, el próximo paso no es cambiar de algoritmo sino cambiar la
  representación: un Dijkstra sobre arreglos primitivos indexados por entero, en lugar de
  mapas de objetos, atacaría justo el factor constante que el experimento identificó.

## 6. Verificación

Una optimización que cambia el resultado no sirve. `AnalisisCostosTest` (12 tests) verifica:

- que los tres motores devuelven exactamente lo mismo que la implementación original, sobre
  el dataset de ejemplo, para los cinco productos, y sobre una red sintética de 60 nodos;
- que el cache no recalcula si la red no cambió (contador de recálculos en 1 tras 6
  consultas);
- que guarda un resultado por producto;
- que se invalida al cerrar un nodo, al agregar un embargo por el getter y al cambiar un
  arancel, y que el resultado recalculado vuelve a coincidir con la implementación original;
- que la huella cambia con cada tipo de modificación de la red y no cambia si no se toca
  nada.
