package com.tyron.code.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.tyron.code.util.CustomMutableLiveData;
import com.tyron.fileeditor.api.FileEditor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<List<FileEditor>> mFiles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> mIndexing = new MutableLiveData<>(false);
    private final MutableLiveData<String> mCurrentState = new MutableLiveData<>(null);
    private final MutableLiveData<String> mToolbarTitle = new MutableLiveData<>();
    private final CustomMutableLiveData<Integer> currentPosition = new CustomMutableLiveData<>(-1);
    private final MutableLiveData<Integer> mBottomSheetState = new MutableLiveData<>(BottomSheetBehavior.STATE_COLLAPSED);
    private final MutableLiveData<Boolean> mDrawerState = new MutableLiveData<>(false);

    public MutableLiveData<String> getCurrentState() { return mCurrentState; }
    public void setCurrentState(@Nullable String message) { mCurrentState.setValue(message); }
    public LiveData<Boolean> getDrawerState() { return mDrawerState; }
    public void setDrawerState(boolean isOpen) { mDrawerState.setValue(isOpen); }
    public LiveData<String> getToolbarTitle() { return mToolbarTitle; }
    public void setToolbarTitle(String title) { mToolbarTitle.setValue(title); }
    public MutableLiveData<Boolean> isIndexing() { return mIndexing; }
    public void setIndexing(boolean indexing) { mIndexing.setValue(indexing); }
    public LiveData<List<FileEditor>> getFiles() { return mFiles; }
    public void setFiles(@NonNull List<FileEditor> files) { mFiles.setValue(files); }
    public LiveData<Integer> getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int pos) { currentPosition.setValue(pos, true); }
    
    public LiveData<Integer> getBottomSheetState() { return mBottomSheetState; }
    public void setBottomSheetState(@BottomSheetBehavior.State int state) { mBottomSheetState.setValue(state); }

    @Nullable
    public FileEditor getCurrentFileEditor() {
        List<FileEditor> files = mFiles.getValue();
        Integer pos = currentPosition.getValue();
        if (files == null || pos == null || pos == -1 || pos >= files.size()) return null;
        return files.get(pos);
    }

    public boolean openFile(FileEditor file) {
        setDrawerState(false);
        List<FileEditor> files = mFiles.getValue();
        if (files == null) files = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < files.size(); i++) {
            if (file.getFile().equals(files.get(i).getFile())) { index = i; break; }
        }
        if (index != -1) { setCurrentPosition(index); return true; }
        files.add(file);
        mFiles.setValue(files);
        setCurrentPosition(files.indexOf(file));
        return true;
    }

    public void removeFile(@NonNull File file) {
        List<FileEditor> files = mFiles.getValue();
        if (files == null) return;
        files.removeIf(editor -> file.equals(editor.getFile()));
        mFiles.setValue(files);
    }

    public void removeOthers(File file) {
        List<FileEditor> files = mFiles.getValue();
        if (files == null) return;
        FileEditor find = null;
        for (FileEditor editor : files) {
            if (file.equals(editor.getFile())) { find = editor; break; }
        }
        if (find != null) {
            files.clear();
            files.add(find);
            mFiles.setValue(files);
            setCurrentPosition(0);
        }
    }

    public void clear() {
        mFiles.setValue(new ArrayList<>());
        currentPosition.setValue(-1, true);
    }
}
