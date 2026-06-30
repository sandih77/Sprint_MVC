#!/bin/bash

JAR_NAME="sprint-mvc.jar"
DEST_DIR="../Test-framework/lib"

echo "[1/3] Clean..."
mvn clean

echo "[2/3] Package..."
mvn package

JAR_PATH="target/sprint-mvc.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR introuvable : $JAR_PATH"
    exit 1
fi

echo "JAR trouvé : $JAR_PATH"

echo "[3/3] Copie vers Test-framework..."

mkdir -p "$DEST_DIR"

cp "$JAR_PATH" "$DEST_DIR/$JAR_NAME"

if [ $? -eq 0 ]; then
    echo "Copie réussie : $DEST_DIR/$JAR_NAME"
else
    echo "Erreur de copie"
    exit 1
fi