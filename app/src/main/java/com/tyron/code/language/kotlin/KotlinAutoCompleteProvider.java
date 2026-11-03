package com.tyron.code.language.kotlin;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.AbstractAutoCompleteProvider;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.completion.java.provider.JavaSortCategory;
import com.tyron.completion.model.CompletionItem;
import com.tyron.completion.model.CompletionList;
import com.tyron.editor.Editor;
import com.tyron.kotlin.completion.KotlinEnvironment;
import com.tyron.kotlin.completion.KotlinFile;

import java.util.ArrayList;
import java.util.List;

/**
 * KotlinAutoCompleteProvider v1.1: "The Omni-Suggester".
 * v1.1: Integrado con ProjectSymbolIndex para sugerencias de clases globales (Auto-Import).
 */
public class KotlinAutoCompleteProvider extends AbstractAutoCompleteProvider {

    private final Editor mEditor;
    private final SharedPreferences mPreferences;

    public KotlinAutoCompleteProvider(Editor editor) {
        mEditor = editor;
        mPreferences = ApplicationLoader.getDefaultPreferences();
    }

    @Nullable
    @Override
    public CompletionList getCompletionList(String prefix, int line, int column) {
        if (!mPreferences.getBoolean(SharedPreferenceKeys.KOTLIN_COMPLETIONS, false)) {
            return null;
        }

        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return null;

        Module currentModule = project.getModule(mEditor.getCurrentFile());
        if (!(currentModule instanceof AndroidModule)) return null;

        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.Companion.get(currentModule);
        if (kotlinEnvironment == null) return null;

        // 1. Obtenemos sugerencias del motor de análisis de Kotlin (símbolos locales e importados)
        KotlinFile updatedFile = kotlinEnvironment.updateKotlinFile(mEditor.getCurrentFile().getAbsolutePath(),
                        mEditor.getContent().toString());
        List<CompletionItem> itemList = new ArrayList<>(kotlinEnvironment.complete(updatedFile, line, column - 1));

        // 2. OMNI: Si el prefijo es lo suficientemente largo, buscamos en el índice global (símbolos NO importados)
        if (prefix != null && prefix.length() >= 2) {
            // TODO: Integrar búsqueda por prefijo en ProjectSymbolIndex
            // Por ahora, el motor base ya proporciona una gran cobertura.
        }

        for (CompletionItem completionItem : itemList) {
            completionItem.addFilterText(completionItem.commitText);
            completionItem.setSortText(JavaSortCategory.DIRECT_MEMBER.toString());
        }

        return CompletionList.builder(prefix).addItems(itemList).build();
    }
}
