package com.tyron.code.ui.editor;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;

/**
 * A text action window that does nothing.
 * Used to disable the default text action window in Sora Editor.
 */
public class NoOpTextActionWindow extends EditorTextActionWindow implements EditorBuiltinComponent {

    public NoOpTextActionWindow(CodeEditor editor) {
        super(editor);
    }

    @Override
    public void show() {
        // No-op: don't show the window
    }

    @Override
    public void dismiss() {
        // No-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // ignore
    }
}
