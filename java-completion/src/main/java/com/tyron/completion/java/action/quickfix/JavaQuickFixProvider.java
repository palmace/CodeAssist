package com.tyron.completion.java.action.quickfix;

import com.tyron.actions.ActionManager;
import com.tyron.actions.AnAction;
import com.tyron.builder.model.DiagnosticWrapper;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaQuickFixProvider v1.2.1: "The Precision Healer".
 * v1.2.1: Refinado fallback proactivo para incluir resolución de campos y métodos estáticos.
 * v1.2.1: Optimizado para el motor Swarm de la Fase 3.
 */
public class JavaQuickFixProvider {

    public static List<AnAction> getFixesFor(DiagnosticWrapper diagnostic) {
        List<AnAction> fixes = new ArrayList<>();
        if (diagnostic == null || diagnostic.getCode() == null) {
            return fixes;
        }
        
        ActionManager am = ActionManager.getInstance();
        String code = diagnostic.getCode();
        
        // Mapeo de precisión quirúrgica basado en los códigos internos de Javac
        switch (code) {
            // Errores de resolución de símbolos y visibilidad
            case "compiler.err.cant.resolve.location":
            case "compiler.err.cant.resolve":
            case "compiler.err.not.def.public.cant.access":
            case "compiler.err.proc.cant.access.1":
                addAction(fixes, am, ImportClassAction.ID);
                addAction(fixes, am, ImportClassFieldFix.ID);
                break;
                
            // Errores de excepciones (Checked Exceptions)
            case "compiler.err.unreported.exception":
            case "compiler.err.unreported.exception.need.to.catch.or.throw":
                addAction(fixes, am, AddThrowsAction.ID);
                addAction(fixes, am, AddCatchClauseAction.ID);
                addAction(fixes, am, SurroundWithTryCatchAction.ID);
                break;
                
            // Errores de herencia y abstracción
            case "compiler.err.abstract.cant.be.instantiated":
            case "compiler.err.does.not.override.abstract":
                addAction(fixes, am, ImplementAbstractMethodsFix.ID);
                break;
                
            default:
                // Fallback proactivo para errores de resolución no tipificados
                if (code.contains("resolve") || code.contains("location")) {
                    addAction(fixes, am, ImportClassAction.ID);
                    addAction(fixes, am, ImportClassFieldFix.ID);
                } else {
                    // Fallback general: Acciones comunes para validación manual
                    addAction(fixes, am, ImportClassAction.ID);
                    addAction(fixes, am, ImplementAbstractMethodsFix.ID);
                }
                break;
        }
        
        return fixes;
    }
    
    private static void addAction(List<AnAction> fixes, ActionManager am, String id) {
        AnAction action = am.getAction(id);
        if (action != null) {
            fixes.add(action);
        }
    }
}
