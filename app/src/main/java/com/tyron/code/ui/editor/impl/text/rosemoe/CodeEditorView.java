package com.tyron.code.ui.editor.impl.text.rosemoe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.code.ui.editor.EditorViewModel;
import com.tyron.code.ui.editor.NoOpTextActionWindow;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.editor.Caret;
import com.tyron.editor.CharPosition;
import com.tyron.editor.Content;
import com.tyron.editor.Editor;

import org.jetbrains.kotlin.com.intellij.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.tools.Diagnostic;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.SelectionMovement;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora2.text.EditorUtil;

/**
 * CodeEditorView v1.14: "The Zenith Swarm Lens - Gesture Sync".
 * v1.14: Implementación de performClick() para compatibilidad con accesibilidad.
 * v1.14: Sincronía de diagnóstico optimizada para Sora 0.23.6.
 */
public class CodeEditorView extends CodeEditor implements Editor {

    private final Set<Character> IGNORED_PAIR_ENDS = ImmutableSet.<Character>builder()
            .add(')')
            .add(']')
            .add('"')
            .add('>')
            .add('\'')
            .add(';')
            .build();

    private boolean mIsBackgroundAnalysisEnabled;
    private List<DiagnosticWrapper> mDiagnostics;
    private Consumer<List<DiagnosticWrapper>> mDiagnosticsListener;
    private File mCurrentFile;
    private EditorViewModel mViewModel;

    public CodeEditorView(Context context) {
        this(context, null);
    }

    public CodeEditorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public boolean performClick() {
        // OMNI: Requerido por el Motor de Gestos Zenith
        return super.performClick();
    }

    @Nullable
    @Override
    public Project getProject() {
        return ProjectManager.getInstance().getCurrentProject();
    }

    @Override
    public void setEditorLanguage(@Nullable Language lang) {
        super.setEditorLanguage(lang);

        if (lang != null) {
            try {
                Class<? extends Language> aClass = lang.getClass();
                Method method = ReflectionUtil.getDeclaredMethod(aClass, "getTabWidth");
                if (method != null) {
                    Object invoke = method.invoke(getEditorLanguage());
                    if (invoke instanceof Integer) {
                        setTabWidth((Integer) invoke);
                    }
                }
            } catch (Throwable e) {
                // use default
            }
        }
    }

    private void init() {
        setColorScheme(EditorUtil.getDefaultColorScheme(getContext()));
        replaceComponent(EditorTextActionWindow.class, new NoOpTextActionWindow(this));
    }

    public void setViewModel(EditorViewModel viewModel) {
        mViewModel = viewModel;
    }

    @Override
    public void setDiagnostics(List<DiagnosticWrapper> diagnostics) {
        mDiagnostics = diagnostics;
        
        DiagnosticsContainer soraContainer = super.getDiagnostics();
        if (soraContainer != null) {
            soraContainer.reset();
            for (DiagnosticWrapper wrapper : diagnostics) {
                short severity = DiagnosticRegion.SEVERITY_NONE;
                if (wrapper.getKind() == Diagnostic.Kind.ERROR) {
                    severity = DiagnosticRegion.SEVERITY_ERROR;
                } else if (wrapper.getKind() == Diagnostic.Kind.WARNING) {
                    severity = DiagnosticRegion.SEVERITY_WARNING;
                }
                
                soraContainer.addDiagnostic(new DiagnosticRegion(
                    (int) wrapper.getStartPosition(),
                    (int) wrapper.getEndPosition(),
                    severity
                ));
            }
            invalidate(); 
        }

        if (mDiagnosticsListener != null) {
            mDiagnosticsListener.accept(diagnostics);
        }
    }

    @Override
    public List<DiagnosticWrapper> getDiagnosticsList() {
        return mDiagnostics;
    }

    public void setDiagnosticsListener(Consumer<List<DiagnosticWrapper>> listener) {
        mDiagnosticsListener = listener;
    }

    @Override
    public File getCurrentFile() {
        return mCurrentFile;
    }

    @Override
    public void openFile(File file) {
        mCurrentFile = file;
    }

