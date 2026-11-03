package com.tyron.builder;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SovereignSignal v1.2: "The Abstract Ghost Tunnel".
 * v1.2: Usa SovereignMessage (Java puro) para evitar errores de compilación en PC.
 * v1.2: Inmune a SecurityException y FGS Timeout de Android 14.
 */
public class SovereignSignal {

    public interface Observer {
        void onSignalReceived(SovereignMessage message);
    }

    private static final List<Observer> observers = new CopyOnWriteArrayList<>();

    public static void register(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public static void unregister(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Envía una señal al enjambre.
     */
    public static void send(SovereignMessage message) {
        for (Observer observer : observers) {
            try {
                observer.onSignalReceived(message);
            } catch (Throwable ignored) {}
        }
    }
}
