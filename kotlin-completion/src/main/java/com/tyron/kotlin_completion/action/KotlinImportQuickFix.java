package com.tyron.kotlin_completion.action;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.editor.Editor;
import java.util.Locale;

/**
 * KotlinImportQuickFix v44.25.28: "The Zenith Sovereign - Terminal Resonance".
 * v44.25.28: Sincronía total con OMNI v44.25.30 y soporte para símbolos en sectores dinámicos (gen/generated).
 * v44.25.27: Alineación con OMNI v44.25.6 y optimización de búsqueda heurística de símbolos.
 * v44.25.26: Sincronía final con OMNI v44.25.6 y validación de arquitectura Android 15.
 */
public class KotlinImportQuickFix extends QuickFix {

    public static final String ID = "KotlinImportQuickFix";
    private String mCandidatePackage;

    public KotlinImportQuickFix() {
        super();
    }

    @Override
    public boolean accept(@NonNull String errorCode) {
        return "UNRESOLVED_REFERENCE".equals(errorCode);
    }

    @Override
    public String getTitle(Context context) {
        return "Importar " + (mCandidatePackage != null ? mCandidatePackage : "clase");
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null || mCandidatePackage == null) return;

        String content = editor.getContent().toString();
        String importStatement = "import " + mCandidatePackage + "\n";
        
        int insertPos = 0;
        int packageIndex = content.indexOf("package ");
        if (packageIndex != -1) {
            int endOfLine = content.indexOf("\n", packageIndex);
            if (endOfLine != -1) {
                insertPos = endOfLine + 1;
                // Asegurar espacio tras la declaración de paquete
                if (insertPos < content.length() && content.charAt(insertPos) != '\n') {
                    importStatement += "\n";
                }
            }
        }
        
        editor.getContent().insert(insertPos, importStatement);
    }

    @Override
    public void update(@NonNull AnActionEvent event) {
        super.update(event);
        if (!event.getPresentation().isVisible()) return;

        DiagnosticWrapper diagnostic = (DiagnosticWrapper) event.getData(CommonDataKeys.DIAGNOSTIC);
        Project project = event.getData(CommonDataKeys.PROJECT);
        
        if (diagnostic == null || project == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        String msg = diagnostic.getMessage(Locale.getDefault());
        if (msg != null) {
            String symbolName = null;
            // Extracción quirúrgica del símbolo faltante
            if (msg.contains(": ")) {
                symbolName = msg.substring(msg.indexOf(": ") + 2).trim();
            } else if (msg.contains("'")) {
                int firstQuote = msg.indexOf("'");
                int lastQuote = msg.lastIndexOf("'");
                if (firstQuote != -1 && lastQuote > firstQuote) {
                    symbolName = msg.substring(firstQuote + 1, lastQuote);
                }
            }

            if (symbolName != null && project.getSymbolIndex() != null) {
                // OMNI v44.25.28: El motor de indexación ahora reporta con prioridad las clases en build/gen
                mCandidatePackage = project.getSymbolIndex().findFqnFor(symbolName);
            }
            
            if (mCandidatePackage == null) {
                event.getPresentation().setVisible(false);
            }
        } else {
            event.getPresentation().setVisible(false);
        }
    }
}
