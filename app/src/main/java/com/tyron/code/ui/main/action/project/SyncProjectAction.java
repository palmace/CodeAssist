package com.tyron.code.ui.main.action.project;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import android.app.Activity;

import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.util.DependencyUtils;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogViewModel;
import com.tyron.common.util.AndroidUtilities;

public class SyncProjectAction extends AnAction {
    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        Activity activity = e.getRequiredData(CommonDataKeys.ACTIVITY);
        if (!(activity instanceof ViewModelStoreOwner)) {
            return;
        }

        LogViewModel logViewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(LogViewModel.class);
        ILogger logger = ILogger.wrap(logViewModel);
        
        MainViewModel mainViewModel = e.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
        if (mainViewModel != null) {
            mainViewModel.setIndexing(true);
            mainViewModel.setCurrentState("Sincronizando...");
        }
        
        // Pasamos el activity como contexto
        new Thread(() -> {
            DependencyUtils.downloadDependencies(activity, project.getRootFile(), logger, () -> {
                activity.runOnUiThread(() -> {
                    if (mainViewModel != null) {
                        mainViewModel.setIndexing(false);
                        mainViewModel.setCurrentState(null);
                    }
                });
            });
        }).start();
    }

    @Override
    public void update(@NonNull AnActionEvent e) {
        e.getPresentation().setText("Sincronizar Gradle");
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
    }
}
