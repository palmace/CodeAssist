package com.tyron.code.language.java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.CompletionItemWrapper;
import com.tyron.code.language.EditorFormatter;
import com.tyron.code.language.LanguageManager;
import com.tyron.code.service.JavacService;
import com.tyron.completion.model.CompletionList;
import com.tyron.editor.Editor;
import com.tyron.language.api.CodeAssistLanguage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

/**
 * JavaLanguage v3.0: "The Sovereign Weaver - Final Diamond Sync".
 * v3.0: Sincronizada comunicación via SovereignConstants (Bypass Total).
 * v3.0: Implementada clase anónima para AsyncFormatter (Fix abstract error).
 * v3.0: Inmune a la "Sordera de Android 14" y bloqueos de HyperOS.
 */
public class JavaLanguage implements Language, EditorFormatter, CodeAssistLanguage {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaLanguage.class);
    private static final AtomicInteger sRequestIdCounter = new AtomicInteger(0);

    private final Editor editor;
    private final TextMateLanguage delegate;
    private final JavaCodeAnalyzer analyzer;
    
    // ASYNCFORMATTER: Implementación anónima para cumplir con Sora 0.23.6
    private final Formatter formatter = new AsyncFormatter() {
        @Nullable @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
            String original = text.toString();
            String formatted = com.tyron.eclipse.formatter.Formatter.format(original,
                    cursorRange.getStartIndex(), cursorRange.getEndIndex() - cursorRange.getStartIndex());
            if (formatted != null && !original.equals(formatted)) {
                text.replace(0, 0, text.getLineCount() - 1, text.getLineString(text.getLineCount() - 1).length(), formatted);
            }
            return cursorRange;
        }

        @Nullable @Override
        public TextRange formatRegionAsync(@NonNull Content text, @NonNull TextRange rangeToFormat, @NonNull TextRange cursorRange) {
            return null;
        }
    };

    private final Map<Integer, CompletionPublisher> mActivePublishers = new ConcurrentHashMap<>();
    
    private final SovereignSignal.Observer mCompletionObserver = message -> {
        if (message == null || !SovereignConstants.ACTION_JAVA_COMPLETION_RESULT.equals(message.action)) return;
        
        int requestId = message.getIntExtra(SovereignConstants.EXTRA_REQUEST_ID, -1);
        CompletionPublisher publisher = mActivePublishers.remove(requestId);
        
        if (publisher != null) {
            CompletionList list = (CompletionList) message.getSerializableExtra(SovereignConstants.EXTRA_COMPLETION_LIST);
            if (list != null) {
                publisher.setUpdateThreshold(0);
                publisher.addItems(list.getItems().stream()
                        .map(CompletionItemWrapper::new)
                        .collect(Collectors.toList()));
            }
        }
    };

    public JavaLanguage(Editor editor) {
        this.editor = editor;
        this.delegate = LanguageManager.createTextMateLanguage("source.java", 
                "textmate/java/syntaxes/java.tmLanguage.json", 
                "textmate/java/configuration.json", editor);
        this.analyzer = new JavaCodeAnalyzer(editor);

        SovereignSignal.register(mCompletionObserver);
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content,
                                    @NonNull CharPosition position,
                                    @NonNull CompletionPublisher publisher,
                                    @NonNull Bundle extraArguments) throws CompletionCancelledException {
        
        Context context = ApplicationLoader.applicationContext;
        File currentFile = editor.getCurrentFile();
        if (context == null || currentFile == null || editor.getProject() == null) return;

        AndroidModuleImpl module = (AndroidModuleImpl) editor.getProject().getModule(currentFile);
        if (module == null) return;

        int requestId = sRequestIdCounter.incrementAndGet();
        mActivePublishers.put(requestId, publisher);

        String prefix = CompletionHelper.computePrefix(content, position, Character::isJavaIdentifierPart);
        
        Intent intent = new Intent(context, JavacService.class);
        intent.setAction(SovereignConstants.ACTION_COMPLETE);
        intent.putExtra(SovereignConstants.EXTRA_FILE_PATH, currentFile.getAbsolutePath());
        intent.putExtra(SovereignConstants.EXTRA_CONTENT, content.toString());
        intent.putExtra(SovereignConstants.EXTRA_CURSOR_OFFSET, (long) position.getIndex());
        intent.putExtra(SovereignConstants.EXTRA_REQUEST_ID, requestId);
        intent.putExtra("prefix", prefix);

        context.startService(intent);
    }

    @Override public void onContentChange(File file, CharSequence contents) { onContentChange(file, contents, null); }

    @Override
    public void onContentChange(File file, CharSequence content, @Nullable Runnable onComplete) {
        analyzer.setOnAnalyzeComplete(onComplete);
        analyzer.rerunWithBg();
    }

    @Override
    public void destroy() {
        delegate.destroy();
        analyzer.destroy();
        SovereignSignal.unregister(mCompletionObserver);
        mActivePublishers.clear();
    }

    @NonNull @Override public AnalyzeManager getAnalyzeManager() { return delegate.getAnalyzeManager(); }
    @Override public int getInterruptionLevel() { return INTERRUPTION_LEVEL_SLIGHT; }
    @Override public boolean useTab() { return true; }
    @NonNull @Override public Formatter getFormatter() { return formatter; }
    @Override public SymbolPairMatch getSymbolPairs() { return delegate.getSymbolPairs(); }
    @Override public NewlineHandler[] getNewlineHandlers() { return new NewlineHandler[0]; }
    @Override public int getIndentAdvance(@NonNull ContentReference content, int line, int column) { return delegate.getIndentAdvance(content, line, column); }
    @NonNull @Override public CharSequence format(@NonNull CharSequence contents, int start, int end) { 
        return com.tyron.eclipse.formatter.Formatter.format(contents.toString(), start, end - start); 
    }
}
