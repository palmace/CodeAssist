package com.tyron.code.ui.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.ui.project.adapter.LibraryManagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.MaterialSharedAxis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LibraryManagerFragment v30.5: "The Zenith Arsenal Monitor".
 * v30.5: Implementada visualización en tiempo real del Classpath y AARs explotados.
 * v30.5: Sincronía con el Motor OMNI v44.25.2 para reportar el estado del Arsenal.
 */
public class LibraryManagerFragment extends Fragment {

    private LibraryManagerAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private View mLoadingView;

    public static LibraryManagerFragment newInstance() {
        return new LibraryManagerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.library_manager_fragment, container, false);
        
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        toolbar.setTitle(R.string.menu_library_manager);

        mRecyclerView = root.findViewById(R.id.libraries_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mAdapter = new LibraryManagerAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mLoadingView = root.findViewById(R.id.loading_layout);

        FloatingActionButton fab = root.findViewById(R.id.fab_add_dependency);
        fab.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Operación Zenith: Inyección de Dependencias Maven en desarrollo.", Toast.LENGTH_SHORT).show();
        });

        loadArsenal();
        
        return root;
    }

    private void loadArsenal() {
        if (mLoadingView != null) mLoadingView.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            Project project = ProjectManager.getInstance().getCurrentProject();
            List<File> libraries = new ArrayList<>();
            
            if (project != null) {
                Module module = project.getMainModule();
                if (module != null) {
                    // v30.5: Obtenemos el Classpath resuelto (incluye AARs mapeados a JARs)
                    libraries.addAll(module.getLibraries());
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (mLoadingView != null) mLoadingView.setVisibility(View.GONE);
                    mAdapter.setLibraries(libraries);
                    if (libraries.isEmpty()) {
                        Toast.makeText(getContext(), "Arsenal vacío. Sincronice el proyecto.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}
