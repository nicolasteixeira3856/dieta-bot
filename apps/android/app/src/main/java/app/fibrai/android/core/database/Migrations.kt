package app.fibrai.android.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Every migration, oldest first: the app and the migration tests open the file with all of them. */
val ALL_MIGRATIONS: Array<Migration> by lazy {
    arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
    )
}

/** v14 -> v15 (A68, ADR-052): saved recipes and their versions; a record keeps the version it used. Nothing to backfill. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipe` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`originMessageId` INTEGER, `createdAtEpochMs` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_originMessageId` ON `recipe` (`originMessageId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipe_version` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, " +
                "`version` INTEGER NOT NULL, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `kcal` INTEGER NOT NULL, `p` INTEGER NOT NULL, " +
                "`c` INTEGER NOT NULL, `g` INTEGER NOT NULL, `yieldText` TEXT, `portion` TEXT, `createdAtEpochMs` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_version_recipeId` ON `recipe_version` (`recipeId`)")
        db.execSQL("ALTER TABLE `meal_log` ADD COLUMN `recipeVersionId` INTEGER")
    }
}

/** v13 -> v14 (A66, ADR-050): the raw typed actions of an answer, on its first row. Old rows: null (a legacy answer). */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `actions` TEXT")
    }
}

/** v12 -> v13 (A64, ADR-053 § 2): the facts an answer saved from an explicit statement, `Anotado: {text}`. Old rows: null. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `noted` TEXT")
    }
}

/**
 * v11 -> v12, the one version of A60 (ADR-039, ADR-044, ADR-046): the plan budget of an answer, the profile tone, the
 * closure table and the planned meal table. Old rows keep null budgets and read back the tone `seco`.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `planBudget` TEXT")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `tone` TEXT NOT NULL DEFAULT 'seco'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `closure` (`key` TEXT NOT NULL, `period` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                "`numbers` TEXT NOT NULL, `text` TEXT, `status` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                "`retried` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`key`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `planned_meal` (`date` TEXT NOT NULL, `slotId` INTEGER NOT NULL, `text` TEXT NOT NULL, " +
                "`kcal` INTEGER NOT NULL, `p` INTEGER NOT NULL, `c` INTEGER NOT NULL, `g` INTEGER NOT NULL, " +
                "`sourceMessageId` INTEGER, PRIMARY KEY(`date`, `slotId`))",
        )
    }
}

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
