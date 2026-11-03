package com.tyron.code.ui.main.action.project;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.R;
import com.tyron.code.ui.project.LibraryManagerFragment;

public class LibraryManagerAction extends AnAction {

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Fragment fragment = e.getData(CommonDataKeys.FRAGMENT);
        if (fragment == null) {
            return;
        }

        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LibraryManagerFragment.newInstance())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void update(@NonNull AnActionEvent e) {
        Context context = e.getData(CommonDataKeys.CONTEXT);
        if (context != null) {
            e.getPresentation().setText(context.getString(R.string.menu_library_manager));
        }
    }
}
