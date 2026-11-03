package com.tyron.builder.gradle.tasks

import com.tyron.builder.compiler.manifest.MergingReport
import com.google.common.base.Charsets
import com.google.common.io.Files
import com.tyron.builder.gradle.internal.tasks.NonIncrementalTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.util.internal.GFileUtils
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException

/** A task that processes the manifest  */
@DisableCachingByDefault
abstract class ManifestProcessorTask : NonIncrementalTask() {

    @get:Optional
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Optional
    @get:OutputFile
    abstract val mergeBlameFile: RegularFileProperty

    companion object {
        @Throws(IOException::class)
        @JvmStatic
        protected fun outputMergeBlameContents(
            mergingReport: MergingReport, mergeBlameFile: File?
        ) {
            if (mergeBlameFile == null) {
                return
            }
            // Usamos el método getMergedDocument() del MergingReport personalizado
            val xmlDocument = mergingReport.getMergedDocument().orNull() ?: return
            val output = xmlDocument.prettyPrint()
            
            GFileUtils.mkdirs(mergeBlameFile.parentFile)
            Files.asCharSink(mergeBlameFile, Charsets.UTF_8).write(output)
        }
    }
}