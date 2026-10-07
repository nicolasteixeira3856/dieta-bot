package app.fibrai.android.core.database

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
        ClosureEntity::class,
        PlannedMealEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
@TypeConverters(FibraiConverters::class)
abstract class FibraiDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dayDao(): DayDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun mealSlotDao(): MealSlotDao
    abstract fun slotSkipDao(): SlotSkipDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun dayDigestDao(): DayDigestDao
    abstract fun closureDao(): ClosureDao
    abstract fun plannedMealDao(): PlannedMealDao
}
