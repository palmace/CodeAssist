package com.tyron.builder.gradle.internal.ide.v2

import com.tyron.builder.api.dsl.AndroidResources
import com.tyron.builder.api.dsl.CompileOptions
import com.tyron.builder.api.variant.impl.SourcesImpl
import com.tyron.builder.api.dsl.Lint
import com.tyron.builder.api.variant.impl.SourceDirectoriesImpl
import com.tyron.builder.gradle.internal.api.DefaultAndroidSourceSet
import com.tyron.builder.gradle.internal.scope.BuildFeatureValues
import com.tyron.builder.gradle.internal.utils.toImmutableList
import com.tyron.builder.gradle.internal.utils.toImmutableMap
import com.tyron.builder.model.TestOptions
import com.tyron.builder.model.v2.dsl.ClassField
import com.tyron.builder.model.v2.ide.AaptOptions.Namespacing.DISABLED
import com.tyron.builder.model.v2.ide.AaptOptions.Namespacing.REQUIRED
import com.tyron.builder.model.v2.ide.CodeShrinker
import com.tyron.builder.model.v2.ide.JavaCompileOptions
import com.tyron.builder.model.v2.ide.TestInfo
import com.tyron.builder.api.dsl.ApkSigningConfig as DslSigningConfig
import com.tyron.builder.api.dsl.BuildType as DslBuildType
import com.tyron.builder.api.dsl.DefaultConfig as DslDefaultConfig
import com.tyron.builder.api.dsl.ProductFlavor as DslProductFlavor
import com.tyron.builder.api.dsl.ApplicationBaseFlavor
import com.tyron.builder.gradle.internal.dsl.VectorDrawablesOptions as InternalVectorDrawablesOptions
import com.tyron.builder.model.ApiVersion as DslApiVersion
import com.tyron.builder.model.ClassField as DslClassField
import com.tyron.builder.model.CodeShrinker as CodeShrinkerV1
import com.tyron.builder.gradle.internal.dsl.BuildType as InternalBuildType
import com.tyron.builder.gradle.internal.dsl.ProductFlavor as InternalProductFlavor
import com.tyron.builder.gradle.internal.dsl.DefaultConfig as InternalDefaultConfig
import com.tyron.builder.gradle.internal.dsl.BaseFlavor as InternalBaseFlavor
import com.tyron.builder.gradle.internal.dsl.VariantDimensionBinaryCompatibilityFix

// Converts DSL items into v2 model instances

internal fun DslDefaultConfig.convert(features: BuildFeatureValues): ProductFlavorImpl {
    val internal = this as? InternalDefaultConfig
    return ProductFlavorImpl(
        name = "default",
        dimension = null,
        applicationId = (this as? ApplicationBaseFlavor)?.applicationId,
        versionCode = (this as? ApplicationBaseFlavor)?.versionCode,
        versionName = (this as? ApplicationBaseFlavor)?.versionName,
        minSdkVersion = (this as? com.tyron.builder.api.dsl.BaseFlavor)?.minSdk?.let { ApiVersionImpl(it, null) },
        targetSdkVersion = (this as? ApplicationBaseFlavor)?.targetSdk?.let { ApiVersionImpl(it, null) },
        maxSdkVersion = (this as? ApplicationBaseFlavor)?.maxSdk,
        renderscriptTargetApi = renderscriptTargetApi,
        renderscriptSupportModeEnabled = renderscriptSupportModeEnabled,
        renderscriptSupportModeBlasEnabled = renderscriptSupportModeBlasEnabled,
        renderscriptNdkModeEnabled = renderscriptNdkModeEnabled,
        testApplicationId = testApplicationId,
        testInstrumentationRunner = testInstrumentationRunner,
        testInstrumentationRunnerArguments = testInstrumentationRunnerArguments,
        testHandleProfiling = testHandleProfiling,
        testFunctionalTest = testFunctionalTest,
        resourceConfigurations = resourceConfigurations.toImmutableList(),
        signingConfig = (internal as? VariantDimensionBinaryCompatibilityFix)?._internal_getSigingConfig()?.name,
        vectorDrawables = (vectorDrawables as? InternalVectorDrawablesOptions)?.convert() ?: VectorDrawableOptionsImpl(emptySet(), false),
        wearAppUnbundled = wearAppUnbundled,
        applicationIdSuffix = (this as? InternalBaseFlavor)?.applicationIdSuffix,
        versionNameSuffix = (this as? InternalBaseFlavor)?.versionNameSuffix,
        buildConfigFields = internal?.buildConfigFields?.convertBuildConfig(features),
        resValues = internal?.resValues?.convertResValues(features),
        proguardFiles = proguardFiles.toImmutableList(),
        consumerProguardFiles = internal?.consumerProguardFiles?.toImmutableList() ?: emptyList(),
        testProguardFiles = testProguardFiles.toImmutableList(),
        manifestPlaceholders = manifestPlaceholders.toImmutableMap(),
        multiDexEnabled = (this as? InternalBaseFlavor)?.multiDexEnabled,
        multiDexKeepFile = multiDexKeepFile,
        multiDexKeepProguard = multiDexKeepProguard,
    )
}

