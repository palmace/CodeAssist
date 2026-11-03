package com.tyron.builder.compiler;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.tyron.builder.BuildModule;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor Zenith v44.25.12: "The Absolute Sovereign".
 * v44.25.12: Optimización de flujo para el Library Guard v44.25.6.
 * v44.25.11: Telemetría térmica avanzada para dispositivos Redmi 13C.
 */
public abstract class BuilderImpl<T extends Module> implements Builder<T> {

    private final Handler mMainHandler;
    private final Project mProject;
    private final T mModule;
    private final ILogger mLogger;
    private final List<Task<? super T>> mTasksRan;
    private TaskListener mTaskListener;

    public BuilderImpl(Project project, T module, ILogger logger) {
        mProject = project;
        mModule = module;
        mLogger = logger;
        mMainHandler = new Handler(Looper.getMainLooper());
        mTasksRan = new ArrayList<>();
    }

    @Override public Project getProject() { return mProject; }
    @Override public void setTaskListener(TaskListener taskListener) { mTaskListener = taskListener; }
    @Override public T getModule() { return mModule; }

    protected void updateProgress(String name, String message, int progress) {
        if (mTaskListener != null) mTaskListener.onTaskStarted(name, message, progress);
    }

    @Override
    public final void build(BuildType type) throws CompilationFailedException, IOException {
        mTasksRan.clear();
        List<Task<? super T>> tasks = getTasks(type);
        
        System.runFinalization();
        System.gc();

        for (int i = 0; i < tasks.size(); i++) {
            Task<? super T> task = tasks.get(i);
            
            // TELEMETRÍA TÉRMICA Y ESTADO DE MEMORIA
            String forensic = "[FORENSIC-PRE] " + BuildModule.getForensicStatus();
            mLogger.info(forensic);
            Log.i("OMNI_LOG", forensic);

            String status = String.format("🚀 Zenith: [%d/%d] Ejecutando %s...", (i+1), tasks.size(), task.getName());
            mLogger.info(status);
            Log.i("OMNI_LOG", status);
            
            try {
                final int currentIdx = i;
                mMainHandler.post(() -> updateProgress(task.getName(), "Zenith Active",
                        (int) (((float)currentIdx / (float) tasks.size()) * 100f)));
                
                task.prepare(type);
                task.run();
                
            } catch (Throwable e) {
                String forensicFail = "[FORENSIC-FAIL] " + BuildModule.getForensicStatus();
                mLogger.info(forensicFail);
                Log.e("OMNI_LOG", forensicFail);
                
                task.clean();
                mTasksRan.forEach(Task::clean);
                
                // Si es un error de Library Guard, no lo envolvemos más para no perder el formato del mensaje instructivo
                if (e instanceof CompilationFailedException) {
                    throw (CompilationFailedException) e;
                }
                throw new CompilationFailedException("🚨 OMNI CRITICAL FAIL en " + task.getName() + ": " + e.getMessage(), e);
            }
            mTasksRan.add(task);
            task.clean();
            System.gc();
        }
        String finalMsg = "✅ Zenith: Sincronía Total Alcanzada. " + BuildModule.getForensicStatus();
        mLogger.info(finalMsg);
        Log.i("OMNI_LOG", finalMsg);
    }

    public abstract List<Task<? super T>> getTasks(BuildType type);
    @Override public ILogger getLogger() { return mLogger; }
}
