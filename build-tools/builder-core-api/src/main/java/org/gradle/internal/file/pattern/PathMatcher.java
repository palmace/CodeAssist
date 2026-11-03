package org.gradle.internal.file.pattern;

public interface PathMatcher {
    int getMinSegments();
    int getMaxSegments();
    boolean matches(String[] segments, int startIndex);
    boolean isPrefix(String[] segments, int startIndex);
}
