#!/bin/sh

BASE="$1"
if [ -z "$BASE" ]; then BASE="`pwd`"; fi

rm -rf "$BASE/src/main"
mkdir -p "$BASE/src/main/java/org/rosuda/REngine"
(cd "$BASE/src/main/java/org/rosuda/REngine" && ln -s ../../../../../../*.java .)
