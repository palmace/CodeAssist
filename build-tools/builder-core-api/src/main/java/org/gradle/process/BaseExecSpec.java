package org.gradle.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Specifies options for launching a child process.
 */
public interface BaseExecSpec extends ProcessForkOptions {
    /**
     * Sets whether a non-zero exit value is ignored, or an exception thrown.
     *
     * @param ignoreExitValue whether a non-zero exit value is ignored, or an exception thrown
     * @return this
     */
    BaseExecSpec setIgnoreExitValue(boolean ignoreExitValue);

    /**
     * Tells whether a non-zero exit value is ignored, or an exception thrown. Defaults to <code>false</code>.
     *
     * @return whether a non-zero exit value is ignored, or an exception thrown
     */
    boolean isIgnoreExitValue();

    /**
     * Sets the standard input stream for the process executing the command. The stream is closed after the process
     * completes.
     *
     * @param inputStream The standard input stream for the process. Must not be null.
     * @return this
     */
    BaseExecSpec setStandardInput(InputStream inputStream);

    /**
     * Returns the standard input stream for the process executing the command. The stream is closed after the process
     * completes. Defaults to an empty stream.
     *
     * @return The standard input stream.
     */
    InputStream getStandardInput();

    /**
     * Sets the output stream to consume standard output from the process executing the command. The stream is closed
     * after the process completes.
     *
     * @param outputStream The standard output stream for the process. Must not be null.
     * @return this
     */
    BaseExecSpec setStandardOutput(OutputStream outputStream);

    /**
     * Returns the output stream to consume standard output from the process executing the command. Defaults to {@code
     * System.out}.
     *
     * @return The output stream
     */
    OutputStream getStandardOutput();

    /**
     * Sets the output stream to consume standard error from the process executing the command. The stream is closed
     * after the process completes.
     *
     * @param outputStream The standard output error stream for the process. Must not be null.
     * @return this
     */
    BaseExecSpec setErrorOutput(OutputStream outputStream);

    /**
     * Returns the output stream to consume standard error from the process executing the command. Default to {@code
     * System.err}.
     *
     * @return The error output stream.
     */
    OutputStream getErrorOutput();

    /**
     * Returns the full command line, including the executable plus its arguments.
     *
     * @return The full command line, including the executable plus its arguments
     */
    List<String> getCommandLine();
}
