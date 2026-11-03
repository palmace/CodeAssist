package com.tyron.kotlin_completion.compiler

import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

fun createContainer(environment: KotlinCoreEnvironment, sourcePath: Collection<KtFile>): Pair<ComponentProvider, CliBindingTrace> {
    val project = environment.project
    // OMNI v15.63: CliBindingTrace en Kotlin 1.9.24 no acepta argumentos.
    val cliBindingTrace = CliBindingTrace()
    val container = TopDownAnalyzerFacadeForJVM.createContainer(
        project,
        sourcePath,
        cliBindingTrace,
        environment.configuration,
        { globalSearchScope -> environment.createPackagePartProvider(globalSearchScope) },
        { storageManager, ktFiles -> FileBasedDeclarationProviderFactory(storageManager, ktFiles) }
    )
    return Pair(container, cliBindingTrace)
}
