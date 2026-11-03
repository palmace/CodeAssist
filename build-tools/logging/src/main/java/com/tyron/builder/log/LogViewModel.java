package com.tyron.builder.log;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tyron.builder.model.DiagnosticWrapper;

import javax.tools.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LogViewModel v44.5.2: "The Forensic Reservoir - Infinite Flow".
 * v44.5.2: Inicialización Atómica - Garantiza que el reservorio esté listo antes de cualquier inyección.
 * v44.5.2: Mirroring a Logcat - Duplicación de logs críticos para depuración externa.
 * v44.5.2: Despacho Protegido - Evita colapsos de UI mediante copias inmutables (Snapshots).
 */
public class LogViewModel extends ViewModel {

    private static int totalCount = 0;
    public static final int APP_LOG = totalCount++;
    public static final int BUILD_LOG = totalCount++;
    public static final int DEBUG = totalCount++;
    public static final int IDE = totalCount++;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<List<DiagnosticWrapper>> mReservoirs = new ArrayList<>();
    private final List<MutableLiveData<List<DiagnosticWrapper>>> mLiveDataLogs = new ArrayList<>();

    public LogViewModel() {
        for (int i = 0; i < totalCount; i++) {
            mReservoirs.add(Collections.synchronizedList(new ArrayList<>()));
            mLiveDataLogs.add(new MutableLiveData<>(new ArrayList<>()));
        }
    }

    public LiveData<List<DiagnosticWrapper>> getLogs(int id) {
        if (id >= 0 && id < mLiveDataLogs.size()) {
            return mLiveDataLogs.get(id);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }
    
    public LiveData<List<DiagnosticWrapper>> getBuildLogs() { return getLogs(BUILD_LOG); }
    public LiveData<List<DiagnosticWrapper>> getAppLogs() { return getLogs(APP_LOG); }
    public LiveData<List<DiagnosticWrapper>> getDiagnosticLogs() { return getLogs(DEBUG); }

    public void updateLogs(int id, List<DiagnosticWrapper> diagnostics) {
        if (id < 0 || id >= mReservoirs.size()) return;
        mReservoirs.get(id).clear();
        mReservoirs.get(id).addAll(diagnostics);
        dispatch(id);
    }

    public void clear(int id) {
        if (id >= 0 && id < mReservoirs.size()) {
            mReservoirs.get(id).clear();
            dispatch(id);
        }
    }

    public void e(int id, DiagnosticWrapper diagnostic) { add(id, diagnostic); }
    public void d(int id, String message) { add(id, wrap(message, Diagnostic.Kind.OTHER)); }
    public void d(int id, DiagnosticWrapper diagnosticWrapper) { add(id, diagnosticWrapper); }
    public void w(int id, DiagnosticWrapper diagnosticWrapper) { add(id, diagnosticWrapper); }
    public void w(int id, String message) { add(id, wrap(message, Diagnostic.Kind.WARNING)); }
    public void e(int id, String message) { add(id, wrap(message, Diagnostic.Kind.ERROR)); }

    private DiagnosticWrapper wrap(String message, Diagnostic.Kind kind) {
        DiagnosticWrapper wrapper = new DiagnosticWrapper();
        wrapper.setMessage(message);
        wrapper.setKind(kind);
        return wrapper;
    }

    private void add(int id, DiagnosticWrapper diagnosticWrapper) {
        if (id < 0 || id >= mReservoirs.size()) return;
        
        mReservoirs.get(id).add(diagnosticWrapper);
        
        // v44.5.2: Mirroring a Logcat para visibilidad forense fuera de la app
        if (id == BUILD_LOG) {
            Log.d("OMNI_BUILD", String.valueOf(diagnosticWrapper.getMessage(null)));
        }

        dispatch(id);
    }

    private void dispatch(int id) {
        // Snapshot para evitar ConcurrentModificationException en el hilo de UI
        List<DiagnosticWrapper> snapshot = new ArrayList<>(mReservoirs.get(id));
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            mLiveDataLogs.get(id).setValue(snapshot);
        } else {
            mainHandler.post(() -> mLiveDataLogs.get(id).setValue(snapshot));
        }
    }
}
