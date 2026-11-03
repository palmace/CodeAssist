package com.tyron.code;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.tyron.code.ui.project.ProjectManagerFragment;

/**
 * MainActivity v41.4.15: "The Titan - Sovereign Protection".
 * v41.4.15: Implementada petición automática de POST_NOTIFICATIONS para Android 13/14.
 * v41.4.15: Blindaje de servicios de primer plano contra el LMK de Xiaomi.
 */
public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "Permiso de notificaciones concedido.");
                } else {
                    Log.w("MainActivity", "Permiso de notificaciones denegado. El motor de compilación podría ser matado por el sistema.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Window window = getWindow();
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(true); 
                controller.setAppearanceLightNavigationBars(false);
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Fallo al configurar insets: " + e.getMessage());
        }

        setContentView(R.layout.main);

        // BLINDAJE ANDROID 13/14: Solicitar permiso de notificaciones para evitar muerte de servicios FGS
        checkNotificationPermission();

        if (savedInstanceState == null) {
            getWindow().getDecorView().post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (getSupportFragmentManager().findFragmentByTag(ProjectManagerFragment.TAG) == null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new ProjectManagerFragment(),
                                         ProjectManagerFragment.TAG)
                                .commitAllowingStateLoss();
                    }
                }
            });
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return super.onKeyShortcut(keyCode, event);
    }
}
