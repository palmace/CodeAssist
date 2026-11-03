package com.tyron.code.language.kotlin;

import android.content.Context;
import android.content.Intent;

import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.AbstractCodeAnalyzer;
import com.tyron.code.service.KotlinService;
import com.tyron.common.util.DebouncerStore;
import com.tyron.editor.Editor;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KotlinCodeAnalyzer v1.23: "The Pure Tunnel Sentinel - Ghost Mode".
 * v1.23: Sincronizado totalmente con SovereignConstants.
 * v1.23: Inmune a la "Sordera de Android 14" y bloqueos de HyperOS.
 */
public class KotlinCodeAnalyzer extends AbstractCodeAnalyzer<List<DiagnosticWrapper>> {

    private final Editor mEditor;
    private final DebouncerStore<String> mDebouncerStore = DebouncerStore.DEFAULT;
    private final SovereignSignal.Observer mDiagnosticObserver;

    public KotlinCodeAnalyzer(final Editor editor) {
        this.mEditor = editor;
        
        this.mDiagnosticObserver = message -> {
            if (message == null || !SovereignConstants.ACTION_KOTLIN_DIAGNOSTIC_RESULT.equals(message.action)) return;
            
            String filePath = message.getStringExtra(SovereignConstants.EXTRA_FILE_PATH);
            File currentFile = editor.getCurrentFile();
            
            if (currentFile != null && currentFile.getAbsolutePath().equals(filePath)) {
                @SuppressWarnings("unchecked")
                ArrayList<DiagnosticWrapper> diagnostics = 
                    (ArrayList<DiagnosticWrapper>) message.getSerializableExtra(SovereignConstants.EXTRA_DIAGNOSTICS);
                if (diagnostics != null) {
                    editor.setDiagnostics(diagnostics);
                }
            }
        };
        
        SovereignSignal.register(mDiagnosticObserver);
    }

    @Override public Lexer getLexer(CharStream input) { return new KotlinLexer(input); }

    @Override
    public void rerunWithBg() {
        mDebouncerStore.registerOrGetDebouncer("kotlin_swarm_analyze").debounce(3000, super::rerunWithBg);
    }

    @Override
    public void analyzeInBackground(CharSequence contents) {
        Project project = mEditor.getProject();
        File currentFile = mEditor.getCurrentFile();
        if (project == null || currentFile == null) return;

        Module module = project.getModule(currentFile);
        if (!(module instanceof AndroidModuleImpl)) return;

        AndroidModuleImpl androidModule = (AndroidModuleImpl) module;
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;

        ArrayList<String> classpath = new ArrayList<>();
        List<String> libPaths = androidModule.getCodeAssistLibraries().stream()
                .map(lib -> lib.getSourceFile().getAbsolutePath())
                .filter(path -> new File(path).exists())
                .collect(Collectors.toList());
        classpath.addAll(libPaths);
        
        File javaClasses = new File(androidModule.getBuildDirectory(), "bin/java/classes");
        if (javaClasses.exists()) {
            classpath.add(javaClasses.getAbsolutePath());
        }

        Intent intent = new Intent(context, KotlinService.class);
        intent.setAction(SovereignConstants.ACTION_DIAGNOSTIC);
        intent.putExtra(SovereignConstants.EXTRA_FILE_PATH, currentFile.getAbsolutePath());
        intent.putExtra(SovereignConstants.EXTRA_CONTENT, contents.toString());
        intent.putStringArrayListExtra(SovereignConstants.EXTRA_CLASSPATH, classpath);

        context.startService(intent);
    }
    
    public void destroy() {
        SovereignSignal.unregister(mDiagnosticObserver);
    }
}
