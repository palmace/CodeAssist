package com.tyron.builder.gradle.tasks.sync

import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

/**
 * Task base para exportar el modelo de la variante a CodeAssist.
 * Suturado: Se ha desactivado la dependencia de filebasedproperties que estaba rota.
 */
@DisableCachingByDefault
abstract class ModuleVariantModelTask: AbstractVariantModelTask() {

    @get:Input
    abstract val manifestPlaceholders: MapProperty<String, String>

    override fun addVariantContent(variant: Any) {
        // No-op: filebasedproperties desactivado
    }
}
