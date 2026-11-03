package io.github.rosemoe.sora.widget.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatImageButton;

import com.tyron.actions.ActionManager;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.actions.AnAction;
import com.tyron.actions.util.DataContextUtils;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.code.R;
import com.tyron.code.util.CoordinatePopupMenu;
import com.tyron.completion.java.util.DiagnosticUtil;
import com.tyron.completion.java.action.quickfix.JavaQuickFixProvider;
import com.tyron.kotlin_completion.action.KotlinQuickFixProvider;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * EditorTextActionWindow v1.7: "The Omni-Intentional".
 * v1.7: Sincronizado con el renombre de getDiagnosticsList en Editor.
 */
public class EditorTextActionWindow extends LinearLayout implements EditorBuiltinComponent {

    protected CodeEditor mEditor;
    private boolean mEnabled = true;
    
    private AppCompatImageButton mBtnQuickFix;
    private AppCompatImageButton mBtnCut;
    private AppCompatImageButton mBtnCopy;
    private AppCompatImageButton mBtnPaste;

    public EditorTextActionWindow(CodeEditor editor) {
        super(editor.getContext());
        mEditor = editor;
        init();
    }

    public EditorTextActionWindow(Context context) {
        super(context);
        init();
    }

    public EditorTextActionWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Context context = getContext();
        int layoutId = context.getResources().getIdentifier("editor_text_action_window", "layout", context.getPackageName());
        if (layoutId != 0) {
            LayoutInflater.from(context).inflate(layoutId, this, true);
            
            mBtnQuickFix = findViewById(R.id.btn_quick_fix);
            mBtnCut = findViewById(R.id.btn_cut);
            mBtnCopy = findViewById(R.id.btn_copy);
            mBtnPaste = findViewById(R.id.btn_paste);

            if (mBtnQuickFix == null) {
                mBtnQuickFix = new AppCompatImageButton(context);
                mBtnQuickFix.setId(R.id.btn_quick_fix);
                mBtnQuickFix.setImageResource(android.R.drawable.ic_menu_help);
                mBtnQuickFix.setVisibility(GONE);
                addView(mBtnQuickFix, 0);
            }
        } else {
            setOrientation(HORIZONTAL);
        }
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        setVisibility(enabled ? VISIBLE : GONE);
    }

    public void onUpdate(int x, int y, int width, int height) {
        if (mEditor == null || mBtnQuickFix == null) return;

        int cursor = mEditor.getCursor().getLeft();
        if (mEditor instanceof com.tyron.editor.Editor) {
            List<DiagnosticWrapper> diags = ((com.tyron.editor.Editor) mEditor).getDiagnosticsList();
            DiagnosticWrapper wrapper = DiagnosticUtil.getDiagnosticWrapper(diags, cursor);
            
            if (wrapper != null) {
                mBtnQuickFix.setVisibility(VISIBLE);
                mBtnQuickFix.setOnClickListener(v -> handleQuickFix(wrapper));
            } else {
                mBtnQuickFix.setVisibility(GONE);
            }
        }
    }

    private void handleQuickFix(DiagnosticWrapper diagnostic) {
        if (mEditor == null) return;

        CoordinatePopupMenu popupMenu = new CoordinatePopupMenu(getContext(), mEditor, Gravity.TOP);
        DataContext dataContext = DataContextUtils.getDataContext(mEditor);
        dataContext.putData(CommonDataKeys.DIAGNOSTIC, diagnostic);
        
        List<AnAction> fixes = new ArrayList<>();
        
        // Detectar el lenguaje del archivo para llamar al proveedor correcto
        if (mEditor instanceof com.tyron.editor.Editor) {
            File currentFile = ((com.tyron.editor.Editor) mEditor).getCurrentFile();
            if (currentFile != null) {
                if (currentFile.getName().endsWith(".kt")) {
                    fixes.addAll(KotlinQuickFixProvider.getFixesFor(diagnostic));
                } else if (currentFile.getName().endsWith(".java")) {
                    fixes.addAll(JavaQuickFixProvider.getFixesFor(diagnostic));
                }
            }
        }
        
        // Poblar el menú con ejecución real y validación de presentación
        for (AnAction fix : fixes) {
            AnActionEvent event = new AnActionEvent(dataContext, ActionPlaces.EDITOR, fix.getTemplatePresentation(), false, false);
            fix.update(event);
            
            if (event.getPresentation().isVisible() && event.getPresentation().isEnabled()) {
                popupMenu.getMenu().add(event.getPresentation().getText())
                    .setOnMenuItemClickListener(item -> {
                        fix.actionPerformed(event);
                        dismiss();
                        return true;
                    });
            }
        }

        if (popupMenu.getMenu().size() > 0) {
            int[] location = new int[2];
            mBtnQuickFix.getLocationOnScreen(location);
            // Ajuste de posición: mostrar justo encima del botón de bombilla
            popupMenu.show(location[0], location[1] - mBtnQuickFix.getHeight() - 10);
        }
    }

    public void show() {
        if (mEnabled) setVisibility(VISIBLE);
    }

    public void dismiss() {
        setVisibility(GONE);
    }
    
    public void unregister() {
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);
    }
}
