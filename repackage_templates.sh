#!/bin/bash

PROJECT_ROOT="/home/vit/Android/CodeAssist"
TEMPLATES_SRC="$PROJECT_ROOT/templates"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets"
TEMP_DIR="$PROJECT_ROOT/temp_templates"

echo "🧹 Limpiando y regenerando plantillas desde cero..."

rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Copiamos solo el contenido de nuestra carpeta de plantillas local
# Esto evita duplicados de versiones antiguas que pudieran estar en el ZIP
cp -r "$TEMPLATES_SRC"/* "$TEMP_DIR/"

# Generamos el ZIP con todas las plantillas en la raíz del archivo
echo "🤐 Empaquetando en assets/templates.zip..."
cd "$TEMP_DIR"
rm -f "$ASSETS_DIR/templates.zip"
zip -qr "$ASSETS_DIR/templates.zip" .

cd "$PROJECT_ROOT"
rm -rf "$TEMP_DIR"
echo "✅ Proceso completado con éxito."
