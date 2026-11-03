package com.tyron.completion.java.util;

import com.tyron.completion.java.compiler.ReflectionUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.sun.tools.javac.util.JCDiagnostic;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.completion.java.action.FindMethodDeclarationAt;
import com.tyron.completion.java.compiler.CompileTask;
import com.tyron.editor.CharPosition;
import com.tyron.editor.Editor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.ClientCodeWrapper;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DiagnosticUtil v29.0: "The Zenith Forensic Core".
 * v29.0: Sincronía con Motor Hybrid Pro V8 para localización de errores.
 * v29.0: Refuerzo contra IndexOutOfBoundsException en mapeo de CharPosition.
 * v29.0: Optimización de filtrado de diagnósticos por cursor con Sellado Forense.
 */
public class DiagnosticUtil {

    private static final String TAG = "DiagnosticUtil";
    private static final Pattern UNREPORTED_EXCEPTION =
            Pattern.compile("unreported exception (" + "(\\w+\\.)*\\w+)");

    /**
     * Ancla las coordenadas de un diagnóstico al buffer actual del editor.
     * v29.0: Implementa validación de límites para evitar crash en ediciones rápidas.
     */
    public static void setLineAndColumn(DiagnosticWrapper diagnostic, Editor editor) {
        if (diagnostic == null || editor == null) return;
        
        try {
            int textLength = editor.getContent().length();
            
            // Procesamiento de Posición de Inicio
            if (diagnostic.getStartLine() <= -1 && diagnostic.getStartPosition() >= 0) {
                int startPos = (int) diagnostic.getStartPosition();
                if (startPos <= textLength) {
                    CharPosition start = editor.getCharPosition(startPos);
                    diagnostic.setStartLine(start.getLine() + 1);
                    diagnostic.setStartColumn(start.getColumn());
                    diagnostic.setLineNumber(start.getLine() + 1);
                    diagnostic.setColumnNumber(start.getColumn());
                }
            }
            
            // Procesamiento de Posición Final
            if (diagnostic.getEndLine() <= -1 && diagnostic.getEndPosition() > 0) {
                int endPos = (int) diagnostic.getEndPosition();
                if (endPos <= textLength) {
                    CharPosition end = editor.getCharPosition(endPos);
                    diagnostic.setEndLine(end.getLine() + 1);
                    diagnostic.setEndColumn(end.getColumn());
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Zenith Forensic: Deriva de sincronía detectada en diagnóstico: " + e.getMessage());
        }
    }

    public static class MethodPtr {
        public String className, methodName;
        public String[] erasedParameterTypes;
        public ExecutableElement method;

        public MethodPtr(JavacTask task, ExecutableElement method) {
            this.method = method;
            Types types = task.getTypes();
            TypeElement parent = (TypeElement) method.getEnclosingElement();
            className = parent.getQualifiedName().toString();
            methodName = method.getSimpleName().toString();
            erasedParameterTypes = new String[method.getParameters().size()];
            for (int i = 0; i < erasedParameterTypes.length; i++) {
                VariableElement param = method.getParameters().get(i);
                TypeMirror type = param.asType();
                TypeMirror erased = types.erasure(type);
                erasedParameterTypes[i] = erased.toString();
            }
        }

        @NonNull
        @Override
        public String toString() {
            return "MethodPtr{" + "className='" + className + '\'' + ", methodName='" + methodName + '\'' + ", erasedParameterTypes=" + Arrays.toString(erasedParameterTypes) + ", method=" + method + '}';
        }
    }

    @Nullable
    public static Diagnostic<? extends JavaFileObject> getDiagnostic(CompileTask task, long cursor) {
        return getDiagnostic(task.diagnostics, cursor);
    }

    public static Diagnostic<? extends JavaFileObject> getDiagnostic(List<Diagnostic<? extends JavaFileObject>> diagnostics, long cursor) {
        if (diagnostics == null) return null;
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getStartPosition() <= cursor && cursor < diagnostic.getEndPosition()) {
                return diagnostic;
            }
        }
        return null;
    }

    public static JCDiagnostic getJCDiagnostic(List<JCDiagnostic> diagnostics, long cursor) {
        if (diagnostics == null) return null;
        for (JCDiagnostic diagnostic : diagnostics) {
            if (diagnostic.getStartPosition() <= cursor && cursor < diagnostic.getEndPosition()) {
                return diagnostic;
            }
        }
        return null;
    }

    public static DiagnosticWrapper getDiagnosticWrapper(List<DiagnosticWrapper> diagnostics, long start, long end) {
        if (diagnostics == null) return null;
        DiagnosticWrapper current = null;
        for (DiagnosticWrapper diagnostic : diagnostics) {
            if (diagnostic.getStartPosition() <= start && end <= diagnostic.getEndPosition()) {
                if (current == null || (diagnostic.getStartPosition() < current.getStartPosition() && diagnostic.getEndPosition() > current.getEndPosition())) {
                    current = diagnostic;
                }
            }
        }
        if (current != null) return current;
        
        current = getDiagnosticWrapper(diagnostics, start);
        if (current != null) return current;
        
        return getDiagnosticWrapper(diagnostics, end);
    }

    @Nullable
    public static DiagnosticWrapper getDiagnosticWrapper(List<DiagnosticWrapper> diagnostics, long cursor) {
        if (diagnostics == null) return null;
        for (DiagnosticWrapper diagnostic : diagnostics) {
            if (diagnostic.getStartPosition() <= cursor && cursor <= diagnostic.getEndPosition()) {
                return diagnostic;
            }
        }
        return null;
    }

    @Nullable
    public static DiagnosticWrapper getXmlDiagnosticWrapper(List<DiagnosticWrapper> diagnostics, int line) {
        if (diagnostics == null) return null;
        for (DiagnosticWrapper diagnostic : diagnostics) {
            if (diagnostic.getLineNumber() - 1 == line) {
                return diagnostic;
            }
        }
        return null;
    }

    @Nullable
    public static ClientCodeWrapper.DiagnosticSourceUnwrapper getDiagnosticSourceUnwrapper(Diagnostic<?> diagnostic) {
        if (diagnostic instanceof DiagnosticWrapper) {
            Object extra = ((DiagnosticWrapper) diagnostic).getExtra();
            if (extra instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper) {
                return (ClientCodeWrapper.DiagnosticSourceUnwrapper) extra;
            }
        }
        if (diagnostic instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper) {
            return (ClientCodeWrapper.DiagnosticSourceUnwrapper) diagnostic;
        }
        return null;
    }

    @NonNull
    public static MethodPtr findMethod(CompileTask task, long position) {
        Trees trees = ReflectionUtil.getTreesInstance(task.task);
        Tree tree = new FindMethodDeclarationAt(task.task).scan(task.root(), position);
        TreePath path = trees.getPath(task.root(), tree);
        ExecutableElement method = (ExecutableElement) trees.getElement(path);
        return new MethodPtr(task.task, method);
    }

    @NonNull
    public static String extractExceptionName(String message) {
        if (message == null) return "";
        Matcher matcher = UNREPORTED_EXCEPTION.matcher(message);
        if (!matcher.find()) return "";
        String group = matcher.group(1);
        return group != null ? group : "";
    }
}
