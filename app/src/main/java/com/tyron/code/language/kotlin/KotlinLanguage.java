package com.tyron.code.language.kotlin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.code.language.CompletionItemWrapper;
import com.tyron.code.language.LanguageManager;
import com.tyron.completion.model.CompletionList;
import com.tyron.completion.util.CompletionUtils;
import com.tyron.editor.Editor;
import com.tyron.language.api.CodeAssistLanguage;

import java.io.File;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

/**
 * KotlinLanguage v1.2: "The Lifecycle Guard".
 * v1.2: Corregida fuga de memoria al liberar el analizador Swarm en destroy().
 */
public class KotlinLanguage implements Language, CodeAssistLanguage {

    private static final String GRAMMAR_NAME = "source.kotlin";
    private static final String LANGUAGE_PATH = "textmate/kotlin/syntaxes/kotlin.tmLanguage";
    private static final String CONFIG_PATH = "textmate/kotlin/language-configuration.json";

    private final TextMateLanguage delegate;
    private final Editor editor;
    private final KotlinCodeAnalyzer analyzer;

    public KotlinLanguage(Editor editor) {
        this.editor = editor;
        this.delegate = LanguageManager.createTextMateLanguage(GRAMMAR_NAME,
                LANGUAGE_PATH,
                CONFIG_PATH,
                editor);
        this.analyzer = new KotlinCodeAnalyzer(editor);
        
        // Iniciamos el warm-up del analizador
        analyzer.rerun();
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return delegate.getAnalyzeManager();
    }

    @Override
    public int getInterruptionLevel() {
        return delegate.getInterruptionLevel();
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content,
                                    @NonNull CharPosition position,
                                    @NonNull CompletionPublisher publisher,
                                    @NonNull Bundle extraArguments) throws CompletionCancelledException {
        String identifierPart = CompletionHelper.computePrefix(content, position, CompletionUtils.JAVA_PREDICATE::test);
        KotlinAutoCompleteProvider provider =
                new KotlinAutoCompleteProvider(editor);
        CompletionList completionList = provider.getCompletionList(identifierPart,
                position.getLine(),
                position.getColumn());
        if (completionList == null) {
            return;
        }
        completionList.getItems().stream().map(CompletionItemWrapper::new).forEach(publisher::addItem);
    }

    @Override
    public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
        return delegate.getIndentAdvance(content, line, column);
    }

    @Override
    public boolean useTab() {
        return delegate.useTab();
    }

    @NonNull
    @Override
    public Formatter getFormatter() {
        return delegate.getFormatter();
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return delegate.getSymbolPairs();
    }

    @Nullable
    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

    @Override
    public void destroy() {
        delegate.destroy();
        // OMNI: Liberamos el receiver para evitar leaks
        analyzer.destroy();
    }

    @Override
    public void onContentChange(File file, CharSequence contents) {
        onContentChange(file, contents, null);
    }

    @Override
    public void onContentChange(File file, CharSequence contents, @Nullable Runnable onComplete) {
        // Vinculamos el callback para que la UI se actualice SOLO cuando el análisis termine
        analyzer.setOnAnalyzeComplete(onComplete);
        analyzer.rerunWithBg();
    }
}
