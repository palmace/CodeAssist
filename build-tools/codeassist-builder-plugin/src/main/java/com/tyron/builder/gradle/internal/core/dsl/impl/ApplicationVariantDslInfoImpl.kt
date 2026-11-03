package com.tyron.builder.gradle.internal.core.dsl.impl

import com.tyron.builder.api.dsl.ApplicationBuildType
import com.tyron.builder.api.dsl.ApplicationProductFlavor
import com.tyron.builder.api.dsl.BuildType
import com.tyron.builder.api.dsl.ProductFlavor
import com.tyron.builder.api.variant.ComponentIdentity
import com.tyron.builder.core.ComponentType
import com.tyron.builder.errors.IssueReporter
import com.tyron.builder.gradle.internal.core.dsl.ApplicationVariantDslInfo
import com.tyron.builder.gradle.internal.dsl.DefaultConfig
import com.tyron.builder.gradle.internal.dsl.InternalApplicationExtension
import com.tyron.builder.gradle.internal.dsl.SigningConfig
import com.tyron.builder.gradle.internal.manifest.ManifestDataProvider
import com.tyron.builder.gradle.internal.profile.ProfilingMode
import com.tyron.builder.gradle.internal.publishing.VariantPublishingInfo
import com.tyron.builder.gradle.internal.services.VariantServices
import com.tyron.builder.gradle.options.StringOption
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

internal class ApplicationVariantDslInfoImpl(
    componentIdentity: ComponentIdentity,
    componentType: ComponentType,
    defaultConfig: DefaultConfig,
    buildTypeObj: BuildType,
    productFlavorList: List<ProductFlavor>,
    dataProvider: ManifestDataProvider,
    services: VariantServices,
    buildDirectory: DirectoryProperty,
    override val publishInfo: VariantPublishingInfo,
    private val signingConfigOverride: SigningConfig?,
    extension: InternalApplicationExtension
) : TestedVariantDslInfoImpl(
    componentIdentity,
    componentType,
    defaultConfig,
    buildTypeObj,
    productFlavorList,
    dataProvider,
    services,
    buildDirectory,
    extension
), ApplicationVariantDslInfo {

    private val applicationBuildType = buildTypeObj as ApplicationBuildType

    override val isDebuggable: Boolean
        get() = ProfilingMode.getProfilingModeType(
            services.projectOptions[StringOption.PROFILING_MODE]
        ).isDebuggable ?: applicationBuildType.isDebuggable

    override val isProfileable: Boolean
        get() {
            val fromProfilingModeOption = ProfilingMode.getProfilingModeType(
                services.projectOptions[StringOption.PROFILING_MODE]
            ).isProfileable
            val minProfileableSdk = 30
            val compileSdk = extension.compileSdk ?: minProfileableSdk
            if ((fromProfilingModeOption == true || applicationBuildType.isProfileable) &&
                compileSdk < minProfileableSdk
            ) {
                services.issueReporter.reportError(
                    IssueReporter.Type.COMPILE_SDK_VERSION_TOO_LOW,
                    """'profileable' is enabled with compile SDK <30.
                            Recommended action: If possible, upgrade compileSdk from ${minSdkVersion.apiLevel} to 30."""
                        .trimIndent()
                )
            }
            return when {
                fromProfilingModeOption != null -> fromProfilingModeOption
                applicationBuildType.isProfileable && isDebuggable -> false
                else -> applicationBuildType.isProfileable
            }
        }

    override val signingConfig: SigningConfig? by lazy {
        getSigningConfig(
            buildTypeObj,
            mergedFlavor,
            signingConfigOverride,
            extension,
            services
        )
    }

    override val isSigningReady: Boolean
        get() = signingConfig?.isSigningReady == true

    override val versionName: Provider<String?> by lazy {
        val versionNameFromFlavors =
            productFlavorList
                .asSequence()
                .filterIsInstance(ApplicationProductFlavor::class.java)
                .map { it.versionName }
                .firstOrNull { it != null }
                ?: defaultConfig.versionName

        if (versionNameFromFlavors == null) {
            dataProvider.manifestData.map {
                it.versionName?.let { versionName ->
                    "$versionName${computeVersionNameSuffix()}"
                } ?: "" // Corregido: Proveer String no nulo para calmar al compilador
            }
        } else {
            services.provider { "$versionNameFromFlavors${computeVersionNameSuffix()}" }
        }
    }

    override val versionCode: Provider<Int?> by lazy {
        val versionCodeFromFlavors =
            productFlavorList
                .asSequence()
                .filterIsInstance(ApplicationProductFlavor::class.java)
                .map { it.versionCode }
                .firstOrNull { it != null }
                ?: defaultConfig.versionCode

        if (versionCodeFromFlavors == null) {
            dataProvider.manifestData.map { it.versionCode ?: 0 } // Corregido: Proveer Int no nulo
        } else {
            services.provider { versionCodeFromFlavors }
        }
    }

    override val isWearAppUnbundled: Boolean?
        get() = mergedFlavor.wearAppUnbundled
    override val isEmbedMicroApp: Boolean
        get() = applicationBuildType.isEmbedMicroApp

    private fun computeVersionNameSuffix(): String {
        val suffixes = mutableListOf<String>()
        defaultConfig.versionNameSuffix?.let {
            suffixes.add(it)
        }
        suffixes.addAll(
            productFlavorList
                .asSequence()
                .filterIsInstance(ApplicationProductFlavor::class.java)
                .mapNotNull { it.versionNameSuffix })
        applicationBuildType.versionNameSuffix?.let {
            suffixes.add(it)
        }
        return suffixes.joinToString(separator = "")
    }
}
