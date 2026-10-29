# Menú interactivo

Desde la raíz del repositorio, con Java 16 o superior:

```bash
bash menu.sh
```

Se carga `src/main/resources/dataset.txt`. También se puede indicar otro archivo
con el formato aceptado por `CargadorDataset`:

```bash
bash menu.sh ruta/al/dataset.txt
```

El menú principal permite explorar los datos, construir el grafo por producto y
criterio, consultar rutas y envíos, ver el centro y los nodos críticos, y cambiar
el estado de un nodo durante la sesión.

La opción **10. Consultas avanzadas y registros** incluye tramos críticos,
impacto de cierre de un nodo, circuitos logísticos, cobertura por medio, red
física mínima, cadena productiva, vulnerabilidad e intercambio comercial.
También permite registrar dependencias e intercambios y simular un cambio de
relación comercial. La simulación restaura el acuerdo y el arancel originales.
El dataset de ejemplo no trae dependencias ni intercambios registrados; se
pueden agregar durante la sesión con las opciones 7 y 10 de este submenú.

La opción de recarga vuelve a cargar el archivo y descarta los cambios de la
sesión. Las consultas usan los métodos de `SistemaComercio`.