internal fun DslProductFlavor.convert(features: BuildFeatureValues): ProductFlavorImpl {
    val internal = this as? InternalProductFlavor
    return ProductFlavorImpl(
        name = name,
        dimension = dimension,
        applicationId = (this as? ApplicationBaseFlavor)?.applicationId,
        versionCode = (this as? ApplicationBaseFlavor)?.versionCode,
        versionName = (this as? ApplicationBaseFlavor)?.versionName,
        minSdkVersion = (this as? com.tyron.builder.api.dsl.BaseFlavor)?.minSdk?.let { ApiVersionImpl(it, null) },
        targetSdkVersion = (this as? ApplicationBaseFlavor)?.targetSdk?.let { ApiVersionImpl(it, null) },
        maxSdkVersion = (this as? ApplicationBaseFlavor)?.maxSdk,
        renderscriptTargetApi = renderscriptTargetApi,
        renderscriptSupportModeEnabled = renderscriptSupportModeEnabled,
        renderscriptSupportModeBlasEnabled = renderscriptSupportModeBlasEnabled,
        renderscriptNdkModeEnabled = renderscriptNdkModeEnabled,
        testApplicationId = testApplicationId,
        testInstrumentationRunner = testInstrumentationRunner,
        testInstrumentationRunnerArguments = testInstrumentationRunnerArguments.toImmutableMap(),
        testHandleProfiling = testHandleProfiling,
        testFunctionalTest = testFunctionalTest,
        resourceConfigurations = resourceConfigurations.toImmutableList(),
        signingConfig = (internal as? VariantDimensionBinaryCompatibilityFix)?._internal_getSigingConfig()?.name,
        vectorDrawables = (vectorDrawables as? InternalVectorDrawablesOptions)?.convert() ?: VectorDrawableOptionsImpl(emptySet(), false),
        wearAppUnbundled = wearAppUnbundled,
        applicationIdSuffix = (this as? InternalBaseFlavor)?.applicationIdSuffix,
        versionNameSuffix = (this as? InternalBaseFlavor)?.versionNameSuffix,
        buildConfigFields = internal?.buildConfigFields?.convertBuildConfig(features),
        resValues = internal?.resValues?.convertResValues(features),
        proguardFiles = proguardFiles,
        consumerProguardFiles = internal?.consumerProguardFiles ?: emptyList(),
        testProguardFiles = testProguardFiles,
        manifestPlaceholders = manifestPlaceholders.toImmutableMap(),
        multiDexEnabled = (this as? InternalBaseFlavor)?.multiDexEnabled,
        multiDexKeepFile = multiDexKeepFile,
        multiDexKeepProguard = multiDexKeepProguard,
        isDefault = internal?.isDefault ?: false
    )
}

internal fun DslBuildType.convert(features: BuildFeatureValues): BuildTypeImpl {
    val internal = this as? InternalBuildType
    return BuildTypeImpl(
        name = name,
        isDebuggable = internal?.isDebuggable ?: false,
        isProfileable = internal?.isProfileable ?: false,
        isTestCoverageEnabled = @Suppress("DEPRECATION") isTestCoverageEnabled,
        isPseudoLocalesEnabled = isPseudoLocalesEnabled,
        isJniDebuggable = isJniDebuggable,
        isRenderscriptDebuggable = isRenderscriptDebuggable,
        renderscriptOptimLevel = renderscriptOptimLevel,
        isMinifyEnabled = isMinifyEnabled,
        isZipAlignEnabled = @Suppress("DEPRECATION") isZipAlignEnabled,
        isEmbedMicroApp = internal?.isEmbedMicroApp ?: false,
        signingConfig = internal?.signingConfig?.name,
        applicationIdSuffix = internal?.applicationIdSuffix,
        versionNameSuffix = internal?.versionNameSuffix,
        buildConfigFields = internal?.buildConfigFields?.convertBuildConfig(features),
        resValues = internal?.resValues?.convertResValues(features),
        proguardFiles = proguardFiles.toImmutableList(),
        consumerProguardFiles = internal?.consumerProguardFiles?.toImmutableList() ?: emptyList(),
        testProguardFiles = testProguardFiles.toImmutableList(),
        manifestPlaceholders = manifestPlaceholders.toImmutableMap(),
        multiDexEnabled = internal?.multiDexEnabled,
        multiDexKeepFile = multiDexKeepFile,
        multiDexKeepProguard = multiDexKeepProguard,
        isDefault = internal?.isDefault ?: false
    )
}

