package com.tyron.completion.model;

import java.io.Serializable;

public class Range implements Serializable {

    private static final long serialVersionUID = 1L;

    public Position start, end;

    public Range(long startPosition, long endPosition) {
        start = new Position(startPosition, startPosition);
        end = new Position(endPosition, endPosition);
    }

    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }

    public static final Range NONE = new Range(Position.NONE, Position.NONE);
}