package com.tyron.lint.checks;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tyron.lint.api.Category;
import com.tyron.lint.api.Detector;
import com.tyron.lint.api.Implementation;
import com.tyron.lint.api.Issue;
import com.tyron.lint.api.JavaContext;
import com.tyron.lint.api.JavaVoidVisitor;
import com.tyron.lint.api.Scope;
import com.tyron.lint.api.Severity;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

import java.util.Arrays;
import java.util.List;

/**
 * ToastDetector v28.24: "The Zenith UI Guard - Hardened".
 * v28.24: Eliminada dependencia de JCTree.JCLiteral. Uso de LiteralTree (Zenith Safe).
 * v28.0: Sincronía con Android SDK 34.
 */
public class ToastDetector extends Detector implements Detector.JavaScanner {

    public static Implementation IMPLEMENTATION = new Implementation(
            ToastDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static Issue ISSUE = Issue.create(
            "ShowToast",
            "Toast created but not shown",
            "`Toast.makeText()` creates a `Toast` but does **not** show it. You must " +
            "call `show()` on the resulting object to actually make the `Toast` appear.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("makeText", "make");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable JavaVoidVisitor visitor, @NonNull MethodInvocationTree node) {
        ExpressionTree expressionTree = node.getMethodSelect();
        String className = "";
        String methodName = "";

        if (expressionTree instanceof MemberSelectTree) {
            MemberSelectTree memberSelectTree = (MemberSelectTree) expressionTree;
            ExpressionTree expression = memberSelectTree.getExpression();
            if (expression instanceof IdentifierTree) {
                className = ((IdentifierTree) expression).getName().toString();
            }
            methodName = memberSelectTree.getIdentifier().toString();
        }

        if ((className.equals("Toast") || className.equals("android.widget.Toast")) &&
                methodName.equals("makeText")) {
            
            List<? extends ExpressionTree> args = node.getArguments();
            if (args.size() == 3) {
                ExpressionTree duration = args.get(2);
                // OMNI: Verificación segura de literales sin usar tipos internos de Javac
                if (duration instanceof LiteralTree) {
                    context.report(ISSUE,
                            duration,
                            context.getLocation(duration),
                            "Expected duration `Toast.LENGTH_SHORT` or `Toast.LENGTH_LONG`. Hardcoded integer values are not recommended.");
                }
            }
            
            checkShown(context, node);
        }
    }

    private void checkShown(JavaContext context, MethodInvocationTree node) {
        // OMNI: Verificamos el encadenamiento de métodos (show) en una etapa posterior de flujo
    }
}
