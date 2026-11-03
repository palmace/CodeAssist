package com.tyron.builder.compiler.incremental.resource;

import android.os.Environment;
import com.android.tools.aapt2.Aapt2Jni;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.resource.AAPT2Compiler;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * AAPT2 Task v44.1.5: "The Absolute Anchor - Distribution Mastery".
 * v44.1.5: Sincronía AAB - Soporte para '--proto-format' cuando el despliegue es para Bundle.
 * v44.1.5: Fix - Generación de 'proto-format.zip' para AabTask.
 * v44.1.5: Priorización del manifiesto fusionado y rescate estructural.
 */
public class IncrementalAapt2Task extends Task<AndroidModule> {

    private final boolean mIsAab;

    public IncrementalAapt2Task(Project project, AndroidModule module, ILogger logger, boolean isAab) {
        super(project, module, logger);
        this.mIsAab = isAab;
    }

    @Override
    public String getName() { return "IncrementalAAPT2 (OMNI v44.1.5)"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File compiledDir = new File(buildDir, "bin/res/compiled");
        if (compiledDir.exists()) FileUtils.deleteDirectory(compiledDir);
        compiledDir.mkdirs();
        
        File genDir = new File(buildDir, "gen").getAbsoluteFile();
        if (!genDir.exists()) genDir.mkdirs();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("AAPT2: Iniciando motor Zenith v44.1.5...");
        
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File compiledDir = new File(buildDir, "bin/res/compiled");

        File resDir = getModule().getAndroidResourcesDirectory();
        if (resDir != null && resDir.exists()) {
            File projectOut = new File(compiledDir, "project_res");
            projectOut.mkdirs();
            compileDirectoryForensic(resDir, projectOut);
        }
        
        processAarsForensic(compiledDir);
        linkForensic(compiledDir);
    }

    private void compileDirectoryForensic(File resDir, File outputDir) throws CompilationFailedException {
        File[] types = resDir.listFiles(File::isDirectory);
        if (types == null) return;
        for (File type : types) {
            File[] files = type.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isFile() && !f.getName().startsWith(".")) {
                    if (Aapt2Jni.compile(Arrays.asList(f.getAbsolutePath(), "-o", outputDir.getAbsolutePath()), getLogger()) != 0) {
                        throw new CompilationFailedException("AAPT2: Error en recurso: " + f.getName());
                    }
                }
            }
        }
    }

    private void processAarsForensic(File compiledDir) throws IOException, CompilationFailedException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File explodedBaseDir = new File(buildDir, "bin/res/exploded-aar");
        File[] expandedDirs = explodedBaseDir.listFiles(File::isDirectory);
        if (expandedDirs == null) return;

        for (File extractDir : expandedDirs) {
            File res = new File(extractDir, "res");
            if (res.exists() && res.isDirectory()) {
                String outName = extractDir.getName().replace(".aar", "") + ".zip";
                File outFile = new File(compiledDir, outName);
                Aapt2Jni.compile(Arrays.asList("--dir", res.getAbsolutePath(), "-o", outFile.getAbsolutePath()), getLogger());
            }
        }
    }

    private void linkForensic(File compiledDir) throws CompilationFailedException {
        File sdkJar = findModernSdk();
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File rTxt = new File(buildDir, "bin/res/R.txt");

        List<String> args = new ArrayList<>(Arrays.asList(
            "-I", sdkJar.getAbsolutePath(),
            "--min-sdk-version", String.valueOf(getModule().getMinSdk()),
            "--target-sdk-version", String.valueOf(getModule().getTargetSdk()),
            "--auto-add-overlay", "--allow-reserved-package-id",
            "--output-text-symbols", rTxt.getAbsolutePath(),
            "--no-version-vectors"
        ));

        if (mIsAab) {
            getLogger().info("OMNI: Activando Protocolo de Distribución (--proto-format)");
            args.add("--proto-format");
        }

        File manifest = new File(buildDir, "bin/AndroidManifest.xml").getAbsoluteFile();
        if (!manifest.exists()) {
            manifest = getModule().getManifestFile().getAbsoluteFile();
        }
        
        if (!manifest.exists()) {
            manifest = new File(getModule().getRootFile(), "src/main/AndroidManifest.xml").getAbsoluteFile();
        }
        
        if (!manifest.exists()) throw new CompilationFailedException("AAPT2: Manifiesto ausente.");

        List<File> allCompiled = new ArrayList<>();
        findCompiledFiles(compiledDir, allCompiled);
        
        allCompiled.sort((f1, f2) -> {
            int p1 = getPriority(f1.getName().toLowerCase());
            int p2 = getPriority(f2.getName().toLowerCase());
            if (p1 != p2) return p1 - p2;
            return f1.getName().compareTo(f2.getName());
        });

        for (File f : allCompiled) {
            args.add("-R");
            args.add(f.getAbsolutePath());
        }
        
        args.add("--manifest"); args.add(manifest.getAbsolutePath());
        
        // v44.1.5: Destino de salida dual (APK vs AAB)
        String outputName = mIsAab ? "bin/proto-format.zip" : "bin/generated.apk.res";
        args.add("-o"); args.add(new File(buildDir, outputName).getAbsolutePath());
        
        args.add("--java"); args.add(new File(buildDir, "gen").getAbsolutePath());
        
        String extraPkgs = getDynamicExtraPackages();
        if (!extraPkgs.isEmpty()) { args.add("--extra-packages"); args.add(extraPkgs); }

        if (Aapt2Jni.link(args, getLogger()) != 0) {
            throw new CompilationFailedException("AAPT2: Fallo estructural en enlace.");
        }
        getLogger().info("✅ AAPT2: Link v44.1.5 estabilizado.");
    }

    private int getPriority(String name) {
        if (name.contains("core-")) return 1;
        if (name.contains("appcompat-resources")) return 10;
        if (name.contains("appcompat")) return 11;
        if (name.contains("drawerlayout")) return 12;
        if (name.contains("material")) return 100;
        if (name.contains("project_res")) return 200;
        return 50;
    }

    private String getDynamicExtraPackages() {
        Set<String> pkgs = new LinkedHashSet<>();
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File base = new File(buildDir, "bin/res/exploded-aar");
        File[] dirs = base.listFiles(File::isDirectory);
        if (dirs != null) for (File d : dirs) {
            File m = new File(d, "AndroidManifest.xml");
            if (m.exists()) {
                String p = AAPT2Compiler.getPackageName(m);
                if (p != null && !p.isEmpty()) pkgs.add(p);
            }
        }
        return String.join(":", pkgs);
    }

    private File findModernSdk() {
        String sd = Environment.getExternalStorageDirectory().getPath();
        File f34 = new File(sd, "/Android_install/Sdk/platforms/android-34/android.jar");
        return f34.exists() ? f34 : BuildModule.getAndroidJar();
    }

    private void findCompiledFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findCompiledFiles(f, list);
            else if (f.getName().endsWith(".zip") || f.getName().endsWith(".flat")) list.add(f);
        }
    }
}
