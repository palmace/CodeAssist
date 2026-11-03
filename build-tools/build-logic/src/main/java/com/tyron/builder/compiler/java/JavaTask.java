package com.tyron.builder.compiler.java;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.model.SourceFileObject;
import com.tyron.builder.parser.FileManager;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaTask v44.9.0: "The Static Sovereign - Zenith Absolute".
 * v44.9.0: Sincronía con Reality Seal - Uso del Radar Zenith para rescate de 93+ unidades.
 * v44.9.0: Protocolo de Rutas Absolutas - Eliminación de desvanecimiento de Classpath.
 * v44.9.0: Blindaje de Interoperabilidad con Kotlin/KAPT en modo estático.
 */
public class JavaTask extends Task<JavaModule> {

    private List<File> mCompiledFiles;

    public JavaTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Java Compiler (Static v44.9.0)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        File outputDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin/classes");
        if (outputDir.exists()) {
            org.apache.commons.io.FileUtils.deleteQuietly(outputDir);
        }
        outputDir.mkdirs();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        compile();
    }

    @SuppressLint("NewApi")
    public void compile() throws CompilationFailedException {
        long startTime = System.currentTimeMillis();
        getLogger().info("🚀 OMNI: Iniciando Compilación Estática Zenith v44.9.0...");

        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File outputDir = new File(buildDir, "bin/classes");
        
        List<File> javaFiles = collectJavaFiles();
        if (javaFiles.isEmpty()) {
            getLogger().warning("⚠️ No se detectaron fuentes Java para transmutar.");
            return;
        }

        mCompiledFiles = new ArrayList<>(javaFiles);

        DiagnosticListener<JavaFileObject> diagnosticCollector = diagnostic -> {
            switch (diagnostic.getKind()) {
                case ERROR:
                    getLogger().error(new DiagnosticWrapper(diagnostic));
                    break;
                case WARNING:
                    getLogger().warning(new DiagnosticWrapper(diagnostic));
                    break;
            }
        };

        JavacTool tool = JavacTool.create();
        StandardJavaFileManager fm = tool.getStandardFileManager(diagnosticCollector, Locale.getDefault(), Charset.defaultCharset());

        try {
            // 1. ANCLAJE DE SALIDA
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir));

            // 2. BOOTSTRAP Y LIBRERÍAS (RADAR ZENITH)
            List<File> bootClasspath = new ArrayList<>();
            if (BuildModule.getAndroidJar() != null) bootClasspath.add(BuildModule.getAndroidJar().getAbsoluteFile());
            if (BuildModule.getLambdaStubs() != null) bootClasspath.add(BuildModule.getLambdaStubs().getAbsoluteFile());
            
            fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, bootClasspath);

            Set<File> classpath = new LinkedHashSet<>();
            // Suministro vía Radar Zenith (AARs y JARs)
            List<File> projectLibs = getModule().getLibraries();
            for (File lib : projectLibs) {
                if (lib != null && lib.exists()) {
                    if (lib.getName().endsWith(".aar")) {
                        File jar = new File(buildDir, "bin/res/exploded-aar/" + lib.getName().replace(".aar", "") + "_ext/classes.jar");
                        classpath.add(jar.exists() ? jar.getAbsoluteFile() : lib.getAbsoluteFile());
                    } else {
                        classpath.add(lib.getAbsoluteFile());
                    }
                }
            }

            // Inyección de Interoperabilidad
            File kotlinClasses = new File(buildDir, "bin/kotlin/classes");
            if (kotlinClasses.exists()) classpath.add(kotlinClasses.getAbsoluteFile());
            
            File kaptClasses = new File(buildDir, "bin/generated/kapt/classes");
            if (kaptClasses.exists()) classpath.add(kaptClasses.getAbsoluteFile());

            fm.setLocation(StandardLocation.CLASS_PATH, new ArrayList<>(classpath));
            
            // 3. FUENTES
            List<JavaFileObject> javaFileObjects = new ArrayList<>();
            for (File file : javaFiles) {
                javaFileObjects.add(new SourceFileObject(file.toPath()));
            }

            List<String> options = new ArrayList<>(Arrays.asList(
                    "-source", "1.8",
                    "-target", "1.8",
                    "-proc:none",
                    "-g",
                    "-encoding", "UTF-8",
                    "-bootclasspath", bootClasspath.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator))
            ));

            getLogger().info("OMNI: Classpath estático anclado con " + classpath.size() + " unidades tácticas.");
            
            JavacTask task = (JavacTask) tool.getTask(null, fm, diagnosticCollector, options, null, javaFileObjects);

            if (!task.call()) {
                throw new CompilationFailedException("La transmutación estática ha fallado. Revisa los diagramas de diagnóstico.");
            }

            Log.d("JavaCompiler", "Zenith Compilation took: " + (System.currentTimeMillis() - startTime) + " ms");
            getLogger().info("✅ OMNI v44.9.0: Compilación estática completada.");

        } catch (IOException e) {
            throw new CompilationFailedException("Fallo estructural en el sistema de archivos: " + e.getMessage());
        } finally {
            try { fm.close(); } catch (IOException ignored) {}
        }
    }

    private List<File> collectJavaFiles() {
        List<File> files = new ArrayList<>();
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File root = getModule().getRootFile().getAbsoluteFile();
        
        File[] searchDirs = {
            new File(root, "src/main/java"),
            new File(root, "app/src/main/java"),
            new File(buildDir, "gen"),
            new File(buildDir, "generated/source"),
            new File(buildDir, "bin/generated/kapt/sources"),
            new File(buildDir, "bin/generated/data_binding_base_class_source_out")
        };
        
        for (File dir : searchDirs) {
            if (dir.exists()) {
                files.addAll(getJavaFilesRecursive(dir));
            }
        }
        return files.stream().distinct().collect(Collectors.toList());
    }

    private Set<File> getJavaFilesRecursive(File dir) {
        Set<File> javaFiles = new HashSet<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(getJavaFilesRecursive(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file.getAbsoluteFile());
                }
            }
        }
        return javaFiles;
    }

    @VisibleForTesting
    public List<File> getCompiledFiles() { return mCompiledFiles; }
}
