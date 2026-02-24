package com.tyron.code.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogExporter {
    
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) 
            context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Logs", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show();
    }
    
    public static void saveToFile(Context context, String content, String logType) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", 
                Locale.getDefault()).format(new Date());
            String filename = logType + "_" + timestamp + ".txt";
            
            File dir = new File(context.getExternalFilesDir(null), "exported_logs");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, filename);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            
            Toast.makeText(context, "Logs guardados en: " + file.getAbsolutePath(), 
                Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al guardar: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
}