    @Override
    public CharPosition getCharPosition(int index) {
        io.github.rosemoe.sora.text.CharPosition charPosition =
                getText().getIndexer().getCharPosition(index);
        return new CharPosition(charPosition.line, charPosition.column);
    }

    @Override
    public int getCharIndex(int line, int column) {
        return getText().getCharIndex(line, column);
    }

    @Override
    public boolean useTab() {
        if (getEditorLanguage() == null) return true;
        return getEditorLanguage().useTab();
    }

    @Override
    public int getTabCount() { return getTabWidth(); }

    @Override
    public int getTabSize() { return getTabWidth(); }

    @Override
    public void insert(int line, int column, String string) {
        getText().insert(line, column, string);
    }

    @Override
    public void insertMultilineString(int line, int column, String string) {
        getText().insert(line, column, string);
    }

    @Override
    public void delete(int startLine, int startColumn, int endLine, int endColumn) {
        getText().delete(startLine, startColumn, endLine, endColumn);
    }

    @Override
    public void delete(int startIndex, int endIndex) {
        getText().delete(startIndex, endIndex);
    }

    @Override
    public void replace(int line, int column, int endLine, int endColumn, String string) {
        getText().replace(line, column, endLine, endColumn, string);
    }

    @Override
    public boolean formatCodeAsync() {
        return false;
    }

    @Override
    public boolean formatCodeAsync(int startIndex, int endIndex) {
        return formatCodeAsync();
    }

    @Override
    public void beginBatchEdit() {
        getText().beginBatchEdit();
    }

    @Override
    public void endBatchEdit() {
        getText().endBatchEdit();
    }

    @Override
    public void commitText(CharSequence text, boolean applyAutoIndent) {
        if (text.length() == 1) {
            int left = getCursor().getLeft();
            if (left < getText().length()) {
                char currentChar = getText().charAt(left);
                char c = text.charAt(0);
                if (IGNORED_PAIR_ENDS.contains(c) && c == currentChar) {
                    setSelection(getCursor().getLeftLine(), getCursor().getLeftColumn() + 1);
                    return;
                }
            }
        }
        super.commitText(text, applyAutoIndent);
    }

    @Override
    public Caret getCaret() {
        return new CursorWrapper(getCursor());
    }

    @Override
    public Content getContent() {
        return (Content) getText();
    }

    public void setBackgroundAnalysisEnabled(boolean enabled) {
        mIsBackgroundAnalysisEnabled = enabled;
    }

    @Override
    public boolean isBackgroundAnalysisEnabled() {
        return mIsBackgroundAnalysisEnabled;
    }

    public void setAnalyzing(boolean analyzing) {
        if (mViewModel != null) {
            mViewModel.setAnalyzeState(analyzing);
        }
    }

    @Override
    public void requireCompletion() {
        getComponent(EditorAutoCompletion.class).requireCompletion();
    }

    @Override
    public void moveSelectionUp() { moveSelection(SelectionMovement.UP); }
    @Override
    public void moveSelectionDown() { moveSelection(SelectionMovement.DOWN); }
    @Override
    public void moveSelectionLeft() { moveSelection(SelectionMovement.LEFT); }
    @Override
    public void moveSelectionRight() { moveSelection(SelectionMovement.RIGHT); }

    @Override
    public void setSelectionRegion(int line, int column, int endLine, int endColumn) {
        getCursor().setLeft(line, column);
        getCursor().setRight(endLine, endColumn);
        invalidate();
    }
    
    @Override
    public void setSelectionRegion(int line, int column, int endLine, int endColumn, boolean scroll) {
        setSelectionRegion(line, column, endLine, endColumn);
        if (scroll) {
            ensureCursorVisible();
        }
    }

    @Override
    public void setSelectionRegion(int startIndex, int endIndex) {
        CharPosition start = getCharPosition(startIndex);
        CharPosition end = getCharPosition(endIndex);
        setSelectionRegion(start.getLine(), start.getColumn(), end.getLine(), end.getColumn());
    }
    
    @Override
    public void ensureCursorVisible() {
        ensurePositionVisible(getCursor().getRightLine(), getCursor().getRightColumn());
    }
}
