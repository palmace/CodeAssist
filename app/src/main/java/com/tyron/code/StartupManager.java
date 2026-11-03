package com.tyron.code;

import android.util.Log;
import java.util.ArrayDeque;

/**
 * StartupManager v41.4.13: "The Shield - Zenith Protocol".
 * Previene crashes durante la secuencia de inicio crítica.
 */
public class StartupManager {

    private final ArrayDeque<Runnable> mStartupActivities;

    public StartupManager() {
        mStartupActivities = new ArrayDeque<>();
    }

    public void addStartupActivity(Runnable runnable) {
        if (runnable != null) {
            mStartupActivities.add(runnable);
        }
    }

    public void startup() {
        while (!mStartupActivities.isEmpty()) {
            Runnable runnable = mStartupActivities.poll();
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.e("StartupManager", "Fallo en actividad de inicio: " + e.getMessage());
                }
            }
        }
    }
}
