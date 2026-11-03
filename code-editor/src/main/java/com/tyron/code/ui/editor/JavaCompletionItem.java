package com.tyron.code.ui.editor;

import android.graphics.drawable.Drawable;

import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * JavaCompletionItem v2.8: "The Zenith Completion Item".
 * v2.8: Sincronía con interfaz Editor Zenith (Uso de getContent() en lugar de getText()).
 * v2.8: Optimización de inserción de texto para el Motor Hybrid Pro V8.
 */
public class JavaCompletionItem extends SimpleCompletionItem {

    private final String mPrefix;

    public JavaCompletionItem(String prefix, String commitText) {
        super(prefix.length(), commitText);
        mPrefix = prefix;
    }

    public JavaCompletionItem(CharSequence label, String prefix, String commitText) {
        super(label, prefix.length(), commitText);
        mPrefix = prefix;
    }

    public JavaCompletionItem(CharSequence label, CharSequence desc, String prefix,
                              String commitText) {
        super(label, desc, prefix.length(), commitText);
        mPrefix = prefix;
    }

    public JavaCompletionItem(CharSequence label, CharSequence desc, Drawable icon,
                              String prefix, String commitText) {
        super(label, desc, icon, prefix.length(), commitText);
        mPrefix = prefix;
    }

    @Override
    public void performCompletion(CodeEditor editor, Content text, int line, int column) {
        int length = prefixLength;
        if (mPrefix.contains(".")) {
            length -= mPrefix.lastIndexOf('.') + 1;
        }
        Cursor cursor = editor.getCursor();
        
        // OMNI: Remachado de firma para compatibilidad con el Enjambre Zenith
        editor.getText().delete(cursor.getLeftLine(), cursor.getLeftColumn() - length,
                cursor.getLeftLine(), cursor.getLeftColumn());
        editor.getText().insert(cursor.getLeftLine(), cursor.getLeftColumn(), commitText);
    }
}
