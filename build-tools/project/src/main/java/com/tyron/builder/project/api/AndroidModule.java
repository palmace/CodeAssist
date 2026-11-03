package com.tyron.builder.project.api;

import androidx.annotation.NonNull;
import com.tyron.builder.model.CodeAssistLibrary;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * AndroidModule v43.1.0: "Zenith API Standard - Full Interop".
 * v43.1.0: Definición de interfaz soberana para gestión de librerías y recursos.
 */
public interface AndroidModule extends JavaModule, KotlinModule {

    File getAndroidResourcesDirectory();

    File getNativeLibrariesDirectory();

    File getAssetsDirectory();

    String getPackageName();

    File getManifestFile();

    int getTargetSdk();

    int getMinSdk();

    /**
     * @return La lista de librerías registradas con metadatos de CodeAssist.
     */
    List<CodeAssistLibrary> getCodeAssistLibraries();

    Map<String, File> getResourceClasses();

    void addResourceClass(@NonNull File file);

    default String getNamespace() {
        throw new UnsupportedOperationException();
    }
}
