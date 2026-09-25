package com.nutri.android.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface MealLogDao {
    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY id ASC")
    Flow<List<MealLogEntity>> observeByDate(String date);

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY id ASC")
    List<MealLogEntity> getByDate(String date);

    @Insert
    void insert(MealLogEntity row);

    @Query("DELETE FROM meal_log WHERE date = :date")
    void deleteByDate(String date);
}
