package com.tyron.builder.compiler.dex;

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.OutputMode;
import com.android.tools.r8.R8;
import com.android.tools.r8.R8Command;
import com.android.tools.r8.origin.Origin;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * R8Task v44.8.0: "The Zenith Shrinker - Sovereign Optimization".
 * v44.8.0: Sincronía con Reality Seal - Uso de rutas absolutas ancladas.
 * v44.8.0: Protocolo de Integridad de Materia - Inclusión de KAPT y clases generadas.
 * v44.8.0: Optimización de memoria agresiva para el despliegue final en Release.
 */
public class R8Task extends Task<AndroidModule> {

    public R8Task(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() { return "R8 Shrinker (OMNI v44.8.0)"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        File binDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin");
        if (!binDir.exists()) binDir.mkdirs();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: [Shrinker] Iniciando optimización Zenith v44.8.0...");
        
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File output = new File(buildDir, "bin");
        File mappingOutput = new File(output, "proguard-mapping.txt");
        
        try {
            R8Command.Builder builder = R8Command.builder(new DexDiagnosticHandler(getLogger(), getModule()))
                    .setMode(CompilationMode.RELEASE)
                    .setMinApiLevel(getModule().getMinSdk() < 24 ? 24 : getModule().getMinSdk())
                    .setProguardMapOutputPath(mappingOutput.toPath())
                    .setOutput(output.toPath(), OutputMode.DexIndexed);

            // 1. INYECCIÓN DE CLASES DEL PROYECTO
            File javaClasses = new File(buildDir, "bin/java/classes");
            if (javaClasses.exists()) builder.addProgramFiles(javaClasses.toPath());
            
            File kotlinClasses = new File(buildDir, "bin/kotlin/classes");
            if (kotlinClasses.exists()) builder.addProgramFiles(kotlinClasses.toPath());
            
            File kaptClasses = new File(buildDir, "bin/generated/kapt/classes");
            if (kaptClasses.exists()) builder.addProgramFiles(kaptClasses.toPath());

            // 2. INYECCIÓN DE LIBRERÍAS (AARs y JARs)
            File explodedDir = new File(buildDir, "bin/res/exploded-aar");
            if (explodedDir.exists()) {
                Collection<File> jars = FileUtils.listFiles(explodedDir, new String[]{"jar"}, true);
                for (File jar : jars) {
                    if (jar.length() > 0) builder.addProgramFiles(jar.toPath());
                }
            }
            
            // 3. RADAR ZENITH: Librerías directas
            List<File> projectLibs = getModule().getLibraries();
            for (File lib : projectLibs) {
                if (lib != null && lib.exists() && lib.getName().endsWith(".jar")) {
                    builder.addProgramFiles(lib.toPath());
                }
            }

            // 4. CLASSPATH Y REGLAS
            builder.addLibraryFiles(getLibraryFiles());
            builder.addProguardConfiguration(getDefaultProguardRule(), Origin.unknown());
            
            List<Path> rules = getProguardRules();
            if (!rules.isEmpty()) {
                builder.addProguardConfigurationFiles(rules);
            }

            getLogger().info("OMNI: Ejecutando R8 Zenith Engine...");
            R8.run(builder.build());
            
            System.runFinalization();
            System.gc();
            
            getLogger().info("✅ OMNI v44.8.0: Optimización Release completada.");
        } catch (com.android.tools.r8.CompilationFailedException e) {
            throw new CompilationFailedException("Fallo en optimización Zenith R8: " + e.getMessage(), e);
        }
    }

    private List<String> getDefaultProguardRule() {
        return Arrays.asList(
            "-dontwarn **", 
            "-keep class **.R$* { public static <fields>; }", 
            "-keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator CREATOR; }",
            "-keep class com.tyron.** { *; }",
            "-keepattributes *Annotation*",
            "-keepattributes Signature",
            "-keepattributes InnerClasses",
            "-keepattributes EnclosingMethod",
            "-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*"
        );
    }

    private List<Path> getProguardRules() {
        List<Path> rules = new ArrayList<>();
        File root = getModule().getRootFile().getAbsoluteFile();
        
        File custom = new File(root, "proguard-rules.pro");
        if (custom.exists()) rules.add(custom.toPath());
        
        File aapt = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin/res/generated-rules.txt");
        if (aapt.exists()) rules.add(aapt.toPath());
        
        return rules;
    }

    private List<Path> getLibraryFiles() {
        List<Path> path = new ArrayList<>();
        File androidJar = BuildModule.getAndroidJar();
        if (androidJar != null && androidJar.exists()) {
            path.add(androidJar.toPath());
        }
        
        File lambdaStubs = BuildModule.getLambdaStubs();
        if (lambdaStubs != null && lambdaStubs.exists()) {
            path.add(lambdaStubs.toPath());
        }

        return path;
    }
}
