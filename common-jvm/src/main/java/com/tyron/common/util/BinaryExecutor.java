package com.tyron.common.util;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Scanner;

public class BinaryExecutor {
    private static final String TAG = BinaryExecutor.class.getSimpleName();

    private final ProcessBuilder mProcess = new ProcessBuilder();
    private final StringWriter mWriter = new StringWriter();

    public void setCommands(List<String> arrayList) {
        mProcess.command(arrayList);
        mProcess.redirectErrorStream(true);
    }

    public String execute() {
        try {
            Process process = mProcess.start();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    synchronized (mWriter) {
                        mWriter.append(line);
                        mWriter.append(System.lineSeparator());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            mWriter.write("Error ejecutando binario: " + e.getMessage());
        }
        return mWriter.toString();
    }

    public String getLog() {
        return mWriter.toString();
    }
}
