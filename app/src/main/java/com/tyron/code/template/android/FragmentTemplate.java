package com.tyron.code.template.android;

import android.os.Parcel;

import com.tyron.code.template.CodeTemplate;
import com.tyron.code.template.java.JavaClassTemplate;

public class FragmentTemplate extends JavaClassTemplate {

    public FragmentTemplate() {
        super();
    }

    public FragmentTemplate(Parcel in) {
        super(in);
    }

    @Override
    public String getName() {
        return "Fragment";
    }

    @Override
    public void setup() {
        setContents("package " + CodeTemplate.PACKAGE_NAME + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import android.view.LayoutInflater;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.annotation.Nullable;\n" +
                "import androidx.fragment.app.Fragment;\n\n" +
                "public class " + CodeTemplate.CLASS_NAME + " extends Fragment {\n\n" +
                "   @Nullable\n" +
                "   @Override\n" +
                "   public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {\n" +
                "       return super.onCreateView(inflater, container, savedInstanceState);\n" +
                "   }\n" +
                "}");
    }
}
