package com.nutri.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        DayEntity::class,
        MealLogEntity::class,
        MealSlotEntity::class,
        SlotSkipEntity::class,
        ChatMessageEntity::class,
        DayDigestEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(DietaBotConverters::class)
abstract class DietaBotDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dayDao(): DayDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun mealSlotDao(): MealSlotDao
    abstract fun slotSkipDao(): SlotSkipDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun dayDigestDao(): DayDigestDao
}
