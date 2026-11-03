package org.gradle.process;

/**
 * Provides arguments to a process.
 */
public interface CommandLineArgumentProvider {
    /**
     * The arguments which will be provided to the process.
     */
    Iterable<String> asArguments();
}