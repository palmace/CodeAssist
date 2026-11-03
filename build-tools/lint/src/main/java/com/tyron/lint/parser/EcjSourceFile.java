package com.tyron.lint.parser;

import androidx.annotation.NonNull;

import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * EcjSourceFile v28.2: "The Zenith Source Node".
 * v28.2: Remache de acceso a contenidos (uso de campo contents heredado de ECJ).
 * v28.0: Sincronía con JDT Core 3.28.0.
 */
public class EcjSourceFile extends CompilationUnit {

    private String mSource;

    public EcjSourceFile(@NonNull char[] source, @NonNull File file,
                         @NonNull String encoding) {
        super(source, file.getPath(), encoding);
        mSource = new String(source);
    }

    public EcjSourceFile(@NonNull String source, @NonNull File file,
                         @NonNull String encoding) {
        super(source.toCharArray(), file.getPath(), encoding);
        mSource = source;
    }

    public EcjSourceFile(@NonNull String source, @NonNull File file) {
        this(source, file, StandardCharsets.UTF_8.toString());
    }

    @NonNull
    public String getSource() {
        if (mSource == null) {
            // OMNI: En JDT 3.28, accedemos al campo heredado 'contents'
            if (this.contents != null) {
                mSource = new String(this.contents);
            } else {
                mSource = "";
            }
        }
        return mSource;
    }
}
