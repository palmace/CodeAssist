package com.tyron.builder.gradle.internal.component

import com.tyron.builder.api.variant.UnitTest

interface UnitTestCreationConfig: TestComponentCreationConfig, UnitTest {

    val isUnitTestCoverageEnabled: Boolean
}
