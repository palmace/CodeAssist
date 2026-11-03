package com.tyron.code.ui.main.action.debug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.actions.ActionGroup;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.Presentation;
import com.tyron.code.BuildConfig;
import com.tyron.code.ui.main.action.project.LibraryManagerAction;
import com.tyron.code.ui.main.action.project.SyncProjectAction;

public class DebugActionGroup extends ActionGroup {

    public static final String ID = "debugActionGroup";

    @Override
    public void update(@NonNull AnActionEvent event) {
        // No mostrar en el menú de pestañas del editor
        if (ActionPlaces.EDITOR_TAB.equals(event.getPlace())) {
            event.getPresentation().setVisible(false);
            return;
        }

        Presentation presentation = event.getPresentation();
        presentation.setVisible(true);
        presentation.setText("Debug");
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{
                new SyncProjectAction(),
                new LibraryManagerAction(),
                new LoadActionJarAction(), 
                new LoadFileEditorProviderAction(),
                new CrashAction(), 
                new LoadXmlRepositoryAction(), 
                new RunLongRunningTaskAction(), 
                new InjectClassesAction()
        };
    }
}
