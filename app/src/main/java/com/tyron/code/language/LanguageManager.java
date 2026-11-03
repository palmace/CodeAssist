package com.tyron.code.language;

import android.content.res.AssetManager;
import android.util.Log;

import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.groovy.Groovy;
import com.tyron.code.language.java.Java;
import com.tyron.code.language.json.Json;
import com.tyron.code.language.kotlin.Kotlin;
import com.tyron.code.language.xml.Xml;
import com.tyron.editor.Editor;

import org.apache.commons.vfs2.FileObject;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

public class LanguageManager {

    private static final String TAG = "LanguageManager";
    private static LanguageManager Instance = null;

    public static LanguageManager getInstance() {
        if (Instance == null) {
            Instance = new LanguageManager();
        }
        return Instance;
    }

    private final Set<Language> mLanguages = new HashSet<>();

    private LanguageManager() {
        initLanguages();
    }

    private void initLanguages() {
        mLanguages.addAll(
                Arrays.asList(
                        new Xml(),
                        new Java(),
                        new Kotlin(),
                        new Groovy(),
                        new Json()));
    }

    public boolean supports(File file) {
        for (Language language : mLanguages) {
            if (language.isApplicable(file)) {
                return true;
            }
        }
        return false;
    }

    public io.github.rosemoe.sora.lang.Language get(Editor editor, FileObject file) {
        for (Language lang : mLanguages) {
            if (lang.isApplicable(file)) {
                return lang.get(editor);
            }
        }
        return null;
    }

    public io.github.rosemoe.sora.lang.Language get(Editor editor, File file) {
        for (Language lang : mLanguages) {
            if (lang.isApplicable(file)) {
                return lang.get(editor);
            }
        }
        return null;
    }

    public static TextMateLanguage createTextMateLanguage(String grammarName, String grammarPath, String configurationPath, Editor editor) {
        AssetManager assets = ApplicationLoader.getInstance().getAssets();
        try {
            IGrammarSource grammarSource = IGrammarSource.fromInputStream(assets.open(grammarPath), grammarPath, null);
            
            // Forzamos el scopeName correcto según el archivo JSON
            String scopeName = grammarName;
            
            Log.d(TAG, "Loading grammar: " + grammarPath + " with scope: " + scopeName);
            
            try {
                DefaultGrammarDefinition definition = DefaultGrammarDefinition.withGrammarSource(grammarSource, grammarName, scopeName);
                GrammarRegistry.getInstance().loadGrammar(definition);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Grammar already registered or scope mismatch: " + scopeName, e);
            }
            
            if (configurationPath != null) {
                try {
                    LanguageConfiguration config = LanguageConfiguration.load(new InputStreamReader(assets.open(configurationPath)));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load language configuration", e);
                }
            }
            
            return TextMateLanguage.create(scopeName, true);
        } catch (Exception e) {
            Log.e(TAG, "Error in createTextMateLanguage", e);
            throw new RuntimeException(e);
        }
    }
}
