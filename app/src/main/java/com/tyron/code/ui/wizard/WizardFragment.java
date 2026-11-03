package com.tyron.code.ui.wizard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.builder.project.Project;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.wizard.adapter.WizardTemplateAdapter;
import com.tyron.code.util.UiUtilsKt;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.common.util.Decompress;
import com.tyron.common.util.SingleTextWatcher;
import com.tyron.completion.progress.ProgressManager;

import org.apache.commons.io.FileUtils;
import javax.lang.model.SourceVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class WizardFragment extends Fragment {

    public interface OnProjectCreatedListener {
        void onProjectCreated(Project project);
    }

    private Button mNavigateButton;
    private Button mExitButton;
    private RecyclerView mRecyclerView;
    private LinearLayout mLoadingLayout;
    private WizardTemplateAdapter mAdapter;

    private View mWizardTemplatesView;
    private View mWizardDetailsView;

    private boolean mLast;
    private WizardTemplate mCurrentTemplate;
    private OnProjectCreatedListener mListener;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onNavigateBack();
        }
    };

    public void setOnProjectCreatedListener(OnProjectCreatedListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        View view = inflater.inflate(R.layout.wizard_fragment, container, false);
        LinearLayout layout = view.findViewById(R.id.setup_wizard_layout);

        mNavigateButton = layout.findViewById(R.id.wizard_next);
        mNavigateButton.setVisibility(View.GONE);
        mNavigateButton.setOnClickListener(this::onNavigateNext);

        mExitButton = layout.findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(v -> onNavigateBack());

        mRecyclerView = layout.findViewById(R.id.template_recyclerview);
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(),
                AndroidUtilities.getRowCount(AndroidUtilities.dp(132))));

        mLoadingLayout = layout.findViewById(R.id.loading_layout);
        mWizardTemplatesView = layout.findViewById(R.id.wizard_templates_layout);
        mWizardDetailsView = layout.findViewById(R.id.wizard_details_layout);

        mAdapter = new WizardTemplateAdapter();
        mRecyclerView.setAdapter(mAdapter);

        initDetailsView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadTemplates();
    }

    private void onNavigateBack() {
        if (!mLast) {
            getParentFragmentManager().popBackStack();
        } else {
            showTemplatesView();
            mLast = false;
        }
    }

    private void onNavigateNext(View view) {
        if (!mLast) {
            if (mCurrentTemplate == null) {
                ApplicationLoader.showToast("Selecciona una plantilla");
                return;
            }
            showDetailsView();
            mLast = true;
        } else {
            if (validateDetails()) {
                createProjectAsync();
            }
        }
    }

    private void showTemplatesView() {
        TransitionManager.beginDelayedTransition((ViewGroup) requireView(), new MaterialSharedAxis(MaterialSharedAxis.X, false));
        mWizardDetailsView.setVisibility(View.GONE);
        mWizardTemplatesView.setVisibility(View.VISIBLE);
        mNavigateButton.setVisibility(View.GONE);
        mExitButton.setText(R.string.wizard_exit);
    }

    private TextInputLayout mNameLayout, mSaveLocationLayout, mPackageNameLayout, mMinSdkLayout, mLanguageLayout;
    private AutoCompleteTextView mLanguageText, mMinSdkText;

    private void initDetailsView() {
        mNameLayout = mWizardDetailsView.findViewById(R.id.til_app_name);
        mPackageNameLayout = mWizardDetailsView.findViewById(R.id.til_package_name);
        mSaveLocationLayout = mWizardDetailsView.findViewById(R.id.til_save_location);
        mLanguageLayout = mWizardDetailsView.findViewById(R.id.til_language);
        mLanguageText = mWizardDetailsView.findViewById(R.id.et_language);
        mMinSdkLayout = mWizardDetailsView.findViewById(R.id.til_min_sdk);
        mMinSdkText = mWizardDetailsView.findViewById(R.id.et_min_sdk);

        mMinSdkText.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, getSdks()));
        
        mNameLayout.getEditText().addTextChangedListener(new SingleTextWatcher() {
            @Override public void afterTextChanged(Editable e) { verifyClassName(e); }
        });
    }

    private boolean validateDetails() {
        String name = mNameLayout.getEditText().getText().toString();
        String pkg = mPackageNameLayout.getEditText().getText().toString();
        
        if (TextUtils.isEmpty(name)) { mNameLayout.setError("Nombre requerido"); return false; }
        if (!pkg.matches("^[a-zA-Z0-9.]+$") || pkg.length() < 5) { mPackageNameLayout.setError("Paquete inválido"); return false; }
        if (TextUtils.isEmpty(mMinSdkText.getText())) { mMinSdkLayout.setError("Selecciona SDK"); return false; }
        
        return mCurrentTemplate != null;
    }

    private void verifyClassName(Editable editable) {
        String currentSavePath = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedPreferenceKeys.PROJECT_SAVE_PATH, 
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CodeAssistProjects").getAbsolutePath());
        mSaveLocationLayout.getEditText().setText(currentSavePath + "/" + editable.toString());
    }

    private void createProjectAsync() {
        final String savePath = mSaveLocationLayout.getEditText().getText().toString();
        final String packageName = mPackageNameLayout.getEditText().getText().toString();
        final String appName = mNameLayout.getEditText().getText().toString();
        final String language = mLanguageText.getText().toString();
        final String minSdkText = mMinSdkText.getText().toString();

        TransitionManager.beginDelayedTransition((ViewGroup) requireView(), new MaterialFadeThrough());
        mWizardTemplatesView.setVisibility(View.GONE);
        mWizardDetailsView.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.VISIBLE);

        ProgressManager.getInstance().runNonCancelableAsync(() -> {
            try {
                createProjectFiles(savePath, packageName, language);
                Project project = new Project(new File(savePath));
                replacePlaceholders(project.getRootFile(), packageName, appName, minSdkText);

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (mListener != null) {
                            mListener.onProjectCreated(project);
                        } else {
                            getParentFragmentManager().popBackStack();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("Wizard", "Error", e);
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        mLoadingLayout.setVisibility(View.GONE);
                        showDetailsView();
                        ApplicationLoader.showToast("Error: " + e.getMessage());
                    });
                }
            }
        });
    }

    @WorkerThread
    private void replacePlaceholders(File file, String pkg, String app, String sdk) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) replacePlaceholders(child, pkg, app, sdk);
        } else {
            String name = file.getName();
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            
            if (name.endsWith(".properties") || name.endsWith(".kts") || name.endsWith(".gradle") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".kt")) {
                String minSdk = sdk.contains("API") ? sdk.split(":")[0].replace("API", "").trim() : "24";
                
                content = content.replaceAll("gradle-[0-9.]+-bin\\.zip", "gradle-8.7-bin.zip")
                                .replace("$packagename", pkg)
                                .replace("$appname", app)
                                .replace("${targetSdkVersion}", "35")
                                .replace("${minSdkVersion}", minSdk);
                
                FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
            }
        }
    }

    @WorkerThread
    private void createProjectFiles(String savePath, String pkg, String lang) throws IOException {
        File root = new File(savePath);
        if (!root.exists() && !root.mkdirs()) throw new IOException("No se pudo crear carpeta");
        
        File templateSrc = new File(mCurrentTemplate.getPath() + "/" + lang.toLowerCase());
        FileUtils.copyDirectory(templateSrc, root);
        
        String srcPath = lang.equalsIgnoreCase("Kotlin") ? "app/src/main/kotlin/" : "app/src/main/java/";
        File pkgDir = new File(root, srcPath + pkg.replace(".", "/"));
        if (!pkgDir.exists()) pkgDir.mkdirs();
        
        File placeholder = new File(root, "app/src/main/java/$packagename");
        if (placeholder.exists()) {
            FileUtils.copyDirectory(placeholder, pkgDir);
            FileUtils.deleteDirectory(placeholder);
        }
        
        File ktPlaceholder = new File(root, "app/src/main/kotlin/$packagename");
        if (ktPlaceholder.exists()) {
            FileUtils.copyDirectory(ktPlaceholder, pkgDir);
            FileUtils.deleteDirectory(ktPlaceholder);
        }
    }

    private List<String> getSdks() {
        return Arrays.asList("API 24: Android 7.0", "API 26: Android 8.0", "API 30: Android 11", "API 33: Android 13", "API 35: Android 15");
    }

    private void showDetailsView() {
        List<String> languages = new ArrayList<>();
        if (mCurrentTemplate.isSupportsJava()) languages.add("Java");
        if (mCurrentTemplate.isSupportsKotlin()) languages.add("Kotlin");
        mLanguageText.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, languages));
        if (!languages.isEmpty()) mLanguageText.setText(languages.get(0), false);

        TransitionManager.beginDelayedTransition((ViewGroup) requireView(), new MaterialSharedAxis(MaterialSharedAxis.X, true));
        mWizardDetailsView.setVisibility(View.VISIBLE);
        mWizardTemplatesView.setVisibility(View.GONE);
        mNavigateButton.setText(R.string.wizard_create);
        mNavigateButton.setVisibility(View.VISIBLE);
        mExitButton.setText(R.string.wizard_previous);
    }

    private void loadTemplates() {
        mLoadingLayout.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WizardTemplate> templates = getTemplates();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mLoadingLayout.setVisibility(View.GONE);
                    if (templates.isEmpty()) {
                        ApplicationLoader.showToast("No se encontraron plantillas.");
                    } else {
                        mRecyclerView.setVisibility(View.VISIBLE);
                        mAdapter.submitList(templates);
                        mAdapter.setOnItemClickListener((item, pos) -> { 
                            mCurrentTemplate = item; 
                            onNavigateNext(null); 
                        });
                    }
                });
            }
        });
    }

    private List<WizardTemplate> getTemplates() {
        List<WizardTemplate> list = new ArrayList<>();
        
        // RUTA 1: Memoria externa (La que vas a crear tú en Download/CodeAssistProjects/templates)
        File externalTemplates = new File(Environment.getExternalStorageDirectory(), "Download/CodeAssistProjects/templates");
        if (externalTemplates.exists()) {
            findTemplatesRecursive(externalTemplates, list);
        }
        
        // RUTA 2: Memoria interna (Donde se extrae el ZIP)
        File internalDir = new File(requireContext().getFilesDir(), "templates");
        if (!internalDir.exists() || internalDir.listFiles() == null || internalDir.listFiles().length == 0) {
             try { FileUtils.deleteDirectory(internalDir); } catch (IOException ignored) {}
             internalDir.mkdirs();
             Decompress.unzipFromAssets(requireContext(), "templates.zip", internalDir.getAbsolutePath());
        }
        
        // Solo buscamos en interna si no hay nada en externa para evitar duplicados
        if (list.isEmpty()) {
            findTemplatesRecursive(internalDir, list);
        }

        // LIMPIEZA DE NOMBRES (Parche de fuerza bruta)
        for (WizardTemplate t : list) {
            if (t.getName().contains("(Kotlin DSL)")) {
                t.setName("Java Modern");
            }
        }

        return list;
    }

    private void findTemplatesRecursive(File directory, List<WizardTemplate> list) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                WizardTemplate t = WizardTemplate.fromFile(f);
                if (t != null) {
                    list.add(t);
                } else {
                    findTemplatesRecursive(f, list);
                }
            }
        }
    }
}
