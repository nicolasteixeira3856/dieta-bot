package app.fibrai.android.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v10 -> v11: the skips listed with an answer (A59). Old rows stay null: nothing to apply. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `skipOutcomes` TEXT")
    }
}

/** v9 -> v10: the meal-change proposal of an answer (A47). Old rows stay null: legacy record flow, nothing guessed. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `mealChange` TEXT")
    }
}

/** v8 -> v9: the newest message a digest summarised (A38). Old digests stay null: cut by creation time. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `day_digest` ADD COLUMN `coversUntilId` INTEGER")
    }
}

/** v7 -> v8: autonomous record and receipt actions (A34). Old rows stay null: no actions. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `recordMode` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `recordState` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `receiptState` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `undoData` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `recordSource` TEXT")
    }
}

/** v6 -> v7: index of the Chat newest-first page (A32). No data change. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_message_createdAtEpochMs_id` ON `chat_message` (`createdAtEpochMs`, `id`)")
    }
}

/** v5 -> v6: memory v2 per message (A28). Old rows: nothing pending, nothing used, not updated. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `pendingMemory` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `memoryUsedKinds` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `memoryUpdated` INTEGER NOT NULL DEFAULT 0")
    }
}

/** v4 -> v5: meal text and intent of the Chat estimate (A27). Old rows stay null. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `estimateMealText` TEXT")
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `intent` TEXT")
    }
}

/** v3 -> v4: existing schedules keep their IDs and apply every day. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `meal_slot` ADD COLUMN `days` INTEGER NOT NULL DEFAULT 127")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `slotMode` TEXT NOT NULL DEFAULT 'same'")
    }
}

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
