package com.tyron.code.ui.main.action.project;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.ui.main.MainFragment;

/**
 * Acción para realizar una limpieza completa de los archivos de compilación.
 */
public class CleanProjectAction extends AnAction {

    @Override
    public void update(@NonNull AnActionEvent event) {
        Context context = event.getData(CommonDataKeys.CONTEXT);
        
        // Solo mostrar si estamos en la barra de herramientas principal
        if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace()) || context == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(true);
        event.getPresentation().setText("Clean Project");
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent event) {
        Fragment fragment = event.getData(CommonDataKeys.FRAGMENT);
        
        if (fragment instanceof MainFragment) {
            ((MainFragment) fragment).cleanProject();
        }
    }
}
