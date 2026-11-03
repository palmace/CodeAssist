package com.tyron.builder.gradle.internal.coverage

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

object JacocoConfigurations {
    const val ANT_CONFIGURATION_NAME = "jacocoAnt"
    const val AGENT_CONFIGURATION_NAME = "jacocoAgent"

    fun getAgentRuntimeDependency(version: String): String {
        return "org.jacoco:org.jacoco.agent:$version:runtime"
    }

    fun getJacocoAntTaskConfiguration(project: Project, version: String): Configuration {
        return project.configurations.maybeCreate(ANT_CONFIGURATION_NAME).apply {
            if (dependencies.isEmpty()) {
                project.dependencies.add(name, "org.jacoco:org.jacoco.ant:$version")
            }
        }
    }
}
