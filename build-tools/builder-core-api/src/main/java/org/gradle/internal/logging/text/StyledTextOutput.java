package org.gradle.internal.logging.text;

/**
 * Provides streaming of styled text.
 */
public interface StyledTextOutput extends Appendable {
    enum Style {
        Normal,
        Header,
        UserInput,
        Identifier,
        Description,
        ProgressStatus,
        Success,
        SuccessHeader,
        Failure,
        FailureHeader,
        Info,
        Error
    }

    @Override
    StyledTextOutput append(char c);

    @Override
    StyledTextOutput append(CharSequence csq);

    @Override
    StyledTextOutput append(CharSequence csq, int start, int end);

    StyledTextOutput style(Style style);

    StyledTextOutput withStyle(Style style);

    StyledTextOutput text(Object text);

    StyledTextOutput println(Object text);

    StyledTextOutput format(String pattern, Object... args);

    StyledTextOutput formatln(String pattern, Object... args);

    StyledTextOutput println();

    StyledTextOutput exception(Throwable throwable);
}
