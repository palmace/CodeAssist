package com.tyron.builder.gradle.internal.component

import com.tyron.builder.api.variant.Variant
import com.tyron.builder.api.variant.UnitTest
import com.tyron.builder.core.ComponentType
import org.gradle.api.provider.MapProperty
import java.io.File

interface VariantCreationConfig: ConsumableCreationConfig, Variant {
    val consumerProguardFiles: List<File>

    override val maxSdkVersion: Int?

    override val experimentalProperties: MapProperty<String, Any>

    val externalNativeExperimentalProperties: Map<String, Any>

//    val ndkConfig: MergedNdkConfig

    val isJniDebuggable: Boolean

    val testComponents: MutableMap<ComponentType, TestComponentCreationConfig>

    override val nestedComponents: List<ComponentCreationConfig>

    override val unitTest: UnitTestCreationConfig?

    var testFixturesComponent: TestFixturesCreationConfig?
}
