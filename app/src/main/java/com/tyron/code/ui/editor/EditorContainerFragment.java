package com.tyron.code.ui.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.UiUtilsKt;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.fileeditor.api.FileEditor;

import java.util.ArrayList;
import java.util.List;

public class EditorContainerFragment extends Fragment implements
        ProjectManager.OnProjectOpenListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TabLayout mTabLayout;
    private FrameLayout mContainer;
    private BottomSheetBehavior<View> mBehavior;
    private MainViewModel mMainViewModel;
    private EditorContainerViewModel mEditorContainerViewModel;
    private final List<FileEditor> mEditors = new ArrayList<>();

    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mMainViewModel.setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mEditorContainerViewModel = new ViewModelProvider(this).get(EditorContainerViewModel.class);
        requireActivity().getOnBackPressedDispatcher().addCallback((LifecycleOwner) this, mOnBackPressedCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.editor_container_fragment, container, false);
        mContainer = root.findViewById(R.id.viewpager);
        mTabLayout = root.findViewById(R.id.tablayout);
        
        View persistentSheet = root.findViewById(R.id.persistent_sheet);
        mBehavior = BottomSheetBehavior.from(persistentSheet);
        
        ViewCompat.setOnApplyWindowInsetsListener(persistentSheet, (v, insets) -> {
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int totalPeek = AndroidUtilities.dp(45 + 50) + navBarHeight;
            mBehavior.setPeekHeight(totalPeek);
            return insets;
        });

        mBehavior.setGestureInsetBottomIgnored(false); 
        mBehavior.setHalfExpandedRatio(0.3f);
        mBehavior.setFitToContents(false);

        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View p1, int state) { mMainViewModel.setBottomSheetState(state); }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (isAdded()) {
                    Bundle bundle = new Bundle();
                    bundle.putFloat("offset", slideOffset);
                    getChildFragmentManager().setFragmentResult(BottomEditorFragment.OFFSET_KEY, bundle);
                }
            }
        });

        ((FileEditorManagerImpl) FileEditorManagerImpl.getInstance()).attach(mMainViewModel, getChildFragmentManager());
        ProjectManager.getInstance().addOnProjectOpenListener(this);
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mMainViewModel.getFiles().observe(getViewLifecycleOwner(), files -> {
            List<FileEditor> oldList = new ArrayList<>(mEditors);
            mEditors.clear();
            mEditors.addAll(files);
            
            // ACTUALIZACIÓN: Pasamos el MainViewModel para que las acciones de las pestañas funcionen
            EditorTabUtil.updateTabLayout(mTabLayout, oldList, files, mMainViewModel);
            
            if (files.isEmpty()) { 
                mContainer.removeAllViews(); 
                mTabLayout.setVisibility(View.GONE); 
            } else { 
                mTabLayout.setVisibility(View.VISIBLE); 
            }
        });

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMainViewModel.setCurrentPosition(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        mMainViewModel.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != -1 && position < mTabLayout.getTabCount()) {
                TabLayout.Tab tab = mTabLayout.getTabAt(position);
                if (tab != null && !tab.isSelected()) {
                    tab.select();
                }
            }
            
            mContainer.removeAllViews();
            FileEditor current = mMainViewModel.getCurrentFileEditor();
            if (position == -1 || current == null) return;
            UiUtilsKt.removeFromParent(current.getView()); 
            mContainer.addView(current.getView());
        });

        mMainViewModel.getBottomSheetState().observe(getViewLifecycleOwner(), state -> {
            if (state != BottomSheetBehavior.STATE_DRAGGING && state != BottomSheetBehavior.STATE_SETTLING) {
                mBehavior.setState(state);
                mOnBackPressedCallback.setEnabled(state == BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    @Override public void onProjectOpen(com.tyron.builder.project.Project project) {}
    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {}
    @Override public void onDestroy() { super.onDestroy(); ApplicationLoader.getDefaultPreferences().unregisterOnSharedPreferenceChangeListener(this); }
}
