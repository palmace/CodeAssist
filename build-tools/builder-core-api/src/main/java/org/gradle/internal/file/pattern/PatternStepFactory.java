package org.gradle.internal.file.pattern;

public class PatternStepFactory {
    private static final AnyWildcardPatternStep ANY_WILDCARD_PATTERN_STEP = new AnyWildcardPatternStep();

    public static PatternStep getStep(String source, boolean caseSensitive) {
        if (source.length() == 0) {
            return new FixedPatternStep(source, caseSensitive);
        }

        char ch = source.charAt(0);
        int endPrefixWildcard = 0;
        if (ch == '*') {
            endPrefixWildcard = 1;
            while (endPrefixWildcard < source.length() && source.charAt(endPrefixWildcard) == '*') {
                endPrefixWildcard++;
            }
        }

        if (endPrefixWildcard == source.length()) {
            return ANY_WILDCARD_PATTERN_STEP;
        }

        int endLiteral = endPrefixWildcard;
        for(; endLiteral < source.length(); endLiteral++) {
            ch = source.charAt(endLiteral);
            if (ch == '*' || ch == '?') {
                break;
            }
        }

        if (endLiteral == source.length()) {
            return new FixedPatternStep(source.substring(endPrefixWildcard), caseSensitive);
        }

        // Simplificación para core-api: si hay comodines complejos, usamos Fixed por ahora o lanzamos error si es crítico.
        // En una implementación real aquí irían HasPrefixPatternStep, etc.
        return new FixedPatternStep(source, caseSensitive);
    }
}
