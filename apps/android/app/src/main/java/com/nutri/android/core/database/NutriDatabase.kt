package com.nutri.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ProfileEntity::class, DayEntity::class, MealLogEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(NutriConverters::class)
abstract class NutriDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dayDao(): DayDao
    abstract fun mealLogDao(): MealLogDao
}
