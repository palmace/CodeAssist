package com.tyron.builder.compiler;

import com.tyron.builder.compiler.aab.AabTask;
import com.tyron.builder.compiler.dex.R8Task;
import com.tyron.builder.compiler.firebase.GenerateFirebaseConfigTask;
import com.tyron.builder.compiler.incremental.dex.IncrementalD8Task;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinCompiler;
import com.tyron.builder.compiler.incremental.resource.IncrementalAapt2Task;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
import com.tyron.builder.compiler.manifest.ManifestMergeTask;
import com.tyron.builder.compiler.symbol.MergeSymbolsTask;
import com.tyron.builder.compiler.viewbinding.GenerateViewBindingTask;
import com.tyron.builder.crashlytics.CrashlyticsTask;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;

import java.util.ArrayList;
import java.util.List;

/**
 * AndroidAppBundleBuilder v44.10.0: "The Bundle Architect - Zenith Absolute".
 * v44.10.0: Sincronía Total con el Nexo Maestro Zenith.
 * v44.10.0: Activación de AabTask y Sincronía de Interfaz (ViewBinding).
 * v44.10.0: Blindaje de Salida para distribución en Google Play.
 */
public class AndroidAppBundleBuilder extends BuilderImpl<AndroidModule> {

    public AndroidAppBundleBuilder(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public List<Task<? super AndroidModule>> getTasks(BuildType type) {
        AndroidModule module = getModule();
        ILogger logger = getLogger();
        List<Task<? super AndroidModule>> tasks = new ArrayList<>();

        // 1. FASE DE PURGA Y SUMINISTRO
        tasks.add(new CleanTask(getProject(), module, logger));
        tasks.add(new CheckLibrariesTask(getProject(), module, logger));
        
        // 2. FASE DE IDENTIDAD
        tasks.add(new ManifestMergeTask(getProject(), module, logger));
        tasks.add(new GenerateFirebaseConfigTask(getProject(), module, logger));
        tasks.add(new CrashlyticsTask(getProject(), module, logger));
        
        // 3. FASE ESTRUCTURAL (AAB REQUIERE PROTO-FORMAT)
        tasks.add(new IncrementalAapt2Task(getProject(), module, logger, true));
        
        if (module.getSettings().getBoolean(ModuleSettings.VIEW_BINDING_ENABLED, false)) {
            tasks.add(new GenerateViewBindingTask(getProject(), module, logger, true));
        }

        tasks.add(new MergeSymbolsTask(getProject(), module, logger));
        
        // 4. FASE DE TRANSMUTACIÓN DE CÓDIGO
        tasks.add(new IncrementalKotlinCompiler(getProject(), module, logger));
        tasks.add(new IncrementalJavaTask(getProject(), module, logger));
        
        // 5. FASE DE MATERIA BINARIA
        if (module.getSettings().getBoolean(ModuleSettings.USE_R8, false)) {
            tasks.add(new R8Task(getProject(), module, logger));
        } else {
            tasks.add(new IncrementalD8Task(getProject(), module, logger));
        }
        
        // 6. FASE DE ENSAMBLE DE BUNDLE (Finalidad Zenith)
        tasks.add(new AabTask(getProject(), module, logger));

        return tasks;
    }
}
