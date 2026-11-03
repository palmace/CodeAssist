package com.tyron.builder.project.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.compiler.manifest.xml.AndroidManifestParser;
import com.tyron.builder.compiler.manifest.xml.ManifestData;
import com.tyron.builder.model.CodeAssistLibrary;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidContentRoot;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.ContentRoot;
import com.tyron.common.util.StringSearch;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AndroidModuleImpl v44.25.88: "The Zenith Sovereign - Thermal Anchor".
 * v44.25.88: Thermal Anchor - Optimización de infiltración profunda para reducir estrés térmico durante la sincronía.
 * v44.25.87: Root Anchor - Fuerza la inclusión de Okio en el set de librerías críticas de forma incondicional.
 */
public class AndroidModuleImpl extends JavaModuleImpl implements AndroidModule {

    private String mPackageName;
    private String mNamespace;
    private String mName;
    private Project mProject;

    private final Map<String, File> mKotlinFiles = new HashMap<>();
    private final Map<String, File> mResourceClasses = new HashMap<>();
    private final Set<ContentRoot> contentRoots = new HashSet<>();
    private final List<CodeAssistLibrary> libraries = new ArrayList<>();
    
    private volatile List<File> mCachedArsenal = null;

    public AndroidModuleImpl(File root) {
        super(root != null ? root.getAbsoluteFile() : null);
    }

    // --- CONTRATOS BASE ---

    @Override public String getPackageName() { return mPackageName; }
    public void setPackageName(String packageName) { this.mPackageName = packageName; }
    @Override public String getNamespace() { return mNamespace; }
    public void setNamespace(String namespace) { this.mNamespace = namespace; }

    @NonNull @Override public Map<String, File> getKotlinFiles() { return ImmutableMap.copyOf(mKotlinFiles); }
    @NonNull @Override public File getKotlinDirectory() { 
        File root = getRootFile().getAbsoluteFile();
        File kt = new File(root, "src/main/kotlin");
        if (!kt.exists()) kt = new File(root, "app/src/main/kotlin");
        return kt.getAbsoluteFile(); 
    }
    @Nullable @Override public File getKotlinFile(String packageName) { return mKotlinFiles.get(packageName); }
    @Override public void addKotlinFile(File file) {
        if (file == null || !file.exists()) return;
        String pkg = StringSearch.packageName(file);
        String fqn = (pkg == null ? "" : pkg + ".") + file.getName().replace(".kt", "");
        mKotlinFiles.put(fqn, file.getAbsoluteFile());
    }

    // --- GESTIÓN DE ARSENAL & FILTROS ANTI-COLISIÓN ---

    public void invalidateLibraryCache() { mCachedArsenal = null; }

    @Override
    public List<File> getLibraries() {
        if (mCachedArsenal != null) return mCachedArsenal;
        synchronized (this) {
            if (mCachedArsenal != null) return mCachedArsenal;
            Set<File> rawArsenal = new LinkedHashSet<>(super.getLibraries());
            for (CodeAssistLibrary lib : libraries) {
                File source = lib.getSourceFile();
                if (source != null && source.exists()) rawArsenal.add(source.getAbsoluteFile());
            }
            File root = getRootFile().getAbsoluteFile();
            collectFilesSelective(new File(root, "libs"), rawArsenal, 4);
            File appDir = new File(root, "app");
            if (appDir.exists()) collectFilesSelective(new File(appDir, "libs"), rawArsenal, 4);
            
            forceFullSpectrumRescue(rawArsenal);
            List<File> uniqueLibs = applyZenithShieldFilter(rawArsenal);
            mCachedArsenal = resolveAarsToExplodedJars(new LinkedHashSet<>(uniqueLibs));
            return mCachedArsenal;
        }
    }

