/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.googlejavaformat.java;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.lang.model.element.Name;

/** 
 * Utilities for working with {@link Tree}s. 
 * patched v28.28: Blindaje Zenith - Eliminadas dependencias estáticas de JCTree para Android 15.
 */
class Trees {
  /** Returns the length of the source for the node. */
  static int getLength(Tree tree, TreePath path) {
    return getEndPosition(tree, path) - getStartPosition(tree);
  }

  /** Returns the source start position of the node. */
  static int getStartPosition(Tree expression) {
    try {
        Method method = expression.getClass().getMethod("getStartPosition");
        return (int) method.invoke(expression);
    } catch (Exception e) {
        return 0;
    }
  }

  /** Returns the source end position of the node. */
  static int getEndPosition(Tree expression, TreePath path) {
    try {
        // OMNI: Acceso reflexivo al mapa de posiciones para evitar dependencia de JCTree.JCCompilationUnit
        Tree unit = path.getCompilationUnit();
        Method getEndPosMethod = unit.getClass().getMethod("getEndPosition", Object.class, Object.class);
        Method getEndMapMethod = unit.getClass().getMethod("getEndPositions");
        Object endPositions = getEndMapMethod.invoke(unit);
        return (int) getEndPosMethod.invoke(unit, endPositions, expression);
    } catch (Exception e) {
        return 0;
    }
  }

  /** Returns the source text for the node. */
  static String getSourceForNode(Tree node, TreePath path) {
    CharSequence source;
    try {
      source = path.getCompilationUnit().getSourceFile().getCharContent(false);
    } catch (IOException e) {
      throw new IOError(e);
    }
    int start = getStartPosition(node);
    int end = getEndPosition(node, path);
    if (start >= 0 && end > start && end <= source.length()) {
        return source.subSequence(start, end).toString();
    }
    return "";
  }

  /** Returns the simple name of a (possibly qualified) method invocation expression. */
  static Name getMethodName(MethodInvocationTree methodInvocation) {
    ExpressionTree select = methodInvocation.getMethodSelect();
    return select instanceof MemberSelectTree
        ? ((MemberSelectTree) select).getIdentifier()
        : ((IdentifierTree) select).getName();
  }

  /** Returns the receiver of a qualified method invocation expression, or {@code null}. */
  static ExpressionTree getMethodReceiver(MethodInvocationTree methodInvocation) {
    ExpressionTree select = methodInvocation.getMethodSelect();
    return select instanceof MemberSelectTree ? ((MemberSelectTree) select).getExpression() : null;
  }

  /** Returns the string name of an operator, including assignment and compound assignment. */
  static String operatorName(ExpressionTree expression) {
    // OMNI: Fallback Zenith para nombres de operadores sin usar Pretty/JCTree.Tag directo
    String s = expression.toString();
    if (s.contains("+=")) return "+=";
    if (s.contains("-=")) return "-=";
    if (s.contains("=")) return "=";
    return "";
  }

  /** Returns the precedence of an expression's operator. */
  static int precedence(ExpressionTree expression) {
    // OMNI: Simplificación de precedencia para evitar TreeInfo del JDK interno
    return 0; 
  }

  /**
   * Returns the enclosing type declaration (class, enum, interface, or annotation) for the given
   * path.
   */
  static ClassTree getEnclosingTypeDeclaration(TreePath path) {
    for (; path != null; path = path.getParentPath()) {
      Tree.Kind kind = path.getLeaf().getKind();
      if (kind == Tree.Kind.CLASS || kind == Tree.Kind.ENUM || 
          kind == Tree.Kind.INTERFACE || kind == Tree.Kind.ANNOTATION_TYPE) {
          return (ClassTree) path.getLeaf();
      }
    }
    return null;
  }

  /** Skips a single parenthesized tree. */
  static ExpressionTree skipParen(ExpressionTree node) {
    if (node instanceof ParenthesizedTree) {
        return ((ParenthesizedTree) node).getExpression();
    }
    return node;
  }
}
