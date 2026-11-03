package com.tyron.code.service;

/**
 * Stub para javax.swing.Icon para evitar NoClassDefFoundError en Android ART.
 * v28.5: Parte de la "Sincronía Zenith" para compatibilidad con Android 15.
 */
public interface IconStub {
    int getIconHeight();
    int getIconWidth();
    void paintIcon(Object c, Object g, int x, int y);
}
