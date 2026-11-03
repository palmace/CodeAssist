package com.omni.stresstest.data;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppDatabase_Impl extends AppDatabase {
    
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @Override
    protected InvalidationTracker createInvalidationTracker() {
        // Ajuste v34.53: Cumpliendo con el contrato Map<String, Set<String>> de Room 2.5+
        Map<String, String> shadowTablesMap = new HashMap<>(0);
        Map<String, Set<String>> viewTablesMap = new HashMap<>(0);
        return new InvalidationTracker(this, shadowTablesMap, viewTablesMap, "image_data", "data_items");
    }

    @Override
    public void clearAllTables() {}

    @Override
    public DataItemDao dataItemDao() {
        return new DataItemDao() {
            @Override public void deleteOldItems(long threshold) {}
            @Override public void insert(com.omni.stresstest.models.DataItem item) {}
            @Override public void insertAll(List<com.omni.stresstest.models.DataItem> items) {}
            @Override public List<com.omni.stresstest.models.DataItem> getAll() { return null; }
            @Override public void deleteAll() {}
        };
    }

    @Override
    public com.omni.stresstest.data.dao.ImageDataDao imageDataDao() {
        return new com.omni.stresstest.data.dao.ImageDataDao() {
            @Override public androidx.lifecycle.LiveData<List<com.omni.stresstest.models.ImageData>> getAllImages() { return null; }
            @Override public androidx.lifecycle.LiveData<List<com.omni.stresstest.models.ImageData>> getImagesByCategory(String category) { return null; }
            @Override public void insert(com.omni.stresstest.models.ImageData image) {}
            @Override public void insertAll(List<com.omni.stresstest.models.ImageData> images) {}
            @Override public void deleteAll() {}
        };
    }
}
