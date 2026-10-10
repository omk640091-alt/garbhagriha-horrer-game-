package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity representing a player's saved checkpoint at a Sacred Shrine inside a temple sanctum.
 * Stores temple progress, player coordinates, vital Health (Prana), Sanity level, Stamina,
 * Diya oil, lit Diyas, unlocked Vedic Seals, and collected inventory artifacts.
 */
@Entity(tableName = "sacred_shrine_saves")
data class SacredShrineSaveEntity(
  @PrimaryKey
  val templeId: String,
  val templeName: String,
  val shrineTitle: String,
  val playerX: Double,
  val playerY: Double,
  val playerAngle: Double,
  val pranaPercent: Int,
  val sanityPercent: Int,
  val staminaPercent: Int,
  val diyaOilPercent: Float,
  val diyaLit: Boolean,
  val litDiyaCoordsSerialized: String,
  val solvedSealCoordsSerialized: String,
  val collectedArtifactIdsCsv: String,
  val survivalSeconds: Int,
  val savedAtEpochMillis: Long = System.currentTimeMillis()
) {
  fun parseLitDiyaCoords(): Set<Pair<Int, Int>> = parseCoordSet(litDiyaCoordsSerialized)

  fun parseSolvedSealCoords(): Set<Pair<Int, Int>> = parseCoordSet(solvedSealCoordsSerialized)

  fun parseCollectedArtifactIds(): List<String> =
    collectedArtifactIdsCsv
      .split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }

  companion object {
    fun serializeCoords(coords: Set<Pair<Int, Int>>): String =
      coords.joinToString(";") { "${it.first}:${it.second}" }

    fun serializeArtifactIds(ids: Collection<String>): String =
      ids.joinToString(",")

    private fun parseCoordSet(raw: String): Set<Pair<Int, Int>> {
      if (raw.isBlank()) return emptySet()
      return raw.split(";")
        .mapNotNull { token ->
          val parts = token.split(":")
          if (parts.size == 2) {
            val x = parts[0].toIntOrNull()
            val y = parts[1].toIntOrNull()
            if (x != null && y != null) x to y else null
          } else {
            null
          }
        }
        .toSet()
    }
  }
}

/**
 * Room Data Access Object (DAO) for querying, saving, and deleting Sacred Shrine checkpoints.
 */
@Dao
interface SacredShrineSaveDao {
  @Query("SELECT * FROM sacred_shrine_saves ORDER BY savedAtEpochMillis DESC")
  fun observeAllShrineSaves(): Flow<List<SacredShrineSaveEntity>>

  @Query("SELECT * FROM sacred_shrine_saves WHERE templeId = :templeId LIMIT 1")
  fun observeShrineSaveForTemple(templeId: String): Flow<SacredShrineSaveEntity?>

  @Query("SELECT * FROM sacred_shrine_saves WHERE templeId = :templeId LIMIT 1")
  suspend fun getShrineSaveForTemple(templeId: String): SacredShrineSaveEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertShrineSave(save: SacredShrineSaveEntity)

  @Query("DELETE FROM sacred_shrine_saves WHERE templeId = :templeId")
  suspend fun deleteShrineSaveForTemple(templeId: String)

  @Query("DELETE FROM sacred_shrine_saves")
  suspend fun clearAllShrineSaves()
}

/**
 * Room Database holder for local Sacred Shrine saves.
 */
@Database(
  entities = [SacredShrineSaveEntity::class],
  version = 1,
  exportSchema = false
)
abstract class SacredShrineDatabase : RoomDatabase() {
  abstract fun sacredShrineSaveDao(): SacredShrineSaveDao

  companion object {
    @Volatile
    private var INSTANCE: SacredShrineDatabase? = null

    fun getInstance(context: Context): SacredShrineDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          SacredShrineDatabase::class.java,
          "garbhagriha_sacred_shrines.db"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}

/**
 * Repository abstracting local Room persistence for Sacred Shrine saves from the UI/ViewModel.
 */
class SacredShrineRepository(
  private val shrineSaveDao: SacredShrineSaveDao
) {
  val allShrineSaves: Flow<List<SacredShrineSaveEntity>> =
    shrineSaveDao.observeAllShrineSaves()

  fun observeShrineSave(templeId: String): Flow<SacredShrineSaveEntity?> =
    shrineSaveDao.observeShrineSaveForTemple(templeId)

  suspend fun getShrineSave(templeId: String): SacredShrineSaveEntity? =
    shrineSaveDao.getShrineSaveForTemple(templeId)

  suspend fun saveAtSacredShrine(
    templeId: String,
    templeName: String,
    shrineTitle: String,
    playerX: Double,
    playerY: Double,
    playerAngle: Double,
    pranaPercent: Int,
    sanityPercent: Int,
    staminaPercent: Int,
    diyaOilPercent: Float,
    diyaLit: Boolean,
    litDiyaCoords: Set<Pair<Int, Int>>,
    solvedSealCoords: Set<Pair<Int, Int>>,
    collectedArtifactIds: Collection<String>,
    survivalSeconds: Int
  ): SacredShrineSaveEntity {
    val entity = SacredShrineSaveEntity(
      templeId = templeId,
      templeName = templeName,
      shrineTitle = shrineTitle,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      pranaPercent = pranaPercent.coerceIn(1, 100),
      sanityPercent = sanityPercent.coerceIn(0, 100),
      staminaPercent = staminaPercent.coerceIn(0, 100),
      diyaOilPercent = diyaOilPercent.coerceIn(0f, 100f),
      diyaLit = diyaLit,
      litDiyaCoordsSerialized = SacredShrineSaveEntity.serializeCoords(litDiyaCoords),
      solvedSealCoordsSerialized = SacredShrineSaveEntity.serializeCoords(solvedSealCoords),
      collectedArtifactIdsCsv = SacredShrineSaveEntity.serializeArtifactIds(collectedArtifactIds),
      survivalSeconds = survivalSeconds.coerceAtLeast(0),
      savedAtEpochMillis = System.currentTimeMillis()
    )
    shrineSaveDao.upsertShrineSave(entity)
    return entity
  }

  suspend fun deleteShrineSave(templeId: String) {
    shrineSaveDao.deleteShrineSaveForTemple(templeId)
  }
}
