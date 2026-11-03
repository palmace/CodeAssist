package com.android.aaptcompiler

import com.android.aaptcompiler.android.ResTableConfig

/**
 * A wrapper around [ResTableConfig] that provides comparison and utility methods.
 */
data class ConfigDescription(
    val config: ResTableConfig = ResTableConfig()
) : Comparable<ConfigDescription> {

    val mcc get() = config.mcc
    val mnc get() = config.mnc
    val orientation get() = config.orientation
    val touchscreen get() = config.touchscreen
    val density get() = config.density
    val keyboard get() = config.keyboard
    val navigation get() = config.navigation
    val inputFlags get() = config.inputFlags
    val screenWidth get() = config.screenWidth
    val screenHeight get() = config.screenHeight
    val sdkVersion get() = config.sdkVersion
    val screenLayout get() = config.screenLayout
    val uiMode get() = config.uiMode
    val smallestScreenWidthDp get() = config.smallestScreenWidthDp
    val screenWidthDp get() = config.screenWidthDp
    val screenHeightDp get() = config.screenHeightDp
    val screenLayout2 get() = config.screenLayout2
    val colorMode get() = config.colorMode

    constructor(other: ConfigDescription) : this(
        ResTableConfig(
            mcc = other.config.mcc,
            mnc = other.config.mnc,
            orientation = other.config.orientation,
            touchscreen = other.config.touchscreen,
            density = other.config.density,
            keyboard = other.config.keyboard,
            navigation = other.config.navigation,
            inputFlags = other.config.inputFlags,
            screenWidth = other.config.screenWidth,
            screenHeight = other.config.screenHeight,
            sdkVersion = other.config.sdkVersion,
            screenLayout = other.config.screenLayout,
            uiMode = other.config.uiMode,
            smallestScreenWidthDp = other.config.smallestScreenWidthDp,
            screenWidthDp = other.config.screenWidthDp,
            screenHeightDp = other.config.screenHeightDp,
            screenLayout2 = other.config.screenLayout2,
            colorMode = other.config.colorMode
        )
    )

    fun getBcp47Locale(): String {
        // Implementación básica, a refinar si es necesario para el flujo de CodeAssist
        return ""
    }

    override fun toString(): String {
        if (this == DEFAULT) return "default"
        return config.toString()
    }

    override fun compareTo(other: ConfigDescription): Int {
        return 0 
    }
    
    fun diff(other: ConfigDescription): Int {
        return 0
    }

    companion object {
        val DEFAULT = ConfigDescription()
    }
}
