package com.tyron.builder.compiler.viewbinding

import android.util.Log
import com.tyron.builder.compiler.BuildType
import com.tyron.builder.compiler.Task
import com.tyron.builder.exception.CompilationFailedException
import com.tyron.builder.log.ILogger
import com.tyron.builder.model.ModuleSettings
import com.tyron.builder.project.Project
import com.tyron.builder.project.api.AndroidModule
import com.tyron.viewbinding.tool.DataBindingBuilder.GradleFileWriter
import com.tyron.viewbinding.tool.processing.Scope
import com.tyron.viewbinding.tool.processing.ScopedException
import com.tyron.viewbinding.tool.store.LayoutFileParser
import com.tyron.viewbinding.tool.store.ResourceBundle
import com.tyron.viewbinding.tool.util.LoggedErrorException
import com.tyron.viewbinding.tool.util.RelativizableFile
import com.tyron.viewbinding.tool.writer.BaseLayoutModel
import com.tyron.viewbinding.tool.writer.toJavaFile
import com.tyron.viewbinding.tool.writer.toViewBinder
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Task de generación de ViewBinding v44.11.11: "The Sovereign Interface Auditor Ultra".
 * v44.11.11: Corregido conteo recursivo de archivos generados (Fix Falso Positivo del Auditor).
 * v44.11.10: Directiva Gemini - Fail-Fast si la generación física es nula.
 */
class GenerateViewBindingTask(
    project: Project?,
    module: AndroidModule,
    logger: ILogger,
    private val addToClasspath: Boolean,
) : Task<AndroidModule>(project, module, logger) {

    private lateinit var outputDir: File

    override fun getName() = TAG

    override fun prepare(type: BuildType?) {
        val buildDir = module.buildDirectory.absoluteFile
        outputDir = File(buildDir, "bin/generated/data_binding_base_class_source_out")
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()
    }

    fun prepareWithOutputDir(outputDir: File) {
        this.outputDir = outputDir.absoluteFile
        if (this.outputDir.exists()) {
            this.outputDir.deleteRecursively()
        }
        this.outputDir.mkdirs()
    }

    override fun run() {
        getLogger().info("🚀 OMNI: Iniciando Sincronía de Interfaz Zenith v44.11.11...")
        try {
            doRun()
        } catch (e: Exception) {
            val log = Log.getStackTraceString(e)
            throw CompilationFailedException("Fallo en Sincronía de Interfaz: \n$log")
        }
    }

    private fun doRun() {
        val resDir = module.androidResourcesDirectory ?: return
        if (!resDir.exists()) return

        val resourceBundle = generateClassesToBundle()
        val layoutsCount = resourceBundle.allLayoutFileBundlesInSource.size
        
        if (layoutsCount == 0) {
             getLogger().info("ℹ️ No se detectaron layouts para procesar.")
             return
        }
        
        writeClassesToDisk(resourceBundle)
        
        // Verificación Forense v44.11.11: Conteo recursivo real de archivos .java
        val generatedFilesCount = outputDir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.count()
        
        if (generatedFilesCount == 0) {
            throw IllegalStateException("ERROR DE MATERIALIZACIÓN: Se procesaron $layoutsCount layouts pero no se generó ninguna interfaz en: ${outputDir.absolutePath}. Revise la integridad del motor de bindeo.")
        }

        getLogger().info("✅ OMNI: Se han materializado $generatedFilesCount interfaces de bindeo.")

        Scope.assertNoError()
        if (addToClasspath) {
            syncToModule()
        }
    }

    private fun generateClassesToBundle(): ResourceBundle {
        val resourceBundle = ResourceBundle(module.packageName, true)
        val resDir = module.androidResourcesDirectory ?: return resourceBundle

        resDir.walkTopDown().filter {
            it.isFile && it.name.endsWith(".xml") && it.parentFile?.name?.startsWith("layout") == true
        }.forEach { file ->
            val content = getUpToDateFileContent(module, file)
            try {
                val bundle = LayoutFileParser.parseXml(
                    RelativizableFile.fromAbsoluteFile(file),
                    module.packageName,
                    content,
                    true
                )
                if (bundle != null) {
                    resourceBundle.addLayoutBundle(bundle, true)
                }
            } catch (e: Exception) {
                getLogger().error("❌ Error parseando layout ${file.name}: ${e.message}")
            }
        }
        resourceBundle.validateAndRegisterErrors()
        return resourceBundle
    }

    private fun writeClassesToDisk(resourceBundle: ResourceBundle) {
        val writer = GradleFileWriter(outputDir.absolutePath)
        val layoutBindings = resourceBundle.allLayoutFileBundlesInSource
            .groupBy(ResourceBundle.LayoutFileBundle::getFileName)

        layoutBindings.forEach { (_, variations) ->
            val layoutModel = BaseLayoutModel(variations)
            val viewBinder = layoutModel.toViewBinder()
            val javaFile = viewBinder.toJavaFile(useLegacyAnnotations = false)
            writer.writeToFile(javaFile)
        }
    }

    private fun syncToModule() {
        outputDir.walkTopDown().filter {
            it.isFile && it.name.endsWith(".java")
        }.forEach { file ->
            module.addResourceClass(file)
            module.addJavaFile(file)
        }
    }

    companion object {
        const val TAG = "GenerateViewBindingTask"

        private fun getUpToDateFileContent(module: AndroidModule, file: File): String? {
            try {
                val fileManager = module.fileManager
                if (fileManager != null && fileManager.isOpened(file)) {
                    val fileContent = fileManager.getFileContent(file)
                    if (fileContent != null && fileContent.isPresent) {
                        return fileContent.get().toString()
                    }
                }
            } catch (ignored: Exception) {}

            try {
                if (file.exists()) {
                    return FileUtils.readFileToString(file, StandardCharsets.UTF_8)
                }
            } catch (ignored: IOException) {}

            return null
        }
    }
}
