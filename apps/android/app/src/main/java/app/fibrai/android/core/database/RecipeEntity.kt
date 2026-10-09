package app.fibrai.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A68 (ADR-052, v15): a saved recipe; its numbers and ingredients live in its versions. [originMessageId]: the plan it came from. */
@Entity(tableName = "recipe", indices = [Index("originMessageId")])
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val originMessageId: Long? = null,
    val createdAtEpochMs: Long,
)

/**
 * One version of a recipe (A68, ADR-052): editing creates a new one, a record keeps the one it used. [ingredients] is the JSON
 * list of `{name, g, kcal}`, [steps] the JSON list of strings; [yieldText] and [portion] are free text, null when unknown.
 */
@Entity(
    tableName = "recipe_version",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class RecipeVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val version: Int,
    val ingredients: String,
    val steps: String,
    val kcal: Int,
    val p: Int,
    val c: Int,
    val g: Int,
    val yieldText: String? = null,
    val portion: String? = null,
    val createdAtEpochMs: Long,
)

/** A recipe with its current (highest) version. */
data class SavedRecipe(val recipe: RecipeEntity, val version: RecipeVersionEntity)

@Dao
interface RecipeDao {
    @Insert
    suspend fun insertRecipe(row: RecipeEntity): Long

    @Insert
    suspend fun insertVersion(row: RecipeVersionEntity): Long

    @Query("SELECT * FROM recipe ORDER BY createdAtEpochMs DESC, id DESC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe ORDER BY createdAtEpochMs DESC, id DESC")
    suspend fun recipes(): List<RecipeEntity>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun recipe(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipe WHERE originMessageId = :messageId LIMIT 1")
    suspend fun byOrigin(messageId: Long): RecipeEntity?

    @Query("SELECT * FROM recipe_version ORDER BY recipeId, version")
    fun observeVersions(): Flow<List<RecipeVersionEntity>>

    @Query("SELECT * FROM recipe_version WHERE recipeId = :recipeId ORDER BY version DESC LIMIT 1")
    suspend fun currentVersion(recipeId: Long): RecipeVersionEntity?

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun delete(id: Long)
}
