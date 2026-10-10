package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExpeditionRoom
import com.example.data.GarbhagrihaRepository
import com.example.data.PlayerPresence
import com.example.data.SacredShrineRepository
import com.example.data.SacredShrineSaveEntity
import com.example.data.SanataniLoreCatalog
import com.example.data.TempleInfo
import com.example.data.UserProgress
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class GarbhagrihaViewModel(
  private val repository: GarbhagrihaRepository,
  private val shrineRepository: SacredShrineRepository? = null,
  val currentUserId: String,
  val defaultDisplayName: String
) : ViewModel() {

  private val _selectedTemple = MutableStateFlow(SanataniLoreCatalog.temples.first())
  val selectedTemple: StateFlow<TempleInfo> = _selectedTemple.asStateFlow()

  private val _activeRoomCode = MutableStateFlow<String?>(null)
  val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  val allShrineSavesState: StateFlow<List<SacredShrineSaveEntity>> =
    (shrineRepository?.allShrineSaves ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
      )

  val selectedTempleShrineSave: StateFlow<SacredShrineSaveEntity?> = _selectedTemple
    .flatMapLatest { temple ->
      shrineRepository?.observeShrineSave(temple.id) ?: flowOf(null)
    }
    .catch { emit(null) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null
    )

  val userProgressState: StateFlow<UiState<UserProgress>> = repository
    .observeUserProgress(currentUserId)
    .map<UserProgress?, UiState<UserProgress>> { progress ->
      UiState.Success(
        progress ?: UserProgress(
          id = currentUserId,
          userId = currentUserId,
          displayName = defaultDisplayName
        )
      )
    }
    .catch { error ->
      Log.w(TAG, "Error observing user progress", error)
      emit(UiState.Error(error.message ?: "Failed to load Sadhaka progress"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val waitingRoomsState: StateFlow<UiState<List<ExpeditionRoom>>> = repository
    .observeWaitingRooms()
    .map<List<ExpeditionRoom>, UiState<List<ExpeditionRoom>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing waiting rooms", error)
      emit(UiState.Error(error.message ?: "Failed to load multiplayer rooms"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val activeRoomState: StateFlow<ExpeditionRoom?> = _activeRoomCode
    .flatMapLatest { code ->
      if (code.isNullOrBlank()) {
        flowOf(null)
      } else {
        repository.observeExpeditionRoom(code)
          .catch { emit(null) }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null
    )

  val activeRoomPlayers: StateFlow<List<PlayerPresence>> = _activeRoomCode
    .flatMapLatest { code ->
      if (code.isNullOrBlank()) {
        flowOf(emptyList())
      } else {
        repository.observeRoomPlayers(code)
          .catch { emit(emptyList()) }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  init {
    viewModelScope.launch {
      repository.ensureUserProgressInitialized(defaultDisplayName)
    }
  }

  fun selectTemple(temple: TempleInfo) {
    _selectedTemple.value = temple
  }

  fun clearBanner() {
    _statusBanner.value = null
  }

  fun saveProgressAtSacredShrine(
    temple: TempleInfo,
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
  ) {
    val repo = shrineRepository ?: return
    viewModelScope.launch {
      repo.saveAtSacredShrine(
        templeId = temple.id,
        templeName = temple.name,
        shrineTitle = shrineTitle,
        playerX = playerX,
        playerY = playerY,
        playerAngle = playerAngle,
        pranaPercent = pranaPercent,
        sanityPercent = sanityPercent,
        staminaPercent = staminaPercent,
        diyaOilPercent = diyaOilPercent,
        diyaLit = diyaLit,
        litDiyaCoords = litDiyaCoords,
        solvedSealCoords = solvedSealCoords,
        collectedArtifactIds = collectedArtifactIds,
        survivalSeconds = survivalSeconds
      )
      _statusBanner.value = "Saved at $shrineTitle (Sanity $sanityPercent%, Relics ${collectedArtifactIds.size})"
    }
  }

  fun clearShrineSaveForTemple(templeId: String) {
    val repo = shrineRepository ?: return
    viewModelScope.launch {
      repo.deleteShrineSave(templeId)
    }
  }

  fun hostMultiplayerExpedition(onRoomReady: (String, TempleInfo) -> Unit) {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val code = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    val temple = _selectedTemple.value
    viewModelScope.launch {
      val res = repository.createExpeditionRoom(
        roomCode = code,
        temple = temple,
        hostName = defaultDisplayName
      )
      res.onSuccess { createdCode ->
        _activeRoomCode.value = createdCode
        _statusBanner.value = "Room $createdCode created! Share code with friends."
        onRoomReady(createdCode, temple)
      }.onFailure { err ->
        _statusBanner.value = err.localizedMessage ?: "Failed to create room"
      }
    }
  }

  fun joinMultiplayerExpedition(
    roomCodeInput: String,
    onRoomReady: (String, TempleInfo) -> Unit
  ) {
    val cleanCode = roomCodeInput.trim().uppercase()
    if (cleanCode.length != 6) {
      _statusBanner.value = "Enter a valid 6-character Sanctum Room Code"
      return
    }
    viewModelScope.launch {
      val res = repository.joinExpeditionRoom(cleanCode, defaultDisplayName)
      res.onSuccess { room ->
        val matchedTemple = SanataniLoreCatalog.temples.find { it.id == room.templeId }
          ?: SanataniLoreCatalog.temples.first()
        _selectedTemple.value = matchedTemple
        _activeRoomCode.value = cleanCode
        _statusBanner.value = "Joined Room $cleanCode (${room.templeName})"
        onRoomReady(cleanCode, matchedTemple)
      }.onFailure { err ->
        _statusBanner.value = err.localizedMessage ?: "Could not join room $cleanCode"
      }
    }
  }

  fun leaveMultiplayerRoom() {
    _activeRoomCode.value = null
  }

  fun publishPlayerTelemetry(
    x: Double,
    y: Double,
    angle: Double,
    prana: Int,
    diyaLit: Boolean,
    chantingMantra: Boolean
  ) {
    val code = _activeRoomCode.value ?: return
    viewModelScope.launch {
      repository.upsertPlayerPresence(
        roomCode = code,
        displayName = defaultDisplayName,
        x = x,
        y = y,
        angle = angle,
        prana = prana,
        diyaLit = diyaLit,
        chantingMantra = chantingMantra
      )
    }
  }

  fun publishRitualEvent(
    status: String,
    diyasLitCount: Int,
    sealsUnlocked: Int,
    entityX: Double,
    entityY: Double,
    entityAlertLevel: Int,
    eventMessage: String
  ) {
    val code = _activeRoomCode.value ?: return
    viewModelScope.launch {
      repository.syncRitualProgress(
        roomCode = code,
        status = status,
        diyasLitCount = diyasLitCount,
        sealsUnlocked = sealsUnlocked,
        entityX = entityX,
        entityY = entityY,
        entityAlertLevel = entityAlertLevel,
        eventMessage = eventMessage
      )
    }
  }

  fun recordCompletedExpedition(
    cleansed: Boolean,
    diyasAdded: Int,
    puzzlesAdded: Int,
    survivalSeconds: Int,
    unlockedCodexId: String?
  ) {
    viewModelScope.launch {
      if (cleansed && unlockedCodexId != null) {
        shrineRepository?.deleteShrineSave(unlockedCodexId)
      }
      repository.recordExpeditionOutcome(
        cleansed = cleansed,
        diyasAdded = diyasAdded,
        puzzlesAdded = puzzlesAdded,
        survivalSeconds = survivalSeconds,
        unlockedId = unlockedCodexId
      )
    }
  }

  private companion object {
    const val TAG = "GarbhagrihaVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
