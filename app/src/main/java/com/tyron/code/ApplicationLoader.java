package com.tyron.code;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;

import com.developer.crashx.config.CrashConfig;
import com.tyron.actions.ActionManager;
import com.tyron.builder.BuildModule;
import com.tyron.code.event.EventManager;
import com.tyron.code.ui.editor.action.CloseAllEditorAction;
import com.tyron.code.ui.editor.action.CloseFileEditorAction;
import com.tyron.code.ui.editor.action.CloseOtherEditorAction;
import com.tyron.code.ui.editor.action.DiagnosticInfoAction;
import com.tyron.code.ui.editor.action.PreviewLayoutAction;
import com.tyron.code.ui.editor.action.text.TextActionGroup;
import com.tyron.code.ui.file.action.ImportFileActionGroup;
import com.tyron.code.ui.file.action.NewFileActionGroup;
import com.tyron.code.ui.file.action.file.DeleteFileAction;
import com.tyron.code.ui.main.action.compile.CompileActionGroup;
import com.tyron.code.ui.main.action.debug.DebugActionGroup;
import com.tyron.code.ui.main.action.other.FormatAction;
import com.tyron.code.ui.main.action.other.OpenSettingsAction;
import com.tyron.code.ui.main.action.project.ProjectActionGroup;
import com.tyron.code.ui.main.action.project.SyncProjectAction;
import com.tyron.code.ui.main.action.project.LibraryManagerAction;
import com.tyron.code.ui.main.action.project.TerminalAction;
import com.tyron.code.ui.settings.ApplicationSettingsFragment;
import com.tyron.common.ApplicationProvider;
import com.tyron.completion.CompletionProvider;
import com.tyron.completion.index.CompilerService;
import com.tyron.completion.java.CompletionModule;
import com.tyron.completion.java.JavaCompilerProvider;
import com.tyron.completion.java.JavaCompletionProvider;
import com.tyron.completion.xml.XmlCompletionModule;
import com.tyron.completion.xml.XmlIndexProvider;
import com.tyron.editor.selection.ExpandSelectionProvider;
import com.tyron.kotlin_completion.KotlinCompletionModule;
import com.tyron.language.fileTypes.FileTypeManager;
import com.tyron.language.java.JavaFileType;
import com.tyron.language.java.JavaLanguage;
import com.tyron.language.kotlin.KotlinFileType;
import com.tyron.language.kotlin.KotlinLanguage;
import com.tyron.language.xml.XmlFileType;
import com.tyron.language.xml.XmlLanguage;
import com.tyron.selection.java.JavaExpandSelectionProvider;
import com.tyron.selection.xml.XmlExpandSelectionProvider;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.File;

/**
 * ApplicationLoader v41.4.18: "The Sovereign Genesis - Terminal Sync".
 * v41.4.18: Registro explícito de TerminalAction para rescate de librerías.
 */
public class ApplicationLoader extends Application {

    private static ApplicationLoader sInstance;
    public static Context applicationContext;
    private EventManager mEventManager;

    public static ApplicationLoader getInstance() { return sInstance; }

    public static SharedPreferences getDefaultPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        applicationContext = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { HiddenApiBypass.addHiddenApiExemptions("Lsun/misc/Unsafe"); } catch (Throwable ignored) {}
        }

        setupTheme();
        mEventManager = new EventManager();

        try {
            ApplicationProvider.initialize(applicationContext);
            CompletionModule.initialize(applicationContext);
            XmlCompletionModule.initialize(applicationContext);
            BuildModule.initialize(applicationContext);
            
            File sdcard = Environment.getExternalStorageDirectory();
            File androidJar = new File(sdcard, "Android_install/Sdk/platforms/android-34/android.jar");
            if (!androidJar.exists()) androidJar = new File(sdcard, "Download/android.jar");
            
            if (androidJar.exists()) BuildModule.setAndroidJar(androidJar);
        } catch (Exception e) {
            Log.e("ApplicationLoader", "Fallo inicialización: " + e.getMessage());
        }

        runStartup();
    }

    public static void showToast(final String message) {
        if (applicationContext != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            );
        }
    }

    @NonNull public EventManager getEventManager() { return mEventManager; }

    private void setupTheme() {
        try {
            ApplicationSettingsFragment.ThemeProvider provider = new ApplicationSettingsFragment.ThemeProvider(this);
            AppCompatDelegate.setDefaultNightMode(provider.getThemeFromPreferences());
        } catch (Exception e) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void runStartup() {
        StartupManager startupManager = new StartupManager();
        startupManager.addStartupActivity(() -> {
            FileTypeManager manager = FileTypeManager.getInstance();
            manager.registerFileType(JavaFileType.INSTANCE);
            manager.registerFileType(XmlFileType.INSTANCE);
            manager.registerFileType(KotlinFileType.INSTANCE);
        });
        startupManager.addStartupActivity(() -> {
            ExpandSelectionProvider.registerProvider(JavaLanguage.INSTANCE, new JavaExpandSelectionProvider());
            ExpandSelectionProvider.registerProvider(XmlLanguage.INSTANCE, new XmlExpandSelectionProvider());
        });
        startupManager.addStartupActivity(() -> {
            CompilerService index = CompilerService.getInstance();
            if (index.isEmpty()) {
                index.registerIndexProvider(JavaCompilerProvider.KEY, new JavaCompilerProvider());
                index.registerIndexProvider(XmlIndexProvider.KEY, new XmlIndexProvider());
            }
        });
        startupManager.addStartupActivity(() -> {
            CompletionProvider.registerCompletionProvider(JavaLanguage.INSTANCE, new JavaCompletionProvider());
        });
        startupManager.addStartupActivity(() -> {
            ActionManager manager = ActionManager.getInstance();
            manager.registerAction(CompileActionGroup.ID, new CompileActionGroup());
            manager.registerAction(ProjectActionGroup.ID, new ProjectActionGroup());
            
            // OMNI: Registro de acciones tácticas
            manager.registerAction(TerminalAction.ID, new TerminalAction());

            manager.registerAction("SyncProjectAction", new SyncProjectAction());
            manager.registerAction("LibraryManagerAction", new LibraryManagerAction());
            manager.registerAction(PreviewLayoutAction.ID, new PreviewLayoutAction());
            manager.registerAction(OpenSettingsAction.ID, new OpenSettingsAction());
            manager.registerAction(FormatAction.ID, new FormatAction());
            manager.registerAction(DebugActionGroup.ID, new DebugActionGroup());
            manager.registerAction(CloseFileEditorAction.ID, new CloseFileEditorAction());
            manager.registerAction(CloseOtherEditorAction.ID, new CloseOtherEditorAction());
            manager.registerAction(CloseAllEditorAction.ID, new CloseAllEditorAction());
            manager.registerAction(TextActionGroup.ID, new TextActionGroup());
            manager.registerAction(DiagnosticInfoAction.ID, new DiagnosticInfoAction());
            manager.registerAction(NewFileActionGroup.ID, new NewFileActionGroup());
            manager.registerAction(DeleteFileAction.ID, new DeleteFileAction());
            CompletionModule.registerActions(manager);
            XmlCompletionModule.registerActions(manager);
            KotlinCompletionModule.registerActions(manager);
        });
        startupManager.startup();
    }
}
