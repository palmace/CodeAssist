package com.tyron.code.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogViewModel;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.project.LibraryManagerFragment;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.DependencyUtils;

public class BuildSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.build_preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        String key = preference.getKey();
        Project project = ProjectManager.getInstance().getCurrentProject();

        if ("sync_gradle".equals(key)) {
            if (project == null) {
                Toast.makeText(getContext(), "Abre un proyecto primero", Toast.LENGTH_SHORT).show();
                return true;
            }
            syncGradle(project);
            return true;
        }

        if ("open_library_manager".equals(key)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, LibraryManagerFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void syncGradle(Project project) {
        Context context = getContext();
        if (context == null) return;

        ILogger logger = ILogger.wrap(new ViewModelProvider(requireActivity()).get(LogViewModel.class));
        Toast.makeText(context, "Sincronizando dependencias...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            DependencyUtils.downloadDependencies(context, project.getRootFile(), logger, () -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Sincronización finalizada", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }).start();
    }
}
