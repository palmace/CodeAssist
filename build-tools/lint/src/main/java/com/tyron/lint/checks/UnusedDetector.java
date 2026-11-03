package com.tyron.lint.checks;

import com.tyron.lint.api.Detector;
import com.tyron.lint.api.JavaContext;
import com.tyron.lint.api.JavaVoidVisitor;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * UnusedDetector v28.38: "The Zenith Garbage Collector - Absolute Shield".
 * v28.38: Desacoplamiento total del tipo Trees en firmas para evadir bloqueos de Android 15.
 * v28.35: Blindaje total mediante desacoplamiento reflexivo de Trees.
 */
public class UnusedDetector extends Detector implements Detector.JavaScanner {

    @Override
    public List<Class<? extends Tree>> getApplicableTypes() {
        return Collections.singletonList(VariableTree.class);
    }

    @Override
    public JavaVoidVisitor getVisitor(JavaContext context) {
        return new UnusedScanner(context);
    }

    private static class UnusedScanner extends JavaVoidVisitor {

        private TreePath path;

        private void scanPath(TreePath path) {
            TreePath prev = this.path;
            this.path = path;
            try {
                path.getLeaf().accept(this, null);
            } finally {
                this.path = prev; 
            }
        }

        @Override
        public Void scan(Tree tree, Void p) {
            if (tree == null) return null;

            TreePath prev = path;
            path = new TreePath(path, tree);
            try {
                return tree.accept(this, p);
            } finally {
                path = prev;
            }
        }

        private final Object trees;
        private final Map<Element, TreePath> privateDeclarations = new HashMap<>();
        private final Map<Element, TreePath> localVariables = new HashMap<>();
        private final Set<Element> used = new HashSet<>();

        public UnusedScanner(JavaContext context) {
            this.trees = (context.getCompileTask() != null) ? context.getCompileTask().getTrees() : null;
        }

        private void foundPrivateDeclaration() {
            Element element = getElementFromPath(path);
            if (element != null) {
                privateDeclarations.put(element, path);
            }
        }

        private void foundLocalVariable() {
            Element element = getElementFromPath(path);
            if (element != null) {
                localVariables.put(element, path);
            }
        }

        private Element getElementFromPath(TreePath treePath) {
            if (trees == null || treePath == null) return null;
            try {
                Method getElementMethod = trees.getClass().getMethod("getElement", TreePath.class);
                return (Element) getElementMethod.invoke(trees, treePath);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private void sweep(Element toEl) {
            boolean firstUse = used.add(toEl);
            boolean notScanned = firstUse && privateDeclarations.containsKey(toEl);
            if (notScanned) scanPath(privateDeclarations.get(toEl));
        }

        private boolean isReachable(TreePath path) {
            if (trees == null) return true;
            Tree t = path.getLeaf();
            if (t instanceof VariableTree) {
                VariableTree v = (VariableTree) t;
                if (!v.getModifiers().getFlags().contains(Modifier.PRIVATE) || isLocalVariable(path)) return true;
            }
            if (t instanceof MethodTree) {
                MethodTree m = (MethodTree) t;
                if (!m.getModifiers().getFlags().contains(Modifier.PRIVATE) || (m.getParameters().isEmpty() && m.getReturnType() == null)) return true;
            }
            if (t instanceof ClassTree) {
                if (!((ClassTree) t).getModifiers().getFlags().contains(Modifier.PRIVATE)) return true;
            }
            Element el = getElementFromPath(path);
            return el != null && used.contains(el);
        }

        private boolean isLocalVariable(TreePath path) {
            Tree.Kind kind = path.getLeaf().getKind();
            if (kind != Tree.Kind.VARIABLE) return false;
            TreePath parentPath = path.getParentPath();
            if (parentPath == null) return false;
            Tree.Kind parent = parentPath.getLeaf().getKind();
            if (parent == Tree.Kind.CLASS || parent == Tree.Kind.INTERFACE) return false;
            if (parent == Tree.Kind.METHOD) {
                if (((MethodTree) parentPath.getLeaf()).getBody() == null) return false;
            }
            return true;
        }

        private CompilationUnitTree root;
        @Override
        public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void unused) {
            root = compilationUnitTree;
            return super.visitCompilationUnit(compilationUnitTree, unused);
        }

        @Override
        public Void visitVariable(VariableTree t, Void __) {
            if (trees == null) return null;
            try {
                Method getPathMethod = trees.getClass().getMethod("getPath", CompilationUnitTree.class, Tree.class);
                TreePath currentPath = (TreePath) getPathMethod.invoke(trees, root, t);
                if (currentPath != null) {
                    if (isLocalVariable(currentPath)) {
                        foundLocalVariable();
                        super.visitVariable(t, null);
                    } else if (isReachable(currentPath)) {
                        super.visitVariable(t, null);
                    } else {
                        foundPrivateDeclaration();
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }
    }
}
