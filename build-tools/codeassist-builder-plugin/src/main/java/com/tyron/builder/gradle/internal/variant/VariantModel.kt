package com.tyron.builder.gradle.internal.variant

import com.tyron.builder.api.artifact.impl.ArtifactsImpl
import com.tyron.builder.api.dsl.BuildType
import com.tyron.builder.api.dsl.ProductFlavor
import com.tyron.builder.api.dsl.BaseFlavor
import com.tyron.builder.gradle.errors.SyncIssueReporter
import com.tyron.builder.gradle.internal.component.TestComponentCreationConfig
import com.tyron.builder.gradle.internal.component.VariantCreationConfig
import com.tyron.builder.gradle.internal.dsl.DefaultConfig
import com.tyron.builder.gradle.internal.scope.BuildFeatureValues
import com.tyron.builder.gradle.internal.services.SdkComponentsBuildService
import com.tyron.builder.gradle.options.ProjectOptions
import com.tyron.builder.model.v2.ide.ProjectType
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * Model representing the variant.
 */
interface VariantModel {

    val inputs: VariantInputModel<out com.tyron.builder.api.dsl.DefaultConfig, out com.tyron.builder.gradle.internal.dsl.BuildType, out com.tyron.builder.gradle.internal.dsl.ProductFlavor, out com.tyron.builder.gradle.internal.dsl.SigningConfig>

    val buildTypes: Map<String, BuildType>

    val productFlavors: Map<String, ProductFlavor>

    val defaultFlavor: BaseFlavor

    val projectTypeV1: Int

    val projectType: ProjectType

    val projectOptions: ProjectOptions

    val syncIssueReporter: SyncIssueReporter

    val variants: List<VariantCreationConfig>

    val testComponents: List<TestComponentCreationConfig>

    val defaultVariant: String?

    val buildFeatures: BuildFeatureValues

    val versionedSdkLoader: Provider<SdkComponentsBuildService.VersionedSdkLoader>

    val mockableJarArtifact: FileCollection

    val filteredBootClasspath: Provider<List<RegularFile>>

    val globalArtifacts: ArtifactsImpl
}
