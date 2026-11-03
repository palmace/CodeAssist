package com.tyron.builder.gradle.internal.scope

import com.android.sdklib.AndroidVersion
import com.tyron.builder.gradle.internal.OptionalLibrary
import com.google.common.base.Verify
import com.google.common.collect.ImmutableList
import com.tyron.builder.core.LibraryRequest
import com.tyron.builder.errors.IssueReporter
import com.tyron.builder.gradle.internal.services.TaskCreationServices
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Utility methods for computing class paths to use for compilation.  */
object BootClasspathBuilder {

    private data class CacheKey(val target: AndroidVersion, val addAllOptionalLibraries: Boolean, val libraryRequests: List<LibraryRequest>)

    private val classpathCache = ConcurrentHashMap<CacheKey, List<RegularFile>>()

    fun computeClasspath(
        services: TaskCreationServices,
        objects: ObjectFactory,
        targetBootClasspath: Provider<List<File>>,
        targetAndroidVersion: Provider<AndroidVersion>,
        additionalLibraries: Provider<List<OptionalLibrary>>,
        optionalLibraries: Provider<List<OptionalLibrary>>,
        annotationsJar: Provider<File>,
        addAllOptionalLibraries: Boolean,
        libraryRequests: List<LibraryRequest>
    ): Provider<List<RegularFile>> {

        return targetBootClasspath.flatMap { bootClasspath ->
            val target = targetAndroidVersion.get()
            val key = CacheKey(target, addAllOptionalLibraries, libraryRequests)

            services.provider {
                classpathCache.getOrPut(key) {
                    val resultList = mutableListOf<RegularFile>()
                    bootClasspath.forEach {
                        resultList.add(objects.fileProperty().fileValue(it).get())
                    }

                    resultList.addAll(
                        computeAdditionalAndRequestedOptionalLibraries(
                            services,
                            additionalLibraries.get(),
                            optionalLibraries.get(),
                            addAllOptionalLibraries,
                            libraryRequests,
                            services.issueReporter
                        )
                    )

                    if (target.apiLevel <= 15) {
                        annotationsJar.orNull?.let {
                            resultList.add(objects.fileProperty().fileValue(it).get())
                        }
                    }

                    resultList
                }
            }
        }
    }

    fun computeAdditionalAndRequestedOptionalLibraries(
        services: TaskCreationServices,
        additionalLibraries: List<OptionalLibrary>,
        optionalLibraries: List<OptionalLibrary>,
        addAllOptionalLibraries: Boolean,
        libraryRequestsArg: List<LibraryRequest>,
        issueReporter: IssueReporter
    ): List<RegularFile> {

        val libraryRequests = libraryRequestsArg.map { it.name }.toMutableSet()
        val files = ImmutableList.builder<RegularFile>()
        
        additionalLibraries.forEach { lib ->
            val jar = lib.jar
            if (libraryRequests.contains(lib.name)) {
                libraryRequests.remove(lib.name)
            }
            services.fileProvider(services.provider { jar }).orNull?.let {
                files.add(it)
            }
        }

        optionalLibraries.forEach { lib ->
            val libraryRequested = libraryRequests.contains(lib.name)
            if (addAllOptionalLibraries || libraryRequested) {
                val jar = lib.jar
                if (libraryRequested) {
                    libraryRequests.remove(lib.name)
                }
                services.fileProvider(services.provider { jar }).orNull?.let {
                    files.add(it)
                }
            }
        }

        for (library in libraryRequests) {
            issueReporter.reportError(
                IssueReporter.Type.OPTIONAL_LIB_NOT_FOUND,
                "Unable to find optional library: $library",
                library)
        }
        return files.build()
    }
}
