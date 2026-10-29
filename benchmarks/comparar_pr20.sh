#!/usr/bin/env bash
set -euo pipefail

raiz=$(git rev-parse --show-toplevel)
temporal=$(mktemp -d)
trap 'rm -rf "$temporal"' EXIT

for version in antes despues; do
  if [[ "$version" == antes ]]; then revision=2400bc8; else revision=0aa9ed2; fi
  mkdir -p "$temporal/$version" "$temporal/$version/clases"
  git -C "$raiz" archive "$revision" src/main/java | tar -x -C "$temporal/$version"
  find "$temporal/$version/src/main/java" -name '*.java' > "$temporal/$version/fuentes.txt"
  javac -encoding UTF-8 -d "$temporal/$version/clases" \
    @"$temporal/$version/fuentes.txt" "$raiz/benchmarks/CompararPr20.java"
  java -Xms256m -Xmx1g -cp "$temporal/$version/clases" CompararPr20 \
    > "$temporal/$version/resultados.csv"
done

awk -F, '
  FNR == NR {
    if (FNR > 1) {
      clave = $1 "," $2 "," $3
      antes[clave] = $4
      comprobacion[clave] = $5
    }
    next
  }
  FNR == 1 { print "prueba,nodos,operaciones,antes_ms,despues_ms,mejora_veces,comprobacion"; next }
  {
    clave = $1 "," $2 "," $3
    if (!(clave in antes) || comprobacion[clave] != $5) {
      print "Resultados distintos para " clave > "/dev/stderr"
      exit 1
    }
    printf "%s,%s,%s,%.3f,%.3f,%.1f,%s\n", $1, $2, $3, antes[clave], $4, antes[clave] / $4, $5
  }
' "$temporal/antes/resultados.csv" "$temporal/despues/resultados.csv"
