package com.tyron.code.ui.editor.log;

import android.os.Bundle;
import android.os.Handler; [span_7](start_span)// Corregido para Android[span_7](end_span)
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.builder.log.LogViewModel;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.editor.log.adapter.LogAdapter;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.LogExporter;

import java.util.List;

public class AppLogFragment extends Fragment
        implements ProjectManager.OnProjectOpenListener {

    public static AppLogFragment newInstance(int id) {
        AppLogFragment fragment = new AppLogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        fragment.setArguments(bundle);
        return fragment;
    }

    private int id;
    private MainViewModel mMainViewModel;
    private LogAdapter mAdapter;
    private Button mCopyButton;
    private Button mExportButton;
    private RecyclerView mRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt("id");
        }
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        ProjectManager.getInstance().addOnProjectOpenListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        [span_8](start_span)// Botones de funcionalidad[span_8](end_span)
        if (id >= 0 && id <= 3) {
            LinearLayout buttonLayout = new LinearLayout(requireContext());
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setPadding(16, 16, 16, 16);

            mCopyButton = new Button(requireContext());
            mCopyButton.setText("📋 Copiar Logs");
            mCopyButton.setOnClickListener(v -> copyLogs());

            mExportButton = new Button(requireContext());
            mExportButton.setText("💾 Exportar TXT");
            mExportButton.setOnClickListener(v -> exportLogs());

            buttonLayout.addView(mCopyButton);
            buttonLayout.addView(mExportButton);
            rootLayout.addView(buttonLayout);
        }

        mRecyclerView = new RecyclerView(requireContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mAdapter = new LogAdapter();
        mRecyclerView.setAdapter(mAdapter);
        rootLayout.addView(mRecyclerView);
        
        observeLogs();
        return rootLayout;
    }

    private void observeLogs() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return;

        LogViewModel logViewModel = project.getLogViewModel();
        if (logViewModel == null) return;

        switch (id) {
            case 0: 
                logViewModel.getBuildLogs().observe(getViewLifecycleOwner(), logs -> mAdapter.submitList(logs));
                break;
            case 1:
                logViewModel.getAppLogs().observe(getViewLifecycleOwner(), logs -> mAdapter.submitList(logs));
                break;
            case 2:
                logViewModel.getDiagnosticLogs().observe(getViewLifecycleOwner(), logs -> mAdapter.submitList(logs));
                break;
        }
    }

    private void copyLogs() {
        String logs = getAllLogsAsString();
        if (logs.isEmpty()) return;
        LogExporter.copyToClipboard(requireContext(), logs);
    }

    private void exportLogs() {
        String logs = getAllLogsAsString();
        if (logs.isEmpty()) return;
        LogExporter.saveToFile(requireContext(), logs, getLogTypeName());
    }

    private String getAllLogsAsString() {
        StringBuilder sb = new StringBuilder();
        if (mAdapter != null) {
            List<DiagnosticWrapper> logs = mAdapter.getCurrentLogs();
            if (logs != null) {
                for (DiagnosticWrapper log : logs) {
                    sb.append(log.getMessage()).append("\n---\n");
                }
            }
        }
        return sb.toString();
    }

    private String getLogTypeName() {
        switch (id) {
            case 0: return "Build";
            case 1: return "App";
            case 2: return "Diagnostic";
            default: return "Logs";
        }
    }

    @Override
    public void onProjectOpen(Project project) {
        if (isAdded()) observeLogs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ProjectManager.getInstance().removeOnProjectOpenListener(this);
    }
}
