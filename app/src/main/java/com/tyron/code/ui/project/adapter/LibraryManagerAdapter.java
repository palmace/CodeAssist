package com.tyron.code.ui.project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.code.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LibraryManagerAdapter v1.2: "The Zenith Arsenal Viewer - Sovereign Sync".
 * v1.2: Sincronizado con OMNI v44.25.14 para visualización de Classpath de rescate.
 * v1.1: Corregido recurso faltante round_folder_24.
 * v1.0: Visualización de librerías locales y globales con detección de tipo (AAR/JAR).
 */
public class LibraryManagerAdapter extends RecyclerView.Adapter<LibraryManagerAdapter.ViewHolder> {

    private final List<File> mLibraries = new ArrayList<>();

    public void setLibraries(List<File> libraries) {
        mLibraries.clear();
        if (libraries != null) {
            mLibraries.addAll(libraries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.library_manager_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File lib = mLibraries.get(position);
        holder.nameText.setText(lib.getName());
        holder.pathText.setText(lib.getParent());
        
        String n = lib.getName().toLowerCase();
        if (n.endsWith(".aar") || lib.isDirectory()) {
            holder.icon.setImageResource(R.drawable.round_folder_24);
        } else {
            holder.icon.setImageResource(R.drawable.ic_baseline_code_24);
        }
    }

    @Override
    public int getItemCount() {
        return mLibraries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView nameText;
        final TextView pathText;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.library_icon);
            nameText = itemView.findViewById(R.id.library_name);
            pathText = itemView.findViewById(R.id.library_path);
        }
    }
}
