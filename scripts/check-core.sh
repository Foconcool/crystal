#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
javac -Xlint:all -d "$OUT" src/main/java/dev/routecleaner/core/*.java src/test/java/dev/routecleaner/core/AnalyzerSelfTest.java
java -ea -cp "$OUT" dev.routecleaner.core.AnalyzerSelfTest
