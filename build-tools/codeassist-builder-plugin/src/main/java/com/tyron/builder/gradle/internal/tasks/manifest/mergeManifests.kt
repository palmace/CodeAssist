package com.tyron.builder.gradle.internal.tasks.manifest

import com.android.SdkConstants.DOT_XML
import com.tyron.builder.compiler.manifest.blame.SourceFile
import com.tyron.builder.compiler.manifest.blame.SourceFilePosition
import com.tyron.builder.compiler.manifest.blame.SourcePosition
import com.tyron.builder.compiler.manifest.ManifestMerger2
import com.tyron.builder.compiler.manifest.ManifestProvider
import com.tyron.builder.compiler.manifest.MergingReport
import com.tyron.builder.compiler.manifest.XmlDocument
import com.tyron.builder.log.ILogger
import com.google.common.io.Files
import java.io.File
import java.io.IOException

fun findOriginalManifestFilePosition(
    manifestMergeBlameContents: List<String>,
    mergedFilePosition: SourceFilePosition
): SourceFilePosition {
    if (mergedFilePosition.file == SourceFile.UNKNOWN || mergedFilePosition.file.sourceFile?.absolutePath?.contains(
            "merged_manifests"
        ) == false
    ) {
        return mergedFilePosition
    }
    try {
        val linePrefix = (mergedFilePosition.position.startLine + 1).toString() + "-->"
        manifestMergeBlameContents.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith(linePrefix)) {
                var position = trimmed.substring(linePrefix.length)
                if (position.startsWith("[")) {
                    val closingIndex = position.indexOf("] ")
                    if (closingIndex >= 0) {
                        position = position.substring(closingIndex + 2)
                    }
                }
                val index = position.indexOf(DOT_XML)
                if (index != -1) {
                    val file = position.substring(0, index + DOT_XML.length)
                    return if (file != position) {
                        val sourcePosition = position.substring(index + DOT_XML.length + 1)
                        SourceFilePosition(File(file), SourcePosition.fromString(sourcePosition))
                    } else {
                        SourceFilePosition(File(file), SourcePosition.UNKNOWN)
                    }
                }
            }
        }
    } catch (e: Exception) {
        return mergedFilePosition
    }
    return mergedFilePosition
}

fun mergeManifests(
    mainManifest: File,
    manifestOverlays: List<File>,
    dependencies: List<ManifestProvider>,
    navigationJsons: Collection<File>,
    featureName: String?,
    packageOverride: String?,
    namespace: String,
    profileable: Boolean,
    versionCode: Int?,
    versionName: String?,
    minSdkVersion: String?,
    targetSdkVersion: String?,
    maxSdkVersion: Int?,
    testOnly: Boolean,
    outMergedManifestLocation: String?,
    outAaptSafeManifestLocation: String?,
    mergeType: ManifestMerger2.MergeType,
    placeHolders: Map<String, String>,
    optionalFeatures: Collection<ManifestMerger2.Invoker.Feature>,
    dependencyFeatureNames: Collection<String>,
    reportFile: File?,
    logger: ILogger
): MergingReport {

    try {
        val manifestMergerInvoker = ManifestMerger2.newMerger(mainManifest, logger, mergeType)
            .setPlaceHolderValues(placeHolders)
            .addFlavorAndBuildTypeManifests(*manifestOverlays.toTypedArray())
            .addManifestProviders(dependencies)
            .addNavigationJsons(navigationJsons)
            .withFeatures(*optionalFeatures.toTypedArray())
            
        if (reportFile != null) {
            manifestMergerInvoker.setMergeReportFile(reportFile)
        }
            
        val isAppMerge = mergeType == ManifestMerger2.MergeType.APPLICATION

        if (isAppMerge) {
            manifestMergerInvoker.withFeatures(ManifestMerger2.Invoker.Feature.REMOVE_TOOLS_DECLARATIONS)
        }

        if (outAaptSafeManifestLocation != null) {
            manifestMergerInvoker.withFeatures(ManifestMerger2.Invoker.Feature.MAKE_AAPT_SAFE)
        }

        setInjectableValues(
            manifestMergerInvoker,
            packageOverride, versionCode, versionName,
            minSdkVersion, targetSdkVersion, maxSdkVersion,
            testOnly
        )

        val mergingReport = manifestMergerInvoker.merge()
        if (mergingReport.result == MergingReport.Result.ERROR) {
            mergingReport.log(logger)
            throw RuntimeException(mergingReport.reportString)
        }
        if (mergingReport.result == MergingReport.Result.WARNING) {
            mergingReport.log(logger)
        }

        if (outMergedManifestLocation != null) {
            val documentOptional = mergingReport.mergedDocument
            if (documentOptional.isPresent) {
                val xmlDoc: XmlDocument = documentOptional.get()
                save(xmlDoc.prettyPrint(), File(outMergedManifestLocation))
            }
        }

        if (outAaptSafeManifestLocation != null) {
             val documentOptional = mergingReport.mergedDocument
             if (documentOptional.isPresent) {
                 val xmlDoc: XmlDocument = documentOptional.get()
                 save(xmlDoc.prettyPrint(), File(outAaptSafeManifestLocation))
             }
        }
        return mergingReport
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

private fun setInjectableValues(
    invoker: ManifestMerger2.Invoker<*>,
    packageOverride: String?,
    versionCode: Int?,
    versionName: String?,
    minSdkVersion: String?,
    targetSdkVersion: String?,
    maxSdkVersion: Int?,
    testOnly: Boolean
) {
    if (packageOverride != null && packageOverride.isNotEmpty()) {
        invoker.setOverride(ManifestMerger2.SystemProperty.PACKAGE, packageOverride)
    }
    versionCode?.let {
        if (it > 0) {
            invoker.setOverride(ManifestMerger2.SystemProperty.VERSION_CODE, it.toString())
        }
    }
    if (versionName != null && versionName.isNotEmpty()) {
        invoker.setOverride(ManifestMerger2.SystemProperty.VERSION_NAME, versionName)
    }
    if (minSdkVersion != null && minSdkVersion.isNotEmpty()) {
        invoker.setOverride(ManifestMerger2.SystemProperty.MIN_SDK_VERSION, minSdkVersion)
    }
    if (targetSdkVersion != null && targetSdkVersion.isNotEmpty()) {
        invoker.setOverride(ManifestMerger2.SystemProperty.TARGET_SDK_VERSION, targetSdkVersion)
    }
    if (maxSdkVersion != null) {
        invoker.setOverride(ManifestMerger2.SystemProperty.MAX_SDK_VERSION, maxSdkVersion.toString())
    }
    if (testOnly) {
        invoker.withFeatures(ManifestMerger2.Invoker.Feature.TEST_ONLY)
    }
}

private fun save(xmlDocument: String?, out: File) {
    if (xmlDocument == null) return
    try {
        Files.createParentDirs(out)
        Files.asCharSink(out, Charsets.UTF_8).write(xmlDocument)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
}
