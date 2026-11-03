package com.tyron.code.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.common.base.Throwables;
import com.tyron.actions.ActionManager;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.actions.util.DataContextUtils;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.AndroidAppBuilder;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Builder;
import com.tyron.builder.compiler.CleanTask;
import com.tyron.builder.compiler.JavaBuilder;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogViewModel;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.file.FileViewModel;
import com.tyron.code.ui.file.event.RefreshRootEvent;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.ApkInstaller;
import com.tyron.code.util.DependencyUtils;
import com.tyron.code.util.UiUtilsKt;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.completion.java.provider.CompletionEngine;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.fileeditor.api.FileEditor;

import org.jetbrains.kotlin.com.intellij.openapi.util.Key;

import java.io.File;

/**
 * MainFragment v44.25.30: "The Absolute Sovereign - Terminal Resonance".
 * v44.25.30: Re-Escaneo en Caliente de suministros tras Fallo Crítico y protocolo de recuperación.
 * v44.25.29: Sincronía Zenith v44.25 - Protocolo de Rescate Heurístico.
 */
public class MainFragment extends Fragment implements ProjectManager.OnProjectOpenListener {

    private static final String TAG = "MainFragment";
    public static final String REFRESH_TOOLBAR_KEY = "refreshToolbar";
    public static final Key<CompileCallback> COMPILE_CALLBACK_KEY = Key.create("compileCallback");
    public static final Key<IndexCallback> INDEX_CALLBACK_KEY = Key.create("indexCallbackKey");
    public static final Key<MainViewModel> MAIN_VIEW_MODEL_KEY = Key.create("mainViewModel");

