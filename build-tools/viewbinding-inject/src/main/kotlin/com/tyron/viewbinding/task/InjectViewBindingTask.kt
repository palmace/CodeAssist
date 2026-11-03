package com.tyron.viewbinding.task

import com.tyron.builder.compiler.viewbinding.GenerateViewBindingTask
import com.tyron.builder.log.ILogger
import com.tyron.builder.model.ModuleSettings
import com.tyron.builder.model.SourceFileObject
import com.tyron.builder.project.Project
import com.tyron.builder.project.api.AndroidModule
import com.tyron.completion.java.JavaCompilerProvider
import java.io.File
import java.time.Instant
import java.nio.file.Path

/**
 * Re-sutura definitiva del inyector de ViewBinding.
 * Utiliza llamadas dinámicas para evitar errores de enlace entre módulos Java/Kotlin.
 */
class InjectViewBindingTask private constructor(
    val project: Project,
    val module: AndroidModule,
) {

    private fun doInject(consumer: (List<File>) -> Unit) {
        // Carpeta local de generación de bindings
        val viewBindingGenDir = "view_binding"
        val genTask = GenerateViewBindingTask(project, module, ILogger.EMPTY, false)
        val outputDir = File(module.buildDirectory, "injected/$viewBindingGenDir")

        try {
            genTask.prepareWithOutputDir(outputDir)
            genTask.run()

            val sources = outputDir.walkTopDown().filter {
                it.isFile && it.name.endsWith(".java")
            }.toList()

            // Inyectamos las clases en el modelo de CodeAssist
            sources.forEach { module.addInjectedClass(it) }

            consumer.invoke(sources)
        } catch (ignored: Throwable) {}
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun inject(project: Project, module: AndroidModule = project.mainModule as AndroidModule) {
            if (!module.settings.getBoolean(ModuleSettings.VIEW_BINDING_ENABLED, false)) {
                return
            }

            // Obtenemos el servicio de compilación mediante su proveedor
            val service = JavaCompilerProvider.get(project, module) ?: return
            val task = InjectViewBindingTask(project, module)
            
            task.doInject { files ->
                if (project.isCompiling || project.isIndexing) {
                    return@doInject
                }

                // Creamos los objetos de fuente necesarios para el compilador
                val sources = files.map { file ->
                    SourceFileObject(file.toPath(), module, Instant.now())
                }
                
                // Invocamos la compilación mediante reflexión para saltar barreras de tipos
                try {
                    val compileMethod = service.javaClass.methods.find { 
                        it.name == "compile" && it.parameterCount == 1 
                    }
                    compileMethod?.invoke(service, sources)
                } catch (e: Exception) {
                    // El fallo en la inyección de autocompletado no debe detener el ensamblado del APK
                }
            }
        }
    }
}
