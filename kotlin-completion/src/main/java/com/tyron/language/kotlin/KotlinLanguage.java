package com.tyron.language.kotlin;

import com.tyron.language.api.Language;
import org.jetbrains.annotations.NotNull;

public class KotlinLanguage extends Language {

    public static final KotlinLanguage INSTANCE = new KotlinLanguage("KOTLIN");

    protected KotlinLanguage(@NotNull String id) {
        super(id);
    }
}
