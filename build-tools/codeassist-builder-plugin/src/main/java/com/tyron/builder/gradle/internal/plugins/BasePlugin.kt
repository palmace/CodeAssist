package com.tyron.builder.gradle.internal.plugins

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions.checkState
import com.tyron.builder.api.dsl.BuildFeatures
import com.tyron.builder.api.dsl.CommonExtension
import com.tyron.builder.api.dsl.SettingsExtension
import com.tyron.builder.api.extension.impl.VariantApiOperationsRegistrar
import com.tyron.builder.api.variant.AndroidComponentsExtension
import com.tyron.builder.api.variant.Variant
import com.tyron.builder.api.variant.VariantBuilder
import com.tyron.builder.errors.IssueReporter
import com.tyron.builder.gradle.BaseExtension
import com.tyron.builder.gradle.api.AndroidBasePlugin
import com.tyron.builder.gradle.internal.*
import com.tyron.builder.gradle.internal.api.DefaultAndroidSourceSet
import com.tyron.builder.gradle.internal.component.TestComponentCreationConfig
import com.tyron.builder.gradle.internal.component.TestFixturesCreationConfig
import com.tyron.builder.gradle.internal.component.VariantCreationConfig
import com.tyron.builder.gradle.internal.core.dsl.VariantDslInfo
import com.tyron.builder.gradle.internal.dependency.CONFIG_NAME_ANDROID_JDK_IMAGE
import com.tyron.builder.gradle.internal.dependency.SourceSetManager
import com.tyron.builder.gradle.internal.dependency.VariantDependencies
import com.tyron.builder.gradle.internal.dsl.*
import com.tyron.builder.gradle.internal.ide.dependencies.LibraryDependencyCacheBuildService
import com.tyron.builder.gradle.internal.ide.dependencies.MavenCoordinatesCacheBuildService
import com.tyron.builder.gradle.internal.ide.v2.GlobalSyncService
import com.tyron.builder.gradle.internal.scope.DelayedActionsExecutor
import com.tyron.builder.gradle.internal.services.*
import com.tyron.builder.gradle.internal.tasks.factory.*
import com.tyron.builder.gradle.internal.utils.syncAgpAndKgpSources
import com.tyron.builder.gradle.internal.variant.*
import com.tyron.builder.model.v2.ide.ProjectType
import com.tyron.builder.plugin.options.SyncOptions
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

