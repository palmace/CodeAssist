package com.tyron.builder.compiler.dex;

import androidx.annotation.VisibleForTesting;

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.OutputMode;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.incremental.dex.IncrementalD8Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.common.util.Cache;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaD8Task v44.9.0: "The Static Quantum Bridge - Zenith Absolute".
 * v44.9.0: Sincronía con Reality Seal - Uso del Radar Zenith para rescate de 93+ unidades.
 * v44.9.0: Protocolo de Rutas Absolutas - Eliminación de desvanecimiento de materia binaria.
 * v44.9.0: Blindaje de Desugaring - Soporte para Java 8+ en despliegue estático.
 */
public class JavaD8Task extends Task<JavaModule> {

    public JavaD8Task(Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "D8 Dexer (Static v44.9.0)";
    }

    private DiagnosticsHandler diagnosticsHandler;
    private List<Path> mFilesToCompile;
    private Path mOutputPath;
    private BuildType mBuildType;

    @Override
    public void prepare(BuildType type) throws IOException {
        mBuildType = type;
        diagnosticsHandler = new DexDiagnosticHandler(getLogger(), getModule());
        
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File output = new File(buildDir, "bin");
        if (!output.exists() && !output.mkdirs()) {
            throw new IOException("Unable to create output directory");
        }
        mOutputPath = output.toPath();

        mFilesToCompile = new ArrayList<>();
        
        // v44.9.0: Localización absoluta de clases transmutadas
        File javaClasses = new File(buildDir, "bin/java/classes");
        if (javaClasses.exists()) {
             mFilesToCompile.addAll(D8Task.getClassFiles(javaClasses));
        }
        
        File kotlinClasses = new File(buildDir, "bin/kotlin/classes");
        if (kotlinClasses.exists()) {
            mFilesToCompile.addAll(D8Task.getClassFiles(kotlinClasses));
        }
        
        File kaptClasses = new File(buildDir, "bin/generated/kapt/classes");
        if (kaptClasses.exists()) {
            mFilesToCompile.addAll(D8Task.getClassFiles(kaptClasses));
        }
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: Iniciando Dexing Estático Zenith v44.9.0...");
        
        try {
            List<File> libraries = getModule().getLibraries();
            List<Path> libraryPaths = new ArrayList<>();
            List<Path> classpathPaths = new ArrayList<>();
            
            File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
            File androidJar = BuildModule.getAndroidJar();
            if (androidJar != null) classpathPaths.add(androidJar.toPath());
            
            // v44.9.0: Integración masiva vía Radar Zenith
            for (File lib : libraries) {
                if (lib == null || !lib.exists()) continue;
                
                String name = lib.getName().toLowerCase();
                if (name.contains("stubs") || name.contains("d8") || name.contains("compiler") || name.contains("annotation")) {
                    classpathPaths.add(lib.toPath());
                    continue;
                }

                if (name.endsWith(".aar")) {
                    File classesJar = new File(buildDir, "bin/res/exploded-aar/" + lib.getName().replace(".aar", "") + "_ext/classes.jar");
                    if (classesJar.exists()) libraryPaths.add(classesJar.toPath());
                } else {
                    libraryPaths.add(lib.toPath());
                }
            }

            int minApi = getModule() instanceof AndroidModule ? ((AndroidModule)getModule()).getMinSdk() : 24;
            if (minApi < 24) minApi = 24;

            D8Command.Builder builder = D8Command.builder(diagnosticsHandler)
                    .setMode(mBuildType == BuildType.RELEASE ? CompilationMode.RELEASE : CompilationMode.DEBUG)
                    .setMinApiLevel(minApi)
                    .addClasspathFiles(classpathPaths)
                    .addProgramFiles(mFilesToCompile)
                    .addProgramFiles(libraryPaths)
                    .setOutput(mOutputPath, OutputMode.DexIndexed);

            getLogger().info("OMNI: Procesando " + mFilesToCompile.size() + " clases y " + libraryPaths.size() + " librerías.");
            
            D8.run(builder.build());
            
            System.runFinalization();
            System.gc();
            
            getLogger().info("✅ OMNI v44.9.0: Dexing estático completado.");
            
        } catch (com.android.tools.r8.CompilationFailedException e) {
            throw new CompilationFailedException("Fallo en Nexo Estático Zenith: " + e.getMessage(), e);
        }
    }

    @Override
    protected void clean() {
        super.clean();
    }
}
