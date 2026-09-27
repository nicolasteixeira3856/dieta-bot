package com.nutri.android.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2 -> v3: estimate slot, question and item names on chat_message (A5). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `estimateSlotId` INTEGER")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `estimateQuestion` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `estimateItems` TEXT")
    }
}

/**
 * v1 -> v2. DDL mirrors schemas/.../2.json. meal_log is rebuilt because
 * SQLite cannot add a foreign key with ALTER TABLE.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `sex` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `ageYears` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `heightCm` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `weightKg` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `proteinTargetG` INTEGER NOT NULL DEFAULT 150")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `carbTargetG` INTEGER NOT NULL DEFAULT 200")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `fatTargetG` INTEGER NOT NULL DEFAULT 67")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_slot` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `minutesFromMidnight` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_log_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `window` TEXT NOT NULL, `text` TEXT NOT NULL, `kcal` INTEGER NOT NULL, " +
                "`p` INTEGER NOT NULL, `stable` INTEGER NOT NULL, `slotId` INTEGER, " +
                "`carbs` INTEGER NOT NULL DEFAULT 0, `fat` INTEGER NOT NULL DEFAULT 0, " +
                "`source` TEXT NOT NULL DEFAULT 'user', " +
                "FOREIGN KEY(`slotId`) REFERENCES `meal_slot`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL(
            "INSERT INTO `meal_log_new` (`id`, `date`, `window`, `text`, `kcal`, `p`, `stable`) " +
                "SELECT `id`, `date`, `window`, `text`, `kcal`, `p`, `stable` FROM `meal_log`",
        )
        db.execSQL("DROP TABLE `meal_log`")
        db.execSQL("ALTER TABLE `meal_log_new` RENAME TO `meal_log`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_log_slotId` ON `meal_log` (`slotId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `slot_skip` (`date` TEXT NOT NULL, `slotId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`, `slotId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                "`createdAtEpochMs` INTEGER NOT NULL, `estimateKcal` INTEGER, `estimateP` INTEGER, " +
                "`estimateC` INTEGER, `estimateG` INTEGER, `estimateConfidence` TEXT, `photoPath` TEXT)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_message_date` ON `chat_message` (`date`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_digest` (`date` TEXT NOT NULL, `seq` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`date`, `seq`))",
        )
    }
}
