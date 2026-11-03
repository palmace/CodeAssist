package com.tyron.code.ui.editor;

import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListUpdateCallback;
import com.google.android.material.tabs.TabLayout;
import com.tyron.actions.ActionManager;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.actions.util.DataContextUtils;
import com.tyron.code.ui.editor.adapter.PageAdapter;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.util.CoordinatePopupMenu;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.fileeditor.api.FileEditor;
import java.util.List;

public class EditorTabUtil {

    public static void updateTabLayout(@NonNull TabLayout mTabLayout, List<FileEditor> oldList, List<FileEditor> files, MainViewModel viewModel) {
        PageAdapter.getDiff(oldList, files, new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                FileEditor editor = files.get(position);
                TabLayout.Tab tab = mTabLayout.newTab();
                tab.setText(editor.getFile().getName());
                mTabLayout.addTab(tab, position, false);
                
                mTabLayout.post(() -> {
                    View tabView = ((LinearLayout) mTabLayout.getChildAt(0)).getChildAt(position);
                    if (tabView != null) {
                        tabView.setOnLongClickListener(v -> {
                            showTabMenu(v, editor, viewModel);
                            return true;
                        });
                        // También activamos por click normal si el usuario lo prefiere
                        tabView.setOnClickListener(v -> {
                            if (tab.isSelected()) {
                                showTabMenu(v, editor, viewModel);
                            } else {
                                tab.select();
                            }
                        });
                    }
                });
            }

            @Override public void onRemoved(int position, int count) {
                for (int i = 0; i < count; i++) { mTabLayout.removeTabAt(position); }
            }
            @Override public void onMoved(int from, int to) {}
            @Override public void onChanged(int p, int c, Object payload) {}
        });
    }

    private static void showTabMenu(View v, FileEditor editor, MainViewModel viewModel) {
        CoordinatePopupMenu popupMenu = new CoordinatePopupMenu(v.getContext(), v, 0);
        DataContext dataContext = DataContextUtils.getDataContext(v);
        dataContext.putData(CommonDataKeys.FILE, editor.getFile());
        dataContext.putData(CommonDataKeys.FILE_EDITOR_KEY, editor);
        // CRÍTICO: Pasar el ViewModel para que las acciones de cierre sean visibles
        dataContext.putData(MainFragment.MAIN_VIEW_MODEL_KEY, viewModel);
        
        ActionManager.getInstance().fillMenu(dataContext, popupMenu.getMenu(), ActionPlaces.EDITOR_TAB, true, false);
        popupMenu.show(0, -AndroidUtilities.dp(40));
    }
}
