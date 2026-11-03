package com.tyron.code.ui.editor.log;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.builder.BuildModule;
import com.tyron.builder.log.LogViewModel;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.editor.log.adapter.LogAdapter;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.common.util.AndroidUtilities;

import java.util.List;
import java.util.Locale;

/**
 * AppLogFragment v43.2: "The Zenith Forensic UI - Sovereign Sync".
 * v43.2: Implementado LongClick para copia selectiva de líneas individuales.
 */
public class AppLogFragment extends Fragment
        implements ProjectManager.OnProjectOpenListener {

    private int id;
    private LogViewModel mLogViewModel;
    private LogAdapter mAdapter;
    private RecyclerView mRecyclerView;

    public static AppLogFragment newInstance(int id) {
        AppLogFragment fragment = new AppLogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) id = getArguments().getInt("id");
        mLogViewModel = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(Color.parseColor("#121212"));

        mRecyclerView = new RecyclerView(requireContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        mAdapter = new LogAdapter();
        mAdapter.setOnLogItemClickListener(new LogAdapter.OnLogItemClickListener() {
            @Override
            public void onLogClick(DiagnosticWrapper diagnostic) {
                // Navegación (implementación futura si es necesario)
            }

            @Override
            public void onLogLongClick(DiagnosticWrapper diagnostic) {
                String message = diagnostic.getMessage(Locale.getDefault());
                AndroidUtilities.copyToClipboardSovereign(message, true);
                AndroidUtilities.showToast("Línea copiada al portapapeles.");
            }
        });

        mRecyclerView.setAdapter(mAdapter);
        root.addView(mRecyclerView);

        // --- CONTENEDOR DE BOTONES ZENITH (TOP-RIGHT) ---
        LinearLayout btnContainer = new LinearLayout(requireContext());
        btnContainer.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        containerLp.gravity = Gravity.TOP | Gravity.END;
        btnContainer.setLayoutParams(containerLp);
        
        AndroidUtilities.setMargins(btnContainer, 0, 12, 12, 0);

        // BOTÓN 1: TELEMETRÍA TÉRMICA (FORENSIC)
        ImageButton tempBtn = createStyledButton(R.drawable.ic_baseline_menu_24, "#E91E63");
        tempBtn.setOnClickListener(v -> {
            String status = "[FORENSIC-USER] " + BuildModule.getForensicStatus();
            mLogViewModel.d(id, status);
            AndroidUtilities.showToast("Telemetría Zenith inyectada.");
        });
        btnContainer.addView(tempBtn);

        // ESPACIADOR TÁCTICO
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(8), 1));
        btnContainer.addView(spacer);

        // BOTÓN 2: COPIA MASIVA UNIFICADA
        ImageButton copyBtn = createStyledButton(R.drawable.round_content_copy_20, "#6200EE");
        copyBtn.setOnClickListener(v -> copyAllLogsToClipboard());
        btnContainer.addView(copyBtn);

        root.addView(btnContainer);
        return root;
    }

    private ImageButton createStyledButton(int resId, String color) {
        ImageButton btn = new ImageButton(requireContext());
        btn.setImageResource(resId);
        btn.setColorFilter(Color.WHITE);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#333333"));
        gd.setStroke(AndroidUtilities.dp(2), Color.parseColor(color));
        btn.setBackground(gd);
        int p = AndroidUtilities.dp(10);
        btn.setPadding(p, p, p, p);
        btn.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(44)));
        btn.setElevation(AndroidUtilities.dp(8));
        return btn;
    }

    private void copyAllLogsToClipboard() {
        List<DiagnosticWrapper> logs = mLogViewModel.getLogs(id).getValue();
        if (logs == null || logs.isEmpty()) return;
        
        StringBuilder sb = new StringBuilder("--- OMNI LOG ZENITH ---\n");
        for (DiagnosticWrapper d : logs) {
            if (d != null) sb.append(d.getMessage(Locale.getDefault())).append("\n");
        }
        
        // OMNI v30.0: Protocolo de Firma Inequívoca
        AndroidUtilities.copyToClipboardSovereign(sb.toString(), true);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ProjectManager.getInstance().addOnProjectOpenListener(this);
        observeLogs();
    }

    private void observeLogs() {
        mLogViewModel.getLogs(id).observe(getViewLifecycleOwner(), logs -> {
            if (mAdapter != null) {
                mAdapter.submitList(logs);
                if (logs != null && !logs.isEmpty()) mRecyclerView.smoothScrollToPosition(logs.size() - 1);
            }
        });
    }

    @Override public void onProjectOpen(Project project) { if (isAdded()) observeLogs(); }
    @Override public void onDestroyView() { super.onDestroyView(); ProjectManager.getInstance().removeOnProjectOpenListener(this); }
}
