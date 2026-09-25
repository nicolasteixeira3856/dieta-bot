package com.nutri.android.data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface DayDao {
    @Query("SELECT * FROM day WHERE date = :date LIMIT 1")
    Flow<DayEntity> observe(String date);

    @Nullable
    @Query("SELECT * FROM day WHERE date = :date LIMIT 1")
    DayEntity get(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DayEntity row);
}
