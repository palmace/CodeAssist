package com.tyron.completion.java.compiler.services;

import com.tyron.completion.java.compiler.ReflectionUtil;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;

/**
 *
 * @author lahvac
 */
public class NBMemberEnter extends MemberEnter {

    public static void preRegister(Context context, boolean backgroundScan) {
        context.put(MemberEnter.class, (Context.Factory<MemberEnter>) c -> new NBMemberEnter(c, backgroundScan));
    }

    private final CancelService cancelService;
    private final JavacTrees trees;
    private final boolean backgroundScan;

    public NBMemberEnter(Context context, boolean backgroundScan) {
        super(context);
        cancelService = CancelService.instance(context);
        
        JavacTrees treesInstance = null;
        try {
            // Use reflection to get instance of JavacTrees (or its implementation)
            // to avoid direct dependency on ProcessingEnvironment which is not visible in Android compilation
            java.lang.reflect.Method instanceMethod = Context.class.getMethod("get", Class.class);
            treesInstance = (JavacTrees) instanceMethod.invoke(context, JavacTrees.class);
        } catch (Exception e) {
            // Fallback
        }
        this.trees = treesInstance;
        this.backgroundScan = backgroundScan;
    }

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        cancelService.abortIfCanceled();
        super.visitTopLevel(tree);
    }

    @Override
    public void visitImport(JCImport tree) {
        cancelService.abortIfCanceled();
        super.visitImport(tree);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        cancelService.abortIfCanceled();
        super.visitMethodDef(tree);
        if (!backgroundScan && trees instanceof NBJavacTrees && !env.enclClass.defs.contains(tree)) {
            TreePath path = trees.getPath(env.toplevel, env.enclClass);
            if (path != null) {
                ((NBJavacTrees)trees).addPathForElement(tree.sym, new TreePath(path, tree));
            }
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        cancelService.abortIfCanceled();
        super.visitVarDef(tree);
    }

}
