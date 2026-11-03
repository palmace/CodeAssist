package com.tyron.completion.java.compiler;

import com.tyron.completion.java.compiler.ReflectionUtil;
import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.tyron.builder.model.SourceFileObject;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.util.PackageTrie;
import com.tyron.common.util.Cache;
import com.tyron.common.util.StringSearch;
import com.tyron.completion.java.CompilerProvider;
import com.tyron.completion.java.Docs;
import com.tyron.completion.java.FindTypeDeclarations;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.file.PathFileObject;
import com.tyron.completion.java.compiler.services.CancelAbort;
import com.tyron.completion.progress.ProcessCanceledException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCompilerService implements CompilerProvider {

    private DiagnosticListener<? super JavaFileObject> mDiagnosticListener;
    public final SourceFileManager mSourceFileManager;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
    private final Project mProject;
    private JavaModule mCurrentModule;
    public final Set<File> classPath, docPath;
    public final Set<String> addExports;
    public ReusableCompiler compiler = new ReusableCompiler();
    private final Docs docs;
    private final CompilerContainer mContainer = new CompilerContainer();
    private CompileBatch cachedCompile;
    private final Map<JavaFileObject, Long> cachedModified = new HashMap<>();
    public final ReentrantLock mLock = new ReentrantLock();

    public JavaCompilerService(Project project, Set<File> classPath, Set<File> docPath, Set<String> addExports) {
        mProject = project;
        this.classPath = Collections.unmodifiableSet(classPath);
        this.docPath = Collections.unmodifiableSet(docPath);
        this.addExports = Collections.unmodifiableSet(addExports);
        this.mSourceFileManager = new SourceFileManager(project);
        this.docs = new Docs(project, docPath);
    }

    public Project getProject() { return mProject; }

    public void setCurrentModule(@NonNull JavaModule module) {
        mSourceFileManager.setCurrentModule(module);
        mCurrentModule = module;
    }

    private boolean needsCompile(Collection<? extends JavaFileObject> sources) {
        if (sources == null || cachedModified.size() != sources.size()) return true;
        for (JavaFileObject f : sources) {
            if (f == null || !cachedModified.containsKey(f)) return true;
            Long cached = cachedModified.get(f);
            if (cached == null || f.getLastModified() != cached) return true;
        }
        return false;
    }

    private synchronized void loadCompile(Collection<? extends JavaFileObject> sources) {
        if (cachedCompile != null && !cachedCompile.closed) {
            cachedCompile.borrow.close();
        }
        cachedCompile = doCompile(sources);
        cachedModified.clear();
        if (sources != null) {
            for (JavaFileObject f : sources) {
                if (f != null) cachedModified.put(f, f.getLastModified());
            }
        }
    }

    public void invalidate(Path source) {
        if (source != null) invalidate(Collections.singletonList(new SourceFileObject(source)));
    }

    public void invalidate(Collection<? extends JavaFileObject> sources) {
        if (sources == null) return;
        for (JavaFileObject source : sources) {
            if (source != null) cachedModified.remove(source);
        }
    }

    private CompileBatch doCompile(Collection<? extends JavaFileObject> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        CompileBatch firstAttempt = new CompileBatch(this, sources);
        Set<Path> addFiles = firstAttempt.needsAdditionalSources();
        if (addFiles.isEmpty()) return firstAttempt;
        
        firstAttempt.close();
        firstAttempt.borrow.close();
        List<JavaFileObject> moreSources = new ArrayList<>(sources);
        for (Path add : addFiles) {
            if (add != null) moreSources.add(new SourceFileObject(add, mCurrentModule));
        }
        return new CompileBatch(this, moreSources);
    }

    private CompilerContainer compileBatch(Collection<? extends JavaFileObject> sources) {
        try {
            mContainer.initialize(() -> {
                if (needsCompile(sources)) loadCompile(sources);
                if (cachedCompile != null) {
                    CompileTask task = new CompileTask(cachedCompile);
                    mContainer.setCompileTask(task);
                }
            });
            return mContainer;
        } catch (Throwable t) {
            if (t instanceof CancelAbort || t.getCause() instanceof CancelAbort) {
                if (cachedCompile != null) cachedCompile.borrow.close();
                throw new ProcessCanceledException();
            }
            throw t;
        }
    }

    public void clearDiagnostics() {
        diagnostics.clear();
        if (mDiagnosticListener != null) mDiagnosticListener.report(null);
    }

    public void addDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic != null) {
            diagnostics.add(diagnostic);
            if (mDiagnosticListener != null) mDiagnosticListener.report(diagnostic);
        }
    }

    public void setDiagnosticListener(DiagnosticListener<? super JavaFileObject> listener) { mDiagnosticListener = listener; }
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() { return ImmutableList.copyOf(diagnostics); }

    @Override public Set<String> imports() { return null; }

    @Override public Set<String> publicTopLevelTypes() {
        Set<String> classes = new HashSet<>();
        for (Module module : mProject.getDependencies(mCurrentModule)) {
            if (module instanceof JavaModule) classes.addAll(((JavaModule) module).getAllClasses());
        }
        return classes;
    }

    public Set<String> findClasses(String packageName) {
        Set<String> classes = new HashSet<>();
        for (Module module : mProject.getDependencies(mCurrentModule)) {
            if (module instanceof JavaModule) {
                PackageTrie classIndex = ((JavaModule) module).getClassIndex();
                classes.addAll(classIndex.getMatchingPackages(packageName));
            }
        }
        return classes;
    }

    public Set<String> getTopLevelNonLeafPackages(Predicate<String> filter) {
        Set<String> packages = new HashSet<>();
        for (Module module : mProject.getDependencies(mCurrentModule)) {
            if (module instanceof JavaModule) {
                PackageTrie classIndex = ((JavaModule) module).getClassIndex();
                for (String node : classIndex.getTopLevelNonLeafNodes()) {
                    if (filter.test(node)) packages.add(node);
                }
            }
        }
        return packages;
    }

    @Override public List<String> packagePrivateTopLevelTypes(String packageName) { return Collections.emptyList(); }
    @Override public Iterable<Path> search(String query) { return null; }

    @SuppressLint("NewApi")
    @Override public Optional<JavaFileObject> findAnywhere(String className) {
        Optional<JavaFileObject> fromDocs = findPublicTypeDeclarationInDocPath(className);
        if (fromDocs.isPresent()) return fromDocs;
        Path fromSource = findTypeDeclaration(className);
        if (fromSource != NOT_FOUND) return Optional.of(new SourceFileObject(fromSource, mCurrentModule));
        return Optional.empty();
    }

    private Optional<JavaFileObject> findPublicTypeDeclarationInDocPath(String className) {
        try {
            JavaFileObject found = docs.fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE);
            return Optional.ofNullable(found);
        } catch (IOException e) { return Optional.empty(); }
    }

    private String packageName(String className) {
        Matcher m = Pattern.compile("^([a-z][_a-zA-Z0-9]*\\.)*[a-z][_a-zA-Z0-9]*").matcher(className);
        return m.find() ? m.group() : "";
    }

    private String simpleName(String className) {
        Matcher m = Pattern.compile("[A-Z][_a-zA-Z0-9]*$").matcher(className);
        return m.find() ? m.group() : "";
    }

    private static final Cache<String, Boolean> cacheContainsWord = new Cache<>();
    private boolean containsWord(Path file, String word) {
        if (cacheContainsWord.needs(file, word)) cacheContainsWord.load(file, word, StringSearch.containsWord(file, word));
        return cacheContainsWord.get(file, word);
    }

    private static final Cache<Void, List<String>> cacheContainsType = new Cache<>();
    private boolean containsType(Path file, String className) {
        if (cacheContainsType.needs(file, null)) {
            CompilationUnitTree root = parse(file).root;
            List<String> types = new ArrayList<>();
            new FindTypeDeclarations().scan(root, types);
            cacheContainsType.load(file, null, types);
        }
        return cacheContainsType.get(file, null).contains(className);
    }

    @Override public Path findTypeDeclaration(String className) {
        Path fastFind = findPublicTypeDeclaration(className);
        if (fastFind != NOT_FOUND) return fastFind;
        String packageName = packageName(className);
        String simpleName = simpleName(className);
        for (Module dependency : mProject.getDependencies(mCurrentModule)) {
            Path path = findPublicTypeDeclarationInModule(dependency, packageName, simpleName, className);
            if (path != NOT_FOUND) return path;
        }
        return NOT_FOUND;
    }

    private Path findPublicTypeDeclarationInModule(Module module, String packageName, String simpleName, String className) {
        for (File file : SourceFileManager.list(module, packageName)) {
            if (containsWord(file.toPath(), simpleName) && containsType(file.toPath(), className)) {
                if (file.getName().endsWith(".java")) return file.toPath();
            }
        }
        return NOT_FOUND;
    }

    private Path findPublicTypeDeclaration(String className) {
        try {
            JavaFileObject source = mSourceFileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE);
            if (source == null) return NOT_FOUND;
            
            // PROTECCIÓN v13.2: Verificar esquema antes de toUri()
            URI uri = source.toUri();
            if (uri == null || !"file".equals(uri.getScheme())) return NOT_FOUND;
            
            Path file = Paths.get(uri);
            if (!containsType(file, className)) return NOT_FOUND;
            return file;
        } catch (Exception e) { return NOT_FOUND; }
    }

    public Optional<JavaFileObject> findPublicTypeDeclarationInJdk(String className) {
        try {
            JavaFileObject source = mSourceFileManager.getJavaFileForInput(StandardLocation.PLATFORM_CLASS_PATH, className, JavaFileObject.Kind.CLASS);
            return Optional.ofNullable(source);
        } catch (IOException e) { return Optional.empty(); }
    }

    @Override public Path[] findTypeReferences(String className) { return null; }
    @Override public Path[] findMemberReferences(String className, String memberName) { return null; }

    private final Cache<String, ParseTask> parseCache = new Cache<>();

    private ParseTask cachedParse(Path file) {
        if (file == null) return null;
        if (parseCache.needs(file, file.toFile().getName())) {
            Parser parser = Parser.parseFile(mProject, file);
            parseCache.load(file, file.toFile().getName(), new ParseTask(parser.task, parser.root));
        }
        return parseCache.get(file, file.toFile().getName());
    }

    private ParseTask cachedParse(JavaFileObject file) {
        if (file == null) return null;
        try {
            if (file instanceof PathFileObject) {
                String uriStr = file.toUri().toString();
                int idx = uriStr.lastIndexOf("!");
                if (idx != -1 && uriStr.startsWith("jar:")) {
                    String path = uriStr.substring(4, idx);
                    Path parsedPath = new File(URI.create(path)).toPath();
                    if (parseCache.needs(parsedPath, file.getName())) {
                        Parser parser = Parser.parseJavaFileObject(mProject, file);
                        parseCache.load(parsedPath, file.getName(), new ParseTask(parser.task, parser.root));
                    }
                    return parseCache.get(parsedPath, file.getName());
                }
            } else if (file instanceof SourceFileObject) {
                return cachedParse(((SourceFileObject) file).mFile);
            }
        } catch (Exception e) { /* fallback */ }
        Parser parser = Parser.parseJavaFileObject(mProject, file);
        return new ParseTask(parser.task, parser.root);
    }

    @Override public ParseTask parse(Path file) { return cachedParse(file); }
    @Override public ParseTask parse(JavaFileObject file) {
        if (file == null) return null;
        Parser parser = Parser.parseJavaFileObject(mProject, file);
        return new ParseTask(parser.task, parser.root);
    }

    public ParseTask parse(Path file, String contents) {
        SourceFileObject object = new SourceFileObject(file, contents, Instant.now());
        Parser parser = Parser.parseJavaFileObject(mProject, object);
        return new ParseTask(parser.task, parser.root);
    }

    @Override public CompilerContainer compile(Path... files) {
        List<JavaFileObject> sources = new ArrayList<>();
        if (files != null) {
            for (Path f : files) {
                if (f != null) sources.add(new SourceFileObject(f, mCurrentModule));
            }
        }
        return compile(sources);
    }

    @Override public synchronized CompilerContainer compile(Collection<? extends JavaFileObject> sources) { return compileBatch(sources); }

    public synchronized void close() {
        if (cachedCompile != null && !cachedCompile.closed) cachedCompile.close();
        if (mLock.isHeldByCurrentThread() && mLock.isLocked()) mLock.unlock();
    }

    public JavaModule getCurrentModule() { return mCurrentModule; }

    public void destroy() {
        mContainer.initialize(() -> {
            close();
            if (cachedCompile != null && cachedCompile.borrow != null) cachedCompile.borrow.close();
            cachedCompile = null;
            cachedModified.clear();
            compiler = new ReusableCompiler();
        });
    }

    public CompileBatch getCompileBatch() { return cachedCompile; }
    @NonNull public CompilerContainer getCachedContainer() { return mContainer; }
}
