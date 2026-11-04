# Demo en consola

Desde la raíz del proyecto:

```sh
mvn -q -DskipTests compile
java -cp target/classes ucu.edu.aed.comercio.DemoConsola
```

La demo carga `src/main/resources/dataset.txt`, muestra la matriz de costos
directos para soja y ejecuta seis consultas. A mitad de la ejecución agrega
`PUERTO_NUEVO` con dos tramos y repite la consulta de menor costo para mostrar
el cambio de ruta.

Al terminar, la demo abre `MenuConsola` con la misma red y el nodo nuevo. Desde
ese menú se pueden hacer consultas en vivo; se sale con `0` o con fin de entrada
(`Ctrl+D`). La opción `9` recarga el dataset original y descarta los cambios.

| Consulta de la demo | Opción en `MenuConsola` |
| --- | --- |
| 1. Menor costo antes del nodo nuevo | `9` para recargar, luego `3` |
| 2. Menor tiempo | `4` |
| 3. Envío entre países | `5` |
| 4. Menor costo con el nodo nuevo | `3` |
| 5. Cobertura por medio | `10` (avanzadas), luego `4` |
| 6. Centro de la red | `6` |

La opción `2` muestra el grafo como lista de tramos; la matriz inicial se
imprime únicamente en la demo.
