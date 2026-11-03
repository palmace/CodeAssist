package com.tyron.code.analyzer;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.tyron.editor.Editor;

import java.lang.reflect.Field;

import io.github.rosemoe.sora.langs.textmate.TextMateAnalyzer;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.text.ContentReference;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

/**
 * A text mate analyzer which does not use a TextMateLanguage
 */
public class BaseTextmateAnalyzer extends TextMateAnalyzer {

    private static final Field THEME_FIELD;

    public BaseTextmateAnalyzer(TextMateLanguage language, IGrammar grammar, LanguageConfiguration languageConfiguration, ThemeRegistry themeRegistry) {
        super(language, grammar, languageConfiguration, themeRegistry);
    }

    @Override
    public void reset(@NonNull ContentReference content, @NonNull Bundle extraArguments) {
        if (!extraArguments.getBoolean("loaded", false)) {
            return;
        }
        super.reset(content, extraArguments);
    }

    public void updateTheme(ThemeModel themeModel) {
        onChangeTheme(themeModel);
    }

    public Theme getTheme() {
        try {
            return (Theme) THEME_FIELD.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            THEME_FIELD = TextMateAnalyzer.class.getDeclaredField("theme");
            THEME_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
