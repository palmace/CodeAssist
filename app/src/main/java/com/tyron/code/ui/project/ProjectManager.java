package com.tyron.code.ui.project;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.base.Charsets;
import com.tyron.builder.BuildModule;
import com.tyron.builder.SovereignConstants;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidContentRoot;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.code.service.KotlinService;
import com.tyron.code.template.CodeTemplate;
import com.tyron.code.util.ProjectSymbolIndex;
import com.tyron.code.util.ProjectUtils;
import com.tyron.completion.progress.ProgressManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ProjectManager v41.4.30: "The Absolute Sovereign - Active Orchestrator".
 * v41.4.30: Sincronía Activa - Invalida la caché de librerías y símbolos tras crear nuevos archivos.
 * v41.4.30: Blindaje de Suministros - Asegura la detección del modelo OMNI en estructuras complejas.
 */
public class ProjectManager {

    private static final String TAG = "ProjectManager";

    public interface TaskListener {
        void onTaskStarted(String message);
        void onComplete(Project project, boolean success, String message);
    }

    public interface OnProjectOpenListener {
        void onProjectOpen(Project project);
    }

    private static volatile ProjectManager INSTANCE = null;

    public static synchronized ProjectManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProjectManager();
        }
        return INSTANCE;
    }

    private final List<OnProjectOpenListener> mProjectOpenListeners = new ArrayList<>();
    private volatile Project mCurrentProject;
    private final ProjectSymbolIndex mSymbolIndex = new ProjectSymbolIndex();

    private ProjectManager() {}

    public void addOnProjectOpenListener(OnProjectOpenListener listener) {
        mProjectOpenListeners.add(listener);
        if (mCurrentProject != null) {
            try { listener.onProjectOpen(mCurrentProject); } catch (Throwable ignored) {}
        }
    }

    public void removeOnProjectOpenListener(OnProjectOpenListener listener) {
        mProjectOpenListeners.remove(listener);
    }

    public synchronized Project getCurrentProject() {
        return mCurrentProject;
    }

    public ProjectSymbolIndex getSymbolIndex() {
        return mSymbolIndex;
    }

    public void openProject(Project project, boolean downloadLibs, TaskListener listener, ILogger logger) {
        ProgressManager.getInstance().runNonCancelableAsync(() -> doOpenProject(project, downloadLibs, listener, logger));
    }

    private void doOpenProject(Project project, boolean downloadLibs, TaskListener mListener, ILogger logger) {
        mCurrentProject = project;
        mListener.onTaskStarted("Reconstruyendo modelo OMNI Zenith...");
        
        boolean success = false;
        try {
            project.clear();
            loadSimpleProjectModel(project);
            success = !project.getModules().isEmpty();
            
            if (success) {
                mListener.onTaskStarted("Sincronizando Arsenal de Rescate...");
                // Notificar cambios al motor de resolución de librerías
                for (Module m : project.getModules()) {
                    if (m instanceof AndroidModuleImpl) {
                        ((AndroidModuleImpl) m).invalidateLibraryCache();
                    }
                }
                
                mListener.onTaskStarted("Indexando jerarquía de símbolos...");
                mSymbolIndex.indexProject(project);
                warmUpKotlinEngine();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fallo estructural en modelo OMNI", e);
            if (logger != null) logger.error("Error estructural: " + e.getMessage());
        }

        if (success) {
            project.setIndexing(false);
            mListener.onComplete(project, true, "Soberanía de Modelo Establecida");
            notifyProjectOpen();
        } else {
            mListener.onComplete(project, false, "Fallo de anclaje: Sin módulos válidos.");
        }
    }

    private void warmUpKotlinEngine() {
        Context context = BuildModule.getContext();
        if (context != null) {
            Intent intent = new Intent()
                    .setComponent(new ComponentName(context.getPackageName(), "com.tyron.code.service.KotlinService"))
                    .setAction(SovereignConstants.ACTION_DIAGNOSTIC);
            context.startService(intent);
        }
    }

    private void loadSimpleProjectModel(Project project) throws IOException {
        File rootDir = project.getRootFile().getAbsoluteFile();
        File appDir = new File(rootDir, "app");
        File targetDir = (appDir.exists() && appDir.isDirectory()) ? appDir : rootDir;
        
        AndroidModuleImpl module = new AndroidModuleImpl(targetDir);
        module.setName(targetDir.getName().equals("app") ? ":app" : ":" + targetDir.getName());
        
        File srcMain = new File(targetDir, "src/main");
        if (!srcMain.exists()) srcMain = targetDir;
        
        AndroidContentRoot contentRoot = new AndroidContentRoot(srcMain);
        List<File> sourceDirs = new ArrayList<>();
        File javaDir = new File(srcMain, "java");
        File tutorialDir = new File(srcMain, "kotlin");
        
        if (javaDir.exists()) sourceDirs.add(javaDir);
        if (tutorialDir.exists()) sourceDirs.add(tutorialDir);
        if (sourceDirs.isEmpty()) sourceDirs.add(srcMain);
        
        contentRoot.setJavaDirectories(sourceDirs);
        
        File resDir = new File(srcMain, "res");
        if (resDir.exists()) {
            contentRoot.setResourceDirectories(Collections.singletonList(resDir));
        }
        
        module.addContentRoot(contentRoot);
        project.addModule(module);
        
        module.open();
        module.index();
    }

    private void notifyProjectOpen() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mCurrentProject == null) return;
            for (OnProjectOpenListener l : mProjectOpenListeners) {
                try { l.onProjectOpen(mCurrentProject); } catch (Throwable ignore) {}
            }
        });
    }

    public void closeProject(@NonNull Project project) {
        if (project.equals(mCurrentProject)) mCurrentProject = null;
    }

    public static File createFile(File directory, String name, CodeTemplate template) throws IOException {
        template.setup();
        String code = template.get().replace(CodeTemplate.CLASS_NAME, name);
        File file = new File(directory, name + template.getExtension());
        if (!file.exists()) file.createNewFile();
        FileUtils.writeStringToFile(file, code, Charsets.UTF_8);
        
        // Notificación al motor OMNI para re-indexado parcial
        invalidateProjectState(file);
        
        return file;
    }

    public static File createClass(File directory, String className, CodeTemplate template) throws IOException {
        String packageName = ProjectUtils.getPackageName(directory);
        if (packageName == null) packageName = "com.example.app"; 
        
        template.setup();
        String code = template.get()
                .replace(CodeTemplate.PACKAGE_NAME, packageName)
                .replace(CodeTemplate.CLASS_NAME, className);

        File classFile = new File(directory, className + template.getExtension());
        if (!classFile.exists()) classFile.createNewFile();
        FileUtils.writeStringToFile(classFile, code, Charsets.UTF_8);

        // Notificación al motor OMNI
        invalidateProjectState(classFile);
        
        return classFile;
    }

    /**
     * OMNI v41.4.30: Invalida el estado del proyecto para forzar la visibilidad de nuevos elementos.
     */
    private static void invalidateProjectState(File affectedFile) {
        Project project = getInstance().getCurrentProject();
        if (project != null) {
            Module module = project.getModule(affectedFile);
            if (module instanceof AndroidModuleImpl) {
                ((AndroidModuleImpl) module).invalidateLibraryCache();
            }
        }
    }
}