internal fun DslSigningConfig.convert() = SigningConfigImpl(
    name = name,
    storeFile = storeFile,
    storePassword = storePassword,
    keyAlias = keyAlias,
    keyPassword = keyPassword,
    enableV1Signing = enableV1Signing,
    enableV2Signing = enableV2Signing,
    enableV3Signing = enableV3Signing,
    enableV4Signing = enableV4Signing
)

private fun Map<String, DslClassField>.convertBuildConfig(
    features: BuildFeatureValues
): Map<String, ClassField>? =
    if (features.buildConfig)
        asSequence().map { it.key to it.value.convert() }.toMap()
    else
        null

private fun Map<String, DslClassField>.convertResValues(
    features: BuildFeatureValues
): Map<String, ClassField>? =
    if (features.resValues)
        asSequence().map { it.key to it.value.convert() }.toMap()
    else
        null

private fun DslClassField.convert() = ClassFieldImpl(
    type = type,
    name = name,
    value = value,
    documentation = documentation,
    annotations = annotations
)

private fun InternalVectorDrawablesOptions.convert() = VectorDrawableOptionsImpl(
    generatedDensities = generatedDensities?.toSet(),
    useSupportLibrary = useSupportLibrary
)

internal fun DslApiVersion.convert() = ApiVersionImpl(
    apiLevel = apiLevel,
    codename = codename
)

internal fun DefaultAndroidSourceSet.convert(
    features: BuildFeatureValues,
) = SourceProviderImpl(
    name = name,
    manifestFile = manifestFile,
    javaDirectories = javaDirectories,
    kotlinDirectories = kotlinDirectories,
    resourcesDirectories = resourcesDirectories,
    aidlDirectories = if (features.aidl) aidlDirectories else null,
    renderscriptDirectories = if (features.renderScript) renderscriptDirectories else null,
    resDirectories = if (features.androidResources) resDirectories else null,
    assetsDirectories = assetsDirectories,
    jniLibsDirectories = jniLibsDirectories,
    shadersDirectories = if (features.shaders) shadersDirectories else null,
    mlModelsDirectories = if (features.mlModelBinding) mlModelsDirectories else null,
    customDirectories = customDirectories,
)

internal fun DefaultAndroidSourceSet.convert(
    features: BuildFeatureValues,
    sources: SourcesImpl,
) = SourceProviderImpl(
    name = name,
    manifestFile = manifestFile,
    javaDirectories = variantSourcesForModel(sources.java),
    kotlinDirectories = variantSourcesForModel(sources.kotlin),
    resourcesDirectories = variantSourcesForModel(sources.resources),
    aidlDirectories = if (features.aidl) aidlDirectories else null,
    renderscriptDirectories = if (features.renderScript) renderscriptDirectories else null,
    resDirectories = if (features.androidResources) variantSourcesForModel(sources.res) else null,
    assetsDirectories = variantSourcesForModel(sources.assets),
    jniLibsDirectories = variantSourcesForModel(sources.jniLibs),
    shadersDirectories = sources.shaders?.let { variantSourcesForModel(it) },
    mlModelsDirectories = if (features.mlModelBinding) mlModelsDirectories else null,
    customDirectories = customDirectories,
)

private fun variantSourcesForModel(sourceDirectories: SourceDirectoriesImpl) =
    sourceDirectories.variantSourcesForModel { it.shouldBeAddedToIdeModel }

internal fun AndroidResources.convert() = AaptOptionsImpl(
    namespacing = if (namespaced) REQUIRED else DISABLED
)

