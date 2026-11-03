package com.tyron.builder.gradle.internal.ide;

import com.tyron.builder.gradle.internal.component.ComponentCreationConfig;

import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Simplified ModelBuilder to satisfy Gradle's Tooling API requirements.
 * Real logic is handled by the V2 ModelBuilder in Kotlin.
 */
public class ModelBuilder implements ToolingModelBuilder {

    public ModelBuilder() {
    }

    public ModelBuilder(Project project, Object variantModel, Object extension, Object extraModelInfo) {
    }

    public static Collection<File> getGeneratedSourceFolders(ComponentCreationConfig component) {
        return Collections.emptyList();
    }

    public static Collection<File> getGeneratedResourceFolders(ComponentCreationConfig component) {
        return Collections.emptyList();
    }

    public static Collection<File> getGeneratedSourceFoldersForUnitTests(ComponentCreationConfig component) {
        return Collections.emptyList();
    }

    @Override
    public boolean canBuild(String modelName) {
        return false;
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        return null;
    }
}
