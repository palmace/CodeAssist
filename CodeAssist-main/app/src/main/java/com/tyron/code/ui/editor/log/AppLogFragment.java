package com.tyron.code.ui.editor.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.editor.log.adapter.LogAdapter;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.LogExporter;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.common.util.ShareUtils;
import com.tyron.fileeditor.api.FileEditorManager;
import com.tyron.terminal.TerminalSession;
import com.tyron.terminal.TerminalSessionClientAdapter;
import com.tyron.terminal.view.TerminalView;
import com.tyron.terminal.view.TerminalViewClientAdapter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Handler;

public class AppLogFragment extends Fragment
        implements ProjectManager.OnProjectOpenListener {

    /** Only used in IDE Logs **/
    private Handler mHandler;

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
    private TerminalView mTerminalView;
    private TerminalSession mTerminalSession;

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
        // Crear layout principal
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Contenedor para botones (solo para ciertos tipos de log)
        if (id == 0 || id == 1 || id == 2 || id == 3) {
            LinearLayout buttonLayout = new LinearLayout(requireContext());
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setPadding(16, 16, 16, 16);
            buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // Botón Copiar
            mCopyButton = new Button(requireContext());
            mCopyButton.setText("📋 Copiar Logs");
            mCopyButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            mCopyButton.setPadding(8, 8, 8, 8);
            mCopyButton.setOnClickListener(v -> copyLogs());

            // Botón Exportar
            mExportButton = new Button(requireContext());
            mExportButton.setText("💾 Exportar TXT");
            mExportButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            mExportButton.setPadding(8, 8, 8, 8);
            mExportButton.setOnClickListener(v -> exportLogs());

            buttonLayout.addView(mCopyButton);
            buttonLayout.addView(mExportButton);
            rootLayout.addView(buttonLayout);
        }

        // Para IDE Logs (id == 3) usamos TerminalView
        if (id == 3) {
            mTerminalView = new TerminalView(requireContext());
            mTerminalView.setTerminalViewClient(new TerminalViewClientAdapter() {
                // Adaptador vacío por ahora
            });
            mTerminalView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            rootLayout.addView(mTerminalView);
            
            initTerminalSession();
        } else {
            // Para otros logs usamos RecyclerView
            mRecyclerView = new RecyclerView(requireContext());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            
            mAdapter = new LogAdapter();
            mRecyclerView.setAdapter(mAdapter);
            rootLayout.addView(mRecyclerView);
            
            // Observar los logs según el tipo
            observeLogs();
        }

        return rootLayout;
    }

    private void initTerminalSession() {
        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject == null) {
            return;
        }

        try {
            mTerminalSession = new TerminalSession(null, null, new String[0], null,
                    new TerminalSessionClientAdapter() {
                        @Override
                        public void onSessionUpdated(TerminalSession session) {
                            if (mTerminalView != null) {
                                mTerminalView.onSessionUpdated(session);
                            }
                        }
                    });
            mTerminalView.attachSession(mTerminalSession);
            
            // Aquí podrías escribir logs del IDE al terminal
            // mTerminalSession.write("IDE Logs inicializados\n");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void observeLogs() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) {
            return;
        }

        LogViewModel logViewModel = project.getLogViewModel();
        if (logViewModel == null) {
            return;
        }

        switch (id) {
            case 0: // Build Logs
                logViewModel.getBuildLogs().observe(getViewLifecycleOwner(), diagnostics -> {
                    if (mAdapter != null) {
                        mAdapter.submitList(diagnostics);
                    }
                });
                break;
            case 1: // App Logs
                logViewModel.getAppLogs().observe(getViewLifecycleOwner(), diagnostics -> {
                    if (mAdapter != null) {
                        mAdapter.submitList(diagnostics);
                    }
                });
                break;
            case 2: // Diagnostic
                logViewModel.getDiagnosticLogs().observe(getViewLifecycleOwner(), diagnostics -> {
                    if (mAdapter != null) {
                        mAdapter.submitList(diagnostics);
                    }
                });
                break;
            // case 3 es IDE Logs, ya manejado con TerminalView
        }
    }

    private void copyLogs() {
        String logs = getAllLogsAsString();
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "No hay logs para copiar", Toast.LENGTH_SHORT).show();
            return;
        }
        LogExporter.copyToClipboard(requireContext(), logs);
        Toast.makeText(requireContext(), "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void exportLogs() {
        String logs = getAllLogsAsString();
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "No hay logs para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String logType = getLogTypeName();
        LogExporter.saveToFile(requireContext(), logs, logType);
    }

    private String getAllLogsAsString() {
        StringBuilder sb = new StringBuilder();
        
        if (id == 3) {
            // Para IDE Logs, obtener del terminal
            if (mTerminalSession != null) {
                CharSequence text = mTerminalSession.getTerminalBuffer().getTranscriptText();
                sb.append(text);
            }
        } else {
            // Para otros logs, obtener del adapter
            if (mAdapter != null) {
                List<DiagnosticWrapper> logs = mAdapter.getCurrentLogs();
                if (logs != null) {
                    for (DiagnosticWrapper log : logs) {
                        sb.append(log.getMessage()).append("\n");
                        if (log.getKind() != null) {
                            sb.append("Tipo: ").append(log.getKind()).append("\n");
                        }
                        if (log.getSource() != null) {
                            sb.append("Archivo: ").append(log.getSource()).append("\n");
                        }
                        sb.append("---\n");
                    }
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
            case 3: return "IDE";
            default: return "Logs";
        }
    }

    @Override
    public void onProjectOpen(Project project) {
        if (isAdded()) {
            if (id == 3) {
                initTerminalSession();
            } else {
                observeLogs();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ProjectManager.getInstance().removeOnProjectOpenListener(this);
        
        if (mTerminalSession != null) {
            mTerminalSession.finish();
        }
    }
}
