package com.tyron.completion.java.provider;

import com.tyron.completion.java.compiler.ReflectionUtil;


import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import java.io.IOException;

public class PruneMethodBodies extends TreeScanner<StringBuilder, Long> {
    private final JavacTask task;
    private final StringBuilder buf = new StringBuilder();
    private CompilationUnitTree root;

    public PruneMethodBodies(JavacTask task) {
        this.task = task;
    }

    @Override
    public StringBuilder visitCompilationUnit(CompilationUnitTree t, Long find) {
        root = t;
        try {
            CharSequence contents = t.getSourceFile().getCharContent(true);
            buf.setLength(0);
            buf.append(contents);
        } catch (IOException e) {
            // throw new RuntimeException(e);
            return buf;
        }
        return super.visitCompilationUnit(t, find);
    }

    @Override
    public StringBuilder visitBlock(BlockTree blockTree, Long find) {
        if (root == null) return buf;
        
        SourcePositions pos = ReflectionUtil.getSourcePositions(task);
        if (pos == null) return buf;
        
        long start = pos.getStartPosition(root, blockTree);
        long end = pos.getEndPosition(root, blockTree);
        
        if (start != -1 && end != -1 && !(start <= find && find < end)) {
            for (int i = (int) start + 1; i < (int) end - 1; i++) {
                if (i < buf.length() && !Character.isWhitespace(buf.charAt(i))) {
                    buf.setCharAt(i, ' ');
                }
            }
            return buf;
        }
        return super.visitBlock(blockTree, find);
    }

    @Override
    public StringBuilder reduce(StringBuilder a, StringBuilder b) {
        return buf;
    }
}