    private List<File> applyZenithShieldFilter(Collection<File> libs) {
        Map<String, File> filtered = new LinkedHashMap<>();
        Pattern p = Pattern.compile("^(.+?)(?:-(\\d+(?:\\.\\d+)*.*?))?\\.(?:aar|jar)$");
        
        Map<String, String> aliasMap = new HashMap<>();
        aliasMap.put("arch-core-runtime", "core-runtime");
        aliasMap.put("arch-core-common", "core-common");
        aliasMap.put("arch-core", "core-runtime");
        aliasMap.put("okio-jvm", "okio");
        aliasMap.put("okio-android", "okio");
        aliasMap.put("kotlin-stdlib-jdk8", "kotlin-stdlib");
        aliasMap.put("kotlin-stdlib-jdk7", "kotlin-stdlib");
        aliasMap.put("kotlin-stdlib-common", "kotlin-stdlib");

        boolean hasModernLibs = false;
        boolean needsNetworkStack = false;
        for (File f : libs) {
            String n = f.getName().toLowerCase();
            if (n.contains("androidx") || n.contains("material") || n.contains("google")) hasModernLibs = true;
            if (n.contains("okhttp") || n.contains("retrofit") || n.contains("okio")) needsNetworkStack = true;
        }

        for (File f : libs) {
            String name = f.getName().toLowerCase();
            Matcher m = p.matcher(name);
            String identity = name;
            String version = "0.0.0";

            if (m.find()) {
                identity = m.group(1);
                if (m.group(2) != null) version = m.group(2);
            }

            if (identity.equals("classes") && hasModernLibs && f.getAbsolutePath().contains("/libs/")) {
                continue;
            }

            if (aliasMap.containsKey(identity)) identity = aliasMap.get(identity);
            
            File existing = filtered.get(identity);
            if (existing == null) {
                filtered.put(identity, f);
            } else {
                String existingVer = getVersionFromFileName(existing.getName());
                int comparison = compareSemanticVersions(version, existingVer);
                if (comparison > 0) {
                    filtered.put(identity, f);
                } else if (comparison == 0) {
                    if (f.getAbsolutePath().contains("/libs/") && !existing.getAbsolutePath().contains("/libs/")) {
                        filtered.put(identity, f);
                    }
                }
            }
        }

        if (needsNetworkStack) {
            ensureCriticalDependency(libs, filtered, "okio");
            ensureCriticalDependency(libs, filtered, "kotlin-stdlib");
            ensureCriticalDependency(libs, filtered, "gson");
            ensureCriticalDependency(libs, filtered, "okhttp");
            ensureCriticalDependency(libs, filtered, "retrofit");
        }

        return new ArrayList<>(filtered.values());
    }

    private void ensureCriticalDependency(Collection<File> pool, Map<String, File> filtered, String target) {
        if (!filtered.containsKey(target)) {
            for (File f : pool) {
                String name = f.getName().toLowerCase();
                if (name.contains(target) && (name.endsWith(".jar") || name.endsWith(".aar"))) {
                    filtered.put(target, f);
                    return;
                }
            }
        }
    }

    private String getVersionFromFileName(String name) {
        Matcher m = Pattern.compile("-(\\d+(?:\\.\\d+)*.*?)\\.(?:aar|jar)$").matcher(name.toLowerCase());
        return m.find() ? m.group(1) : "0.0.0";
    }

    private int compareSemanticVersions(String v1, String v2) {
        String cleanV1 = v1.replaceAll("[^\\d\\.]", "");
        String cleanV2 = v2.replaceAll("[^\\d\\.]", "");
        String[] parts1 = cleanV1.split("\\.");
        String[] parts2 = cleanV2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length && !parts1[i].isEmpty() ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length && !parts2[i].isEmpty() ? Integer.parseInt(parts2[i]) : 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }

    private List<File> resolveAarsToExplodedJars(Set<File> rawLibs) {
        List<File> resolved = new ArrayList<>();
        File buildDir = getBuildDirectory().getAbsoluteFile();
        File explodedBaseDir = new File(buildDir, "bin/res/exploded-aar");
        for (File lib : rawLibs) {
            if (lib.getName().toLowerCase().endsWith(".aar")) {
                String folderName = lib.getName().replace(".aar", "") + "_ext";
                File classesJar = new File(explodedBaseDir, folderName + "/classes.jar");
                if (classesJar.exists()) { resolved.add(classesJar); continue; }
            }
            resolved.add(lib);
        }
        return resolved;
    }

    // --- RECURSOS & CICLO DE VIDA ---

    @Override public void open() throws IOException {
        super.open();
        File manifestFile = getManifestFile();
        if (manifestFile != null && manifestFile.exists()) {
            try {
                ManifestData data = AndroidManifestParser.parse(manifestFile);
                if (data.getPackage() != null) setPackageName(data.getPackage());
            } catch (Exception ignored) {}
        }
        index(); 
    }

    @Override public void index() {
        super.index();
        mKotlinFiles.clear();
        mResourceClasses.clear();
        File root = getRootFile().getAbsoluteFile();
        String[] searchPaths = { "src/main/java", "src/main/kotlin", "app/src/main/java", "app/src/main/kotlin", "build/gen", "app/build/gen", "build/generated/data_binding_base_class_source_out/debug/out" };
        for (String path : searchPaths) {
            File folder = new File(root, path).getAbsoluteFile();
            if (folder.exists() && folder.isDirectory()) {
                Collection<File> kts = FileUtils.listFiles(folder, new String[]{"kt"}, true);
                for (File f : kts) addKotlinFile(f);
                Collection<File> javas = FileUtils.listFiles(folder, new String[]{"java"}, true);
                for (File f : javas) addResourceClass(f);
            }
        }
    }

