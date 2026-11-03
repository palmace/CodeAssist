package com.tyron.code.util;

import android.view.View;
import java.lang.reflect.Field;

/**
 * PopupMenuHelper v28.0: "The Zenith Menu Guard".
 * v28.0: Blindaje contra restricciones de visibilidad de AndroidX.
 * v28.0: Uso de reflexión atómica para habilitar el reenvío de gestos (ForwardingListener).
 */
public class PopupMenuHelper {

    private PopupMenuHelper() {}

    private static final Field FORWARDING_FIELD;
    private static final Class<?> FORWARDING_LISTENER_CLASS;

    static {
        Field field = null;
        Class<?> clazz = null;
        try {
            clazz = Class.forName("androidx.appcompat.widget.ForwardingListener");
            field = clazz.getDeclaredField("mForwarding");
            field.setAccessible(true);
        } catch (Throwable e) {
            // OMNI: Fallback silencioso si la clase no existe en el entorno actual
        }
        FORWARDING_FIELD = field;
        FORWARDING_LISTENER_CLASS = clazz;
    }

    /**
     * Habilita el reenvío de eventos táctiles de forma atómica.
     * @param listener El listener que se sospecha es un ForwardingListener.
     */
    public static void setForwarding(Object listener) {
        if (FORWARDING_FIELD == null || listener == null) return;
        
        try {
            if (FORWARDING_LISTENER_CLASS.isInstance(listener)) {
                FORWARDING_FIELD.set(listener, true);
            }
        } catch (IllegalAccessException e) {
            // Error de acceso en el Motor de Gestos
        }
    }
}
