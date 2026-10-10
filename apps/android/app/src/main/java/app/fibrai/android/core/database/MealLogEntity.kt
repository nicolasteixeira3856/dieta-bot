package app.fibrai.android.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_log",
    foreignKeys = [
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("slotId")],
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    val window: String = "",
    val text: String = "",
    val kcal: Int = 0,
    val p: Int = 0,
    val stable: Int = 0,
    /** Null = orphan, shown as "Outros". */
    val slotId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val carbs: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val fat: Int = 0,
    @ColumnInfo(defaultValue = "'user'")
    val source: String = "user",
    /** A68 (v15): the recipe version a record by recipe used; null otherwise. Not a foreign key: deleting a recipe keeps the record. */
    val recipeVersionId: Long? = null,
    /** A72 (ADR-058, v17): `slot` (a meal record) | `extra` (eaten outside the meals: no slot, its own [time]). */
    @ColumnInfo(defaultValue = "'slot'")
    val kind: String = "slot",
    /** A72 (v17): the time of an extra, `HH:mm` America/Sao_Paulo; null on a meal record. */
    val time: String? = null,
    /** A72 (v17): the key of an extra, so its receipt can undo, delete or move exactly that row; null on a meal record. */
    val extraId: Long? = null,
)
