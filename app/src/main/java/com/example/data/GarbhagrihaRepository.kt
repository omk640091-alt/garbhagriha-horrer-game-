package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GarbhagrihaRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- USER PROGRESS ---

  fun observeUserProgress(userId: String): Flow<UserProgress?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun ensureUserProgressInitialized(displayName: String): Result<UserProgress> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        val existing = snapshot.toObject(
          UserProgress::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: UserProgress(id = uid, userId = uid, displayName = displayName)
        Result.success(existing)
      } else {
        val safeName = displayName.trim().take(50).ifEmpty { "Sadhaka Explorer" }
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "sanctumsCleansed" to 0L,
          "diyasLit" to 0L,
          "puzzlesSolved" to 0L,
          "highestSurvivalSec" to 0L,
          "unlockedCodexIds" to listOf("brahmarakshasa", "brihadeeswarar"),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        val created = docRef.get().await().toObject(
          UserProgress::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: UserProgress(id = uid, userId = uid, displayName = safeName)
        Result.success(created)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordExpeditionOutcome(
    cleansed: Boolean,
    diyasAdded: Int,
    puzzlesAdded: Int,
    survivalSeconds: Int,
    unlockedId: String? = null
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        ensureUserProgressInitialized(auth.currentUser?.displayName ?: "Sadhaka")
      }
      val freshSnap = docRef.get().await()
      val currentCleansed = freshSnap.getLong("sanctumsCleansed") ?: 0L
      val currentDiyas = freshSnap.getLong("diyasLit") ?: 0L
      val currentPuzzles = freshSnap.getLong("puzzlesSolved") ?: 0L
      val currentBestSurvival = freshSnap.getLong("highestSurvivalSec") ?: 0L
      val existingCodex = (freshSnap.get("unlockedCodexIds") as? List<*>)
        ?.mapNotNull { it as? String }
        ?: listOf("brahmarakshasa", "brihadeeswarar")

      val updatedCodex = if (unlockedId != null && !existingCodex.contains(unlockedId)) {
        (existingCodex + unlockedId).take(25)
      } else {
        existingCodex.take(25)
      }

      val updates = mapOf(
        "sanctumsCleansed" to (currentCleansed + if (cleansed) 1L else 0L).coerceAtMost(10000L),
        "diyasLit" to (currentDiyas + diyasAdded.coerceAtLeast(0)).coerceAtMost(100000L),
        "puzzlesSolved" to (currentPuzzles + puzzlesAdded.coerceAtLeast(0)).coerceAtMost(100000L),
        "highestSurvivalSec" to maxOf(currentBestSurvival, survivalSeconds.toLong()).coerceIn(0L, 86400L),
        "unlockedCodexIds" to updatedCodex,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- MULTIPLAYER EXPEDITION ROOMS ---

  fun observeWaitingRooms(): Flow<List<ExpeditionRoom>> {
    val path = "expedition_rooms"
    return db.collection(path)
      .whereEqualTo("status", "waiting")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          ExpeditionRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeExpeditionRoom(roomCode: String): Flow<ExpeditionRoom?> {
    val cleanCode = roomCode.trim().uppercase()
    val path = "expedition_rooms/$cleanCode"
    return db.collection("expedition_rooms").document(cleanCode)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            ExpeditionRoom::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun createExpeditionRoom(
    roomCode: String,
    temple: TempleInfo,
    hostName: String
  ): Result<String> {
    val uid = requireUserId()
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("expedition_rooms").document(cleanCode)
    val safeHostName = hostName.trim().take(50).ifEmpty { "Sadhaka" }
    val payload = mapOf(
      "roomCode" to cleanCode,
      "templeId" to temple.id,
      "templeName" to temple.name,
      "entityName" to temple.entityName,
      "hostUid" to uid,
      "hostName" to safeHostName,
      "status" to "waiting",
      "diyasLitCount" to 0L,
      "sealsUnlocked" to 0L,
      "entityX" to 6.5,
      "entityY" to 5.5,
      "entityAlertLevel" to 15L,
      "lastRitualEvent" to "$safeHostName opened the sanctum gates",
      "memberUids" to listOf(uid),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      upsertPlayerPresence(
        roomCode = cleanCode,
        displayName = safeHostName,
        x = 1.5,
        y = 1.5,
        angle = 0.0,
        prana = 100,
        diyaLit = true,
        chantingMantra = false
      )
      Result.success(cleanCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinExpeditionRoom(roomCode: String, playerName: String): Result<ExpeditionRoom> {
    val uid = requireUserId()
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("expedition_rooms").document(cleanCode)
    val safeName = playerName.trim().take(50).ifEmpty { "Sadhaka" }
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        return Result.failure(IllegalArgumentException("Room $cleanCode not found"))
      }
      val currentMembers = (snap.get("memberUids") as? List<*>)
        ?.mapNotNull { it as? String }
        ?: emptyList()

      val updatedMembers = if (currentMembers.contains(uid)) {
        currentMembers
      } else {
        (currentMembers + uid).take(4)
      }

      if (snap.getString("status") == "waiting") {
        docRef.update(
          mapOf(
            "memberUids" to updatedMembers,
            "lastRitualEvent" to "$safeName joined the co-op ritual",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      upsertPlayerPresence(
        roomCode = cleanCode,
        displayName = safeName,
        x = 1.5,
        y = 1.5,
        angle = 0.0,
        prana = 100,
        diyaLit = true,
        chantingMantra = false
      )

      val updatedSnap = docRef.get().await()
      val room = updatedSnap.toObject(
        ExpeditionRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw IllegalStateException("Failed to parse joined room")
      Result.success(room)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun syncRitualProgress(
    roomCode: String,
    status: String,
    diyasLitCount: Int,
    sealsUnlocked: Int,
    entityX: Double,
    entityY: Double,
    entityAlertLevel: Int,
    eventMessage: String
  ): Result<Unit> {
    requireUserId()
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("expedition_rooms").document(cleanCode)
    return try {
      val updates = mapOf(
        "status" to status,
        "diyasLitCount" to diyasLitCount.coerceIn(0, 10).toLong(),
        "sealsUnlocked" to sealsUnlocked.coerceIn(0, 10).toLong(),
        "entityX" to entityX,
        "entityY" to entityY,
        "entityAlertLevel" to entityAlertLevel.coerceIn(0, 100).toLong(),
        "lastRitualEvent" to eventMessage.take(160),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- PLAYER PRESENCE SUBCOLLECTION ---

  fun observeRoomPlayers(roomCode: String): Flow<List<PlayerPresence>> {
    val cleanCode = roomCode.trim().uppercase()
    val path = "expedition_rooms/$cleanCode/players"
    return db.collection("expedition_rooms")
      .document(cleanCode)
      .collection("players")
      .whereEqualTo("roomCode", cleanCode)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          PlayerPresence::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun upsertPlayerPresence(
    roomCode: String,
    displayName: String,
    x: Double,
    y: Double,
    angle: Double,
    prana: Int,
    diyaLit: Boolean,
    chantingMantra: Boolean
  ): Result<Unit> {
    val uid = requireUserId()
    val cleanCode = roomCode.trim().uppercase()
    val docRef = db.collection("expedition_rooms")
      .document(cleanCode)
      .collection("players")
      .document(uid)
    val safeName = displayName.trim().take(50).ifEmpty { "Sadhaka" }
    val payload = mapOf(
      "userId" to uid,
      "roomCode" to cleanCode,
      "displayName" to safeName,
      "x" to x,
      "y" to y,
      "angle" to angle,
      "prana" to prana.coerceIn(0, 100).toLong(),
      "diyaLit" to diyaLit,
      "chantingMantra" to chantingMantra,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
