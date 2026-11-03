package com.android.aaptcompiler.android

/**
 * Kotlin implementation of the native ResTable_config.
 * Holds configuration qualifiers for resources.
 */
class ResTableConfig(
    var mcc: Short = 0,
    var mnc: Short = 0,
    val language: ByteArray = ByteArray(2),
    val country: ByteArray = ByteArray(2),
    var orientation: Byte = 0,
    var touchscreen: Byte = 0,
    var density: Int = 0,
    var keyboard: Byte = 0,
    var navigation: Byte = 0,
    var inputFlags: Byte = 0,
    var screenWidth: Int = 0,
    var screenHeight: Int = 0,
    var sdkVersion: Short = 0,
    var screenLayout: Byte = 0,
    var uiMode: Byte = 0,
    var smallestScreenWidthDp: Int = 0,
    var screenWidthDp: Int = 0,
    var screenHeightDp: Int = 0,
    var screenLayout2: Byte = 0,
    var colorMode: Byte = 0
) {
    object SCREEN_LAYOUT {
        const val DIR_ANY: Byte = 0x00
        const val DIR_LTR: Byte = 0x01
        const val DIR_RTL: Byte = 0x02
        const val DIR_MASK: Byte = 0x03

        const val SIZE_ANY: Byte = 0x00
        const val SIZE_SMALL: Byte = 0x01
        const val SIZE_NORMAL: Byte = 0x02
        const val SIZE_LARGE: Byte = 0x03
        const val SIZE_XLARGE: Byte = 0x04
        const val SIZE_MASK: Byte = 0x0f

        const val SCREENLONG_ANY: Byte = 0x00
        const val SCREENLONG_NO: Byte = 0x10
        const val SCREENLONG_YES: Byte = 0x20
        const val SCREENLONG_MASK: Byte = 0x30
    }

    object SCREEN_LAYOUT2 {
        const val SCREENROUND_ANY: Byte = 0x00
        const val SCREENROUND_NO: Byte = 0x01
        const val SCREENROUND_YES: Byte = 0x02
        const val SCREENROUND_MASK: Byte = 0x03
    }

    object COLOR_MODE {
        const val WIDE_GAMUT_ANY: Byte = 0x00
        const val WIDE_GAMUT_NO: Byte = 0x01
        const val WIDE_GAMUT_YES: Byte = 0x02
        const val WIDE_GAMUT_MASK: Byte = 0x03

        const val HDR_ANY: Byte = 0x00
        const val HDR_NO: Byte = 0x04
        const val HDR_YES: Byte = 0x08
        const val HDR_MASK: Byte = 0x0c
    }

    object ORIENTATION {
        const val ANY: Byte = 0
        const val PORT: Byte = 1
        const val LAND: Byte = 2
        const val SQUARE: Byte = 3
    }

    object UI_MODE {
        const val TYPE_ANY: Byte = 0x00
        const val TYPE_NORMAL: Byte = 0x01
        const val TYPE_DESK: Byte = 0x02
        const val TYPE_CAR: Byte = 0x03
        const val TYPE_TELEVISION: Byte = 0x04
        const val TYPE_APPLIANCE: Byte = 0x05
        const val TYPE_WATCH: Byte = 0x06
        const val TYPE_VR_HEADSET: Byte = 0x07
        const val TYPE_MASK: Byte = 0x0f

        const val NIGHT_ANY: Byte = 0x00
        const val NIGHT_NO: Byte = 0x10
        const val NIGHT_YES: Byte = 0x20
        const val NIGHT_MASK: Byte = 0x30
    }

    object TOUCHSCREEN {
        const val ANY: Byte = 0
        const val NOTOUCH: Byte = 1
        const val STYLUS: Byte = 2
        const val FINGER: Byte = 3
    }

    object INPUT_FLAGS {
        const val KEYSHIDDEN_ANY: Byte = 0x00
        const val KEYSHIDDEN_NO: Byte = 0x01
        const val KEYSHIDDEN_YES: Byte = 0x02
        const val KEYSHIDDEN_SOFT: Byte = 0x03
        const val KEYSHIDDEN_MASK: Byte = 0x03

        const val NAVHIDDEN_ANY: Byte = 0x00
        const val NAVHIDDEN_NO: Byte = 0x04
        const val NAVHIDDEN_YES: Byte = 0x08
        const val NAVHIDDEN_MASK: Byte = 0x0c
    }

    object KEYBOARD {
        const val ANY: Byte = 0
        const val NOKEYS: Byte = 1
        const val QWERTY: Byte = 2
        const val TWELVEKEY: Byte = 3
    }

    object NAVIGATION {
        const val ANY: Byte = 0
        const val NONAV: Byte = 1
        const val DPAD: Byte = 2
        const val TRACKBALL: Byte = 3
        const val WHEEL: Byte = 4
    }

    companion object {
        const val CONFIG_MCC = 0x0001
        const val CONFIG_MNC = 0x0002
        const val CONFIG_LOCALE = 0x0004
        const val CONFIG_TOUCHSCREEN = 0x0008
        const val CONFIG_KEYBOARD = 0x0010
        const val CONFIG_KEYBOARD_HIDDEN = 0x0020
        const val CONFIG_NAVIGATION = 0x0040
        const val CONFIG_ORIENTATION = 0x0080
        const val CONFIG_SCREEN_LAYOUT = 0x0100
        const val CONFIG_UI_MODE = 0x0200
        const val CONFIG_SCREEN_SIZE = 0x0400
        const val CONFIG_VERSION = 0x0800
        const val CONFIG_SCREEN_LAYOUT2 = 0x1000
        const val CONFIG_COLOR_MODE = 0x2000
        const val CONFIG_SMALLEST_SCREEN_SIZE = 0x4000
    }
}
