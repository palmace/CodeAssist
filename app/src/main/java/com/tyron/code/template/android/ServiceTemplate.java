package com.tyron.code.template.android;

import android.os.Parcel;

import com.tyron.code.template.CodeTemplate;
import com.tyron.code.template.java.JavaClassTemplate;

public class ServiceTemplate extends JavaClassTemplate {

    public ServiceTemplate() {
        super();
    }

    public ServiceTemplate(Parcel in) {
        super(in);
    }

    @Override
    public String getName() {
        return "Service";
    }

    @Override
    public void setup() {
        setContents("package " + CodeTemplate.PACKAGE_NAME + ";\n\n" +
                "import android.app.Service;\n" +
                "import android.content.Intent;\n" +
                "import android.os.IBinder;\n" +
                "import androidx.annotation.Nullable;\n\n" +
                "public class " + CodeTemplate.CLASS_NAME + " extends Service {\n\n" +
                "   @Nullable\n" +
                "   @Override\n" +
                "   public IBinder onBind(Intent intent) {\n" +
                "       return null;\n" +
                "   }\n" +
                "}");
    }
}
