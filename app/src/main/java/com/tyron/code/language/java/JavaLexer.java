package com.tyron.code.language.java;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.atn.ATN;

/**
 * JavaLexer v1.0: "The Swift Scanner".
 * Lexer simplificado para soportar el framework de AbstractCodeAnalyzer.
 */
public class JavaLexer extends Lexer {
    public JavaLexer(CharStream input) {
        super(input);
    }

    @Override
    public String[] getRuleNames() { return new String[0]; }

    @Override
    public String getGrammarFileName() { return "JavaLexer.g4"; }

    @Override
    public ATN getATN() { return null; }

    @Override
    public String[] getTokenNames() { return new String[0]; }
}
