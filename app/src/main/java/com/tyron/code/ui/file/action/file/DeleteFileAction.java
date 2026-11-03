package com.tyron.code.ui.file.action.file;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.FileManager;
import com.tyron.code.event.FileDeletedEvent;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.file.tree.TreeUtil;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.common.util.StringSearch;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kotlin.io.FileWalkDirection;
import kotlin.io.FilesKt;

/**
 * DeleteFileAction v30.4: "The Zenith Eraser - Sovereign Signature".
 * v30.4: Sincronía con AndroidUtilities v30.4 (Zenith).
 * v30.4: Blindaje de Purga - Asegura el cierre de descriptores de archivo antes de la eliminación física.
 */
public class DeleteFileAction extends FileAction {

    public static final String ID = "fileManagerDeleteFileAction";

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.dialog_delete);
    }

    @Override
    public boolean isApplicable(File file) {
        return true;
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        TreeFileManagerFragment fragment =
                (TreeFileManagerFragment) e.getRequiredData(CommonDataKeys.FRAGMENT);
        TreeView<TreeFile> treeView = fragment.getTreeView();
        TreeNode<TreeFile> currentNode = e.getRequiredData(CommonFileKeys.TREE_NODE);
        File fileToDelete = currentNode.getValue().getFile();

        String message = String.format(fragment.getString(R.string.dialog_confirm_delete),
                fileToDelete.getName());
        
        // Sincronía Zenith v30.4 (Firma absoluta de 8 argumentos)
        AndroidUtilities.showTacticalAlert(
                fragment.requireActivity(),
                fragment.getString(R.string.dialog_delete),
                message,
                fragment.getString(R.string.dialog_delete),
                () -> performDeletion(fragment, treeView, currentNode),
                fragment.getString(android.R.string.cancel),
                null,
                null
        );
    }

    private void performDeletion(TreeFileManagerFragment fragment, TreeView<TreeFile> treeView, TreeNode<TreeFile> currentNode) {
        ProgressManager progress = ProgressManager.getInstance();
        progress.runNonCancelableAsync(() -> {
            boolean success = deleteFiles(currentNode, fragment);
            progress.runLater(() -> {
                if (fragment.isDetached() || fragment.getContext() == null) return;
                
                if (success) {
                    treeView.deleteNode(currentNode);
                    TreeUtil.updateNode(currentNode.getParent());
                    treeView.refreshTreeView();
                    FileEditorManagerImpl.getInstance().closeFile(currentNode.getValue().getFile());
                } else {
                    // OMNI: Firma Zenith Unificada v30.4 (Contexto de Activity + ResID)
                    AndroidUtilities.showSimpleAlert(fragment.requireActivity(), 
                            R.string.error, R.string.error_dir_access);
                }
            });
        });
    }

    private boolean deleteFiles(TreeNode<TreeFile> currentNode, TreeFileManagerFragment fragment) {
        List<File> deletedFiles = new ArrayList<>();
        File currentFile = currentNode.getContent().getFile();
        
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return false;

        FilesKt.walk(currentFile, FileWalkDirection.TOP_DOWN).iterator().forEachRemaining(file -> {
            Module module = project.getModule(file);
            if (module == null) return;

            if (file.getName().endsWith(".java") || file.getName().endsWith(".kt")) {
                ProgressManager.getInstance().runLater(() ->
                        fragment.getMainViewModel().removeFile(file));

                if (module instanceof JavaModule && file.getName().endsWith(".java")) {
                    String packageName = StringSearch.packageName(file);
                    if (packageName != null) {
                        packageName += "." + file.getName().substring(0, file.getName().lastIndexOf("."));
                        ((JavaModule) module).removeJavaFile(packageName);
                    }
                }
            }

            // Purga de descriptores Zenith
            ProgressManager.getInstance().runLater(() -> {
                FileManager fileManager = module.getFileManager();
                if (fileManager.isOpened(file)) {
                    fileManager.closeFileForSnapshot(file);
                }
            });

            deletedFiles.add(file);
        });
        
        try {
            FileUtils.forceDelete(currentFile);
        } catch (IOException e) {
            return false;
        }

        for (File deletedFile : deletedFiles) {
            project.getEventManager().dispatchEvent(new FileDeletedEvent(deletedFile));
        }

        return true;
    }
}
