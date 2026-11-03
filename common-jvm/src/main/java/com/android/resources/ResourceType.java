package com.android.resources;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Patched ResourceType to avoid NoSuchMethodError: Sets.toImmutableEnumSet()
 */
public enum ResourceType {
    ANIM("anim", "Animation"),
    ANIMATOR("animator", "Animator"),
    ARRAY("array", "Array", "string-array", "integer-array"),
    ATTR("attr", "Attr"),
    BOOL("bool", "Boolean"),
    COLOR("color", "Color"),
    DIMEN("dimen", "Dimension"),
    DRAWABLE("drawable", "Drawable"),
    FONT("font", "Font"),
    FRACTION("fraction", "Fraction"),
    ID("id", "ID"),
    INTEGER("integer", "Integer"),
    INTERPOLATOR("interpolator", "Interpolator"),
    LAYOUT("layout", "Layout"),
    MENU("menu", "Menu"),
    MIPMAP("mipmap", "Mip Map"),
    NAVIGATION("navigation", "Navigation"),
    PLURALS("plurals", "Plurals"),
    RAW("raw", "Raw"),
    STRING("string", "String"),
    STYLE("style", "Style"),
    STYLEABLE("styleable", "Styleable", Kind.STYLEABLE),
    TRANSITION("transition", "Transition"),
    XML("xml", "XML"),
    PUBLIC("public", "Public visibility modifier", Kind.SYNTHETIC),
    AAPT("_aapt", "Aapt Attribute", Kind.SYNTHETIC),
    OVERLAYABLE("overlayable", "Overlayable tag", Kind.SYNTHETIC),
    STYLE_ITEM("item", "Style Item", Kind.SYNTHETIC),
    SAMPLE_DATA("sample", "Sample data", Kind.SYNTHETIC),
    MACRO("macro", "Macro resource replacement", Kind.SYNTHETIC),
    ;

    private enum Kind {
        REAL,
        STYLEABLE,
        SYNTHETIC,
    }

    @NotNull private final String mName;
    @NotNull private final Kind mKind;
    @NotNull private final String mDisplayName;
    @NotNull private final String[] mAlternateXmlNames;

    ResourceType(
            @NotNull String name,
            @NotNull String displayName,
            @NotNull String... alternateXmlNames) {
        mName = name;
        mKind = Kind.REAL;
        mDisplayName = displayName;
        mAlternateXmlNames = alternateXmlNames;
    }

    ResourceType(@NotNull String name, @NotNull String displayName, @NotNull Kind kind) {
        mName = name;
        mKind = kind;
        mDisplayName = displayName;
        mAlternateXmlNames = new String[0];
    }

    public static final ImmutableSet<ResourceType> REFERENCEABLE_TYPES;
    private static final ImmutableMap<String, ResourceType> TAG_NAMES;
    private static final ImmutableMap<String, ResourceType> CLASS_NAMES;

    static {
        ImmutableMap.Builder<String, ResourceType> tagNames = ImmutableMap.builder();
        tagNames.put("declare-styleable", STYLEABLE);
        tagNames.put("public", PUBLIC);
        tagNames.put(OVERLAYABLE.getName(), OVERLAYABLE);
        tagNames.put(MACRO.getName(), MACRO);

        ImmutableMap.Builder<String, ResourceType> classNames = ImmutableMap.builder();
        classNames.put(STYLEABLE.mName, STYLEABLE);

        for (ResourceType type : ResourceType.values()) {
            if (type.mKind != Kind.REAL || type == STYLEABLE) {
                continue;
            }
            classNames.put(type.getName(), type);
            tagNames.put(type.getName(), type);
            for (String alternateName : type.mAlternateXmlNames) {
                tagNames.put(alternateName, type);
            }
        }

        TAG_NAMES = tagNames.build();
        CLASS_NAMES = classNames.build();

        // FIX: Manual implementation instead of Sets.toImmutableEnumSet()
        EnumSet<ResourceType> set = EnumSet.noneOf(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            if (type.getCanBeReferenced()) {
                set.add(type);
            }
        }
        REFERENCEABLE_TYPES = ImmutableSet.copyOf(set);
    }

    @NotNull
    public String getName() {
        return mName;
    }

    @NotNull
    public String getDisplayName() {
        return mDisplayName;
    }

    @Nullable
    public static ResourceType fromClassName(@NotNull String className) {
        return CLASS_NAMES.get(className);
    }

    @Nullable
    public static ResourceType fromFolderName(@NotNull String folderName) {
        return CLASS_NAMES.get(folderName);
    }

    @Nullable
    public static ResourceType fromXmlTagName(@NotNull String tagName) {
        return TAG_NAMES.get(tagName);
    }

    @Nullable
    public static ResourceType fromXmlValue(@NotNull String xmlValue) {
        if (xmlValue.equals("declare-styleable") || xmlValue.equals(STYLEABLE.mName)) {
            return null;
        }
        if (xmlValue.equals(SAMPLE_DATA.mName)) {
            return SAMPLE_DATA;
        }
        if (xmlValue.equals(AAPT.mName)) {
            return AAPT;
        }
        if (xmlValue.equals(OVERLAYABLE.mName)) {
            return OVERLAYABLE;
        }
        if (xmlValue.equals(MACRO.mName)) {
            return MACRO;
        }
        return CLASS_NAMES.get(xmlValue);
    }

    @Nullable
    public static <T> ResourceType fromXmlTag(
            @NotNull T tag,
            @NotNull Function<T, String> nameFunction,
            @NotNull BiFunction<? super T, ? super String, String> attributeFunction) {
        String tagName = nameFunction.apply(tag);
        switch (tagName) {
            case "eat-comment":
                return null;
            case "item":
                String typeAttribute = attributeFunction.apply(tag, "type");
                if (typeAttribute != null && !typeAttribute.isEmpty()) {
                    return fromClassName(typeAttribute);
                } else {
                    return null;
                }
            default:
                return fromXmlTagName(tagName);
        }
    }

    @Nullable
    public static ResourceType fromXmlTag(@NotNull Node domNode) {
        if (!(domNode instanceof Element)) {
            return null;
        }
        Element tag = (Element) domNode;
        return fromXmlTag(
                tag,
                element -> {
                    String name = element.getLocalName();
                    return name != null ? name : element.getTagName();
                },
                Element::getAttribute);
    }

    public static Collection<String> getClassNames() {
        return CLASS_NAMES.keySet();
    }

    public boolean getHasInnerClass() {
        return mKind != Kind.SYNTHETIC;
    }

    public boolean getCanBeReferenced() {
        return (mKind == Kind.REAL && this != ATTR) || this == MACRO;
    }

    public boolean isSynthetic() {
        return mKind == Kind.SYNTHETIC;
    }

    @Override
    @NotNull
    public String toString() {
        return getName();
    }
}
