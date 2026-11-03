package com.tyron.common.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.common.ApplicationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * AndroidUtilities v30.4 [The Sovereign Unification - Full Spectrum Signature].
 * v30.4: Inyectada firma (String, @StringRes) para cobertura total de alertas.
 * v30.3: Firmas hiper-explícitas para evitar ambigüedad de tipos en el compilador Java.
 */
public class AndroidUtilities {

    public static void showToast(String message) {
        if (message == null) return;
        try {
            Toast.makeText(ApplicationProvider.getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {}
    }

    public static void showToast(@StringRes int id) {
        try {
            Toast.makeText(ApplicationProvider.getApplicationContext(), id, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    public static int dp(float px) {
        try {
            return Math.round(ApplicationProvider.getApplicationContext()
                    .getResources().getDisplayMetrics().density * px);
        } catch (Exception e) {
            return (int) px;
        }
    }

    public static int dpToPx(float dp) {
        try {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    dp, ApplicationProvider.getApplicationContext().getResources().getDisplayMetrics()));
        } catch (Exception e) {
            return (int) dp;
        }
    }

    public static void setMargins(View view, int start, int top, int end, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.setMargins(dp(start), dp(top), dp(end), dp(bottom));
            lp.setMarginStart(dp(start));
            lp.setMarginEnd(dp(end));
            view.setLayoutParams(lp);
        }
    }

    public static void hideKeyboard(View view) {
        if (view == null) return;
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    public static Activity getActivity(Context context) {
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper) return getActivity(((ContextWrapper) context).getBaseContext());
        return null;
    }

    public static String calculateMD5(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            return String.format("%32s", bigInt.toString(16)).replace(' ', '0');
        } catch (Exception e) {
            return null;
        }
    }

    public static int getRowCount(int itemWidthPx) {
        if (itemWidthPx <= 0) return 1;
        DisplayMetrics dm = ApplicationProvider.getApplicationContext().getResources().getDisplayMetrics();
        return Math.max(1, dm.widthPixels / itemWidthPx);
    }

    public static int getHeight(ViewGroup vg) {
        int height = 0;
        for (int i = 0; i < vg.getChildCount(); i++) {
            height += vg.getChildAt(i).getMeasuredHeight();
        }
        return height;
    }

    /** FIRMA A: Texto puro */
    public static void showSimpleAlert(Context context, String title, String message) {
        showTacticalAlert(context, title, message, "OK", null, null, null, null);
    }

    /** FIRMA B: Título como Recurso, Mensaje como Texto */
    public static void showSimpleAlert(Context context, @StringRes int titleRes, String message) {
        showSimpleAlert(context, context.getString(titleRes), message);
    }

    /** FIRMA C: Título y Mensaje como Recursos */
    public static void showSimpleAlert(Context context, @StringRes int titleRes, @StringRes int messageRes) {
        showSimpleAlert(context, context.getString(titleRes), context.getString(messageRes));
    }
    
    /** FIRMA D: Título como Texto, Mensaje como Recurso (v30.4) */
    public static void showSimpleAlert(Context context, String title, @StringRes int messageRes) {
        showSimpleAlert(context, title, context.getString(messageRes));
    }

    public static void showTacticalAlert(Context context, String title, String message, String positiveText, @Nullable Runnable onPositive, @Nullable String negativeText, @Nullable Runnable onNegative, @Nullable View customView) {
        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(title);
            if (message != null) builder.setMessage(message);
            if (customView != null) builder.setView(customView);
            if (positiveText != null) {
                builder.setPositiveButton(positiveText, (d, w) -> { if (onPositive != null) onPositive.run(); });
            }
            if (negativeText != null) {
                builder.setNegativeButton(negativeText, (d, w) -> { if (onNegative != null) onNegative.run(); });
            }
            builder.show();
        } catch (Exception e) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                if (message != null) builder.setMessage(message);
                if (customView != null) builder.setView(customView);
                builder.setPositiveButton(positiveText, (d, w) -> { if (onPositive != null) onPositive.run(); });
                builder.setNegativeButton(negativeText, (d, w) -> { if (onNegative != null) onNegative.run(); });
                builder.show();
            } catch (Exception ignored) {}
        }
    }

    public static void copyToClipboardSovereign(String text, boolean showToast) {
        try {
            ClipboardManager cm = (ClipboardManager) ApplicationProvider.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("", text));
                if (showToast) {
                    showToast("Copied to clipboard");
                }
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    public static CharSequence getPrimaryClip() {
        try {
            ClipboardManager cm = (ClipboardManager) ApplicationProvider.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData data = cm.getPrimaryClip();
                if (data != null && data.getItemCount() > 0) {
                    return data.getItemAt(0).getText();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
