package com.tyron.builder.compiler;

import androidx.annotation.VisibleForTesting;

import com.tyron.builder.BuildModule;
import com.tyron.common.util.Decompress;
import com.android.apksig.ApkSigner.Builder;
import com.android.apksig.ApkSigner.SignerConfig;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.ArrayList;

public class ApkSigner {

    public static class Mode {
        public static int TEST = 0;
    }

    private final String mApkInputPath;
    private final String mApkOutputPath;

    public ApkSigner(String inputPath, String outputPath, int mode) {
        mApkInputPath = inputPath;
        mApkOutputPath = outputPath;
    }

    public void sign() throws Exception {
        File keyFile = new File(getTestKeyFilePath());
        File certFile = new File(getTestCertFilePath());

        // Cargar Clave Privada (PKCS8)
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

        // Cargar Certificado (X509)
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate;
        try (InputStream is = Files.newInputStream(certFile.toPath())) {
            certificate = (X509Certificate) certFactory.generateCertificate(is);
        }

        SignerConfig signerConfig = new SignerConfig.Builder(
                "CERT", privateKey, Collections.singletonList(certificate)).build();

        com.android.apksig.ApkSigner.Builder builder = new com.android.apksig.ApkSigner.Builder(
                Collections.singletonList(signerConfig))
                .setInputApk(new File(mApkInputPath))
                .setOutputApk(new File(mApkOutputPath))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setV4SigningEnabled(false)
                .setMinSdkVersion(21);

        builder.build().sign();
    }

    private String getTestKeyFilePath() {
        if (sTestKeyFile != null) {
            return sTestKeyFile.getAbsolutePath();
        }
        File check = new File(BuildModule.getContext().getFilesDir() + "/temp/testkey.pk8");

        if (check.exists()) {
            sTestKeyFile = check;
            return check.getAbsolutePath();
        }

        Decompress.unzipFromAssets(BuildModule.getContext(), "testkey.pk8.zip",
                check.getParentFile().getAbsolutePath());

        return check.getAbsolutePath();
    }

    private String getTestCertFilePath() {
        if (sTestCertFile != null) {
            return sTestCertFile.getAbsolutePath();
        }

        File check = new File(BuildModule.getContext().getFilesDir() +
                "/temp/testkey.x509.pem");

        if (check.exists()) {
            sTestCertFile = check;
            return check.getAbsolutePath();
        }

        Decompress.unzipFromAssets(BuildModule.getContext(), "testkey.x509.pem.zip",
                check.getParentFile().getAbsolutePath());

        return check.getAbsolutePath();
    }

    @VisibleForTesting
    public static void setTestKeyFile(File file) {
        sTestKeyFile = file;
    }

    @VisibleForTesting
    public static void setTestCertFile(File file) {
        sTestCertFile = file;
    }

    private static File sTestKeyFile;
    private static File sTestCertFile;
}
