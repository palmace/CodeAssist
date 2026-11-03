package com.developer.crashx.config;

import android.content.Context;

public class CrashConfig {
    public static final int BACKGROUND_MODE_SILENT = 0;
    public static final int BACKGROUND_MODE_SHOW_CUSTOM = 1;
    public static final int BACKGROUND_MODE_CRASH = 2;

    public static class Builder {
        public static Builder create() {
            return new Builder();
        }
        public Builder backgroundMode(int mode) { return this; }
        public Builder enabled(boolean enabled) { return this; }
        public Builder showErrorDetails(boolean show) { return this; }
        public Builder showRestartButton(boolean show) { return this; }
        public Builder logErrorOnRestart(boolean log) { return this; }
        public Builder trackActivities(boolean track) { return this; }
        public void apply() {}
    }
}
