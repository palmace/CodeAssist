package com.tyron.code.ui.wizard;

import android.util.Log;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WizardTemplate {

    public static WizardTemplate fromFile(File parent) {
        if (parent == null || !parent.exists() || !parent.isDirectory()) {
            return null;
        }

        File infoFile = new File(parent, "info.json");
        if (!infoFile.exists()) {
            return null;
        }

        WizardTemplate template = new WizardTemplate();
        try {
            String jsonContent = FileUtils.readFileToString(infoFile, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonContent);
            
            // Usamos opt para ser tolerantes a cambios en el formato de las plantillas originales
            template.setName(jsonObject.optString("name", parent.getName()));
            template.setMinSdk(jsonObject.optInt("minSdk", 24));
            template.setPath(parent.getAbsolutePath());
            template.setSupportsJava(jsonObject.optBoolean("supportsJava", true));
            template.setSupportsKotlin(jsonObject.optBoolean("supportsKotlin", false));
            
            return template;
        } catch (JSONException | IOException e) {
            Log.e("WizardTemplate", "Error parsing template at " + parent.getAbsolutePath(), e);
            return null;
        }
    }

    private String name;
    private int minSdk;
    private String path;
    private boolean supportsKotlin;
    private boolean supportsJava;

    public WizardTemplate() {
    }

    public int getMinSdk() {
        return minSdk;
    }

    public void setMinSdk(int minSdk) {
        this.minSdk = minSdk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSupportsJava() {
        return supportsJava;
    }

    public void setSupportsJava(boolean supportsJava) {
        this.supportsJava = supportsJava;
    }

    public boolean isSupportsKotlin() {
        return supportsKotlin;
    }

    public void setSupportsKotlin(boolean supportsKotlin) {
        this.supportsKotlin = supportsKotlin;
    }
}
