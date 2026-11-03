package com.tyron.kotlin_completion;

import com.tyron.actions.ActionManager;
import com.tyron.kotlin_completion.action.ImplementAbstractFunctionsQuickFix;
import com.tyron.kotlin_completion.action.KotlinImportQuickFix;

/**
 * KotlinCompletionModule v1.3: "The Modular Intention Registry".
 * v1.3: Sincronizado con el nuevo sistema de paquetes modular.
 * v1.3: Registro de acciones de auto-import y resolución de miembros para el motor Swarm.
 */
public class KotlinCompletionModule {

    public static void registerActions(ActionManager actionManager) {
        // Registro de acciones usando los IDs únicos y el nuevo sistema de paquetes
        actionManager.registerAction("ImplementKotlinMembers", new ImplementAbstractFunctionsQuickFix());
        actionManager.registerAction(KotlinImportQuickFix.ID, new KotlinImportQuickFix());
    }
}
