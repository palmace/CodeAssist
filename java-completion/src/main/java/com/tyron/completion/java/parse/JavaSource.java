package com.tyron.completion.java.parse;

import com.tyron.completion.java.compiler.ReflectionUtil;

public class JavaSource {

    public static enum Phase {
        MODIFIED,

        PARSED,

        ELEMENTS_RESOLVED,

        RESOLVED,

        UP_TO_DATE;

    };

    public static enum Priority {
        MAX,
        HIGH,
        ABOVE_NORMAL,
        NORMAL,
        BELOW_NORMAL,
        LOW,
        MIN
    };
}
