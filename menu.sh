#!/usr/bin/env bash
set -euo pipefail

raiz=$(cd "$(dirname "$0")" && pwd)
temporal=$(mktemp -d)
trap 'rm -rf "$temporal"' EXIT

mkdir -p "$temporal/clases"
find "$raiz/src/main/java" -name '*.java' > "$temporal/fuentes.txt"
javac -encoding UTF-8 -d "$temporal/clases" @"$temporal/fuentes.txt"
java -cp "$temporal/clases:$raiz/src/main/resources" ucu.edu.aed.comercio.MenuConsola "$@"
