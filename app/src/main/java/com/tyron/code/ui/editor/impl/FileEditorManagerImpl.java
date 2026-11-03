package com.tyron.code.ui.editor.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.code.R;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.fileeditor.api.FileEditor;
import com.tyron.fileeditor.api.FileEditorManager;
import com.tyron.fileeditor.api.FileEditorProvider;
import com.tyron.fileeditor.api.TextEditor;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * FileEditorManagerImpl v1.2: "The Precision Switcher".
 * v1.2: Implementado el salto de cursor en archivos ya abiertos.
 */
public class FileEditorManagerImpl extends FileEditorManager {

    private static volatile FileEditorManagerImpl sInstance = null;

    public static synchronized FileEditorManagerImpl getInstance() {
        if (sInstance == null) {
            sInstance = new FileEditorManagerImpl();
        }
        return sInstance;
    }

    private MainViewModel mViewModel;
    private FragmentManager mFragmentManager;

    FileEditorManagerImpl() {

    }

    public void attach(MainViewModel mainViewModel, FragmentManager fragmentManager) {
        mViewModel = mainViewModel;
        mFragmentManager = fragmentManager;
    }

    @Override
    public void openFile(@NonNull Context context, File file, Consumer<FileEditor> callback) {
        checkAttached();
        FileEditor[] fileEditors = getFileEditors(context, file);
        openChooser(context, fileEditors, callback);
    }

    @Override
    public void openFile(@NonNull Context context, @NonNull File file, int line, int column) {
        checkAttached();

        List<FileEditor> openFiles = mViewModel.getFiles().getValue();
        if (openFiles != null) {
            for (FileEditor editor : openFiles) {
                if (editor.getFile().equals(file)) {
                    mViewModel.openFile(editor);
                    
                    // OMNI: Si el fragmento es un editor de texto, movemos el cursor
                    if (editor instanceof CodeEditorFragment) {
                        ((CodeEditorFragment) editor).setCursorPosition(line, column);
                    }
                    return;
                }
            }
        }

        // Si no está abierto, creamos una nueva instancia con posición
        CodeEditorFragment fragment = CodeEditorFragment.newInstance(file, line, column);
        openFileEditor(fragment);
    }

    @NonNull
    @Override
    public FileEditor[] openFile(@NonNull Context context, @NonNull File file, boolean focus) {
        checkAttached();
        FileEditor[] editors = getFileEditors(context, file);
        openChooser(context, editors, this::openFileEditor);
        return editors;
    }

    @Override
    public FileEditor[] getFileEditors(Context context, @NonNull File file) {
        FileEditorProvider[] providers = FileEditorProviderManagerImpl.getInstance().getProviders(file);
        FileEditor[] editors = new FileEditor[providers.length];
        for (int i = 0; i < providers.length; i++) {
            FileEditor editor = providers[i].createEditor(context, file);
            editors[i] = editor;
        }
        return editors;
    }

    @Override
    public void openFileEditor(@NonNull FileEditor fileEditor) {
        mViewModel.openFile(fileEditor);
    }

    @Override
    public void closeFile(@NonNull File file) {
        mViewModel.removeFile(file);
    }

    @Override
    public void openChooser(Context context, FileEditor[] fileEditors, Consumer<FileEditor> callback) {
        if (fileEditors.length == 0) return;
        if (fileEditors.length > 1) {
            CharSequence[] items = Arrays.stream(fileEditors).map(FileEditor::getName).toArray(String[]::new);
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.file_editor_selection_title)
                    .setItems(items, (__, which) -> callback.accept(fileEditors[which]))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            callback.accept(fileEditors[0]);
        }
    }

    private void checkAttached() {
        if (mViewModel == null || mFragmentManager == null) {
            throw new IllegalStateException("File editor manager is not yet attached to a ViewModel");
        }
    }
}
