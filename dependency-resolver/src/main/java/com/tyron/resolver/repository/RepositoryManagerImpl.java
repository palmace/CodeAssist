package com.tyron.resolver.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.io.CharStreams;
import com.tyron.common.logging.IdeLog;
import com.tyron.resolver.model.Pom;
import com.tyron.resolver.parser.PomParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public class RepositoryManagerImpl implements RepositoryManager {

    private static final Logger sLogger = IdeLog.getCurrentLogger(RepositoryManagerImpl.class);

    private File cacheDir;
    private final List<Repository> repositories;
    private final List<Pom> pomFiles;

    public RepositoryManagerImpl() {
        this.repositories = new ArrayList<>();
        this.pomFiles = new ArrayList<>();
    }

    @Override
    @Nullable
    public Pom getPom(String declaration) {
        String[] pomNames = parsePomDeclaration(declaration);
        if (pomNames == null) {
            return null;
        }
        
        // 1. Buscar en caché cargada (Memoria)
        for (Pom pom : pomFiles) {
            if (pomNames[0].equals(pom.getGroupId()) &&
                pomNames[1].equals(pom.getArtifactId()) &&
                pomNames[2].equals(pom.getVersionName())) {
                return pom;
            }
        }
        
        // 2. Buscar en Disco (Caché local) antes de ir a la red
        Pom cachedPom = getPomFromCache(pomNames);
        if (cachedPom != null) {
            return cachedPom;
        }

        // 3. Descargar de la red (Fallback entre repositorios)
        return getPomFromUrls(pomNames);
    }

    private Pom getPomFromCache(String[] names) {
        String path = getPathFromDeclaration(names) + ".pom";
        for (Repository repository : repositories) {
             try {
                 File file = repository.getCachedFile(path);
                 if (file != null && file.exists()) {
                     // ESCUDO DE SALUD: Verificar si el POM está vacío o es un error 404/HTML
                     if (file.length() < 100) {
                         sLogger.warning("POM corrupto detectado en caché: " + file.getName() + ". Eliminando...");
                         file.delete();
                         continue;
                     }
                     try {
                         Pom parsed = new PomParser(this).parse(file);
                         parsed.setGroupId(names[0]);
                         parsed.setArtifactId(names[1]);
                         parsed.setVersionName(names[2]);
                         pomFiles.add(parsed);
                         return parsed;
                     } catch (Exception e) {
                         sLogger.severe("Error al parsear POM de caché: " + file.getAbsolutePath());
                         file.delete();
                     }
                 }
             } catch (IOException e) {
                 sLogger.warning("Error al acceder a caché para " + path + " en repo " + repository.getName());
             }
        }
        return null;
    }

    private Pom getPomFromUrls(String[] names) {
        String path = getPathFromDeclaration(names) + ".pom";
        InputStream is = getFromUrls(path);
        if (is != null) {
            try {
                String contents = CharStreams.toString(new InputStreamReader(is));
                // ESCUDO DE SALUD: Verificar si el contenido es XML válido y no un HTML 404
                if (contents.trim().startsWith("<!DOCTYPE html") || contents.trim().startsWith("<html")) {
                    sLogger.warning("El servidor devolvió HTML en lugar de POM para: " + Arrays.toString(names));
                    return null;
                }
                
                Pom parsed = new PomParser(this).parse(contents);
                parsed.setGroupId(names[0]);
                parsed.setArtifactId(names[1]);
                parsed.setVersionName(names[2]);
                pomFiles.add(parsed);
                return parsed;
            } catch (IOException | XmlPullParserException | SAXException e) {
                sLogger.severe("Fallo al procesar POM descargado: " + Arrays.toString(names));
            }
        }
        return null;
    }

    private InputStream getFromUrls(String appendUrl) {
        for (Repository repository : repositories) {
            try {
                InputStream is = repository.getInputStream(appendUrl);
                if (is != null) return is;
            } catch (IOException ignored) {
                // Silencio: Pasamos al siguiente repositorio (Fallback)
            }
        }
        return null;
    }

    private String getPathFromDeclaration(String[] pomNames) {
        String groupId = pomNames[0].replace('.', '/');
        String artifactId = pomNames[1];
        String path = groupId + "/" + artifactId + "/" + pomNames[2];
        return path + "/" + pomNames[1] + "-" + pomNames[2];
    }

    private String[] parsePomDeclaration(String declaration) {
        if (declaration.endsWith(".pom")) {
            declaration = declaration.substring(0, declaration.length() - 4);
        }
        String[] strings = declaration.split(":");
        if (strings.length < 3) return null;
        return strings;
    }

    @Override
    @Nullable
    public File getLibrary(Pom pom) throws IOException {
        String path = getLibraryPath(pom);

        // 1. Buscar en caché con validación de integridad
        for (Repository repository : repositories) {
            try {
                File file = repository.getCachedFile(path);
                if (file != null && file.exists()) {
                    if (isValidLibrary(file)) {
                        return file;
                    } else {
                        sLogger.warning("Librería corrupta detectada en caché: " + file.getName() + ". Eliminando...");
                        file.delete();
                    }
                }
            } catch (IOException ignored) {}
        }

        // 2. Descargar de la red (Fallback automático)
        for (Repository repository : repositories) {
            try {
                File file = repository.getFile(path);
                if (file != null && file.exists()) {
                    if (isValidLibrary(file)) {
                        return file;
                    } else {
                        file.delete();
                    }
                }
            } catch (IOException ignored) {}
        }
        return null;
    }

    private String getLibraryPath(Pom pom) {
        StringBuilder sb = new StringBuilder();
        sb.append(pom.getGroupId().replace('.', '/')).append('/');
        sb.append(pom.getArtifactId()).append('/');
        sb.append(pom.getVersionName()).append('/');
        sb.append(pom.getArtifactId()).append('-').append(pom.getVersionName());
        if ("aar".equals(pom.getPackaging())) {
            sb.append(".aar");
        } else {
            sb.append(".jar");
        }
        return sb.toString();
    }

    private boolean isValidLibrary(File file) {
        if (file.length() < 100) return false;
        
        // Verificación de firma ZIP (PK\003\004)
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 4) return false;
            int header = raf.readInt();
            return header == 0x504B0304; 
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setCacheDirectory(File directory) {
        if (directory.isFile()) throw new IllegalArgumentException("Debe ser un directorio");
        this.cacheDir = directory;
    }

    @Override
    public void addRepository(@NonNull Repository repository) {
        repositories.add(repository);
    }

    @Override
    public void addRepository(@NonNull String name, @NonNull String url) {
        addRepository(new RemoteRepository(name, url));
    }

    @Override
    public void initialize() {
        if (cacheDir == null) throw new IllegalStateException("Caché no configurada.");

        for (Repository repository : repositories) {
            repository.setCacheDirectory(cacheDir);
            File root = repository.getRootDirectory();
            if (root == null || !root.exists()) continue;

            Iterator<File> poms = FileUtils.iterateFiles(root, new SuffixFileFilter(".pom"), TrueFileFilter.INSTANCE);
            while (poms.hasNext()) {
                File pomFile = poms.next();
                // ESCUDO DE SALUD: Limpieza inicial de Poms inválidos
                if (pomFile.length() < 100) {
                    pomFile.delete();
                    continue;
                }
                try {
                    PomParser parser = new PomParser(this);
                    Pom parsed = parser.parse(pomFile);
                    this.pomFiles.add(parsed);
                } catch (Exception e) {
                    sLogger.severe("Error al inicializar POM: " + pomFile.getName());
                }
            }
        }
    }
}
