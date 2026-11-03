package com.tyron.language.api;

import java.io.File;

/**
 * An interface that is implemented by programming languages supported by CodeAssist
 */
public interface CodeAssistLanguage {

    /**
     * Called from the background thread to notify the language that a content of a file has changed.
     * This is useful for updating the AST of the current file
     *
     * @param file The file that is updated
     * @param contents The updated contents of the file
     */
    void onContentChange(File file, CharSequence contents);

    /**
     * Overloaded method to allow notifying when the analysis is complete
     * Default implementation calls the simple onContentChange for backward compatibility
     */
    default void onContentChange(File file, CharSequence contents, Runnable onComplete) {
        onContentChange(file, contents);
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
