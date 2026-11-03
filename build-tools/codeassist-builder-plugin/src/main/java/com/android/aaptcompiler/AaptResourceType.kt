package com.android.aaptcompiler

enum class AaptResourceType(val tagName: String) {
    ANIM("anim"),
    ANIMATOR("animator"),
    ARRAY("array"),
    ATTR("attr"),
    BOOL("bool"),
    COLOR("color"),
    CONFIG_VARYING("configVarying"),
    DIMEN("dimen"),
    DRAWABLE("drawable"),
    FONT("font"),
    FRACTION("fraction"),
    ID("id"),
    INTEGER("integer"),
    INTERPOLATOR("interpolator"),
    LAYOUT("layout"),
    MACRO("macro"),
    MENU("menu"),
    MIPMAP("mipmap"),
    NAVIGATION("navigation"),
    PLURALS("plurals"),
    RAW("raw"),
    STRING("string"),
    STYLE("style"),
    STYLEABLE("styleable"),
    TRANSITION("transition"),
    XML("xml");

    override fun toString(): String = tagName

    companion object {
        fun fromString(type: String): AaptResourceType? {
            return values().find { it.tagName == type }
        }
    }
}

fun resourceTypeFromTag(type: String): AaptResourceType? {
    return AaptResourceType.fromString(type)
}
