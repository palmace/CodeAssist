package com.tyron.kotlin_completion.action;

import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.actions.AnAction;
import com.tyron.actions.ActionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * KotlinQuickFixProvider v1.3: "The Modular Intention Dispatcher".
 * v1.3: Corregido paquete para evitar conflictos de visibilidad.
 * v1.3: Sincronizado con KotlinImportQuickFix modular.
 */
public class KotlinQuickFixProvider {

    public static List<AnAction> getFixesFor(DiagnosticWrapper diagnostic) {
        List<AnAction> fixes = new ArrayList<>();
        if (diagnostic == null) return fixes;
        
        String code = diagnostic.getCode();
        if (code == null) return fixes;

        ActionManager am = ActionManager.getInstance();

        switch (code) {
            case "ABSTRACT_MEMBER_NOT_IMPLEMENTED":
            case "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED":
                AnAction implementAction = am.getAction("ImplementKotlinMembers");
                if (implementAction != null) {
                    fixes.add(implementAction);
                }
                break;
            
            case "UNRESOLVED_REFERENCE":
                // ID de la acción corregida
                AnAction importAction = am.getAction(KotlinImportQuickFix.ID);
                if (importAction != null) {
                    fixes.add(importAction);
                }
                break;
        }

        return fixes;
    }
}
