package org.gradle.internal.file.pattern;

public class FixedStepPathMatcher implements PathMatcher {
    private final PatternStep step;
    private final PathMatcher next;
    private final int minSegments;
    private final int maxSegments;

    public FixedStepPathMatcher(PatternStep step, PathMatcher next) {
        this.step = step;
        this.next = next;
        minSegments = 1 + next.getMinSegments();
        maxSegments = next.getMaxSegments() == Integer.MAX_VALUE ? Integer.MAX_VALUE : next.getMaxSegments() + 1;
    }

    @Override
    public String toString() {
        return "{fixed-step: " + step + ", next: " + next + "}";
    }

    @Override
    public int getMinSegments() {
        return minSegments;
    }

    @Override
    public int getMaxSegments() {
        return maxSegments;
    }

    @Override
    public boolean matches(String[] segments, int startIndex) {
        int remaining = segments.length - startIndex;
        if (remaining < minSegments || remaining > maxSegments) {
            return false;
        }
        if (!step.matches(segments[startIndex])) {
            return false;
        }
        return next.matches(segments, startIndex + 1);
    }

    @Override
    public boolean isPrefix(String[] segments, int startIndex) {
        if (startIndex == segments.length) {
            return true;
        }
        if (!step.matches(segments[startIndex])) {
            return false;
        }
        if (startIndex + 1 == segments.length) {
            return true;
        }
        return next.isPrefix(segments, startIndex + 1);
    }
}
