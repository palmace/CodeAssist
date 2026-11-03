package com.tyron.code.ui.editor.action.text;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.editor.Editor;
import com.tyron.kotlin.completion.KotlinDefinitionResolver;
import com.tyron.kotlin.completion.KotlinEnvironment;
import com.tyron.kotlin.completion.KotlinFile;
import com.tyron.code.util.SourceResolver;
import com.tyron.completion.java.parse.CompilationInfo;
import com.tyron.completion.java.action.FindCurrentPath;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ElementKind;
import java.io.File;

/**
 * GotoDeclarationAction v1.7: "The Omni-Link Surgical".
 * v1.7: Corregido acceso a Caret (getStartLine/getStartColumn).
 */
public class GotoDeclarationAction extends AnAction {

    public static final String ID = "GotoDeclaration";

    public GotoDeclarationAction() {
        super("Ir a declaración");
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Context context = event.getData(CommonDataKeys.CONTEXT);
        if (editor == null || context == null) return;

        Project project = event.getData(CommonDataKeys.PROJECT);
        File currentFile = editor.getCurrentFile();
        if (project == null || currentFile == null) return;

        Module module = project.getModule(currentFile);
        if (module == null) return;

        int line = editor.getCaret().getStartLine();
        int col = editor.getCaret().getStartColumn();

        if (currentFile.getName().endsWith(".kt")) {
            handleKotlinNavigation(context, editor, module, line, col);
        } else if (currentFile.getName().endsWith(".java")) {
            handleJavaNavigation(context, editor, project, line, col);
        }
    }

    private void handleKotlinNavigation(Context context, Editor editor, Module module, int line, int col) {
        KotlinEnvironment env = KotlinEnvironment.Companion.get(module);
        if (env == null) return;

        KotlinDefinitionResolver resolver = new KotlinDefinitionResolver(env);
        KotlinFile ktFile = env.updateKotlinFile(editor.getCurrentFile().getAbsolutePath(), editor.getContent().toString());
        
        KotlinDefinitionResolver.DefinitionLocation loc = resolver.resolveDefinition(ktFile, line, col);
        if (loc != null) {
            FileEditorManagerImpl.getInstance().openFile(context, loc.getFile(), loc.getLine(), loc.getColumn());
        }
    }

    private void handleJavaNavigation(Context context, Editor editor, Project project, int line, int col) {
        CompilationInfo info = CompilationInfo.get(project, editor.getCurrentFile());
        if (info == null) return;

        try {
            int offset = editor.getCharIndex(line, col);
            Trees trees = info.getTrees();
            FindCurrentPath scanner = new FindCurrentPath(trees);
            TreePath path = scanner.scan(info.getCompilationUnit(), (long) offset);
            
            if (path != null) {
                Element element = trees.getElement(path);
                if (element != null) {
                    TypeElement typeElement = null;
                    String memberName = null;

                    if (element instanceof TypeElement) {
                        typeElement = (TypeElement) element;
                    } else {
                        // Si no es una clase, buscamos la clase envolvente y guardamos el nombre del miembro
                        Element enclosing = element.getEnclosingElement();
                        if (enclosing instanceof TypeElement) {
                            typeElement = (TypeElement) enclosing;
                            memberName = element.getSimpleName().toString();
                        }
                    }
                    
                    if (typeElement != null) {
                        String fqn = typeElement.getQualifiedName().toString();
                        // Usamos el Surgical Navigator para encontrar el archivo y la posición exacta
                        SourceResolver.JumpLocation loc = SourceResolver.findMemberLocation(project, fqn, memberName);
                        if (loc != null) {
                            FileEditorManagerImpl.getInstance().openFile(context, loc.file, loc.line, loc.column);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void update(@NonNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabled(editor != null && editor.getCurrentFile() != null);
    }
}
