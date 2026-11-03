package com.tyron.completion.java.parse;

import com.tyron.completion.java.compiler.ReflectionUtil;
import androidx.annotation.NonNull;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Pair;
import com.tyron.builder.model.CodeAssistAndroidLibrary;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.common.util.DebouncerStore;
import com.tyron.completion.java.compiler.services.NBEnter;
import com.tyron.completion.java.compiler.services.NBLog;
import org.jetbrains.kotlin.com.intellij.openapi.util.Key;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class CompilationInfo {

    public static final Key<CompilationInfo> COMPILATION_INFO_KEY = Key.create("compilationInfo");

    public static CompilationInfo get(Module module) {
        if (!(module instanceof JavaModule)) return null;
        JavaModule javaModule = (JavaModule) module;
        CompilationInfo info = module.getUserData(COMPILATION_INFO_KEY);
        if (info == null) {
            List<File> libraries = new ArrayList<>(javaModule.getLibraries());
            // v41.5.07: Cambio estratégico a interfaz AndroidModule para evitar errores de símbolo
            if (module instanceof AndroidModule) {
                AndroidModule androidModule = (AndroidModule) module;
                if (androidModule.getCodeAssistLibraries() != null) {
                    libraries.addAll(androidModule.getCodeAssistLibraries().stream()
                            .filter(it -> it instanceof CodeAssistAndroidLibrary)
                            .map(it -> (CodeAssistAndroidLibrary) it)
                            .flatMap(it -> it.getCompileJarFiles() != null ? it.getCompileJarFiles().stream() : java.util.stream.Stream.empty())
                            .collect(Collectors.toList()));
                }
                
                File kotlinClasses = new File(module.getBuildDirectory(), "bin/kotlin/classes");
                if (kotlinClasses.exists()) {
                    libraries.add(kotlinClasses);
                }
            }
            info = new CompilationInfo(new CompilationInfoImpl(new JavacParser(), null, null, libraries, Collections.emptyList(), null, null));
            module.putUserData(COMPILATION_INFO_KEY, info);
        }
        return info;
    }
    
    public static CompilationInfo get(Project project, File file) {
        if (project == null || file == null) return null;
        Module module = project.getModule(file);
        return get(module);
    }

    public final CompilationInfoImpl impl;
    private final Map<URI, JCCompilationUnit> compiledMap = new HashMap<>();
    private final DebouncerStore<String> debouncerStore = DebouncerStore.DEFAULT;
    private final Object parseLock = new Object();
    private Trees trees;

    public CompilationInfo(final CompilationInfoImpl impl) { this.impl = impl; }

    public JCCompilationUnit updateImmediately(JavaFileObject fileObject) {
        CompletableFuture<JCCompilationUnit> future = new CompletableFuture<>();
        update(fileObject, 0, future::complete);
        try { return future.get(); } catch (Exception e) { return null; }
    }

    public void update(JavaFileObject fileObject) { this.update(fileObject, 300, __ -> {}); }

    public synchronized void update(JavaFileObject fileObject, long delay, Consumer<JCCompilationUnit> treeConsumer) {
        if (fileObject == null) {
            if (treeConsumer != null) treeConsumer.accept(null);
            return;
        }
        debouncerStore.registerOrGetDebouncer("update").debounce(delay, () -> {
            synchronized (parseLock) {
                try {
                    JavacTaskImpl javacTask = impl.getJavacTask();
                    NBLog log = NBLog.instance(javacTask.getContext());
                    log.useSource(fileObject);

                    URI fileUri = safeGetUri(fileObject);
                    if (fileUri == null) {
                        if (treeConsumer != null) treeConsumer.accept(null);
                        return;
                    }

                    Set<Pair<JavaFileObject, Integer>> toRemove = new HashSet<>();
                    for (Pair<JavaFileObject, Integer> pair : log.getRecorded()) {
                        URI pairUri = safeGetUri(pair.fst);
                        if (pairUri != null && pairUri.equals(fileUri)) toRemove.add(pair);
                    }
                    log.getRecorded().removeAll(toRemove);
                    log.removeDiagnostics(fileUri);
                    log.removeFileObject(fileObject);

                    JCCompilationUnit previous = compiledMap.get(fileUri);
                    if (previous != null) {
                        NBEnter enter = (NBEnter) NBEnter.instance(javacTask.getContext());
                        enter.unenter(previous, previous);
                        enter.removeCompilationUnit(fileObject);
                    }

                    JavaCompiler compiler = JavaCompiler.instance(javacTask.getContext());
                    JCCompilationUnit unit = compiler.parse(fileObject);
                    Iterable<? extends Element> enter = javacTask.enter(List.of(unit));
                    if (previous != null) unit.packge = previous.packge;
                    javacTask.analyze(enter);

                    compiledMap.put(fileUri, unit);
                    if (treeConsumer != null) treeConsumer.accept(unit);
                } catch (Throwable t) {
                    if (treeConsumer != null) treeConsumer.accept(null);
                }
            }
        });
    }

    private URI safeGetUri(JavaFileObject file) {
        try { return file != null ? file.toUri() : null; } catch (Exception e) { return null; }
    }

    public JCCompilationUnit getCompilationUnit(JavaFileObject fileObject) {
        URI uri = safeGetUri(fileObject);
        return uri != null ? getCompilationUnit(uri) : null;
    }

    public JCCompilationUnit getCompilationUnit(URI uri) {
        return uri != null ? compiledMap.get(uri) : null;
    }

    public synchronized @NonNull Trees getTrees() {
        if (trees == null) {
            com.sun.tools.javac.main.JavaCompiler.instance(impl.getJavacTask().getContext());
            trees = (Trees) ReflectionUtil.getJavacTreesInstance(impl.getJavacTask().getContext());
        }
        return trees;
    }

    public @NonNull Elements getElements() {
        com.sun.tools.javac.main.JavaCompiler.instance(impl.getJavacTask().getContext());
        return impl.getJavacTask().getElements();
    }

    public CompilationUnitTree getCompilationUnit() { return impl.getCompilationUnit(); }
}
