package com.tyron.language.kotlin;

import android.graphics.drawable.Drawable;
import com.tyron.language.api.Language;
import com.tyron.language.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

public class KotlinFileType extends LanguageFileType {

    public static final KotlinFileType INSTANCE = new KotlinFileType(KotlinLanguage.INSTANCE);

    protected KotlinFileType(@NotNull Language instance) {
        super(instance);
    }

    @Override
    public @NotNull String getName() {
        return "Kotlin";
    }

    @Override
    public @NotNull String getDescription() {
        return "Kotlin programming language file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "kt";
    }

    @Override
    public @NotNull Drawable getIcon() {
        return null;
    }
}
