package com.tyron.code.ui.project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.file.FilePickerDialogFixed;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.project.adapter.ProjectManagerAdapter;
import com.tyron.code.ui.settings.SettingsActivity;
import com.tyron.code.ui.wizard.WizardFragment;
import com.tyron.code.util.UiUtilsKt;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.completion.progress.ProgressManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class ProjectManagerFragment extends Fragment {

    public static final String TAG = "ProjectManagerFragment";

    private SharedPreferences mPreferences;
    private RecyclerView mRecyclerView;
    private ProjectManagerAdapter mAdapter;
    private ExtendedFloatingActionButton mCreateProjectFab;
    private ActivityResultLauncher<String[]> mPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
            loadProjects();
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.project_list_fragment_menu);
        
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.projects_path) {
                showDirectorySelectDialog();
                return true;
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(requireActivity(), SettingsActivity.class));
                return true;
            }
            return false;
        });

        mCreateProjectFab = view.findViewById(R.id.create_project_fab);
        mCreateProjectFab.setOnClickListener(v -> {
            WizardFragment wizard = new WizardFragment();
            // CORRECCIÓN: Escuchamos cuando el proyecto se crea para navegar al editor
            wizard.setOnProjectCreatedListener(project -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, MainFragment.newInstance(project.getRootFile().getAbsolutePath()))
                        .addToBackStack(null)
                        .commit();
            });

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, wizard)
                    .addToBackStack(null)
                    .commit();
        });

        mAdapter = new ProjectManagerAdapter();
        mAdapter.setOnProjectSelectedListener(this::openProject);
        mAdapter.setOnProjectLongClickListener(this::inflateProjectMenus);
        mRecyclerView = view.findViewById(R.id.projects_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mAdapter);
    }

    private boolean inflateProjectMenus(View view, Project project) {
        view.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.add(R.string.dialog_delete)
                    .setOnMenuItemClickListener(item -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.dialog_delete)
                                .setMessage(getString(R.string.dialog_confirm_delete, project.getRootFile().getName()))
                                .setPositiveButton(android.R.string.yes, (d, which) -> deleteProject(project))
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                        return true;
                    });
        });
        view.showContextMenu();
        return true;
    }

    private void deleteProject(Project project) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FileUtils.forceDelete(project.getRootFile());
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(this::loadProjects);
                }
            } catch (IOException e) {
                Log.e(TAG, "Delete failed", e);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.project_manager_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        String path = mPreferences.getString(SharedPreferenceKeys.PROJECT_SAVE_PATH, "");
        if (path.isEmpty() || path.contains("Android/data")) {
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CodeAssistProjects");
            if (!downloadDir.exists()) downloadDir.mkdirs();
            mPreferences.edit().putString(SharedPreferenceKeys.PROJECT_SAVE_PATH, downloadDir.getAbsolutePath()).apply();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             if (!Environment.isExternalStorageManager()) {
                 requestAllFilesAccess();
             } else {
                 loadProjects();
             }
        } else {
            if (permissionsGranted()) {
                loadProjects();
            } else {
                requestPermissions();
            }
        }
    }
    
    private void requestAllFilesAccess() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permiso de Almacenamiento")
                .setMessage("CodeAssist necesita acceso a la carpeta de Descargas para gestionar proyectos. Por favor, concede el permiso.")
                .setPositiveButton("Conceder", (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showDirectorySelectDialog() {
        DialogProperties properties = new DialogProperties();
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.root = Environment.getExternalStorageDirectory();
        
        String currentPath = mPreferences.getString(SharedPreferenceKeys.PROJECT_SAVE_PATH, properties.root.getAbsolutePath());
        properties.offset = new File(currentPath);
        
        FilePickerDialogFixed dialog = new FilePickerDialogFixed(requireContext(), properties);
        dialog.setTitle(getString(R.string.project_manager_save_location_title));
        dialog.setDialogSelectionListener(files -> {
            mPreferences.edit().putString(SharedPreferenceKeys.PROJECT_SAVE_PATH, files[0]).apply();
            loadProjects();
        });
        dialog.show();
    }

    private boolean permissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager();
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        mPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE});
    }

    private void openProject(Project project) {
        Project current = ProjectManager.getInstance().getCurrentProject();
        if (current != null && !current.equals(project)) {
             ProjectManager.getInstance().closeProject(current);
        }

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, MainFragment.newInstance(project.getRootFile().getAbsolutePath()))
                .addToBackStack(null)
                .commit();
    }

    private void loadProjects() {
        if (!permissionsGranted()) return;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            String path = mPreferences.getString(SharedPreferenceKeys.PROJECT_SAVE_PATH, "");
            File projectDir = new File(path);
            if (!projectDir.exists()) projectDir.mkdirs();
            
            File[] directories = projectDir.listFiles(File::isDirectory);
            List<Project> projects = new ArrayList<>();
            if (directories != null) {
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified).reversed());
                for (File directory : directories) {
                    if (isGradleProject(directory)) {
                        projects.add(new Project(directory));
                    }
                }
            }

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    mAdapter.submitList(projects);
                    View view = getView();
                    if (view != null) {
                        view.findViewById(R.id.projects_recycler).setVisibility(projects.isEmpty() ? View.GONE : View.VISIBLE);
                        view.findViewById(R.id.empty_projects).setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
                        view.findViewById(R.id.empty_container).setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private boolean isGradleProject(File directory) {
        return new File(directory, "build.gradle").exists() || 
               new File(directory, "settings.gradle").exists() || 
               new File(directory, "build.gradle.kts").exists() ||
               new File(directory, "settings.gradle.kts").exists() ||
               new File(directory, "app").exists();
    }
}
