@file:OptIn(FrontendInternals::class)

package com.tyron.kotlin.completion

import android.util.Log
import com.tyron.builder.project.api.Module
import com.tyron.builder.project.api.AndroidModule
import com.tyron.completion.model.CompletionItem
import com.tyron.completion.model.DrawableKind
import com.tyron.kotlin.completion.codeInsight.ReferenceVariantsHelper
import com.tyron.kotlin.completion.model.Analysis
import com.tyron.kotlin.completion.util.*
import com.tyron.kotlin_completion.util.PsiUtils
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import java.util.*
import javax.tools.Diagnostic

/**
 * KotlinEnvironment v28.2: "The Zenith IDE Core - Final Visibility Restoration".
 * v28.2: Fix - Propiedades públicas para asegurar generación de getters para Java.
 * v28.2: Sincronía definitiva con AndroidModule Interface v41.8.0.
 */
class KotlinEnvironment(
    val classpath: List<File>,
    val kotlinEnvironment: KotlinCoreEnvironment
) {
    private val kotlinFiles = mutableMapOf<String, KotlinFile>()
    private val diagnosticsMap = mutableMapOf<String, List<Diagnostic<File>>>()

    fun updateKotlinFile(name: String, contents: String): KotlinFile {
        val kotlinFile = KotlinFile.from(kotlinEnvironment.project, name, contents)
        kotlinFiles[name] = kotlinFile
        return kotlinFile
    }

    fun removeKotlinFile(name: String) {
        kotlinFiles.remove(name)
        diagnosticsMap.remove(name)
    }

    fun getAllKtFiles(): List<KtFile> = kotlinFiles.values.map { it.kotlinFile }

    fun setDiagnosticsForFile(path: String, list: List<Diagnostic<File>>) {
        diagnosticsMap[path] = list
    }

    fun getDiagnosticsForFile(path: String): List<Diagnostic<File>> {
        return diagnosticsMap[path] ?: emptyList()
    }

    private val renderer = IdeDescriptorRenderersScripting.SOURCE_CODE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        typeNormalizer = { if (it.isFlexible()) it.asFlexibleType().upperBound else it }
    }

    fun getPrefix(element: PsiElement): String {
        val simpleName = if (element is KtSimpleNameExpression) element else PsiUtils.findParent(element, KtSimpleNameExpression::class.java)
        return (simpleName?.text ?: "").substringBefore(COMPLETION_SUFFIX).let { if (it.endsWith(".")) "" else it }
    }

    fun complete(file: KotlinFile, line: Int, character: Int): List<CompletionItem> {
        val inserted = file.insert("$COMPLETION_SUFFIX ", line, character)
        kotlinFiles[file.name] = inserted
        val element = inserted.elementAt(line, character) ?: return emptyList()
        
        val analysis = analysisOf(kotlinFiles.values.map { it.kotlinFile }.toList())
        val prefix = getPrefix(element)
        
        val simpleName = if (element is KtSimpleNameExpression) element else PsiUtils.findParent(element, KtSimpleNameExpression::class.java)
        if (simpleName !is KtSimpleNameExpression) return keywordsCompletionVariants(KtTokens.KEYWORDS, prefix)

        return try {
            val variants = ReferenceVariantsHelper(
                analysis.analysisResult.bindingContext,
                resolutionFacade = KotlinResolutionFacade(element.project, analysis.componentProvider, analysis.analysisResult.moduleDescriptor),
                moduleDescriptor = analysis.analysisResult.moduleDescriptor,
                visibilityFilter = { true }
            ).getReferenceVariants(simpleName, DescriptorKindFilter.ALL, { true })
            
            variants.mapNotNull { completionVariantFor(prefix, it) } + 
            keywordsCompletionVariants(KtTokens.KEYWORDS, prefix)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun completionVariantFor(prefix: String, descriptor: DeclarationDescriptor): CompletionItem? {
        val name = descriptor.name.asString()
        if (prefix.isEmpty() || name.startsWith(prefix)) {
            return CompletionItem(name).apply {
                iconKind = DrawableKind.Snippet
                detail = renderer.render(descriptor)
            }
        }
        return null
    }

    private fun keywordsCompletionVariants(keywords: TokenSet, prefix: String) =
        keywords.types.mapNotNull {
            if (it is KtKeywordToken && it.value.startsWith(prefix)) {
                CompletionItem(it.value, "Keyword", it.value, DrawableKind.Keyword)
            } else null
        }

    fun analysisOf(files: List<KtFile>): Analysis {
        val project = kotlinEnvironment.project
        val trace = CliBindingTrace()
        val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
            project, files, trace, kotlinEnvironment.configuration,
            { scope -> kotlinEnvironment.createPackagePartProvider(scope) },
            { storage, ktFiles -> FileBasedDeclarationProviderFactory(storage, ktFiles) }
        )
        componentProvider.getService(LazyTopDownAnalyzer::class.java)
            .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files, DataFlowInfo.EMPTY)
        val moduleDescriptor = componentProvider.getService(ModuleDescriptor::class.java)
        return Analysis(componentProvider, AnalysisResult.success(trace.bindingContext, moduleDescriptor))
    }

    companion object {
        private const val COMPLETION_SUFFIX = "IntellijIdeaRulezzz"
        @JvmField
        val ENVIRONMENT_KEY = Key.create<KotlinEnvironment>("kotlinEnvironmentKey")

        @JvmStatic
        fun with(classpath: List<File>): KotlinEnvironment {
            System.setProperty("idea.io.use.nio2", "true")
            val disposable = object : Disposable { override fun dispose() {} }
            val config = CompilerConfiguration().apply {
                addJvmClasspathRoots(classpath.filter { it.exists() })
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, LoggingMessageCollector)
                put(CommonConfigurationKeys.MODULE_NAME, "Zenith-Analysis-" + UUID.randomUUID())
                put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_9, ApiVersion.KOTLIN_1_9))
            }
            return KotlinEnvironment(classpath, KotlinCoreEnvironment.createForProduction(disposable, config, EnvironmentConfigFiles.JVM_CONFIG_FILES))
        }

        @JvmStatic
        fun get(module: Module): KotlinEnvironment? {
            val androidModule = module as? AndroidModule ?: return null
            val existing = androidModule.getUserData(ENVIRONMENT_KEY)
            if (existing != null) return existing

            val jars = mutableListOf<File>()
            androidModule.getCodeAssistLibraries()?.forEach { lib ->
                val source = lib.getSourceFile()
                if (source != null && source.exists()) jars.add(source.getAbsoluteFile())
            }
            
            val buildDir = androidModule.getBuildDirectory()?.getAbsoluteFile()
            if (buildDir != null) {
                val kotlinClasses = File(buildDir, "bin/kotlin/classes")
                if (kotlinClasses.exists()) jars.add(kotlinClasses)
            }

            val environment = with(jars)
            androidModule.getKotlinFiles().values.forEach { if (it.exists()) environment.updateKotlinFile(it.absolutePath, it.readText()) }
            androidModule.putUserData(ENVIRONMENT_KEY, environment)
            return environment
        }
    }
}
