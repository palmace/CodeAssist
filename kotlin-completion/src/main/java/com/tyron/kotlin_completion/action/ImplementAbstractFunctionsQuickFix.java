package com.tyron.kotlin_completion.action;

import static org.jetbrains.kotlin.js.resolve.diagnostics.SourceLocationUtilsKt.findPsi;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.completion.model.Range;
import com.tyron.completion.model.Rewrite;
import com.tyron.completion.model.TextEdit;
import com.tyron.completion.util.RewriteUtil;
import com.tyron.editor.Caret;
import com.tyron.editor.Editor;
import com.tyron.kotlin_completion.CompiledFile;
import com.tyron.kotlin_completion.util.PsiUtils;

import org.jetbrains.kotlin.com.intellij.openapi.util.Pair;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.psi.KtTypeReference;
import javax.tools.Diagnostic;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kotlin.text.StringsKt;

/**
 * ImplementAbstractFunctionsQuickFix v1.2: "The Code Weaver".
 * v1.2: Corregida creación de TextEdit usando el objeto Range.
 */
public class ImplementAbstractFunctionsQuickFix extends QuickFix {

    public static final String ID = "kotlinImplementAbstractFunctionsQuickFIx";
    public static final String ERROR_CODE = "ABSTRACT_MEMBER_NOT_IMPLEMENTED";
    public static final String ERROR_CODE_CLASS = "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED";
    
    @Override
    public boolean accept(@NonNull String errorCode) {
        return ERROR_CODE.equals(errorCode) || ERROR_CODE_CLASS.equals(errorCode);
    }

    @Override
    public String getTitle(Context context) {
        return "Implementar miembros abstractos";
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        File file = e.getRequiredData(CommonDataKeys.FILE);
        Caret caret = editor.getCaret();
        CompiledFile compiledFile = e.getRequiredData(CommonKotlinKeys.COMPILED_FILE);

        // Encontramos la clase bajo el cursor
        KtElement element = compiledFile.parseAtPoint(caret.getStart(), false);
        KtClass kotlinClass = (element instanceof KtClass) ? (KtClass) element : PsiUtils.findParent(element, KtClass.class);

        if (kotlinClass != null) {
            List<String> stubs = getAbstractFunctionsStubs(compiledFile, kotlinClass);
            if (stubs.isEmpty()) return;

            // Generamos el bloque de código a insertar
            StringBuilder sb = new StringBuilder("\n");
            for (String stub : stubs) {
                sb.append("\t").append(stub).append("\n");
            }

            // Calculamos la posición de inserción (justo antes del cierre de la clase '}')
            int insertOffset = kotlinClass.getTextRange().getEndOffset() - 1;

            Rewrite<Void> rewrite = new Rewrite<Void>() {
                @Override
                public Map<Path, TextEdit[]> rewrite(Void unused) {
                    Map<Path, TextEdit[]> map = new HashMap<>();
                    TextEdit edit = new TextEdit(new Range(insertOffset, insertOffset), sb.toString());
                    map.put(file.toPath(), new TextEdit[]{edit});
                    return map;
                }
            };

            // Ejecutamos la inserción de código con soporte de Undo
            RewriteUtil.performRewrite(editor, file, null, rewrite);
        }
    }

    private List<String> getAbstractFunctionsStubs(CompiledFile file, KtClass kotlinClass) {
        List<KtSuperTypeListEntry> superTypeListEntries = kotlinClass.getSuperTypeListEntries();
        return superTypeListEntries.stream().flatMap(it -> {
            Pair<KtExpression, DeclarationDescriptor> pair =
                    file.referenceAtPoint(PsiUtils.getStartOffset(it));
            if (pair == null) return Stream.empty();
            
            DeclarationDescriptor descriptor = pair.getSecond();
            PsiElement superClass = (descriptor != null) ? findPsi(descriptor) : null;
            
            if (superClass instanceof KtClass) {
                KtClass ktSuperClass = (KtClass) superClass;
                return ktSuperClass.getDeclarations().stream()
                        .filter(decl -> isAbstractFunction(decl) && !overridesDeclaration(kotlinClass, decl))
                        .map(function -> getFunctionStub(((KtNamedFunction) function)));
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }

    private String getFunctionStub(KtNamedFunction function) {
        // Generamos un stub limpio: override fun nombre(args): Tipo = TODO()
        String text = function.getText();
        String signature = StringsKt.substringAfter(text, "fun", "");
        return "override fun" + signature.trim() + " { TODO(\"No implementado\") }";
    }

    private boolean isAbstractFunction(KtDeclaration decl) {
        if (decl instanceof KtNamedFunction) {
            KtNamedFunction func = (KtNamedFunction) decl;
            if (func.hasBody()) return false;
            KtModifierList modifiers = func.getModifierList();
            return modifiers != null && modifiers.hasModifier(KtTokens.ABSTRACT_KEYWORD);
        }
        return false;
    }

    private boolean overridesDeclaration(KtClass ktClass, KtDeclaration declaration) {
        return ktClass.getDeclarations().stream().anyMatch(it -> 
            Objects.equals(it.getName(), declaration.getName()) && it.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        );
    }
}
