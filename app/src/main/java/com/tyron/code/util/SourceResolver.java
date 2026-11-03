package com.tyron.code.util;

import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.kotlin.completion.KotlinEnvironment;
import com.tyron.kotlin.completion.KotlinFile;

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiDocumentManager;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;

/**
 * SourceResolver v1.11: "The Surgical Navigator".
 * v1.11: Integrado con el FileManager del proyecto para obtener contenido en tiempo real del editor 
 * y realizar saltos quirúrgicos sin necesidad de guardar el archivo.
 */
public class SourceResolver {

    public static class JumpLocation {
        public final File file;
        public final int line;
        public final int column;

        public JumpLocation(File file, int line, int column) {
            this.file = file;
            this.line = line;
            this.column = column;
        }
    }

    /**
     * Busca el archivo fuente asociado a una clase (FQN) en el proyecto.
     */
    public static File findSourceFile(Project project, String className) {
        if (project == null || className == null) return null;
        for (Module module : project.getModules()) {
            if (module instanceof AndroidModuleImpl) {
                AndroidModuleImpl am = (AndroidModuleImpl) module;
                Map<String, File> kotlinFiles = am.getKotlinFiles();
                if (kotlinFiles != null && kotlinFiles.containsKey(className)) {
                    return kotlinFiles.get(className);
                }
                File javaFile = am.getJavaFile(className);
                if (javaFile != null) return javaFile;
            }
        }
        return null;
    }

    /**
     * Localiza un miembro específico dentro de una clase para un salto de precisión.
     */
    public static JumpLocation findMemberLocation(Project project, String className, String memberName) {
        File file = findSourceFile(project, className);
        
        // Caso especial Kotlin: Funciones de nivel superior (Top-level)
        if (file == null && className != null && className.endsWith("Kt")) {
             String baseClass = className.substring(0, className.length() - 2);
             file = findSourceFile(project, baseClass);
        }
        
        if (file == null) return null;
        return findMemberInFile(project, file, memberName);
    }

    /**
     * Localiza un miembro dentro de un archivo conocido, prefiriendo el contenido en memoria del editor.
     */
    public static JumpLocation findMemberInFile(Project project, File file, String memberName) {
        if (file == null) return null;
        String content = getFileContent(project, file);
        
        if (file.getName().endsWith(".kt")) {
            return findKotlinMemberLocation(project, file, content, memberName);
        } else if (file.getName().endsWith(".java")) {
            return findJavaMemberLocation(file, content, memberName);
        }
        return new JumpLocation(file, 0, 0);
    }

    private static String getFileContent(Project project, File file) {
        if (project != null) {
            Module module = project.getModule(file);
            if (module != null) {
                return module.getFileManager().getFileContent(file)
                        .map(CharSequence::toString)
                        .orElseGet(() -> readFromFile(file));
            }
        }
        return readFromFile(file);
    }

    private static String readFromFile(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }

    private static JumpLocation findKotlinMemberLocation(Project project, File file, String content, String memberName) {
        Module module = project.getModule(file);
        if (module == null) return new JumpLocation(file, 0, 0);
        
        KotlinEnvironment env = KotlinEnvironment.Companion.get(module);
        if (env == null) return new JumpLocation(file, 0, 0);

        try {
            KotlinFile ktFile = env.updateKotlinFile(file.getAbsolutePath(), content);
            KtFile psiFile = ktFile.getKotlinFile();
            
            KtNamedDeclaration target = findMemberInPsi(psiFile, memberName);
            if (target != null) {
                int offset = target.getTextOffset();
                Document doc = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
                if (doc != null) {
                    int line = doc.getLineNumber(offset);
                    int col = offset - doc.getLineStartOffset(line);
                    return new JumpLocation(file, line, col);
                }
            }
        } catch (Exception ignored) {}
        
        return new JumpLocation(file, 0, 0);
    }

    private static JumpLocation findJavaMemberLocation(File file, String content, String memberName) {
        if (memberName == null || content == null || content.isEmpty()) return new JumpLocation(file, 0, 0);
        
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String lineText = lines[i];
            if (lineText.contains(" " + memberName + "(") || 
                lineText.contains(" " + memberName + " ") ||
                lineText.contains(memberName + " =") ||
                lineText.contains(memberName + ";")) {
                int col = lineText.indexOf(memberName);
                return new JumpLocation(file, i, col);
            }
        }
        return new JumpLocation(file, 0, 0);
    }

    private static KtNamedDeclaration findMemberInPsi(PsiElement container, String name) {
        if (container == null || name == null) return null;
        for (PsiElement child : container.getChildren()) {
            if (child instanceof KtNamedDeclaration && name.equals(((KtNamedDeclaration) child).getName())) {
                return (KtNamedDeclaration) child;
            }
            if (child instanceof KtClass || child instanceof KtObjectDeclaration || child instanceof KtClassBody) {
                KtNamedDeclaration found = findMemberInPsi(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