    @Override public File getAndroidResourcesDirectory() { 
        File root = getRootFile().getAbsoluteFile();
        File res = new File(root, "src/main/res");
        if (!res.exists()) res = new File(root, "app/src/main/res");
        return res.getAbsoluteFile(); 
    }
    @Override public File getNativeLibrariesDirectory() { return new File(getRootFile(), "src/main/jniLibs").getAbsoluteFile(); }
    @Override public File getAssetsDirectory() { return new File(getRootFile(), "src/main/assets").getAbsoluteFile(); }
    @Override public File getManifestFile() {
        File root = getRootFile().getAbsoluteFile();
        String[] paths = { "app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml", "AndroidManifest.xml" };
        for (String p : paths) { File f = new File(root, p).getAbsoluteFile(); if (f.exists()) return f; }
        return new File(root, "src/main/AndroidManifest.xml").getAbsoluteFile();
    }
    @Override public Map<String, File> getResourceClasses() { return ImmutableMap.copyOf(mResourceClasses); }
    @Override public void addResourceClass(@NonNull File file) {
        if (!file.getName().endsWith(".java")) return;
        String pkg = StringSearch.packageName(file);
        String className = (pkg == null) ? file.getName().replace(".java", "") : pkg + "." + file.getName().replace(".java", "");
        mResourceClasses.put(className, file);
    }
    @Override public Project getProject() { return mProject; }
    @Override public void setProject(Project project) { this.mProject = project; }
    @Override public String getName() { return mName != null ? mName : getRootFile().getName(); }
    public void setName(String name) { this.mName = name; }
    @Override public void clear() { super.clear(); mKotlinFiles.clear(); mResourceClasses.clear(); invalidateLibraryCache(); }
    @Override public int getTargetSdk() { return getSettings().getInt(ModuleSettings.TARGET_SDK_VERSION, 34); }
    @Override public int getMinSdk() { return getSettings().getInt(ModuleSettings.MIN_SDK_VERSION, 26); }

    @Override public List<CodeAssistLibrary> getCodeAssistLibraries() { return libraries; }
    @Override public void addLibrary(@NonNull CodeAssistLibrary library) { if (!libraries.contains(library)) libraries.add(library); super.addLibrary(library); invalidateLibraryCache(); }
    @Override public Set<String> getAllClasses() { Set<String> classes = new HashSet<>(super.getAllClasses()); classes.addAll(mKotlinFiles.keySet()); return classes; }

    private void collectFilesSelective(File dir, Set<File> set, int depth) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth < 0) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().equals("build") && !f.getName().equals("bin")) collectFilesSelective(f, set, depth - 1);
            } else if (f.isFile()) {
                String n = f.getName().toLowerCase();
                if ((n.endsWith(".aar") || n.endsWith(".jar")) && f.length() > 0) set.add(f.getAbsoluteFile());
            }
        }
    }

    private void forceFullSpectrumRescue(Set<File> libs) {
        String[] targets = { "appcompat", "material", "recyclerview", "constraintlayout", "cardview", "fragment", "activity", "customview", "drawerlayout", "coordinatorlayout", "lifecycle-runtime", "lifecycle-viewmodel", "lifecycle-common", "room-runtime", "core-ktx", "core-runtime", "core-common", "arch-core", "tracing", "vectordrawable", "vectordrawable-animated", "interpolator", "cursoradapter", "navigation-common", "navigation-runtime", "navigation-ui", "navigation-fragment", "retrofit", "okhttp", "emoji2", "transition", "okio", "gson", "kotlin-stdlib" };
        File sdcard = new File("/storage/emulated/0");
        String[] spots = { "Android/data/com.tyron.code/files/repo", "CodeAssist/repo", "Download/CodeAssistProjects/repo", "Download/libs", "Download/CodeAssist/libs", "CodeAssistProjects/repo", "Download", "Android/media/com.tyron.code/repo" };
        for (String s : spots) {
            File dir = new File(sdcard, s).getAbsoluteFile();
            // v44.25.88: Reducción de profundidad de infiltración a 5 para mitigar sobrecarga térmica
            if (dir.exists()) infiltrateDeep(dir, libs, targets, 5);
        }
    }

    private void infiltrateDeep(File dir, Set<File> libs, String[] targets, int depth) {
        if (dir == null || !dir.exists() || depth < 0) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) infiltrateDeep(f, libs, targets, depth - 1);
            else {
                String n = f.getName().toLowerCase();
                for (String t : targets) { if (n.contains(t) && (n.endsWith(".aar") || n.endsWith(".jar"))) { libs.add(f.getAbsoluteFile()); break; } }
            }
        }
    }

    @Override public Set<ContentRoot> getContentRoots() {
        if (contentRoots.isEmpty()) {
            File root = getRootFile().getAbsoluteFile();
            AndroidContentRoot contentRoot = new AndroidContentRoot(root);
            contentRoot.setJavaDirectories(Arrays.asList(new File(root, "src/main/java"), new File(root, "src/main/kotlin"), new File(root, "app/src/main/java"), new File(root, "app/src/main/kotlin"), new File(root, "build/gen"), new File(root, "app/build/gen")));
            contentRoot.setResourceDirectories(Collections.singletonList(new File(root, "src/main/res")));
            contentRoots.add(contentRoot);
        }
        return contentRoots;
    }
}
