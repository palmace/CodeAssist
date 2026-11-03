package com.tyron.code.ui.main.action.compile;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.tyron.actions.ActionGroup;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.R;

public class CompileActionGroup extends ActionGroup {

    public static final String ID = "compileActionGroup";

    @Override
    public void update(@NonNull AnActionEvent event) {
        // No mostrar en el menú de pestañas del editor
        if (ActionPlaces.EDITOR_TAB.equals(event.getPlace())) {
            event.getPresentation().setVisible(false);
            return;
        }

        Context context = event.getData(CommonDataKeys.CONTEXT);
        if (context != null) {
            event.getPresentation().setVisible(true);
            event.getPresentation().setEnabled(true);
            event.getPresentation().setText(context.getString(R.string.menu_run));
            event.getPresentation().setIcon(ContextCompat.getDrawable(context, R.drawable.round_play_arrow_24));
        } else {
            event.getPresentation().setVisible(false);
        }
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
       return new AnAction[]{
               new CompileReleaseAction(),
               new CompileDebugAction(), 
               new CompileAabAction()
       };
    }
}
