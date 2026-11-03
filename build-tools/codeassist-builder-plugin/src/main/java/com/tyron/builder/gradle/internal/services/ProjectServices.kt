package com.tyron.builder.gradle.internal.services

import com.android.tools.aapt2.Aapt2Jni
import com.tyron.builder.gradle.errors.DeprecationReporter
import com.tyron.builder.gradle.errors.SyncIssueReporter
import com.tyron.builder.gradle.internal.res.Aapt2FromMaven
import com.tyron.builder.gradle.internal.scope.ProjectInfo
import com.tyron.builder.gradle.internal.utils.GradleEnvironmentProvider
import com.tyron.builder.gradle.options.ProjectOptions
import com.tyron.builder.internal.utils.setDisallowChanges
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildServiceRegistry
import java.io.File

/**
 * Service object for the project, containing a bunch of project-provided items that can be exposed
 * to different stages of the plugin work.
 */
class ProjectServices constructor(
    override val issueReporter: SyncIssueReporter,
    override val deprecationReporter: DeprecationReporter,
    val objectFactory: ObjectFactory,
    val logger: Logger,
    val providerFactory: ProviderFactory,
    val projectLayout: ProjectLayout,
    override val projectOptions: ProjectOptions,
    override val buildServiceRegistry: BuildServiceRegistry,
    private val aapt2FromMaven: Aapt2FromMaven? = null,
    private val maxWorkerCount: Int,
    override val projectInfo: ProjectInfo,
    val fileResolver: (Any) -> File,
    val configurationContainer: ConfigurationContainer,
    val dependencyHandler: DependencyHandler,
    val extraProperties: ExtraPropertiesExtension
) : BaseServices {

    override val gradleEnvironmentProvider: GradleEnvironmentProvider = object : GradleEnvironmentProvider {
        override fun getSystemProperty(key: String): Provider<String> = providerFactory.systemProperty(key)
        override fun getEnvVariable(key: String): Provider<String> = providerFactory.environmentVariable(key)
    }

    override fun <T> newInstance(type: Class<T>, vararg args: Any?): T = 
        objectFactory.newInstance(type, *args)

    override fun file(file: Any): File = fileResolver(file)

    fun initializeAapt2Input(aapt2Input: Aapt2Input) {
        aapt2Input.buildService.setDisallowChanges(getBuildService(buildServiceRegistry))
        aapt2Input.threadPoolBuildService.setDisallowChanges(getBuildService(buildServiceRegistry))

        // Usamos la implementación nativa vinculada
        aapt2Input.binaryDirectory.from(Aapt2Jni.getSymlinkedAapt2Directory())
        aapt2Input.version.setDisallowChanges(aapt2FromMaven?.version ?: "0")
        aapt2Input.maxWorkerCount.setDisallowChanges(maxWorkerCount)
        aapt2Input.maxAapt2Daemons.setDisallowChanges(computeMaxAapt2Daemons(projectOptions))
    }
}
