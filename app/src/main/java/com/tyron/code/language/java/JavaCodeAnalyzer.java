package com.tyron.code.language.java;

import android.content.Context;
import android.content.Intent;

import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.AbstractCodeAnalyzer;
import com.tyron.code.service.JavacService;
import com.tyron.common.util.DebouncerStore;
import com.tyron.editor.Editor;
import com.tyron.builder.model.DiagnosticWrapper;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaCodeAnalyzer v1.20: "The Pure Tunnel Sentinel - Ghost Mode".
 * v1.20: Migrado a SovereignSignal con SovereignMessage (Java puro).
 * v1.20: Inmune a la "Sordera de Android 14" y bloqueos de HyperOS.
 */
public class JavaCodeAnalyzer extends AbstractCodeAnalyzer<List<DiagnosticWrapper>> {

    private final Editor mEditor;
    private final DebouncerStore<String> mDebouncerStore = DebouncerStore.DEFAULT;
    private final SovereignSignal.Observer mDiagnosticObserver;

    public JavaCodeAnalyzer(final Editor editor) {
        this.mEditor = editor;
        
        this.mDiagnosticObserver = message -> {
            if (message == null || !SovereignConstants.ACTION_JAVA_DIAGNOSTIC_RESULT.equals(message.action)) return;
            
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

    @Override public Lexer getLexer(CharStream input) { return new com.tyron.code.language.java.JavaLexer(input); }

    @Override
    public void rerunWithBg() {
        mDebouncerStore.registerOrGetDebouncer("java_swarm_analyze").debounce(1000, super::rerunWithBg);
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
        classpath.addAll(androidModule.getCodeAssistLibraries().stream()
                .map(lib -> lib.getSourceFile().getAbsolutePath())
                .filter(path -> new File(path).exists())
                .collect(Collectors.toList()));
        
        File kotlinClasses = new File(androidModule.getBuildDirectory(), "bin/kotlin/classes");
        if (kotlinClasses.exists()) classpath.add(kotlinClasses.getAbsolutePath());

        Intent intent = new Intent(context, JavacService.class);
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