    private LogViewModel mLogViewModel;
    private MainViewModel mMainViewModel;
    private FileViewModel mFileViewModel;
    private ProjectManager mProjectManager;
    private View mRoot;
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private LinearProgressIndicator mProgressBar;
    private BroadcastReceiver mLogReceiver;
    private Project mProject;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mRoot instanceof DrawerLayout && ((DrawerLayout) mRoot).isDrawerOpen(GravityCompat.START)) {
                mMainViewModel.setDrawerState(false);
            } else {
                setEnabled(false);
                requireActivity().onBackPressed();
                setEnabled(true);
            }
        }
    };

    private final CompileCallback mCompileCallback = this::compile;
    private final IndexCallback mIndexCallback = this::openProject;

    public static MainFragment newInstance(@NonNull String projectPath) {
        Bundle bundle = new Bundle();
        bundle.putString("project_path", projectPath);
        MainFragment fragment = new MainFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        String projectPath = requireArguments().getString("project_path");
        mProject = new Project(new File(projectPath));
        mProjectManager = ProjectManager.getInstance();
        mProjectManager.addOnProjectOpenListener(this);
        
        mLogViewModel = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mFileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.main_fragment, container, false);
        mProgressBar = mRoot.findViewById(R.id.progressbar);
        mToolbar = mRoot.findViewById(R.id.toolbar);
        mAppBarLayout = mRoot.findViewById(R.id.app_bar);
        
        mToolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24);
        
        if (mAppBarLayout != null) {
            UiUtilsKt.addSystemWindowInsetToPadding(mAppBarLayout, false, true, false, false);
        }

        View syncBtn = mRoot.findViewById(R.id.sync_button_fallback);
        if (syncBtn != null) {
            syncBtn.setOnClickListener(v -> syncProject());
            syncBtn.setOnLongClickListener(v -> {
                AndroidUtilities.showTacticalAlert(requireActivity(), "Purga Forense",
                    "¿Desea limpiar todos los rastros de compilaciones anteriores?", "LIMPIAR", () -> {
                        cleanProject();
                        Toast.makeText(getContext(), "Caché de Hydra purgada.", Toast.LENGTH_SHORT).show();
                    }, "CANCELAR", null, null);
                return true;
            });
        }

        getChildFragmentManager().setFragmentResultListener(REFRESH_TOOLBAR_KEY, getViewLifecycleOwner(), (k, b) -> refreshToolbar());
        refreshToolbar();
        return mRoot;
    }

    private void syncProject() {
        if (mProject == null) return;
        mMainViewModel.setIndexing(true);
        mMainViewModel.setCurrentState("Sincronizando...");
        mLogViewModel.clear(LogViewModel.BUILD_LOG);
        
        mMainViewModel.setBottomSheetState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        
        new Thread(() -> {
            DependencyUtils.downloadDependencies(getContext(), mProject.getRootFile(), ILogger.wrap(mLogViewModel), () -> {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    mMainViewModel.setIndexing(false);
                    mMainViewModel.setCurrentState(null);
                    AndroidUtilities.showSimpleAlert(requireActivity(), "Sincronización", "Finalizada con éxito.");
                });
            });
        }).start();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mRoot instanceof DrawerLayout) {
            DrawerLayout dl = (DrawerLayout) mRoot;
            mToolbar.setNavigationOnClickListener(v -> mMainViewModel.setDrawerState(!dl.isDrawerOpen(GravityCompat.START)));
            dl.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override public void onDrawerOpened(@NonNull View p1) { mMainViewModel.setDrawerState(true); onBackPressedCallback.setEnabled(true); }
                @Override public void onDrawerClosed(@NonNull View p1) { mMainViewModel.setDrawerState(false); onBackPressedCallback.setEnabled(false); }
            });
        }
        
        mMainViewModel.isIndexing().observe(getViewLifecycleOwner(), indexing -> {
            mProgressBar.setVisibility(indexing ? View.VISIBLE : View.GONE);
            CompletionEngine.setIndexing(indexing);
            refreshToolbar();
        });
        mMainViewModel.getToolbarTitle().observe(getViewLifecycleOwner(), mToolbar::setTitle);
        mMainViewModel.getCurrentState().observe(getViewLifecycleOwner(), mToolbar::setSubtitle);

        if (!mProject.equals(mProjectManager.getCurrentProject())) {
            mRoot.postDelayed(() -> openProject(mProject), 200);
        }
    }

    public void openFile(FileEditor editor) {
        if (editor != null) mMainViewModel.openFile(editor);
    }

    public void cleanProject() {
        if (Boolean.TRUE.equals(mMainViewModel.isIndexing().getValue())) return;
        mLogViewModel.clear(LogViewModel.BUILD_LOG);
        mMainViewModel.setIndexing(true);
        mMainViewModel.setCurrentState("Purgando...");
        mMainViewModel.setBottomSheetState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        
        ProgressManager.getInstance().runNonCancelableAsync(() -> {
            try {
                Module module = mProject.getMainModule();
                if (module instanceof AndroidModule) {
                    new CleanTask(mProject, (AndroidModule) module, ILogger.wrap(mLogViewModel)).run();
                }
                File cacheDir = requireContext().getCacheDir();
                File[] temps = cacheDir.listFiles((d, n) -> n.endsWith(".tmp"));
                if (temps != null) for (File f : temps) f.delete();
                
            } catch (Throwable ignored) {
            } finally {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    mMainViewModel.setIndexing(false);
                    mMainViewModel.setCurrentState(null);
                });
            }
        });
    }

    public void openProject(@NonNull Project project) {
        mProject = project;
        mMainViewModel.setToolbarTitle(project.getRootFile().getName());
        mMainViewModel.setIndexing(true);
        ApplicationLoader.getInstance().getEventManager().dispatchEvent(new RefreshRootEvent(project.getRootFile()));
        ProgressManager.getInstance().runNonCancelableAsync(() -> ProjectManager.getInstance().openProject(project, false, new TaskListener(), ILogger.wrap(mLogViewModel)));
    }

    private class TaskListener implements ProjectManager.TaskListener {
        @Override public void onTaskStarted(String msg) { if (getActivity() != null) getActivity().runOnUiThread(() -> mMainViewModel.setCurrentState(msg)); }
        @Override public void onComplete(Project p, boolean s, String m) {
            if (getActivity() != null) getActivity().runOnUiThread(() -> { mMainViewModel.setIndexing(false); mMainViewModel.setCurrentState(null); });
        }
    }

    private void compile(BuildType type) {
        if (Boolean.TRUE.equals(mMainViewModel.isIndexing().getValue())) return;
        mMainViewModel.setIndexing(true);
        mMainViewModel.setCurrentState("Compilando...");
        mLogViewModel.clear(LogViewModel.BUILD_LOG);
        mMainViewModel.setBottomSheetState(BottomSheetBehavior.STATE_HALF_EXPANDED);

        ProgressManager.getInstance().runNonCancelableAsync(() -> {
            try {
                Module module = mProject.getMainModule();
                Builder<?> builder;
                if (module instanceof AndroidModule) builder = new AndroidAppBuilder(mProject, (AndroidModule) module, ILogger.wrap(mLogViewModel));
                else if (module instanceof JavaModule) builder = new JavaBuilder(mProject, (JavaModule) module, ILogger.wrap(mLogViewModel));
                else throw new IllegalStateException("Módulo no soportado");

                builder.setTaskListener((name, message, progress) -> {
                    if (getActivity() != null) requireActivity().runOnUiThread(() -> mMainViewModel.setCurrentState(name + ": " + message));
                });

                builder.build(type);

                if (module instanceof AndroidModule) {
                    File apkFile = new File(module.getBuildDirectory(), "bin/signed.apk");
                    if (apkFile.exists() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> ApkInstaller.installApplication(requireContext(), requireContext().getPackageName(), apkFile.getAbsolutePath()));
                    }
                }
            } catch (Throwable t) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> {
                    String stack = Throwables.getStackTraceAsString(t);
                    mLogViewModel.e(LogViewModel.BUILD_LOG, stack);
                    mMainViewModel.setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                    
                    if (stack.contains("FALLO DE SUMINISTRO")) {
                        // RE-ESCANEO EN CALIENTE: Forzamos al módulo a olvidar el arsenal incompleto
                        Module m = mProject.getMainModule();
                        if (m instanceof AndroidModuleImpl) {
                            ((AndroidModuleImpl) m).invalidateLibraryCache();
                        }
                        AndroidUtilities.showSimpleAlert(requireActivity(), "🚨 Alerta de Suministro", "Suministros incompletos. Se ha forzado un re-escaneo heurístico. Si ya copió los archivos a 'repo', intente compilar de nuevo.");
                    }
                });
            } finally {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> { mMainViewModel.setCurrentState(null); mMainViewModel.setIndexing(false); });
            }
        });
    }

    @Override public void onProjectOpen(Project project) {
        if (mProjectManager == null) mProjectManager = ProjectManager.getInstance();
        if (mLogReceiver != null) { try { requireActivity().unregisterReceiver(mLogReceiver); } catch (Exception ignored) {} }
        Module module = project.getMainModule();
        if (module instanceof AndroidModule) {
            mLogReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    String type = intent.getStringExtra("type");
                    DiagnosticWrapper wrapped = ILogger.wrap(intent.getStringExtra("message"));
                    if ("ERROR".equals(type)) mLogViewModel.e(LogViewModel.APP_LOG, wrapped);
                    else if ("WARNING".equals(type)) mLogViewModel.w(LogViewModel.APP_LOG, wrapped);
                    else mLogViewModel.d(LogViewModel.APP_LOG, wrapped);
                }
            };
            String pkg = ((AndroidModule) module).getPackageName();
            if (pkg != null) {
                BuildModule.registerReceiverSecure(requireContext(), mLogReceiver, new IntentFilter(pkg + ".LOG"), 2);
            }
        }
    }

    public void refreshToolbar() {
        mToolbar.getMenu().clear();
        DataContext context = DataContextUtils.getDataContext(mToolbar);
        if (context != null) {
            context.putData(CommonDataKeys.PROJECT, ProjectManager.getInstance().getCurrentProject());
            context.putData(CommonDataKeys.ACTIVITY, getActivity());
            context.putData(CommonDataKeys.CONTEXT, getContext());
            context.putData(CommonDataKeys.FRAGMENT, this);
            context.putData(MAIN_VIEW_MODEL_KEY, mMainViewModel);
            context.putData(COMPILE_CALLBACK_KEY, mCompileCallback);
            context.putData(INDEX_CALLBACK_KEY, mIndexCallback);
            ActionManager.getInstance().fillMenu(context, mToolbar.getMenu(), ActionPlaces.MAIN_TOOLBAR, false, true);
        }
    }
}
