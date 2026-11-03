# =====================================================================
# ESCUDO ZENITH: PRESERVACIÓN DEL ENTORNO DE COMPILACIÓN KOTLIN
# =====================================================================

# Evitar que R8/ProGuard elimine o mutile el compilador y sus dependencias
-keep class org.jetbrains.kotlin.** { *; }
-keep class org.jetbrains.kotlin.com.intellij.** { *; }
-keep class org.jetbrains.org.objectweb.asm.** { *; }
-keep class com.intellij.** { *; }

# Preservar constantes críticas y llaves de configuración
-keepclassmembers class org.jetbrains.kotlin.** {
    public static *** NONE;
    public static *** EMPTY;
    public static *** Companion;
    public static *** INSTANCE;
    public static *** MESSAGE_COLLECTOR;
    public static *** MESSAGE_COLLECTOR_KEY;
}

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preservar la infraestructura de servicios y extensiones XML de IntelliJ/Kotlin
-keepresources META-INF/services/**
-keepresources META-INF/extensions/**
-keepresources META-INF/MANIFEST.MF
-keepresources META-INF/*.xml
-keepresources META-INF/*.properties

# No ofuscar ni optimizar el compilador para mantener la integridad de la reflexión
-dontobfuscate
-dontoptimize
-dontshrink
