package com.nutri.android.data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    Flow<ProfileEntity> observe();

    @Nullable
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    ProfileEntity get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProfileEntity row);
}
