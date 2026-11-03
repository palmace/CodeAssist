package com.tyron.code.ui.file.action.kotlin;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.api.KotlinModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.template.CodeTemplate;
import com.tyron.code.template.kotlin.KotlinAbstractClassTemplate;
import com.tyron.code.template.kotlin.KotlinClassTemplate;
import com.tyron.code.template.kotlin.KotlinInterfaceTemplate;
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
 * CreateKotlinClassAction v30.3: "The Zenith Kotlin Creator - Sovereign Signature".
 * v30.3: Sincronía con AndroidUtilities v30.3 (Zenith).
 * v30.3: Blindaje de Transmutación - Asegura la indexación inmediata en el Motor de Análisis Kotlin.
 */
public class CreateKotlinClassAction extends FileAction {

    public CreateKotlinClassAction() {
    }

    @Override
    public void update(@NonNull AnActionEvent e) {
        super.update(e);
        Context context = e.getData(CommonDataKeys.CONTEXT);
        if (context != null) {
            e.getPresentation().setIcon(ContextCompat.getDrawable(context, R.drawable.ic_kotlin_logo));
        }
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.menu_action_new_kotlin_class);
    }

    @Override
    public boolean isApplicable(File file) {
        if (file.isDirectory()) {
            return ProjectUtils.getPackageName(file) != null;
        }
        return false;
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
                File createdFile = ProjectManager.createClass(
                        treeNode.getContent().getFile(),
                        className, template
                );
                
                if (createdFile == null) throw new IOException("Unable to create Kotlin file");

                // Actualización del Índice de Símbolos Zenith
                String packageName = ProjectUtils.getPackageName(treeNode.getContent().getFile());
                String fqn = (packageName == null ? "" : packageName + ".") + className;
                ProjectManager.getInstance().getSymbolIndex().addSymbol(fqn, createdFile);

                TreeNode<TreeFile> newNode = new TreeNode<>(
                        TreeFile.fromFile(createdFile),
                        treeNode.getLevel() + 1
                );

                treeView.addNode(treeNode, newNode);
                treeView.refreshTreeView();
                
                FileEditorManagerImpl.getInstance().openFile(fragment.requireContext(),
                        createdFile,
                        fileEditor -> fragment.getMainViewModel().openFile(fileEditor));

                Module currentModule = ProjectManager.getInstance()
                        .getCurrentProject()
                        .getModule(createdFile);
                if (currentModule instanceof KotlinModule) {
                    ((KotlinModule) currentModule).addKotlinFile(createdFile);
                }
            } catch (IOException exception) {
                // OMNI: Firma Zenith Unificada v30.3
                String msg = exception.getMessage();
                AndroidUtilities.showSimpleAlert(fragment.requireContext(), 
                        R.string.error, msg != null ? msg : "Kotlin creation failed");
            }
        });
    }

    private List<CodeTemplate> getTemplates() {
        return Arrays.asList(
                new KotlinClassTemplate(),
                new KotlinInterfaceTemplate(),
                new KotlinAbstractClassTemplate());
    }
}
