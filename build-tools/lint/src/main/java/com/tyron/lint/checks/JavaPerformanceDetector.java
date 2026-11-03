package com.tyron.lint.checks;

import android.util.Log;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.tyron.lint.api.Category;
import com.tyron.lint.api.Detector;
import com.tyron.lint.api.Implementation;
import com.tyron.lint.api.Issue;
import com.tyron.lint.api.JavaContext;
import com.tyron.lint.api.JavaVoidVisitor;
import com.tyron.lint.api.Scope;
import com.tyron.lint.api.Severity;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;

/**
 * JavaPerformanceDetector v28.33: "The Zenith Speed Anchor - Absolute Shield".
 * v28.33: Blindaje total mediante desacoplamiento reflexivo de Trees para Android 15.
 * v28.20: Blindaje de acceso mediante el puente seguro de CompileTask.
 */
public class JavaPerformanceDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            JavaPerformanceDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue PAINT_ALLOC = Issue.create(
            "DrawAllocation",
            "Memory allocations within drawing code",
            "Avoid allocating objects during a drawing or layout operation.",
            Category.PERFORMANCE,
            9,
            Severity.WARNING,
            IMPLEMENTATION);

    static final String ON_MEASURE = "onMeasure";                           
    static final String ON_DRAW = "onDraw";                                 
    static final String ON_LAYOUT = "onLayout";                             
    private static final String CANVAS = "Canvas";                          

    @Override
    public JavaVoidVisitor getVisitor(JavaContext context) {
        return new Visitor(context);
    }

    @Override
    public List<Class<? extends Tree>> getApplicableTypes() {
        return Arrays.asList(
                MethodTree.class,
                MethodInvocationTree.class
        );
    }

    private static class Visitor extends JavaVoidVisitor {

        private final JavaContext mContext;
        private boolean mFlagAllocations;

        public Visitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public Void visitMethod(MethodTree methodTree, Void unused) {
            mFlagAllocations = isBlockedAllocationMethod(methodTree);
            return super.visitMethod(methodTree, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            // OMNI: Puente reflexivo para evadir restricciones de Android 15
            Object trees = (mContext.getCompileTask() != null) ? mContext.getCompileTask().getTrees() : null;
            if (trees == null) return super.visitNewClass(node, unused);

            try {
                Method getPathMethod = trees.getClass().getMethod("getPath", com.sun.source.tree.CompilationUnitTree.class, com.sun.source.tree.Tree.class);
                TreePath path = (TreePath) getPathMethod.invoke(trees, mContext.getCompilationUnit(), node);
                
                if (path != null && mFlagAllocations && !(path.getParentPath().getLeaf() instanceof ThrowTree)) {
                    Tree method = node;
                    while (method != null) {
                        if (method instanceof MethodTree) break;
                        TreePath currentPath = (TreePath) getPathMethod.invoke(trees, mContext.getCompilationUnit(), method);
                        method = (currentPath != null && currentPath.getParentPath() != null) ? 
                                 currentPath.getParentPath().getLeaf() : null;
                    }

                    if (method instanceof MethodTree && isBlockedAllocationMethod((MethodTree) method)
                            && !isLazilyInitialized(node)) {
                        reportAllocation(node);
                    }
                }
            } catch (Throwable ignored) {}
            
            return super.visitNewClass(node, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            if (mFlagAllocations && node.getMethodSelect() != null) {
                String methodName = getMethodName(node);
                if (methodName.equals("createBitmap") || methodName.equals("createScaledBitmap")) {
                    if (node.getMethodSelect() instanceof MemberSelectTree) {
                        ExpressionTree expression = ((MemberSelectTree) node.getMethodSelect()).getExpression();
                        if (expression != null && expression.toString().endsWith("Bitmap")) {
                             if (!isLazilyInitialized(node)) reportAllocation(node);
                        }
                    }
                } else if (methodName.equals("getClipBounds") && node.getArguments().isEmpty()) {
                    mContext.report(PAINT_ALLOC, node, mContext.getLocation(node), 
                            "Avoid object allocations during draw operations: Use Canvas.getClipBounds(Rect)");
                }
            }
            return super.visitMethodInvocation(node, unused);
        }

        private String getMethodName(MethodInvocationTree node) {
            if (node.getMethodSelect() instanceof MemberSelectTree) {
                return ((MemberSelectTree) node.getMethodSelect()).getIdentifier().toString();
            } else if (node.getMethodSelect() instanceof IdentifierTree) {
                return ((IdentifierTree) node.getMethodSelect()).getName().toString();
            }
            return "";
        }

        private void reportAllocation(Tree node) {
            mContext.report(PAINT_ALLOC, node, mContext.getLocation(node),
                    "Avoid object allocations during draw/layout operations");
        }

        private static boolean isBlockedAllocationMethod(MethodTree node) {
            String name = node.getName().toString();
            return name.equals(ON_DRAW) || name.equals(ON_MEASURE) || name.equals(ON_LAYOUT);
        }

        private boolean isLazilyInitialized(Tree node) {
            return false;
        }
    }
}
