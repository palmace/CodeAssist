package org.gradle.internal.logging.text;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * Constructs a tree of diagnostic messages.
 */
public class TreeFormatter implements DiagnosticsVisitor {
    private final StringBuilder buffer = new StringBuilder();
    private int indent = 0;

    @Override
    public TreeFormatter node(String text) {
        if (buffer.length() > 0) {
            buffer.append("\n");
        }
        for (int i = 0; i < indent; i++) {
            buffer.append("  ");
        }
        buffer.append("- ").append(text);
        return this;
    }

    @Override
    public TreeFormatter startChildren() {
        indent++;
        return this;
    }

    @Override
    public TreeFormatter endChildren() {
        indent--;
        return this;
    }

    public TreeFormatter append(CharSequence text) {
        buffer.append(text);
        return this;
    }

    public TreeFormatter appendType(Type type) {
        buffer.append(type.toString());
        return this;
    }

    public TreeFormatter appendValue(@Nullable Object value) {
        buffer.append(String.valueOf(value));
        return this;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
