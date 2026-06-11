#!/bin/bash

JAR_NAME="sprint-mvc.jar"

echo "[1/3] Nettoyage..."
mvn clean

echo "[2/3] Packaging..."
mvn package

JAR_PATH=$(find target -maxdepth 1 -name "*.jar" | head -n 1)

if [ ! -f "$JAR_PATH" ]; then
    echo "❌ Aucun JAR trouvé"
    exit 1
fi

echo "✔ JAR généré : $JAR_NAME"