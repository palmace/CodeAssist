package com.tyron.builder.crashlytics;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.manifest.resources.ResourceType;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.xml.completion.repository.Repository;
import com.tyron.xml.completion.repository.ResourceItem;
import com.tyron.xml.completion.repository.api.ResourceNamespace;
import com.tyron.xml.completion.repository.api.ResourceReference;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CrashlyticsTask v44.8.0: "The Zenith Sentry - Build ID Injection".
 * v44.8.0: Sincronía con Reality Seal - Uso de rutas absolutas ancladas.
 * v44.8.0: Protocolo de Detección Silenciosa - Verificación vía Radar Zenith.
 * v44.8.0: Operación Blindada para evitar fallos de recursos post-crash.
 */
public class CrashlyticsTask extends Task<AndroidModule> {

    private static final String LEGACY_MAPPING_FILE_ID_RESOURCE_NAME =
            "com_crashlytics_android_build_id";
    private static final String CORE_CLASS =
            "com.google.firebase.crashlytics.internal.common.CrashlyticsCore";
    private static final String CRASHLYTICS_RESOURCE_FILE = "code_assist_crashlytics__.xml";
    private static final String CRASHLYTICS_BUILD_ID = "zenith_absolute_build_v44_8_0";
    private static final String XML_REPOSITORY_CLASS = "com.tyron.completion.xml.XmlRepository";

    private boolean mContainsCrashlytics;

    public CrashlyticsTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Crashlytics (OMNI v44.8.0)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        // v44.8.0: Verificación táctica de presencia de Crashlytics
        mContainsCrashlytics = false;
        List<File> libs = getModule().getLibraries();
        for (File lib : libs) {
            if (lib.getName().contains("crashlytics")) {
                mContainsCrashlytics = true;
                break;
            }
        }
        
        if (!mContainsCrashlytics) {
            // Fallback al índice de clases si el radar no es concluyente
            try {
                mContainsCrashlytics = getModule().getAllClasses().contains(CORE_CLASS);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        if (!mContainsCrashlytics) {
            return;
        }

        getLogger().info("🚀 OMNI: [Sentry] Inyectando Build ID de Crashlytics...");

        Repository repository = getRepository(getProject(), getModule());
        if (repository != null) {
            ResourceNamespace namespace = repository.getNamespace();
            ResourceReference resourceReference = new ResourceReference(namespace, ResourceType.STRING,
                                                                        LEGACY_MAPPING_FILE_ID_RESOURCE_NAME);
            List<ResourceItem> resources = repository.getResources(resourceReference);
            if (!resources.isEmpty()) {
                return;
            }
        }

        File resDir = getModule().getAndroidResourcesDirectory().getAbsoluteFile();
        File valuesDir = new File(resDir, "values").getAbsoluteFile();
        
        if (!valuesDir.exists() && !valuesDir.mkdirs()) {
            throw new IOException("Error Estructural: No se pudo crear el sector 'values' en " + valuesDir.getAbsolutePath());
        }

        File resFile = new File(valuesDir, CRASHLYTICS_RESOURCE_FILE).getAbsoluteFile();
        
        String contents = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" + 
                "    <string name=\"" + LEGACY_MAPPING_FILE_ID_RESOURCE_NAME + "\" translatable=\"false\">" + CRASHLYTICS_BUILD_ID + "</string>\n" + 
                "</resources>";

        try {
            FileUtils.writeStringToFile(resFile, contents, StandardCharsets.UTF_8);
            getLogger().info("✅ OMNI: Build ID inyectado con éxito.");
        } catch (IOException e) {
            throw new CompilationFailedException("Fallo en inyección de seguridad Crashlytics: " + e.getMessage());
        }
    }

    public static Repository getRepository(Project project, AndroidModule module) {
        try {
            Class<?> clazz = Class.forName(XML_REPOSITORY_CLASS);
            Method method = clazz.getDeclaredMethod("getRepository", Project.class, AndroidModule.class);
            Object invoke = method.invoke(null, project, module);

            Method getRepository = clazz.getDeclaredMethod("getRepository");
            Object invoke1 = getRepository.invoke(invoke);
            if (invoke1 instanceof Repository) {
                return ((Repository) invoke1);
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}
