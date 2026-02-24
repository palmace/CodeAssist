package com.tyron.code.ui.editor.log.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.code.R;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends ListAdapter<DiagnosticWrapper, LogAdapter.LogViewHolder> {

    private List<DiagnosticWrapper> mLogs = new ArrayList<>();

    public LogAdapter() {
        super(new DiffUtil.ItemCallback<DiagnosticWrapper>() {
            @Override
            public boolean areItemsTheSame(@NonNull DiagnosticWrapper oldItem,
                                           @NonNull DiagnosticWrapper newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull DiagnosticWrapper oldItem,
                                              @NonNull DiagnosticWrapper newItem) {
                return oldItem.getMessage().equals(newItem.getMessage()) &&
                       oldItem.getKind() == newItem.getKind() &&
                       oldItem.getLineNumber() == newItem.getLineNumber();
            }
        });
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DiagnosticWrapper diagnostic = getItem(position);
        holder.bind(diagnostic);
    }

    @Override
    public void submitList(List<DiagnosticWrapper> list) {
        super.submitList(list);
        if (list != null) {
            mLogs = new ArrayList<>(list);
        } else {
            mLogs = new ArrayList<>();
        }
    }

    /**
     * Obtiene la lista actual de logs
     * @return Lista de DiagnosticWrapper
     */
    public List<DiagnosticWrapper> getCurrentLogs() {
        return mLogs;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {

        private final TextView mMessageText;
        private final TextView mKindText;
        private final TextView mLocationText;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            mMessageText = itemView.findViewById(R.id.tv_log_message);
            mKindText = itemView.findViewById(R.id.tv_log_kind);
            mLocationText = itemView.findViewById(R.id.tv_log_location);
        }

        public void bind(DiagnosticWrapper diagnostic) {
            mMessageText.setText(diagnostic.getMessage());
            
            if (diagnostic.getKind() != null) {
                mKindText.setVisibility(View.VISIBLE);
                mKindText.setText("Tipo: " + diagnostic.getKind().toString());
            } else {
                mKindText.setVisibility(View.GONE);
            }
            
            if (diagnostic.getSource() != null && diagnostic.getLineNumber() >= 0) {
                mLocationText.setVisibility(View.VISIBLE);
                String location = diagnostic.getSource() + ":" + diagnostic.getLineNumber();
                mLocationText.setText(location);
            } else {
                mLocationText.setVisibility(View.GONE);
            }
        }
    }
}
