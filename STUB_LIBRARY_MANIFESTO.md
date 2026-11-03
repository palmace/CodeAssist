# 🏛️ Manifiesto: La Biblioteca de Stubs Soberana (Omni 2026)

## 🎯 Visión General
Este documento nace de la necesidad imperante de **Ingeniería de Resistencia** en entornos de hardware limitado. Mientras la industria asume recursos infinitos, nosotros forjamos la **Soberanía del Desarrollo Móvil** mediante una capa de abstracción selectiva, ligera y de alta fidelidad.

Una vez que el proyecto **Omni Stress Test** alcance el éxito funcional, los stubs generados aquí dejarán de ser simples parches para convertirse en la primera **Biblioteca de Stubs de Bajo Consumo** para el ecosistema Android moderno (API 35+).

---

## 🔍 ¿Por qué crear esta biblioteca?

| Desafío en Maripa | Solución Soberana |
| :--- | :--- |
| **android.jar (Oficial)**: +200MB de peso. | **Stubs Quirúrgicos**: < 1MB. Solo el ADN necesario. |
| **Consumo RAM**: +4GB para indexación. | **Indexación Selectiva**: Corriendo bajo 512MB de RAM. |
| **Dependencia**: Internet constante para Maven. | **Soberanía Local**: 100% Offline y portable. |

---

## 🛠️ Pilares de la Arquitectura (v33.xx)

### 1. El "God Bridge" (Soberanía de View)
En lugar de herencias complejas que colapsan el motor de inferencia, inyectamos métodos de alto nivel (`setText`, `setAdapter`, `setLayoutManager`) directamente en la clase base `android.view.View`. Esto permite que el DataBinding y los Adapters operen con la fluidez de una PC de escritorio en un entorno móvil.

### 2. El Sello de Persistencia (AutoCloseable Cursor)
Hemos rediseñado el stub de `android.database.Cursor` para que herede explícitamente de `java.io.Closeable`. Esto no es solo para compilar; es para asegurar que la gestión de memoria de **Room Database** sea respetada incluso en stubs mínimos.

### 3. La Jerarquía de Fragmentos Blindada
Implementación de la cadena de mando completa:
- `Fragment` -> `FragmentActivity` -> `AppCompatActivity`.
- Sincronización del tipo de retorno de `getSupportFragmentManager()` para evitar el bucle de objetos genéricos.

### 4. Inmunidad a Expresiones Lambda
Soporte nativo inyectado para `java.lang.invoke.LambdaMetafactory`, permitiendo que el código de Java 8 (programación funcional) sea procesado por `javac` en Android 14/15 sin buscar archivos de la JVM de escritorio.

---

## 🚀 Hoja de Ruta Post-Omni

1.  **Consolidación**: Extraer los archivos `.java` de `IncrementalJavaTask` a un repositorio independiente llamado `omni-stubs-sdk`.
2.  **Modularización**: Crear módulos por familia (`lifecycle-stubs`, `material-stubs`, `persistence-stubs`).
3.  **Distribución**: Incluir esta biblioteca como opción por defecto en el fork de CodeAssist para desarrolladores en regiones con hardware limitado.

> "Usted no solo está arreglando un proyecto; está documentando cómo programar el futuro en las máquinas del pasado."

---
*Documento anexo al Roadmap General - CodeAssist Omni Maripa 2026*
