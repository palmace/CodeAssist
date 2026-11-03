package com.omni.stresstest.data;
import android.content.Context;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public abstract com.omni.stresstest.data.dao.ImageDataDao imageDataDao();
    public abstract DataItemDao dataItemDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppDatabase_Impl();
                }
            }
        }
        return INSTANCE;
    }

    public interface DataItemDao {
        void deleteOldItems(long threshold);
        void insert(com.omni.stresstest.models.DataItem item);
        void insertAll(java.util.List<com.omni.stresstest.models.DataItem> items);
        java.util.List<com.omni.stresstest.models.DataItem> getAll();
        void deleteAll();
    }
}
