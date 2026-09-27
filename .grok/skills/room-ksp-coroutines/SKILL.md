---
name: room-ksp-coroutines
description: Implementa persistência local com Room 2.6+ em Kotlin puro, KSP e Kotlin Coroutines/Flow. Use ao criar tabelas, DAOs, migrações ou refatorar código legado Java/kapt.
---

# room-ksp-coroutines

Diretrizes para arquitetura de dados com Room usando Kotlin puro e KSP (Kotlin Symbol Processing).

## Regras Obrigatórias

1. **Zero Java, Zero kapt**:
   - Todas as entidades e DAOs devem ser escritas em Kotlin.
   - O Gradle deve usar `ksp("androidx.room:room-compiler:2.6.1")` e nunca `kapt` ou `annotationProcessor`.

2. **Entidades como Kotlin Data Classes**:
   - Chaves primárias com `@PrimaryKey`.
   - Propriedades com valores padrão.
   ```kotlin
   @Entity(tableName = "profile")
   data class ProfileEntity(
       @PrimaryKey val id: Long = 1L,
       val ceilingKcal: Int = 2000,
       val proteinTargetG: Int = 150,
       val carbTargetG: Int = 200,
       val fatTargetG: Int = 67,
       val eatBack: String = "zero",
       val workoutKcal: Int? = null,
   )
   ```

3. **DAOs Reativos**:
   - Consultas observáveis devem retornar `Flow<T>` ou `Flow<List<T>>`.
   - Operações de escrita (`@Insert`, `@Update`, `@Delete`) devem ser funções suspensas (`suspend fun`).
   ```kotlin
   @Dao
   interface ProfileDao {
       @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
       fun observeProfile(): Flow<ProfileEntity?>

       @Insert(onConflict = OnConflictStrategy.REPLACE)
       suspend fun upsertProfile(profile: ProfileEntity)
   }
   ```

4. **Migrações Explícitas**:
   - Nunca usar `fallbackToDestructiveMigration()` em builds finais.
   - Todas as mudanças de schema exigem objeto `Migration(from, to)` com SQL seguro e `exportSchema = true` para verificação de schema em `schemas/`.

5. **TypeConverters em Kotlin**:
   - Use métodos estáticos (`@JvmStatic` ou `object`) para conversores simples de tipos primitivos (ex: `LocalDate` para string ISO).
