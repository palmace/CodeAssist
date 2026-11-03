package com.tyron.kotlin.completion.diagnostics;

import com.tyron.builder.project.api.Module;
import com.tyron.diagnostics.DiagnosticProvider;
import com.tyron.kotlin.completion.KotlinEnvironment;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;

public class KotlinDiagnosticsProvider implements DiagnosticProvider {
    
    @Override
    public List<? extends Diagnostic<?>> getDiagnostics(Module module, File file) {
        KotlinEnvironment environment = KotlinEnvironment.Companion.get(module);
        if (environment == null) {
            return Collections.emptyList();
        }
        
        // Retornar los diagnósticos almacenados en el environment para este archivo
        List<Diagnostic<File>> diagnostics = environment.getDiagnosticsForFile(file.getAbsolutePath());
        return diagnostics != null ? diagnostics : Collections.emptyList();
    }
}
