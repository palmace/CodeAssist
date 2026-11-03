package org.gradle.process;

import org.gradle.process.internal.ExecException;

/**
 * Represents the result of running an external process.
 */
public interface ExecResult {
    /**
     * Returns the exit value of the process.
     */
    int getExitValue();

    /**
     * Throws an {@link org.gradle.process.internal.ExecException} if the process exited with a non-zero exit value.
     *
     * @return this
     * @throws ExecException if the process exited with a non-zero exit value
     */
    ExecResult assertNormalExitValue() throws ExecException;

    /**
     * Re-throws any failure executing this process.
     *
     * @return this
     * @throws ExecException the execution failure
     */
    ExecResult rethrowFailure() throws ExecException;
}
