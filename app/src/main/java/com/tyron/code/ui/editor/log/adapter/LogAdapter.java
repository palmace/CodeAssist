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
import java.util.Locale;

/**
 * LogAdapter v1.1: "The Interactive Chronicle".
 * v1.1: Añadido soporte para clics simples (Navegación) y mantenido long click (Copiar).
 */
public class LogAdapter extends ListAdapter<DiagnosticWrapper, LogAdapter.LogViewHolder> {

    public interface OnLogItemClickListener {
        void onLogClick(DiagnosticWrapper diagnostic);
        void onLogLongClick(DiagnosticWrapper diagnostic);
    }

    private OnLogItemClickListener mListener;
    private List<DiagnosticWrapper> mLogs = new ArrayList<>();

    public LogAdapter() {
        super(new DiffUtil.ItemCallback<DiagnosticWrapper>() {
            @Override
            public boolean areItemsTheSame(@NonNull DiagnosticWrapper oldItem, @NonNull DiagnosticWrapper newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DiagnosticWrapper oldItem, @NonNull DiagnosticWrapper newItem) {
                return oldItem.getMessage(Locale.getDefault()).equals(newItem.getMessage(Locale.getDefault())) &&
                       oldItem.getKind() == newItem.getKind() &&
                       oldItem.getLineNumber() == newItem.getLineNumber();
            }
        });
    }

    public void setOnLogItemClickListener(OnLogItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DiagnosticWrapper diagnostic = getItem(position);
        holder.bind(diagnostic);
    }

    @Override
    public void submitList(List<DiagnosticWrapper> list) {
        super.submitList(list != null ? new ArrayList<>(list) : null);
        mLogs = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView mMessageText;
        private final TextView mKindText;
        private final TextView mLocationText;
        private final OnLogItemClickListener mListener;

        public LogViewHolder(@NonNull View itemView, OnLogItemClickListener listener) {
            super(itemView);
            mListener = listener;
            mMessageText = itemView.findViewById(R.id.tv_log_message);
            mKindText = itemView.findViewById(R.id.tv_log_kind);
            mLocationText = itemView.findViewById(R.id.tv_log_location);
        }

        public void bind(DiagnosticWrapper diagnostic) {
            mMessageText.setText(diagnostic.getMessage(Locale.getDefault()));
            mKindText.setText(diagnostic.getKind() != null ? diagnostic.getKind().toString() : "");
            mKindText.setVisibility(diagnostic.getKind() != null ? View.VISIBLE : View.GONE);
            
            if (diagnostic.getSource() != null && diagnostic.getLineNumber() >= 0) {
                mLocationText.setVisibility(View.VISIBLE);
                mLocationText.setText(String.format(Locale.getDefault(), "%s:%d", diagnostic.getSource().getName(), diagnostic.getLineNumber()));
            } else {
                mLocationText.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (mListener != null) mListener.onLogClick(diagnostic);
            });
            
            itemView.setOnLongClickListener(v -> {
                if (mListener != null) {
                    mListener.onLogLongClick(diagnostic);
                    return true;
                }
                return false;
            });
        }
    }
}
