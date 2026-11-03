package com.tyron.code.ui.main.action.project;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.common.util.AndroidUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * TerminalAction v2026.121: "The Sovereign Terminal - Final Polish".
 * v2026.121: Copia Selectiva en Resultados - Habilitado textIsSelectable en el output.
 * v2026.120: Inyectado comando 'tree' virtual y formateador de 'ls' táctico.
 */
public class TerminalAction extends AnAction {

    public static final String ID = "terminalAction";

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Context context = e.getData(CommonDataKeys.CONTEXT);
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (context == null || project == null) return;

        EditText input = new EditText(context);
        input.setHint("Comandos: tree, ls, o curl de rescate...");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextSize(14);
        
        FrameLayout container = new FrameLayout(context);
        int p = AndroidUtilities.dp(20);
        container.setPadding(p, p / 2, p, 0);
        container.addView(input);

        AndroidUtilities.showTacticalAlert(context, 
            "📟 TERMINAL OMNI", 
            "Entorno de Rescate: " + project.getRootFile().getName(), 
            "EJECUTAR", () -> {
                String command = input.getText().toString().trim();
                if (!command.isEmpty()) executeCommand(context, project.getRootFile(), command);
            }, 
            "CANCELAR", null, container);
    }

    private void executeCommand(Context context, File root, String command) {
        if (command.equalsIgnoreCase("tree")) {
            StringBuilder treeBuilder = new StringBuilder("📁 " + root.getName() + "\n");
            generateTree(root, "", true, treeBuilder);
            showOutputDialog(context, treeBuilder.toString());
            return;
        }

        if (command.equalsIgnoreCase("ls")) {
            File[] files = root.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                StringBuilder lsBuilder = new StringBuilder();
                for (File f : files) {
                    lsBuilder.append(f.isDirectory() ? "📁 " : "📄 ")
                             .append(f.getName()).append("\n");
                }
                showOutputDialog(context, lsBuilder.toString());
                return;
            }
        }

        new Thread(() -> {
            StringBuilder output = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(
                    new String[]{"/system/bin/sh", "-c", command}, 
                    null, 
                    root
                );
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append("\n");
                while ((line = errReader.readLine()) != null) output.append("ERR: ").append(line).append("\n");
                
                process.waitFor();
                
                if (AndroidUtilities.getActivity(context) != null) {
                    AndroidUtilities.getActivity(context).runOnUiThread(() -> showOutputDialog(context, output.toString()));
                }
                
            } catch (Exception e) {
                if (AndroidUtilities.getActivity(context) != null) {
                    AndroidUtilities.getActivity(context).runOnUiThread(() -> 
                        Toast.makeText(context, "❌ Error de ejecución: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void generateTree(File folder, String prefix, boolean isLast, StringBuilder sb) {
        File[] files = folder.listFiles(f -> !f.getName().startsWith("."));
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (int i = 0; i < files.length; i++) {
            boolean lastItem = (i == files.length - 1);
            sb.append(prefix).append(lastItem ? "└── " : "├── ")
              .append(files[i].isDirectory() ? "📁 " : "📄 ")
              .append(files[i].getName()).append("\n");
            if (files[i].isDirectory()) {
                generateTree(files[i], prefix + (lastItem ? "    " : "│   "), lastItem, sb);
            }
        }
    }

    private void showOutputDialog(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text.isEmpty() ? "Comando ejecutado sin salida (Success)." : text);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(13);
        textView.setPadding(40, 40, 40, 40);
        textView.setTextColor(0xFFE0E0E0);
        textView.setBackgroundColor(0xFF121212);
        // OMNI v2026.121: Habilitar copia selectiva en la terminal
        textView.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(textView);

        AndroidUtilities.showTacticalAlert(context, "📡 RESULTADO TERMINAL", null, "OK", null, null, null, scroll);
    }

    @Override
    public void update(@NonNull AnActionEvent e) {
        Context context = e.getData(CommonDataKeys.CONTEXT);
        e.getPresentation().setText("📟 Terminal OMNI");
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
        
        if (context != null) {
            e.getPresentation().setIcon(ContextCompat.getDrawable(context, R.drawable.ic_baseline_code_24));
        }
    }
}
