#!/bin/bash

APP_FILE="app/src/main/java/com/tyron/code/CodeAssistApplication.java"

if [ -f "$APP_FILE" ]; then
    echo "Modificando CodeAssistApplication.java"
    
    # Agregar import
    if ! grep -q "import com.tyron.code.GuavaPatcher;" "$APP_FILE"; then
        sed -i '1iimport com.tyron.code.GuavaPatcher;' "$APP_FILE"
    fi
    
    # Agregar llamada a patch en onCreate
    sed -i '/super.onCreate()/a\        GuavaPatcher.patch();' "$APP_FILE"
    
    echo "✓ Parche agregado"
else
    echo "Archivo no encontrado: $APP_FILE"
fi