abstract class BasePlugin<
        BuildFeaturesT : BuildFeatures,
        BuildTypeT : com.tyron.builder.api.dsl.BuildType,
        DefaultConfigT : com.tyron.builder.api.dsl.DefaultConfig,
        ProductFlavorT : com.tyron.builder.api.dsl.ProductFlavor,
        AndroidT : CommonExtension<
                BuildFeaturesT,
                BuildTypeT,
                DefaultConfigT,
                ProductFlavorT>,
        AndroidComponentsT :
        AndroidComponentsExtension<
                in AndroidT,
                in VariantBuilderT,
                in VariantT>,
        VariantBuilderT : VariantBuilder,
        VariantDslInfoT : VariantDslInfo,
        CreationConfigT : VariantCreationConfig,
        VariantT : Variant>(
    val registry: ToolingModelBuilderRegistry,
    val componentFactory: SoftwareComponentFactory,
    listenerRegistry: BuildEventsListenerRegistry
) : AndroidPluginBaseServices(listenerRegistry), Plugin<Project> {

    protected class ExtensionData<
            BuildFeaturesT: BuildFeatures,
            BuildTypeT: com.tyron.builder.api.dsl.BuildType,
            DefaultConfigT: com.tyron.builder.api.dsl.DefaultConfig,
            ProductFlavorT: com.tyron.builder.api.dsl.ProductFlavor,
            AndroidT: CommonExtension<
                    out BuildFeaturesT,
                    out BuildTypeT,
                    out DefaultConfigT,
                    out ProductFlavorT>>(
        val oldExtension: BaseExtension,
        val newExtension: AndroidT,
        val bootClasspathConfig: BootClasspathConfigImpl,
    )

    @Suppress("DEPRECATION")
    private val buildOutputs by lazy {
        withProject("buildOutputs") {
            it.container(
                com.tyron.builder.gradle.api.BaseVariantOutput::class.java
            )
        }
    }

    private val variantApiOperations by lazy {
        VariantApiOperationsRegistrar<AndroidT, VariantBuilderT, VariantT>(
            extensionData.newExtension
        )
    }

    @get:VisibleForTesting
    val variantManager: VariantManager<AndroidT, VariantBuilderT, VariantDslInfoT, CreationConfigT> by lazy {
        withProject("variantManager") { project ->
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (VariantManager(
        project,
        dslServices,
        extension,
        newExtension,
        variantApiOperations as VariantApiOperationsRegistrar<AndroidT, VariantBuilder, Variant>,
        variantFactory,
        variantInputModel,
        globalConfig,
        projectServices
    ))
        }
    }

    @get:VisibleForTesting
    val variantInputModel: LegacyVariantInputManager by lazy {
        withProject("LegacyVariantInputManager") { project ->
            LegacyVariantInputManager(
                dslServices,
                variantFactory.componentType,
                SourceSetManager(
                    project,
                    isPackagePublished(),
                    dslServices,
                    DelayedActionsExecutor()
                )
            )
        }
    }

    private val extensionData by lazy {
        createExtension(
            dslServices,
            variantInputModel,
            buildOutputs,
            extraModelInfo,
            versionedSdkLoaderService
        )
    }

    private val globalConfig by lazy {
        withProject("globalConfig") { project ->
            @Suppress("DEPRECATION")
            (GlobalTaskCreationConfigImpl(
        project,
        extension,
        (newExtension as CommonExtensionImpl<*, *, *, *>),
        dslServices,
        versionedSdkLoaderService,
        bootClasspathConfig,
        createCustomLintPublishConfig(project),
        createCustomLintChecksConfig(project),
        createAndroidJarConfig(project),
    ))
        }
    }

    // Suturado: Eliminamos el lazy delegado que chocaba con genéricos y usamos inicialización directa
    private var _sdkComponentsBuildService: Provider<SdkComponentsBuildService>? = null
    protected val sdkComponentsBuildService: Provider<SdkComponentsBuildService>
        get() = _sdkComponentsBuildService ?: throw IllegalStateException("SDK service not initialized")

    protected val dslServices: DslServicesImpl by lazy {
        DslServicesImpl(
            projectServices,
            sdkComponentsBuildService
        ) {
            versionedSdkLoaderService
        }
    }

    private val taskManagerConfig: TaskManagerConfig by lazy {
        TaskManagerConfigImpl(dslServices, componentFactory)
    }

    protected val versionedSdkLoaderService: VersionedSdkLoaderService by lazy {
        withProject("versionedSdkLoaderService") { project ->
            VersionedSdkLoaderService(
                dslServices,
                project,
                {
                    @Suppress("DEPRECATION")
                    extension.compileSdkVersion
                },
            )
        }
    }

    private val bootClasspathConfig: BootClasspathConfigImpl by lazy {
        extensionData.bootClasspathConfig
    }

    private val variantFactory: VariantFactory<VariantBuilderT, VariantDslInfoT, CreationConfigT> by lazy {
        createVariantFactory()
    }

    protected val extraModelInfo: ExtraModelInfo = ExtraModelInfo()

    @Deprecated("use newExtension")
    val extension: BaseExtension by lazy { extensionData.oldExtension }
    private val newExtension: AndroidT by lazy { extensionData.newExtension }

    protected abstract fun createExtension(
        dslServices: DslServices,
        dslContainers: DslContainerProvider<DefaultConfig, BuildType, ProductFlavor, SigningConfig>,
        @Suppress("DEPRECATION")
        buildOutputs: NamedDomainObjectContainer<com.tyron.builder.gradle.api.BaseVariantOutput>,
        extraModelInfo: ExtraModelInfo,
        versionedSdkLoaderService: VersionedSdkLoaderService
    ): ExtensionData<BuildFeaturesT, BuildTypeT, DefaultConfigT, ProductFlavorT, AndroidT>

    protected abstract fun createVariantFactory(): VariantFactory<VariantBuilderT, VariantDslInfoT, CreationConfigT>

    protected abstract fun createTaskManager(
        project: Project,
        variants: Collection<ComponentInfo<VariantBuilderT, CreationConfigT>>,
        testComponents: Collection<TestComponentCreationConfig>,
        testFixturesComponents: Collection<TestFixturesCreationConfig>,
        globalTaskCreationConfig: GlobalTaskCreationConfig,
        localConfig: TaskManagerConfig,
        extension: BaseExtension,
    ): TaskManager<VariantBuilderT, CreationConfigT>

    private val hasCreatedTasks = AtomicBoolean(false)

    override fun apply(project: Project) {
        // Inicializamos el servicio del SDK inmediatamente para evitar problemas de lazy
        _sdkComponentsBuildService = SdkComponentsBuildService.RegistrationAction(
            project, projectServices.projectOptions
        ).execute()

        project.getAllTasks(true).values
        basePluginApply(project)
        pluginSpecificApply(project)
        project.pluginManager.apply(AndroidBasePlugin::class.java)
    }

    protected abstract fun pluginSpecificApply(project: Project)

    override fun configureProject(project: Project) {
        val gradle = project.gradle

        val stringCachingService: Provider<StringCachingBuildService> =
            StringCachingBuildService.RegistrationAction(project).execute()
        val mavenCoordinatesCacheBuildService =
            MavenCoordinatesCacheBuildService.RegistrationAction(project, stringCachingService)
                .execute()

        LibraryDependencyCacheBuildService.RegistrationAction(
            project, mavenCoordinatesCacheBuildService
        ).execute()

        GlobalSyncService.RegistrationAction(project, mavenCoordinatesCacheBuildService)
            .execute()

        val projectOptions = projectServices.projectOptions
        val issueReporter = projectServices.issueReporter

        Aapt2ThreadPoolBuildService.RegistrationAction(project, projectOptions).execute()
        Aapt2DaemonBuildService.RegistrationAction(project, projectOptions).execute()
        val locationsProvider = getBuildService(
            project.gradle.sharedServices,
            AndroidLocationsBuildService::class.java,
        )

        SymbolTableBuildService.RegistrationAction(project).execute()

        project.plugins.apply(JavaBasePlugin::class.java)

        project.tasks
            .named("assemble")
            .configure { task ->
                task.description = "Assembles all variants of all applications and secondary packages."
            }

        createAndroidJdkImageConfiguration(project)
    }

    private fun createAndroidJdkImageConfiguration(project: Project) {
        val config = project.configurations.create(CONFIG_NAME_ANDROID_JDK_IMAGE)
        config.isVisible = false
        config.isCanBeConsumed = false
        config.description = "Configuration providing JDK image for compiling Java 9+ sources"
    }

    override fun configureExtension(project: Project) {
        project.extensions.add("buildOutputs", buildOutputs)
        registerModels(
            project,
            registry,
            variantInputModel,
            extensionData,
            extraModelInfo,
            globalConfig)

        val unused = extensionData.newExtension

        variantFactory.createDefaultComponents(variantInputModel)
    }

    override fun createTasks(project: Project) {
        @Suppress("DEPRECATION")
        TaskManager.createTasksBeforeEvaluate(
            project,
            variantFactory.componentType,
            extension.sourceSets,
            variantManager.globalTaskCreationConfig
        )

        project.afterEvaluate {
            variantInputModel.sourceSetManager.runBuildableArtifactsActions()

            createAndroidTasks(project)
        }
    }

    fun createAndroidTasks(project: Project) {
        val globalConfig = variantManager.globalTaskCreationConfig
        if (hasCreatedTasks.get()) {
            return
        }
        hasCreatedTasks.set(true)
        variantManager.variantApiOperationsRegistrar.executeDslFinalizationBlocks()
        if (extension.compileSdkVersion == null) {
            if (SyncOptions.getModelQueryMode(projectServices.projectOptions)
                == SyncOptions.EvaluationMode.IDE
            ) {
                val newCompileSdkVersion: String = findHighestSdkInstalled()
                extension.compileSdkVersion = newCompileSdkVersion
            }
            dslServices
                .issueReporter
                .reportError(
                    IssueReporter.Type.COMPILE_SDK_VERSION_NOT_SET,
                    "compileSdkVersion is not specified. Please add it to build.gradle"
                )
        }

        checkState(extension.compileSdkVersion != null, "compileSdkVersion is not specified.")

        if (project.plugins.hasPlugin(JavaPlugin::class.java)) {
            throw BadPluginException(
                "The 'java' plugin has been applied, but it is not compatible with the Android plugins."
            )
        }

        if (project.plugins.hasPlugin("me.tatarka.retrolambda")) {
            val warningMsg =
                """One of the plugins you are using supports Java 8 language features. To try the support built into the Android plugin, remove the following from your build.gradle:
    apply plugin: 'me.tatarka.retrolambda'
To learn more, go to https://d.android.com/r/tools/java-8-support-message.html
"""
            dslServices.issueReporter.reportWarning(IssueReporter.Type.GENERIC, warningMsg)
        }

        project.repositories
            .forEach(
                Consumer { artifactRepository: ArtifactRepository ->
                    if (artifactRepository is FlatDirectoryArtifactRepository) {
                        val warningMsg = String.format(
                            "Using %s should be avoided because it doesn't support any meta-data formats.",
                            artifactRepository.getName()
                        )
                        dslServices
                            .issueReporter
                            .reportWarning(IssueReporter.Type.GENERIC, warningMsg)
                    }
                })

        if ((!project.state.executed || project.state.failure != null)

        ) {
            return
        }

        variantInputModel.lock()
        extension.disableWrite()

        val buildFeatureValues = variantFactory.createBuildFeatureValues(
            extension.buildFeatures, projectServices.projectOptions
        )

        variantManager
            .variantApiOperationsRegistrar
            .onEachSourceSetExtensions { name: String ->
                extension
                    .sourceSets
                    .forEach(
                        Consumer { androidSourceSet: com.tyron.builder.gradle.api.AndroidSourceSet? ->
                            if (androidSourceSet is DefaultAndroidSourceSet) {
                                androidSourceSet.extras.create(name)
                            }
                        })
            }
        variantManager.createVariants(buildFeatureValues)
        val variants = variantManager.mainComponents
        val taskManager = createTaskManager(
            project,
            variants,
            variantManager.testComponents,
            variantManager.testFixturesComponents,
            globalConfig,
            taskManagerConfig,
            extension
        )
        taskManager.createTasks(variantFactory.componentType, createVariantModel(globalConfig))
        DependencyConfigurator(
            project,
            projectServices
        )
            .configureCodeAssistTransformers()
            .configureDependencySubstitutions()
            .configureDependencyChecks()
            .configureGeneralTransforms(globalConfig.namespacedAndroidResources)
            .configureVariantTransforms(variants, variantManager.nestedComponents, globalConfig)
            .configureAttributeMatchingStrategies(variantInputModel)
            .configureJacocoTransforms()
            .configureCalculateStackFramesTransforms(globalConfig)

        @Suppress("DEPRECATION")
        val apiObjectFactory = ApiObjectFactory(extension, variantFactory, dslServices)
        for (variant in variants) {
            apiObjectFactory.create(variant.variant)
        }

        variantManager.lockVariantProperties()

        variantInputModel.sourceSetManager.checkForUnconfiguredSourceSets()
        @Suppress("DEPRECATION")
        syncAgpAndKgpSources(project, extension.sourceSets)

        taskManager.createPostApiTasks()

        for (component in variants) {
            component.variant.publishBuildArtifacts()
        }

        for (testFixturesComponent in variantManager.testFixturesComponents) {
            testFixturesComponent.publishBuildArtifacts()
        }
        checkSplitConfiguration()
        variantManager.setHasCreatedTasks(true)
        variantManager.finalizeAllVariants()
    }

    protected open fun registerModels(
        project: Project,
        registry: ToolingModelBuilderRegistry,
        variantInputModel: VariantInputModel<DefaultConfig, BuildType, ProductFlavor, SigningConfig>,
        extensionData: ExtensionData<BuildFeaturesT, BuildTypeT, DefaultConfigT, ProductFlavorT, AndroidT>,
        extraModelInfo: ExtraModelInfo,
        globalConfig: GlobalTaskCreationConfig
    ) {
        val variantModel: VariantModel = createVariantModel(globalConfig)
        registerModelBuilder(project, registry, variantModel, extensionData.oldExtension, extraModelInfo)
        registry.register(
            com.tyron.builder.gradle.internal.ide.v2.ModelBuilder<BuildFeaturesT, BuildTypeT, DefaultConfigT, ProductFlavorT, AndroidT>(
                project, variantModel, extensionData.newExtension
            )
        )
    }

    private fun createVariantModel(globalConfig: GlobalTaskCreationConfig): VariantModel {
        return VariantModelImpl(
            variantInputModel as VariantInputModel<DefaultConfig, BuildType, ProductFlavor, SigningConfig>,
            {
                @Suppress("DEPRECATION")
                extension.testBuildType
            },
            { variantManager.mainComponents.map { it.variant } },
            { variantManager.testComponents },
            { variantManager.buildFeatureValues },
            getProjectType(),
            getProjectTypeV2(),
            globalConfig)
    }

    protected open fun registerModelBuilder(
        project: Project,
        registry: ToolingModelBuilderRegistry,
        variantModel: VariantModel,
        extension: BaseExtension,
        extraModelInfo: ExtraModelInfo
    ) {
        registry.register(
            com.tyron.builder.gradle.internal.ide.ModelBuilder()
        )
    }

    protected abstract fun getProjectType(): Int

    protected abstract fun getProjectTypeV2(): ProjectType

    private fun findHighestSdkInstalled(): String {
        return "31"
    }

    private fun checkSplitConfiguration() {
        val configApkUrl = "https://d.android.com/topic/instant-apps/guides/config-splits.html"
        @Suppress("DEPRECATION")
        val generatePureSplits = extension.generatePureSplits
        @Suppress("DEPRECATION")
        val splits = extension.splits

        if (generatePureSplits) {
            dslServices
                .issueReporter
                .reportWarning(
                    IssueReporter.Type.GENERIC,
                    "Configuration APKs are supported by the Google Play Store only when publishing Android Instant Apps. To instead generate stand-alone APKs for different device configurations, set generatePureSplits=false. For more information, go to "
                            + configApkUrl
                )
        }
        if (!generatePureSplits && splits.language.isEnable) {
            dslServices
                .issueReporter
                .reportWarning(
                    IssueReporter.Type.GENERIC,
                    "Per-language APKs are supported only when building Android Instant Apps. For more information, go to "
                            + configApkUrl
                )
        }
    }

    companion object {
        fun createCustomLintChecksConfig(project: Project): Configuration {
            val lintChecks = project.configurations.maybeCreate(VariantDependencies.CONFIG_NAME_LINTCHECKS)
            lintChecks.isVisible = false
            lintChecks.description = "Configuration to apply external lint check jar"
            lintChecks.isCanBeConsumed = false
            return lintChecks
        }

        private fun createCustomLintPublishConfig(project: Project): Configuration {
            val lintChecks = project.configurations
                .maybeCreate(VariantDependencies.CONFIG_NAME_LINTPUBLISH)
            lintChecks.isVisible = false
            lintChecks.description = "Configuration to publish external lint check jar"
            lintChecks.isCanBeConsumed = false
            return lintChecks
        }

        private fun createAndroidJarConfig(project: Project): Configuration  {
            val androidJarConfig: Configuration = project.configurations
                .maybeCreate(VariantDependencies.CONFIG_NAME_ANDROID_APIS)
            androidJarConfig.description = "Configuration providing various types of Android JAR file"
            androidJarConfig.isCanBeConsumed = false
            return androidJarConfig
        }
    }

    protected open fun isPackagePublished(): Boolean {
        return false
    }

    private val settingsExtension: SettingsExtension? by lazy(LazyThreadSafetyMode.NONE) {
        val properties = project?.extensions?.extraProperties
        if (properties == null) {
            null
        } else if (properties.has("_android_settings")) {
            properties.get("_android_settings") as? SettingsExtension
        } else {
            null
        }
    }

    protected open fun AndroidT.doInitExtensionFromSettings(settings: SettingsExtension) {
        settings.compileSdk?.let { compileSdk ->
            this.compileSdk = compileSdk

            settings.compileSdkExtension?.let { compileSdkExtension ->
                this.compileSdkExtension = compileSdkExtension
            }
        }

        settings.compileSdkPreview?.let { compileSdkPreview ->
            this.compileSdkPreview = compileSdkPreview
        }

        settings.minSdk?.let { minSdk ->
            this.defaultConfig.minSdk = minSdk
        }

        settings.minSdkPreview?.let { minSdkPreview ->
            this.defaultConfig.minSdkPreview = minSdkPreview
        }

        settings.ndkVersion?.let { ndkVersion ->
            this.ndkVersion = ndkVersion
        }

        settings.ndkPath?.let { ndkPath ->
            this.ndkPath = ndkPath
        }

        settings.buildToolsVersion?.let { buildToolsVersion ->
            this.buildToolsVersion = buildToolsVersion
        }
    }

    protected fun initExtensionFromSettings(extension: AndroidT) {
        settingsExtension?.let {
            extension.doInitExtensionFromSettings(it)
        }
    }
}
