package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GarbhagrihaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProgress_andRecordOutcome_succeedsForOwner() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = GarbhagrihaRepository(firestore, auth)

    val initResult = withTimeout(TIMEOUT_MS) {
      repo.ensureUserProgressInitialized("Aarav Sharma")
    }
    assertTrue("Expected initResult success", initResult.isSuccess)

    val updateResult = withTimeout(TIMEOUT_MS) {
      repo.recordExpeditionOutcome(
        cleansed = true,
        diyasAdded = 5,
        puzzlesAdded = 3,
        survivalSeconds = 320,
        unlockedId = "surya_yantra"
      )
    }
    assertTrue("Expected recordExpeditionOutcome success", updateResult.isSuccess)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProgress(uid).first { it != null && it.sanctumsCleansed >= 1L }
    }
    assertNotNull(observed)
    assertTrue(observed!!.unlockedCodexIds.contains("surya_yantra"))
  }

  @Test
  fun createAndJoinExpeditionRoom_andObservePlayers_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = GarbhagrihaRepository(firestore, auth)
    val roomCode = UUID.randomUUID().toString().replace("-", "").uppercase().take(6)
    val temple = SanataniLoreCatalog.temples.first()

    val createRes = withTimeout(TIMEOUT_MS) {
      aliceRepo.createExpeditionRoom(roomCode, temple, "Aarav")
    }
    assertTrue("Expected createExpeditionRoom success: ${createRes.exceptionOrNull()}", createRes.isSuccess)

    // Switch to Bob and join room
    signInTestUser(BOB_EMAIL)
    val bobRepo = GarbhagrihaRepository(firestore, auth)
    val joinRes = withTimeout(TIMEOUT_MS) {
      bobRepo.joinExpeditionRoom(roomCode, "Devika")
    }
    assertTrue("Expected joinExpeditionRoom success: ${joinRes.exceptionOrNull()}", joinRes.isSuccess)
    assertEquals(2, joinRes.getOrThrow().memberUids.size)

    val players = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeRoomPlayers(roomCode).first { it.size >= 2 }
    }
    assertTrue(players.size >= 2)
  }

  @Test
  fun crossUserProgressAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = GarbhagrihaRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) {
      aliceRepo.ensureUserProgressInitialized("Aarav")
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun unauthenticatedQuery_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = GarbhagrihaRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeWaitingRooms().first()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
    } catch (e: Exception) {
      val fsEx = generateSequence<Throwable>(e) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      assertNotNull("Expected FirebaseFirestoreException in cause chain, got: $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_garbhagriha@test.com"
    const val BOB_EMAIL = "bob_garbhagriha@test.com"
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
