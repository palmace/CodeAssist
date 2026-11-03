package com.tyron.builder.gradle.internal.services

import org.gradle.api.DomainObjectSet
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File

class DslServicesImpl(
    private val projectServices: ProjectServices,
    override val sdkComponents: Provider<SdkComponentsBuildService>,
    private val versionedSdkLoaderServiceFactory: () -> VersionedSdkLoaderService
) : DslServices, BaseServices {

    // BaseServices delegation
    override val issueReporter get() = projectServices.issueReporter
    override val deprecationReporter get() = projectServices.deprecationReporter
    override val projectOptions get() = projectServices.projectOptions
    override val buildServiceRegistry get() = projectServices.buildServiceRegistry
    override val gradleEnvironmentProvider get() = throw UnsupportedOperationException("Not implemented")
    override val projectInfo get() = projectServices.projectInfo
    override fun <T> newInstance(type: Class<T>, vararg args: Any?): T = projectServices.objectFactory.newInstance(type, *args)
    override fun file(file: Any): File = projectServices.fileResolver(file)

    override val logger: Logger get() = projectServices.logger
    override val buildDirectory: DirectoryProperty get() = projectServices.projectLayout.buildDirectory

    override val versionedSdkLoaderService: VersionedSdkLoaderService by lazy {
        versionedSdkLoaderServiceFactory()
    }

    override fun <T> domainObjectSet(type: Class<T>): DomainObjectSet<T> =
        projectServices.objectFactory.domainObjectSet(type)

    override fun <T> domainObjectContainer(
        type: Class<T>,
        factory: NamedDomainObjectFactory<T>
    ): NamedDomainObjectContainer<T> =
        projectServices.objectFactory.domainObjectContainer(type, factory)

    override fun <T> domainObjectContainer(type: Class<T>): NamedDomainObjectContainer<T> =
        projectServices.objectFactory.domainObjectContainer(type)

    override fun <T> polymorphicDomainObjectContainer(type: Class<T>): ExtensiblePolymorphicDomainObjectContainer<T> =
        projectServices.objectFactory.polymorphicDomainObjectContainer(type)

    override fun <T> property(type: Class<T>): Property<T> =
        projectServices.objectFactory.property(type)

    override fun directoryProperty(): DirectoryProperty =
        projectServices.objectFactory.directoryProperty()

    override fun <T> provider(type: Class<T>, value: T?): Provider<T> =
        projectServices.providerFactory.provider { value }

    override fun <T : Any> newDecoratedInstance(dslClass: Class<T>, vararg args: Any): T =
        projectServices.objectFactory.newInstance(dslClass, *args)
}