internal fun Lint.convert() = LintOptionsImpl(
    disable = this.disable.toSet(),
    enable = this.enable.toSet(),
    informational = this.informational.toSet(),
    warning = this.warning.toSet(),
    error = this.error.toSet(),
    fatal = this.fatal.toSet(),
    checkOnly = this.checkOnly.toSet(),
    lintConfig = lintConfig,
    textReport = textReport,
    textOutput = textOutput,
    htmlOutput = htmlOutput,
    htmlReport = htmlReport,
    xmlReport = xmlReport,
    xmlOutput = xmlOutput,
    sarifReport = sarifReport,
    sarifOutput = sarifOutput,
    abortOnError = abortOnError,
    absolutePaths = absolutePaths,
    noLines = noLines,
    quiet = quiet,
    checkAllWarnings = checkAllWarnings,
    ignoreWarnings = ignoreWarnings,
    warningsAsErrors = warningsAsErrors,
    showAll = showAll,
    explainIssues = explainIssues,
    checkReleaseBuilds = checkReleaseBuilds,
    checkTestSources = checkTestSources,
    ignoreTestSources = ignoreTestSources,
    ignoreTestFixturesSources = ignoreTestFixturesSources,
    checkGeneratedSources = checkGeneratedSources,
    checkDependencies = checkDependencies,
    baseline = baseline,
)

@Suppress("DEPRECATION")
internal fun com.tyron.builder.api.dsl.LintOptions.convert() = LintOptionsImpl(
    disable = this.disable,
    enable = this.enable,
    informational = emptySet(), // No disponible directamente en interfaz antigua
    warning = emptySet(),
    error = emptySet(),
    fatal = emptySet(),
    checkOnly = this.checkOnly,
    lintConfig = lintConfig,
    textReport = textReport,
    textOutput = textOutput,
    htmlOutput = htmlOutput,
    htmlReport = htmlReport,
    xmlReport = xmlReport,
    xmlOutput = xmlOutput,
    sarifReport = sarifReport,
    sarifOutput = sarifOutput,
    abortOnError = isAbortOnError,
    absolutePaths = isAbsolutePaths,
    noLines = isNoLines,
    quiet = isQuiet,
    checkAllWarnings = isCheckAllWarnings,
    ignoreWarnings = isIgnoreWarnings,
    warningsAsErrors = isWarningsAsErrors,
    showAll = isShowAll,
    explainIssues = isExplainIssues,
    checkReleaseBuilds = isCheckReleaseBuilds,
    checkTestSources = isCheckTestSources,
    ignoreTestSources = isIgnoreTestSources,
    ignoreTestFixturesSources = false,
    checkGeneratedSources = isCheckGeneratedSources,
    checkDependencies = isCheckDependencies,
    baseline = baselineFile,
)

internal fun CompileOptions.convert(): JavaCompileOptions {
    return JavaCompileOptionsImpl(
        encoding = encoding,
        sourceCompatibility = sourceCompatibility.toString(),
        targetCompatibility = targetCompatibility.toString(),
        isCoreLibraryDesugaringEnabled = isCoreLibraryDesugaringEnabled
    )
}

internal fun CodeShrinkerV1.convert(): CodeShrinker = when (this) {
    CodeShrinkerV1.PROGUARD -> CodeShrinker.PROGUARD
    CodeShrinkerV1.R8 -> CodeShrinker.R8
}

internal fun TestOptions.Execution.convert(): TestInfo.Execution = when (this) {
    TestOptions.Execution.HOST -> TestInfo.Execution.HOST
    TestOptions.Execution.ANDROID_TEST_ORCHESTRATOR -> TestInfo.Execution.ANDROID_TEST_ORCHESTRATOR
    TestOptions.Execution.ANDROIDX_TEST_ORCHESTRATOR -> TestInfo.Execution.ANDROIDX_TEST_ORCHESTRATOR
}

internal fun String.convertToExecution(): TestInfo.Execution? = when (this) {
    // The string coming from the old DSL (which is the enum converted) can be lowercase, so we
    // need to handle both
    "host","HOST" -> TestInfo.Execution.HOST
    "android_test_orchestrator","ANDROID_TEST_ORCHESTRATOR" -> TestInfo.Execution.ANDROID_TEST_ORCHESTRATOR
    "androidx_test_orchestrator","ANDROIDX_TEST_ORCHESTRATOR" -> TestInfo.Execution.ANDROIDX_TEST_ORCHESTRATOR
    else -> null
}
