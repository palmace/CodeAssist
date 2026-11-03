package org.gradle.process;

import java.util.List;

/**
 * Specified the options for executing some command.
 */
public interface ExecSpec extends BaseExecSpec {
    /**
     * Sets the full command line, including the executable to be executed plus its arguments.
     *
     * @param args the command plus the args to be executed
     */
    void setCommandLine(List<String> args);

    /**
     * Sets the full command line, including the executable to be executed plus its arguments.
     *
     * @param args the command plus the args to be executed
     */
    void setCommandLine(Object... args);

    /**
     * Sets the full command line, including the executable to be executed plus its arguments.
     *
     * @param args the command plus the args to be executed
     */
    void setCommandLine(Iterable<?> args);

    /**
     * Sets the full command line, including the executable to be executed plus its arguments.
     *
     * @param args the command plus the args to be executed
     * @return this
     */
    ExecSpec commandLine(Object... args);

    /**
     * Sets the full command line, including the executable to be executed plus its arguments.
     *
     * @param args the command plus the args to be executed
     * @return this
     */
    ExecSpec commandLine(Iterable<?> args);

    /**
     * Adds arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    ExecSpec args(Object... args);

    /**
     * Adds arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    ExecSpec args(Iterable<?> args);

    /**
     * Sets the arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    ExecSpec setArgs(List<String> args);

    /**
     * Sets the arguments for the command to be executed.
     *
     * @param args args for the command
     * @return this
     */
    ExecSpec setArgs(Iterable<?> args);

    /**
     * Returns the arguments for the command to be executed. Defaults to an empty list.
     */
    List<String> getArgs();

    /**
     * Argument providers for the application.
     */
    List<CommandLineArgumentProvider> getArgumentProviders();
}
