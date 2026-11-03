package com.tyron.code.ui.file.action.android;

import android.content.Context;
import androidx.annotation.NonNull;

import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.template.CodeTemplate;
import com.tyron.code.template.android.ActivityTemplate;
import com.tyron.code.template.android.FragmentTemplate;
import com.tyron.code.template.android.ServiceTemplate;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.code.ui.file.dialog.CreateClassDialogFragment;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.ProjectUtils;
import com.tyron.common.util.AndroidUtilities;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CreateAndroidClassAction v30.3: "The Zenith Android Creator - Sovereign Signature".
 * v30.3: Sincronía con AndroidUtilities v30.3 (Firma Zenith Unificada).
 * v30.3: Blindaje de Transmutación - Asegura que los nuevos componentes se indexen en el motor OMNI inmediatamente.
 */
public class CreateAndroidClassAction extends FileAction {

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.menu_action_new_android_component);
    }

    @Override
    public boolean isApplicable(File file) {
        if (file.isDirectory()) {
            return ProjectUtils.getPackageName(file) != null;
        }
        return false;
    }

    private List<CodeTemplate> getTemplates() {
        return Arrays.asList(
                new ActivityTemplate(),
                new FragmentTemplate(),
                new ServiceTemplate());
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        TreeFileManagerFragment fragment = (TreeFileManagerFragment) e.getData(CommonDataKeys.FRAGMENT);
        if (fragment == null) return;

        TreeView<TreeFile> treeView = fragment.getTreeView();
        TreeNode<TreeFile> treeNode = e.getData(CommonFileKeys.TREE_NODE);
        if (treeNode == null) return;

        CreateClassDialogFragment dialogFragment =
                CreateClassDialogFragment.newInstance(getTemplates(),
                        Collections.emptyList());
        dialogFragment.show(fragment.getChildFragmentManager(), null);
        dialogFragment.setOnClassCreatedListener((className, template) -> {
            try {
                File createdFile = ProjectManager.createClass(treeNode.getContent().getFile(), className, template);
                if (createdFile == null) throw new IOException("Unable to create Android component");

                // Sincronía con el Índice de Símbolos Zenith
                String packageName = ProjectUtils.getPackageName(treeNode.getContent().getFile());
                String fqn = (packageName == null ? "" : packageName + ".") + className;
                ProjectManager.getInstance().getSymbolIndex().addSymbol(fqn, createdFile);

                TreeNode<TreeFile> newNode = new TreeNode<>(TreeFile.fromFile(createdFile), treeNode.getLevel() + 1);
                treeView.addNode(treeNode, newNode);
                treeView.refreshTreeView();
                
                FileEditorManagerImpl.getInstance().openFile(fragment.requireContext(),
                        createdFile,
                        fileEditor -> fragment.getMainViewModel().openFile(fileEditor));

                Module currentModule = ProjectManager.getInstance().getCurrentProject().getModule(createdFile);
                if (currentModule instanceof JavaModule) {
                    ((JavaModule) currentModule).addJavaFile(createdFile);
                }
            } catch (IOException exception) {
                // OMNI: Firma Zenith Unificada v30.3
                String errorMsg = exception.getMessage();
                AndroidUtilities.showSimpleAlert(fragment.requireContext(),
                        R.string.error, errorMsg != null ? errorMsg : "Component creation failed");
            }
        });
    }
}
