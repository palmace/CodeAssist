package com.tyron.code.ui.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatViewInflater;

/**
 * Custom ViewInflater to prevent Material Components from replacing ImageButtons
 * with incompatible classes that cause ClassCastException in sora-editor library.
 */
public class NoMaterialViewInflater extends AppCompatViewInflater {

    @Override
    protected View createView(Context context, String name, AttributeSet attrs) {
        // Interceptamos la creación de ImageButton y Button para devolver la versión base
        // Esto evita que MaterialComponents los convierta en algo que no hereda de ImageButton
        if ("ImageButton".equals(name)) {
            return new ImageButton(context, attrs);
        }
        if ("Button".equals(name)) {
            return new Button(context, attrs);
        }
        return super.createView(context, name, attrs);
    }
}
