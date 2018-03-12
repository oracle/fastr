#!/bin/sh

BASE="$1"
if [ -z "$BASE" ]; then BASE="`pwd`"; fi

rm -rf "$BASE/src/main"
mkdir -p "$BASE/src/main/java/org/rosuda/REngine/Rserve/protocol"
(cd "$BASE/src/main/java/org/rosuda/REngine/Rserve" && ln -s ../../../../../../../*.java .) && \
(cd "$BASE/src/main/java/org/rosuda/REngine/Rserve/protocol" && ln -s ../../../../../../../../protocol/*.java .)
