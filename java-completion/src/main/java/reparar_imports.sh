#!/bin/bash
# Script para mover imports de ReflectionUtil después del package

find com/tyron/completion/java -name "*.java" -type f | while read file; do
    echo "Procesando: $file"
    
    # Verificar si el archivo tiene el import antes del package
    if grep -q "^import com.tyron.completion.java.compiler.ReflectionUtil;" "$file" && grep -q "^package " "$file"; then
        # Crear archivo temporal
        tmpfile=$(mktemp)
        
        # Extraer la línea del package
        package_line=$(grep "^package " "$file")
        
        # Extraer la línea del import de ReflectionUtil
        import_line=$(grep "^import com.tyron.completion.java.compiler.ReflectionUtil;" "$file")
        
        # Extraer el resto del archivo (excluyendo esas líneas)
        grep -v "^package " "$file" | grep -v "^import com.tyron.completion.java.compiler.ReflectionUtil;" > "$tmpfile"
        
        # Reconstruir el archivo en el orden correcto
        {
            echo "$package_line"
            echo ""
            echo "$import_line"
            cat "$tmpfile"
        } > "$file"
        
        rm "$tmpfile"
        echo "  ✅ Reparado"
    else
        echo "  ⏭️  No necesita reparación"
    fi
done
