package com.android.aaptcompiler

/**
 * Accessor for internal members of ResourceTable.
 */
object ResourceTableAcessor {
    fun getPackages(table: ResourceTable): List<ResourceTablePackage> {
        return table.packages
    }
}
