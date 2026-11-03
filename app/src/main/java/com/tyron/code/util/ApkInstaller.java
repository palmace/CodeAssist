package com.tyron.code.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

public class ApkInstaller {

    private static final String TAG = "ApkInstaller";

    public static void installApplication(Context context, String applicationId, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "APK file not found: " + filePath);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        
        // En Android 7.0+ (N), es obligatorio usar FileProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Usamos directamente FileProvider de androidx para mayor estabilidad
                Uri apkUri = FileProvider.getUriForFile(context, 
                        context.getPackageName() + ".provider", file);
                
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "Error getting URI for file via FileProvider", e);
                return;
            }
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            Log.d(TAG, "Starting install activity for: " + filePath);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found to handle APK installation!", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during installation trigger", e);
        }
    }
}
