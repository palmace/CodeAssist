#!/bin/bash

# Archivos encontrados por el grep
FILES=(
    "com/tyron/completion/java/provider/FindCompletionsAt.java"
    "com/tyron/completion/java/provider/FindHelper.java"
    "com/tyron/completion/java/provider/PruneMethodBodies.java"
    "com/tyron/completion/java/hover/FindHoverElement.java"
    "com/tyron/completion/java/action/FindMethodDeclarationAt.java"
    "com/tyron/completion/java/action/FindCurrentPath.java"
    "com/tyron/completion/java/rewrite/OverrideInheritedMethod.java"
    "com/tyron/completion/java/FindNewTypeDeclarationAt.java"
)

echo "Iniciando reemplazo de ReflectionUtil.getTreesInstance(...).getSourcePositions() -> ReflectionUtil.getSourcePositions(...)"

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "Procesando: $file"
        
        # Crear backup
        cp "$file" "$file.bak"
        
        # Reemplazar el patrón
        sed -i 's/ReflectionUtil\.getTreesInstance(\([^)]*\))\.getSourcePositions()/ReflectionUtil.getSourcePositions(\1)/g' "$file"
        
        # Verificar si FindHelper.java tiene la línea comentada (caso especial)
        if [[ "$file" == *"FindHelper.java" ]]; then
            sed -i 's/\/\/        var pos = ReflectionUtil\.getTreesInstance(\([^)]*\))\.getSourcePositions();/        SourcePositions pos = ReflectionUtil.getSourcePositions(\1);/g' "$file"
        fi
        
        echo "  ✅ Reemplazado"
    else
        echo "  ⚠️ Archivo no encontrado: $file"
    fi
done

# Caso especial: FindHoverElement.java tiene múltiples ocurrencias, asegurarnos de reemplazar todas
for file in $(grep -l "ReflectionUtil\.getTreesInstance.*getSourcePositions" com/tyron/completion/java/hover/FindHoverElement.java 2>/dev/null); do
    if [ -f "$file" ]; then
        sed -i 's/ReflectionUtil\.getTreesInstance(\([^)]*\))\.getSourcePositions()/ReflectionUtil.getSourcePositions(\1)/g' "$file"
    fi
done

echo ""
echo "✅ Reemplazo completado"
echo ""
echo "Archivos modificados:"
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "  - $file"
    fi
done
