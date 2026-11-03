package com.tyron.code.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Corregido para Android 15: Ahora permite que los insets fluyan hacia los hijos,
 * permitiendo que el BottomSheet detecte correctamente la barra de navegación.
 */
public class NoInsetFrameLayout extends FrameLayout {
    public NoInsetFrameLayout(@NonNull Context context) {
        super(context);
    }

    public NoInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Dejamos que los insets pasen a los hijos sin alterarlos radicalmente
        return super.onApplyWindowInsets(insets);
    }
}
