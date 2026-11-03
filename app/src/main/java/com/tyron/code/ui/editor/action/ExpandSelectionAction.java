package com.tyron.code.ui.editor.action;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.common.collect.Range;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.actions.Presentation;
import com.tyron.code.R;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.editor.Editor;
import com.tyron.editor.selection.ExpandSelectionProvider;

import java.io.File;

/**
 * ExpandSelectionAction v30.3: "The Zenith Selector - Sovereign Signature".
 * v30.3: Sincronía con AndroidUtilities v30.3 (Zenith).
 * v30.3: Refuerzo de flujo de selección para evitar colisiones con el teclado virtual.
 */
public class ExpandSelectionAction extends AnAction {

    public static final String ID = "expandSelection";

    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);

        if (!ActionPlaces.EDITOR.equals(event.getPlace())) {
            return;
        }

        File file = event.getData(CommonDataKeys.FILE);
        if (file == null) {
            return;
        }

        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        ExpandSelectionProvider provider = ExpandSelectionProvider.forEditor(editor);
        if (provider == null) {
            return;
        }

        if (editor.getProject() == null) {
            return;
        }

        DataContext context = event.getDataContext();
        presentation.setVisible(true);
        presentation.setText(context.getString(R.string.expand_selection));
        presentation.setIcon(ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.ic_baseline_code_24, null));
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        Activity activity = e.getRequiredData(CommonDataKeys.ACTIVITY);
        
        ExpandSelectionProvider provider = ExpandSelectionProvider.forEditor(editor);
        if (provider == null) {
            // OMNI: Firma Zenith v30.3 (Contexto explícito)
            AndroidUtilities.showSimpleAlert(activity, "No provider",
                                             "No expand selection provider found.");
            return;
        }
        
        Range<Integer> range = provider.expandSelection(editor);
        if (range == null) {
            AndroidUtilities.showSimpleAlert(activity, "Error", "Cannot expand selection");
            return;
        }
        editor.setSelectionRegion(range.lowerEndpoint(), range.upperEndpoint());
    }
}
