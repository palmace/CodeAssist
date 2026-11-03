package com.tyron.builder.gradle.internal.api

import com.tyron.builder.api.variant.impl.ProviderBasedDirectoryEntryImpl
import com.tyron.builder.api.variant.impl.SourceDirectoriesImpl
import com.tyron.builder.gradle.api.AndroidSourceDirectorySet
import com.tyron.builder.gradle.internal.api.artifact.SourceArtifactType
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import java.util.ArrayList
import java.util.concurrent.Callable

class DefaultAndroidSourceDirectorySet(
    private val sourceSetName: String,
    private val name: String,
    private val project: Project,
    private val type: SourceArtifactType
) : AndroidSourceDirectorySet {
    private val source = Lists.newArrayList<Any>()
    override val filter = PatternSet()
    private val lateAdditionsDelegates = mutableListOf<SourceDirectoriesImpl>()

    override fun getName(): String = "$sourceSetName $name"

    fun getSourceSetName() = name

    override fun srcDir(srcDir: Any): AndroidSourceDirectorySet {
        if (srcDir is Iterable<*>) {
            srcDir.forEach { src -> src?.let { srcDir(it) } }
            return this
        }
        source.add(srcDir)
        if (lateAdditionsDelegates.isNotEmpty()) {
            val directoryEntry = ProviderBasedDirectoryEntryImpl(
                name, project.files(srcDir).elements, filter
            )
            lateAdditionsDelegates.forEach { it.addSource(directoryEntry) }
        }
        return this
    }

    override fun srcDirs(vararg srcDirs: Any): AndroidSourceDirectorySet {
        for (dir in srcDirs) { srcDir(dir) }
        return this
    }

    override fun setSrcDirs(srcDirs: Iterable<*>): AndroidSourceDirectorySet {
        if (lateAdditionsDelegates.isNotEmpty()) {
            val previousFiles = this.srcDirs
            val newFiles = project.files(srcDirs).files
            for (newFile in (newFiles - previousFiles)) {
                val directoryEntry = ProviderBasedDirectoryEntryImpl(
                    name, project.files(newFile).elements, filter
                )
                lateAdditionsDelegates.forEach { it.addSource(directoryEntry) }
            }
        }
        source.clear()
        for (dir in srcDirs) { source.add(dir) }
        return this
    }

    override fun getSourceFiles(): FileTree {
        var src: FileTree? = null
        val sources = srcDirs
        if (sources.isNotEmpty()) {
            src = project.files(ArrayList<Any>(sources)).asFileTree.matching(filter)
        }
        return src ?: project.files().asFileTree
    }

    override fun getSourceDirectoryTrees(): List<ConfigurableFileTree> {
        return source.stream().map { sourceDir ->
            project.fileTree(sourceDir) {
                it.include(filter.asIncludeSpec)
                it.exclude(filter.asExcludeSpec)
            }
        }.collect(ImmutableList.toImmutableList())
    }

    override val srcDirs: Set<File>
        get() = ImmutableSet.copyOf(project.files(*source.toTypedArray()).files)

    override fun getBuildableArtifact(): FileCollection {
        return project.files(Callable<Collection<File>> { srcDirs })
    }

    override fun getIncludes(): Set<String> = filter.includes
    override fun getExcludes(): Set<String> = filter.excludes
    override fun setIncludes(includes: Iterable<String>): PatternFilterable { filter.setIncludes(includes); return this }
    override fun setExcludes(excludes: Iterable<String>): PatternFilterable { filter.setExcludes(excludes); return this }
    override fun include(vararg includes: String): PatternFilterable { filter.include(*includes); return this }
    override fun include(includes: Iterable<String>): PatternFilterable { filter.include(includes); return this }
    override fun include(includeSpec: Spec<FileTreeElement>): PatternFilterable { filter.include(includeSpec); return this }
    override fun include(includeSpec: Closure<*>): PatternFilterable { filter.include(includeSpec); return this }
    override fun exclude(excludes: Iterable<String>): PatternFilterable { filter.exclude(excludes); return this }
    override fun exclude(vararg excludes: String): PatternFilterable { filter.exclude(*excludes); return this }
    override fun exclude(excludeSpec: Spec<FileTreeElement>): PatternFilterable { filter.exclude(excludeSpec); return this }
    override fun exclude(excludeSpec: Closure<*>): PatternFilterable { filter.exclude(excludeSpec); return this }

    internal fun addLateAdditionDelegate(lateAdditionDelegate: SourceDirectoriesImpl) {
        lateAdditionsDelegates += lateAdditionDelegate
    }
}
