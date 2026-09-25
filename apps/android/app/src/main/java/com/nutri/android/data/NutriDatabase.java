package com.nutri.android.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
        entities = {ProfileEntity.class, DayEntity.class, MealLogEntity.class},
        version = 1,
        exportSchema = false
)
@TypeConverters(NutriConverters.class)
public abstract class NutriDatabase extends RoomDatabase {
    public abstract ProfileDao profileDao();
    public abstract DayDao dayDao();
    public abstract MealLogDao mealLogDao();
}
