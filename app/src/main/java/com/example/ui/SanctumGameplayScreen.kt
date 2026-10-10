package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.AtmosphereState
import com.example.audio.SoundManager
import com.example.audio.SpatialCueType
import com.example.audio.SurfaceMaterial
import com.example.audio.TempleSoundEngine
import com.example.data.ExpeditionRoom
import com.example.data.PlayerPresence
import com.example.data.SacredShrineSaveEntity
import com.example.data.TempleInfo
import com.example.ui.theme.BloodAltarRed
import com.example.ui.theme.MantraJade
import com.example.ui.theme.MoltenBrass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.SacredCardSurface
import com.example.ui.theme.SacredTeal
import com.example.ui.theme.SaffronFlame
import com.example.ui.theme.SandstoneMuted
import com.example.ui.theme.SanctumGranite
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun SanctumGameplayScreen(
  temple: TempleInfo,
  roomCode: String?,
  activeRoom: ExpeditionRoom?,
  coOpPlayers: List<PlayerPresence>,
  currentUserId: String,
  soundEngine: TempleSoundEngine,
  initialShrineSave: SacredShrineSaveEntity? = null,
  existingShrineSave: SacredShrineSaveEntity? = initialShrineSave,
  soundManager: SoundManager = remember(soundEngine) { SoundManager(soundEngine) },
  postProcessManager: PostProcessEffectManager = remember { PostProcessEffectManager() },
  inventoryManager: ArtifactInventoryManager = remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    ArtifactInventoryManager().apply {
      initialShrineSave?.let { replaceCollectedArtifacts(it.parseCollectedArtifactIds()) }
    }
  },
  sanityManager: SanityManager = remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    SanityManager(initialSanity = (initialShrineSave?.sanityPercent ?: 100).toFloat())
  },
  encounterManager: EncounterManager = remember(temple.id) { EncounterManager() },
  staminaManager: StaminaManager = remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    StaminaManager(initialStamina = (initialShrineSave?.staminaPercent ?: 100).toFloat())
  },
  torchManager: TorchLightManager = remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    TorchLightManager(
      initialFuel = initialShrineSave?.diyaOilPercent ?: 100f,
      initiallyLit = initialShrineSave?.diyaLit ?: true
    )
  },
  environmentalManager: EnvironmentalEffectManager = remember(temple.id) { EnvironmentalEffectManager() },
  puzzleManager: PuzzleManager = remember(temple.id) { PuzzleManager() },
  dialogueManager: DialogueManager = remember(temple.id) { DialogueManager() },
  screenShakeController: ScreenShakeController = rememberScreenShakeController(temple.id),
  shadowCasterSystem: TorchShadowCasterSystem = remember(temple.id) { TorchShadowCasterSystem() },
  ritualManager: RitualInteractionManager = remember(temple.id) { RitualInteractionManager(temple.id) },
  fogOfWarManager: FogOfWarMiniMapManager = remember(temple.id) { FogOfWarMiniMapManager(temple.id) },
  voiceAndBossManager: VoiceMantraAndBossManager = remember(temple.id) { VoiceMantraAndBossManager() },
  godsFavorManager: GodsFavorManager = remember(temple.id) { GodsFavorManager(temple.id) },
  onSaveAtSacredShrine: (
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
    collectedArtifactIds: List<String>,
    survivalSeconds: Int
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
  onPublishTelemetry: (Double, Double, Double, Int, Boolean, Boolean) -> Unit,
  onPublishRitualEvent: (String, Int, Int, Double, Double, Int, String) -> Unit,
  onExpeditionFinished: (Boolean, Int, Int, Int, String?) -> Unit,
  onExitToHub: () -> Unit
) {
  BackHandler {
    soundManager.stopAtmosphere()
    onExitToHub()
  }

  DisposableEffect(soundManager) {
    soundManager.triggerAtmosphere(AtmosphereState.CALM_SANCTUM)
    onDispose {
      soundManager.stopAtmosphere()
    }
  }

  val mapGrid = ritualManager.applySecretChamberToMapGrid(temple.mapGrid)
  var playerX by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableDoubleStateOf(initialShrineSave?.playerX ?: 1.5)
  }
  var playerY by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableDoubleStateOf(initialShrineSave?.playerY ?: 1.5)
  }
  var playerAngle by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableDoubleStateOf(initialShrineSave?.playerAngle ?: 0.0)
  }
  var prana by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableIntStateOf(initialShrineSave?.pranaPercent ?: 100)
  }
  val sanity = sanityManager.sanityPercent
  val diyaOil = torchManager.fuelPercent
  val diyaLit = torchManager.isLit
  var chantingMantra by remember(temple.id) { mutableStateOf(false) }

  val entityX = encounterManager.entityX
  val entityY = encounterManager.entityY
  val entityAlertLevel = encounterManager.entityAlertLevel

  var litDiyaCoords by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableStateOf(initialShrineSave?.parseLitDiyaCoords() ?: emptySet())
  }
  var solvedSealCoords by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableStateOf(initialShrineSave?.parseSolvedSealCoords() ?: emptySet())
  }
  var activePuzzleCoord by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var showInventoryDialog by remember { mutableStateOf(false) }
  var showSacredShrineDialog by remember { mutableStateOf(false) }

  var survivalSeconds by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableIntStateOf(initialShrineSave?.survivalSeconds ?: 0)
  }
  var animTick by remember { mutableFloatStateOf(0f) }
  var eventToast by remember(initialShrineSave?.savedAtEpochMillis) {
    mutableStateOf(
      if (initialShrineSave != null) {
        "Restored Sacred Shrine [${initialShrineSave.shrineTitle}] • Sanity ${initialShrineSave.sanityPercent}%, Relics ${initialShrineSave.parseCollectedArtifactIds().size}"
      } else {
        "Explore the Garbhagriha. Light 5 Diyas, unlock 3 Vedic Seals, or commune at Sacred Shrines."
      }
    )
  }
  var outcomeState by remember { mutableStateOf<String?>(null) } // "escaped" or "consumed"
  var isMuted by remember { mutableStateOf(soundManager.isAudioMuted()) }
  var currentAtmosphere by remember { mutableStateOf(AtmosphereState.CALM_SANCTUM) }
  var lastCreatureAudioCue by remember(temple.id) { mutableStateOf<SpatialCueType?>(null) }
  var currentSurfaceMaterial by remember(temple.id, initialShrineSave?.savedAtEpochMillis) {
    mutableStateOf(
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = mapGrid,
        playerX = initialShrineSave?.playerX ?: 1.5,
        playerY = initialShrineSave?.playerY ?: 1.5,
        templeId = temple.id
      )
    )
  }

  // Sync shared co-op progress from activeRoom if present
  val effectiveDiyasLit = maxOf(litDiyaCoords.size, (activeRoom?.diyasLitCount ?: 0L).toInt())
  val effectiveSealsSolved = maxOf(solvedSealCoords.size, (activeRoom?.sealsUnlocked ?: 0L).toInt())

  // Initialize Fog-of-War around the player's starting position
  LaunchedEffect(temple.id, playerX, playerY, playerAngle, diyaLit) {
    fogOfWarManager.updateExploration(
      mapGrid = mapGrid,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      diyaLit = diyaLit,
      torchRadiusTiles = torchManager.currentLightRadiusTiles
    )
  }

  LaunchedEffect(activeRoom?.lastRitualEvent) {
    activeRoom?.lastRitualEvent?.let { msg ->
      if (msg.isNotBlank()) eventToast = msg
    }
    if (activeRoom?.status == "escaped" && outcomeState == null) {
      outcomeState = "escaped"
    }
  }

  // Main 10Hz Game Loop: AI stalking, Prana/Sanity/Diya oil physics, spatial audio cues, and atmosphere triggers
  LaunchedEffect(temple.id, outcomeState) {
    if (outcomeState != null) {
      soundManager.stopAtmosphere()
      return@LaunchedEffect
    }
    var tickCount = 0
    while (outcomeState == null) {
      delay(100L)
      tickCount++
      animTick += 0.1f

      val dx = playerX - entityX
      val dy = playerY - entityY
      val dist = hypot(dx, dy)

      // Determine whether player's first-person camera is facing the mythological entity
      val angleToEntity = atan2(entityY - playerY, entityX - playerX)
      val angleDiff = kotlin.math.abs(atan2(sin(angleToEntity - playerAngle), cos(angleToEntity - playerAngle)))
      val isLookingAtEntity = angleDiff < (PI / 4.2)

      // Update flickering torch/lantern fuel consumption & dynamic light radius at 10Hz
      val extinguishedNow = torchManager.onTick(
        animTick = animTick,
        isSprinting = staminaManager.isSprinting,
        chantingMantra = chantingMantra,
        entityDistance = dist
      )
      if (extinguishedNow) {
        eventToast = sanityManager.recordEvent(MythologicalSanityEvent.DIYA_FLAME_EXTINGUISHED)
      }

      // Update local environmental changes (falling ceiling dust & cobweb cling) at 10Hz
      environmentalManager.onTick(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        isSprinting = staminaManager.isSprinting && !staminaManager.isExhausted,
        animTick = animTick
      )

      // 1-second timer
      if (tickCount % 10 == 0) {
        survivalSeconds++

        // Evaluate God's Favor passive buff timers & environmental hazard immunities
        val favorPranaDelta = godsFavorManager.onSecondTick(
          diyaLit = torchManager.isLit,
          environmentalState = environmentalManager.effectState,
          entityDistance = dist
        )
        if (favorPranaDelta != 0) {
          prana = (prana + favorPranaDelta).coerceIn(1, 100)
        }
        if (godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN) && torchManager.isLit) {
          torchManager.refillFuel(0.45f, autoIgnite = false)
        }
        if (godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.SUFFOCATING_MASONRY_SPORES)) {
          staminaManager.restoreStamina(6f)
        }

        // Update SanityManager telemetry & apply Prana drain or Mantra recovery
        val effectiveDiyaLit = torchManager.isLit ||
          godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN)
        val pranaDrainFromSanity = sanityManager.onTelemetryTick(
          entityDistance = if (godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.SHADOW_AURA_CORRUPTION)) {
            maxOf(dist, 6.5)
          } else {
            dist
          },
          isLookingAtEntity = isLookingAtEntity &&
            !godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.CURSED_COBWEB_SNARE),
          diyaLit = effectiveDiyaLit,
          chantingMantra = chantingMantra,
          entityName = temple.entityName
        )

        if (chantingMantra) {
          prana = (prana + 3).coerceAtMost(100)
          if (tickCount % 30 == 0) chantingMantra = false
        } else if (pranaDrainFromSanity > 0 &&
          !godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN)
        ) {
          prana = (prana - pranaDrainFromSanity).coerceAtLeast(0)
        }
      }

      // Update dynamic background atmosphere trigger every second
      if (tickCount % 10 == 0) {
        currentAtmosphere = soundManager.updateAtmosphereFromTelemetry(
          entityDistance = dist,
          diyaLit = diyaLit,
          chantingMantra = chantingMantra
        )
      }

      // Creature AI & Procedural Spawn evaluation via EncounterManager
      val encounterOutcome = encounterManager.onTick(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        playerAngle = playerAngle,
        sanityPercent = sanity,
        diyaLit = diyaLit,
        chantingMantra = chantingMantra,
        entityName = temple.entityName
      )

      val updatedDist = encounterOutcome.distanceToPlayer
      val isChaseActive = encounterOutcome.behaviorState == CreatureBehaviorState.FRENZIED_CHASE ||
        encounterOutcome.behaviorState == CreatureBehaviorState.SANITY_AMBUSH ||
        updatedDist < 4.2

      // Update StaminaManager regeneration & exhaustion recovery at 10Hz
      staminaManager.onTick(
        sanityPercent = sanity,
        chantingMantra = chantingMantra,
        isChaseActive = isChaseActive
      )

      // Update ScreenShakeController continuous proximity/chase tremor & trauma decay at 10Hz
      screenShakeController.onTick(
        entityDistance = updatedDist,
        behaviorState = encounterOutcome.behaviorState,
        sanityPercent = sanity,
        chantingMantra = chantingMantra,
        animTick = animTick
      )

      if (encounterOutcome.triggeredProceduralSpawn) {
        sanityManager.recordEvent(MythologicalSanityEvent.CURSED_INSCRIPTION_WHISPER)
        screenShakeController.triggerShake(ScreenShakeTrigger.PROCEDURAL_AMBUSH)
        encounterOutcome.eventMessage?.let { eventToast = it }
      }

      val isFrenziedOrClose = updatedDist < 3.6 ||
        encounterOutcome.behaviorState == CreatureBehaviorState.FRENZIED_CHASE ||
        encounterOutcome.behaviorState == CreatureBehaviorState.SANITY_AMBUSH

      // Evaluate proximity zones via SoundManager and trigger distinct terrifying creature growls / shrieks / rasps
      val audioTriggerResult = soundManager.triggerEncounterProximityAudio(
        tickCount = tickCount,
        distanceTiles = updatedDist,
        listenerX = playerX,
        listenerY = playerY,
        listenerAngle = playerAngle,
        sourceX = encounterOutcome.entityX,
        sourceY = encounterOutcome.entityY,
        entityName = temple.entityName,
        templeId = temple.id,
        isAggressiveHunt = isFrenziedOrClose,
        isStunned = encounterOutcome.behaviorState == CreatureBehaviorState.STUNNED_BY_NADA,
        forceImmediateRoar = encounterOutcome.triggeredProceduralSpawn || encounterOutcome.enteredDangerZone
      )

      if (audioTriggerResult.triggeredCue != null) {
        lastCreatureAudioCue = audioTriggerResult.triggeredCue
        if (isFrenziedOrClose) {
          val proxScale = ((5.5 - updatedDist) / 4.5).toFloat().coerceIn(0.45f, 1.15f)
          screenShakeController.triggerShake(
            trigger = ScreenShakeTrigger.CHASE_FOOTSTEP_TREMOR,
            customIntensityMultiplier = proxScale
          )
        } else if (updatedDist < 6.2) {
          screenShakeController.triggerShake(
            trigger = ScreenShakeTrigger.PROXIMITY_RESONANCE,
            customIntensityMultiplier = 0.7f
          )
        }
        if (!encounterOutcome.triggeredProceduralSpawn) {
          audioTriggerResult.warningBanner?.let { eventToast = it }
        }
      }

      if (encounterOutcome.repelledByKavach) {
        chantingMantra = false
        soundManager.playTempleBell(639.0)
        screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
        sanityManager.recordEvent(MythologicalSanityEvent.GHANTA_BELL_RESONANCE)
        encounterOutcome.eventMessage?.let { eventToast = it }
      } else if (encounterOutcome.triggeredAttack) {
        val strikeDmg = (18 * godsFavorManager.entityStrikeDamageMultiplier).toInt().coerceAtLeast(6)
        prana = (prana - strikeDmg).coerceAtLeast(0)
        screenShakeController.triggerShake(ScreenShakeTrigger.JUMPSCARE_STRIKE)
        sanityManager.recordEvent(MythologicalSanityEvent.ENTITY_SHADOW_STRIKE)
        soundManager.playEntityStrikeAlert()
        encounterOutcome.eventMessage?.let { eventToast = it }
      }

      if (prana <= 0) {
        outcomeState = "consumed"
        soundManager.stopAtmosphere()
        soundManager.playEntityStrikeAlert()
        onPublishRitualEvent(
          "consumed",
          effectiveDiyasLit,
          effectiveSealsSolved,
          entityX,
          entityY,
          100,
          "A Sadhaka was consumed by ${temple.entityName}"
        )
        onExpeditionFinished(
          false,
          litDiyaCoords.size,
          solvedSealCoords.size,
          survivalSeconds,
          null
        )
      }

      // Sync multiplayer presence every 1.2s
      if (roomCode != null && tickCount % 12 == 0) {
        onPublishTelemetry(playerX, playerY, playerAngle, prana, diyaLit, chantingMantra)
      }
    }
  }

  // Check nearby interactive tile (within 1.5 tiles)
  val nearbyInteraction = remember(
    playerX,
    playerY,
    litDiyaCoords,
    solvedSealCoords,
    effectiveDiyasLit,
    effectiveSealsSolved,
    ritualManager.isSecretChamberUnlocked,
    ritualManager.isSecretSanctuaryClaimed
  ) {
    findNearbyInteractiveTile(mapGrid, playerX, playerY)
  }

  val templeDialogueEncounters = remember(temple.id) {
    dialogueManager.getEncountersForTemple(temple.id)
  }
  val nearbyDialogueEncounter = remember(temple.id, playerX, playerY) {
    dialogueManager.findNearbyEncounter(temple.id, playerX, playerY)
  }
  val nearbyMoralNode = remember(temple.id, playerX, playerY, godsFavorManager.resolvedNodeIds.size) {
    godsFavorManager.findNearbyMoralNode(playerX, playerY)
  }

  fun applyMoralOutcome(outcome: GodsFavorOutcome) {
    if (outcome.pranaDelta != 0) {
      prana = (prana + outcome.pranaDelta).coerceIn(1, 100)
    }
    if (outcome.sanityDelta > 0) {
      sanityManager.restoreSanity(outcome.sanityDelta, outcome.message)
    } else if (outcome.sanityDelta < 0) {
      sanityManager.recordEvent(MythologicalSanityEvent.CURSED_INSCRIPTION_WHISPER)
    }
    if (outcome.fuelDelta > 0f) {
      torchManager.refillFuel(outcome.fuelDelta, autoIgnite = true)
    }
    eventToast = outcome.message
  }

  fun applyRitualOutcome(outcome: RitualStepOutcome, fallbackMessage: String? = null) {
    if (outcome.pranaDelta != 0) {
      prana = (prana + outcome.pranaDelta).coerceIn(1, 100)
    }
    if (outcome.sanityDelta > 0) {
      sanityManager.restoreSanity(outcome.sanityDelta, outcome.message)
    }
    if (outcome.fuelDelta > 0f) {
      torchManager.refillFuel(outcome.fuelDelta, autoIgnite = true)
    }
    if (outcome.stunTicks > 0) {
      encounterManager.stunEntity(ticks = outcome.stunTicks)
    }
    if (outcome.didUnlockSecretChamber) {
      soundEngine.playStoneChakraGrind()
      soundEngine.playTempleBell(528.0)
      screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
      val door = ritualManager.activeRitual.secretDoorTileCoord
      fogOfWarManager.revealAreaAround(mapGrid, door.first, door.second, radiusTiles = 2)
      val relic = inventoryManager.collectNextUndiscoveredArtifact()
      eventToast = if (relic != null) {
        "${outcome.message} • Unearthed ${relic.name}!"
      } else {
        outcome.message
      }
    } else if (outcome.isStepCorrect) {
      eventToast = outcome.message
    } else if (fallbackMessage != null) {
      eventToast = "$fallbackMessage • ${outcome.message}"
    } else {
      eventToast = outcome.message
    }
  }

  fun movePlayer(forwardSign: Double) {
    val isChase = encounterManager.behaviorState == CreatureBehaviorState.FRENZIED_CHASE ||
      encounterManager.behaviorState == CreatureBehaviorState.SANITY_AMBUSH
    val wasExhausted = staminaManager.isExhausted
    val speedMult = staminaManager.consumeStepStamina(isChaseActive = isChase)
    if (!wasExhausted && staminaManager.isExhausted) {
      eventToast = "Breath exhausted from sprinting! Recover Stamina to 30% to sprint again!"
    }
    val step = 0.34 * forwardSign * speedMult
    val nx = playerX + cos(playerAngle) * step
    val ny = playerY + sin(playerAngle) * step
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12

    var moved = false
    if (nx.toInt() in 0 until cols && playerY.toInt() in 0 until rows) {
      val tileX = mapGrid[playerY.toInt()][nx.toInt()]
      if (tileX != '1') {
        playerX = nx
        moved = true
      }
    }
    if (playerX.toInt() in 0 until cols && ny.toInt() in 0 until rows) {
      val tileY = mapGrid[ny.toInt()][playerX.toInt()]
      if (tileY != '1') {
        playerY = ny
        moved = true
      }
    }

    if (moved) {
      val previousSurface = currentSurfaceMaterial
      val isPlayerSprinting = staminaManager.isSprinting && !staminaManager.isExhausted
      val detectedSurface = soundManager.playPlayerFootstepForPosition(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        templeId = temple.id,
        isSprinting = isPlayerSprinting
      )
      currentSurfaceMaterial = detectedSurface
      val envNotice = environmentalManager.onPlayerMoveStep(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        stepMagnitude = step,
        isSprinting = isPlayerSprinting
      )
      if (detectedSurface != previousSurface) {
        eventToast = "Footsteps [${detectedSurface.environmentTag}]: ${detectedSurface.displayName} (${detectedSurface.sanskritName})"
      } else if (envNotice != null) {
        if (environmentalManager.effectState == EnvironmentalEffectState.FALLING_MASONRY_DUST &&
          godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.SUFFOCATING_MASONRY_SPORES)
        ) {
          eventToast = "🛡 Vayu Prana Purity active: Immune to choking masonry dust & ceiling spores!"
        } else if (environmentalManager.effectState == EnvironmentalEffectState.COBWEB_BRUSH &&
          godsFavorManager.hasHazardImmunity(SanctumEnvironmentalHazard.CURSED_COBWEB_SNARE)
        ) {
          eventToast = "🛡 Saraswati Smriti Ward active: Immune to cursed cobweb snares!"
        } else {
          eventToast = envNotice
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid)
  ) {
    // 1. 3D Raycast First-Person Sanctum Viewport
    FirstPersonSanctumViewport(
      mapGrid = mapGrid,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      diyaLit = diyaLit,
      torchLightRadiusTiles = torchManager.currentLightRadiusTiles,
      torchLightRadiusNormalized = torchManager.lightRadiusNormalized,
      torchFlickerMultiplier = torchManager.flickerMultiplier,
      chantingMantra = chantingMantra,
      prana = minOf(prana, sanity),
      entityX = entityX,
      entityY = entityY,
      entityAlertLevel = entityAlertLevel,
      litDiyaCoords = litDiyaCoords,
      solvedSealCoords = solvedSealCoords,
      coOpPlayers = coOpPlayers,
      currentUserId = currentUserId,
      animTick = animTick,
      onLookDelta = { delta ->
        playerAngle = (playerAngle + delta) % (2.0 * PI)
      },
      effectManager = postProcessManager,
      environmentalManager = environmentalManager,
      screenShakeController = screenShakeController,
      shadowCasterSystem = shadowCasterSystem,
      dialogueEncounters = templeDialogueEncounters,
      ritualManager = ritualManager,
      fogOfWarManager = fogOfWarManager
    )

    // 2. First-Person Tactical HUD (Health, Sanity Meter, Stamina, Diya Fuel, Compass & Active Objectives)
    FirstPersonSanctumHud(
      temple = temple,
      roomCode = roomCode,
      coOpPlayerCount = coOpPlayers.size.coerceAtLeast(1),
      healthPercent = prana,
      sanityPercent = sanity,
      staminaPercent = staminaManager.staminaPercent,
      isSprinting = staminaManager.isSprinting,
      isExhausted = staminaManager.isExhausted,
      mentalState = sanityManager.mentalState,
      sanityWhisper = sanityManager.activeWhisper,
      creatureBehaviorState = encounterManager.behaviorState,
      diyaOilPercent = diyaOil,
      diyaLit = diyaLit,
      torchFlameState = torchManager.flameState,
      torchLightRadiusTiles = torchManager.currentLightRadiusTiles,
      chantingMantra = chantingMantra,
      diyasLitCount = effectiveDiyasLit,
      sealsSolvedCount = effectiveSealsSolved,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      entityX = entityX,
      entityY = entityY,
      currentAtmosphere = currentAtmosphere,
      currentSurfaceMaterial = currentSurfaceMaterial,
      environmentalEffectState = environmentalManager.effectState,
      shakeIntensityState = screenShakeController.intensityState,
      shakePercent = screenShakeController.shakePercent,
      isShakeEnabled = screenShakeController.isHapticsAndShakeEnabled,
      torchShadowState = shadowCasterSystem.shadowState,
      activeShadowCasters = shadowCasterSystem.activeCasterCount,
      ritualProgressState = ritualManager.progressState,
      ritualStepsCompleted = ritualManager.currentStepIndex,
      ritualTotalSteps = ritualManager.activeRitual.steps.size,
      secretChamberName = ritualManager.activeRitual.secretChamberName,
      creatureProximityZone = encounterManager.activeProximityZone,
      lastCreatureAudioCue = lastCreatureAudioCue,
      mapExplorationPercent = fogOfWarManager.explorationPercent,
      languageMode = voiceAndBossManager.languageMode,
      isBossBanished = voiceAndBossManager.isBossBanished,
      godsFavorScore = godsFavorManager.favorScore,
      godsFavorStanding = godsFavorManager.standing,
      activeFavorBuff = godsFavorManager.primaryActiveBuff,
      isMuted = isMuted,
      onOpenExpandedMap = {
        fogOfWarManager.openExpandedMap()
      },
      onOpenGodsFavor = {
        soundEngine.playTempleBell(528.0)
        godsFavorManager.openOverlay(nearbyMoralNode)
      },
      onToggleLanguage = {
        soundEngine.playTempleBell(528.0)
        eventToast = voiceAndBossManager.toggleLanguageMode()
      },
      onToggleShake = {
        eventToast = screenShakeController.toggleEnabled()
      },
      onCyclePostProcess = {
        val nextPreset = postProcessManager.cyclePreset()
        eventToast = "Post-Process FX: ${nextPreset.label} (Grain ${(nextPreset.baseGrainDensity * 100).toInt()}%)"
      },
      onToggleMute = {
        isMuted = !isMuted
        soundManager.setMuted(isMuted)
        if (!isMuted) {
          soundManager.triggerAtmosphere(currentAtmosphere)
        }
      },
      onExitSanctum = {
        soundManager.stopAtmosphere()
        onExitToHub()
      },
      modifier = Modifier
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(start = 136.dp, end = 12.dp, top = 10.dp)
    )

    // 3. Center-Bottom Event Banner + Interactive Altar Prompt + First-Person Controls
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Context-sensitive interaction button when standing near a Diya sconce ('2'), Vedic Seal ('3'), or Exit Gate ('4')
      nearbyInteraction?.let { (coord, tileChar) ->
        when (tileChar) {
          '2' -> {
            val alreadyLit = litDiyaCoords.contains(coord)
            Button(
              onClick = {
                soundEngine.playDiyaIgnite()
                torchManager.refillFuel(100f, autoIgnite = true)
                val ritualOutcome = ritualManager.recordAction(
                  actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
                  coord = coord
                )
                if (!alreadyLit) {
                  litDiyaCoords = litDiyaCoords + coord
                  sanityManager.recordEvent(MythologicalSanityEvent.DIYA_SCONCE_IGNITED)
                  val newCount = maxOf(litDiyaCoords.size, effectiveDiyasLit + 1)
                  val unearthed = inventoryManager.collectNextUndiscoveredArtifact()
                  val baseMsg = if (unearthed != null) {
                    "Diya #$newCount lit! Unearthed ${unearthed.name} — tap Relics to inspect!"
                  } else {
                    "Sacred Diya #$newCount ignited! Shadow darkness recedes."
                  }
                  applyRitualOutcome(ritualOutcome, fallbackMessage = baseMsg)
                  onPublishRitualEvent(
                    "active",
                    newCount,
                    effectiveSealsSolved,
                    entityX,
                    entityY,
                    entityAlertLevel,
                    eventToast
                  )
                } else {
                  applyRitualOutcome(
                    ritualOutcome,
                    fallbackMessage = "Brass Diya oil refilled from sacred temple sconce!"
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MoltenBrass,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("interact_diya_button")
            ) {
              Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (alreadyLit) "REFILL DIYA GHEE AT SCONCE" else "IGNITE SACRED DIYA & SEARCH RELIC",
                fontWeight = FontWeight.Bold
              )
            }
          }

          '3' -> {
            val alreadySolved = solvedSealCoords.contains(coord)
            if (!alreadySolved) {
              Button(
                onClick = {
                  activePuzzleCoord = coord
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SacredTeal,
                  contentColor = Color.Black
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .padding(bottom = 8.dp)
                  .testTag("interact_seal_button")
              ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "DECIPHER ANCIENT VEDIC SEAL",
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          '4' -> {
            val canEscape = effectiveDiyasLit >= temple.requiredDiyas || effectiveSealsSolved >= temple.requiredSeals
            Button(
              onClick = {
                if (canEscape && voiceAndBossManager.isBossBanished) {
                  soundEngine.playMantraChantPulse()
                  outcomeState = "escaped"
                  onPublishRitualEvent(
                    "escaped",
                    effectiveDiyasLit,
                    effectiveSealsSolved,
                    entityX,
                    entityY,
                    0,
                    "Swarga Dwara unsealed! Sanctum cleansed!"
                  )
                  onExpeditionFinished(
                    true,
                    maxOf(1, litDiyaCoords.size),
                    maxOf(1, solvedSealCoords.size),
                    survivalSeconds,
                    temple.id
                  )
                } else if (canEscape) {
                  soundEngine.playTempleBell(528.0)
                  voiceAndBossManager.openMahaAartiBossEncounter()
                } else {
                  soundEngine.playStoneChakraGrind()
                  eventToast = "Swarga Dwara is sealed! Light ${temple.requiredDiyas} Diyas or unlock ${temple.requiredSeals} Vedic Seals first!"
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (canEscape) MantraJade else SaffronFlame,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("interact_exit_gate_button")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (canEscape && voiceAndBossManager.isBossBanished) {
                  "ENTER SWARGA DWARA (CLEANSE & ESCAPE)"
                } else if (canEscape) {
                  "BEGIN MAHA-AARTI FINAL EXORCISM AT SWARGA DWARA"
                } else {
                  "SWARGA DWARA LOCKED (COMPLETE RITUALS)"
                },
                fontWeight = FontWeight.Bold
              )
            }
          }

          '5', '6' -> {
            Button(
              onClick = {
                soundEngine.playMantraChantPulse()
                if (!ritualManager.isSecretSanctuaryClaimed) {
                  val claimOutcome = ritualManager.claimSecretSanctuaryBlessing()
                  applyRitualOutcome(claimOutcome)
                  val relic = inventoryManager.collectNextUndiscoveredArtifact()
                  if (relic != null) {
                    eventToast = "${claimOutcome.message} • Found ${relic.name}!"
                  }
                } else {
                  ritualManager.openOverlay()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MantraJade,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("interact_secret_chamber_button")
            ) {
              Icon(Icons.Default.LockOpen, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (!ritualManager.isSecretSanctuaryClaimed) {
                  "CLAIM SECRET CHAMBER RELIC (${ritualManager.activeRitual.secretChamberName.uppercase()})"
                } else {
                  "INSPECT UNSEALED SECRET CHAMBER"
                },
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Context-sensitive interaction button when standing near an NPC Spirit or Ancient Stone Inscription
      nearbyDialogueEncounter?.let { enc ->
        val isSpirit = enc.speakerType == DialogueSpeakerType.NPC_SPIRIT
        Button(
          onClick = {
            if (isSpirit) {
              soundEngine.playMantraChantPulse()
            } else {
              soundEngine.playStoneChakraGrind()
            }
            dialogueManager.startDialogue(enc)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isSpirit) SacredTeal else MoltenBrass,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("interact_dialogue_button")
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${enc.speakerType.actionVerb}: ${enc.speakerName.uppercase()}",
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Context-sensitive moral choice prompt when near a Sacred Reliquary / Votive Vessel
      nearbyMoralNode?.let { moralNode ->
        Button(
          onClick = {
            soundEngine.playTempleBell(528.0)
            godsFavorManager.openOverlay(moralNode)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MantraJade,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("interact_moral_shrine_button")
        ) {
          Icon(Icons.Default.SelfImprovement, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "MORAL CHOICE: ${moralNode.name.uppercase()} (PRAY OR PLUNDER)",
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Atmospheric status banner
      Surface(
        color = SanctumGranite.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp)
      ) {
        Text(
          text = eventToast,
          style = MaterialTheme.typography.bodyMedium,
          color = ParchmentCream,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }

      // First-Person Movement & Sacred Ritual Action Deck
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Navigation D-Pad (Turn Left, Forward, Back, Turn Right)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ControlPadButton(
            icon = Icons.AutoMirrored.Filled.RotateLeft,
            contentDescription = "Turn Left",
            tag = "turn_left_button",
            onClick = { playerAngle -= PI / 6.0 }
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ControlPadButton(
              icon = Icons.Default.ArrowUpward,
              contentDescription = "Step Forward",
              tag = "move_forward_button",
              onClick = { movePlayer(1.0) }
            )
            ControlPadButton(
              icon = Icons.Default.ArrowDownward,
              contentDescription = "Step Backward",
              tag = "move_backward_button",
              onClick = { movePlayer(-0.8) }
            )
          }
          ControlPadButton(
            icon = Icons.AutoMirrored.Filled.RotateRight,
            contentDescription = "Turn Right",
            tag = "turn_right_button",
            onClick = { playerAngle += PI / 6.0 }
          )
          RitualActionButton(
            icon = Icons.Default.DirectionsRun,
            label = when {
              staminaManager.isExhausted -> "Winded"
              staminaManager.isSprinting -> "Sprint ON"
              else -> "Sprint"
            },
            tint = when {
              staminaManager.isExhausted -> BloodAltarRed
              staminaManager.isSprinting -> SaffronFlame
              else -> MoltenBrass
            },
            tag = "toggle_sprint_button",
            onClick = {
              eventToast = staminaManager.toggleSprint()
            }
          )
        }

        // Right Ritual Actions: Ring Ghanta Bell, Chant Mantra, Open Puzzle, Inspect Relics Satchel
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RitualActionButton(
            icon = Icons.Default.LocalFireDepartment,
            label = if (diyaLit) "Torch ON" else "Torch OFF",
            tint = if (diyaLit) SaffronFlame else SandstoneMuted,
            tag = "toggle_torch_button",
            onClick = {
              soundEngine.playDiyaIgnite()
              eventToast = torchManager.toggleFlame()
            }
          )

          RitualActionButton(
            icon = Icons.Default.NotificationsActive,
            label = "Ghanta",
            tint = MoltenBrass,
            tag = "ring_ghanta_button",
            onClick = {
              soundEngine.playTempleBell(528.0)
              encounterManager.stunEntity(ticks = 30) // Stuns creature for 3 seconds
              screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
              sanityManager.recordEvent(MythologicalSanityEvent.GHANTA_BELL_RESONANCE)
              val expected = ritualManager.nextRequiredStep
              if (expected?.actionType == RitualActionType.RING_SACRED_GHANTA) {
                val outcome = ritualManager.recordAction(RitualActionType.RING_SACRED_GHANTA)
                applyRitualOutcome(outcome)
              } else {
                eventToast = "Saptadhatu Ghanta rung! ${temple.entityName} is stunned by 528Hz resonance!"
              }
            }
          )

          RitualActionButton(
            icon = Icons.Default.SelfImprovement,
            label = "Mantra",
            tint = MantraJade,
            tag = "chant_mantra_button",
            onClick = {
              soundEngine.playMantraChantPulse()
              chantingMantra = true
              torchManager.refillFuel(25f, autoIgnite = true)
              prana = (prana + 12).coerceAtMost(100)
              staminaManager.restoreStamina(35f)
              sanityManager.recordEvent(MythologicalSanityEvent.MANTRA_KAVACH_CHANTED)
              val expected = ritualManager.nextRequiredStep
              if (expected?.actionType == RitualActionType.CHANT_VEDIC_MANTRA) {
                val outcome = ritualManager.recordAction(RitualActionType.CHANT_VEDIC_MANTRA)
                applyRitualOutcome(outcome)
              } else {
                eventToast = "ॐ त्र्यम्बकं यजामहे • Maha Mrityunjaya Kavach restored Sanity, Stamina & Prana!"
              }
            }
          )

          RitualActionButton(
            icon = Icons.Default.LockOpen,
            label = if (ritualManager.isSecretChamberUnlocked) "Chamber" else "Ritual (${ritualManager.currentStepIndex}/${ritualManager.activeRitual.steps.size})",
            tint = if (ritualManager.isSecretChamberUnlocked) MantraJade else SaffronFlame,
            tag = "open_ritual_sequence_button",
            onClick = {
              soundEngine.playStoneChakraGrind()
              ritualManager.openOverlay()
            }
          )

          RitualActionButton(
            icon = Icons.Default.LockOpen,
            label = "Yantra",
            tint = SacredTeal,
            tag = "open_yantra_puzzle_button",
            onClick = {
              activePuzzleCoord = (playerX.toInt() to playerY.toInt())
            }
          )

          RitualActionButton(
            icon = Icons.Default.AutoAwesome,
            label = "Relics (${inventoryManager.collectedArtifactIds.size})",
            tint = SaffronFlame,
            tag = "open_inventory_button",
            onClick = {
              showInventoryDialog = true
            }
          )

          RitualActionButton(
            icon = Icons.Default.BookmarkAdded,
            label = "Shrine",
            tint = MoltenBrass,
            tag = "open_sacred_shrine_button",
            onClick = {
              soundEngine.playTempleBell(528.0)
              showSacredShrineDialog = true
            }
          )

          RitualActionButton(
            icon = Icons.Default.SelfImprovement,
            label = "Favor (${godsFavorManager.favorScore}%)",
            tint = when (godsFavorManager.standing) {
              GodsFavorStanding.EXALTED_ANUGRAHA -> MantraJade
              GodsFavorStanding.REVERENT_DEVOTEE -> MoltenBrass
              GodsFavorStanding.NEUTRAL_SEEKER -> SacredTeal
              GodsFavorStanding.TRANSGRESSOR_DOSHA -> BloodAltarRed
            },
            tag = "open_gods_favor_button",
            onClick = {
              soundEngine.playTempleBell(528.0)
              godsFavorManager.openOverlay(nearbyMoralNode)
            }
          )

          RitualActionButton(
            icon = Icons.Default.RecordVoiceOver,
            label = "Spirits",
            tint = SacredTeal,
            tag = "open_dialogue_overlay_button",
            onClick = {
              soundEngine.playMantraChantPulse()
              val opened = dialogueManager.openNearbyOrNextEncounter(
                templeId = temple.id,
                playerX = playerX,
                playerY = playerY
              )
              if (opened != null) {
                eventToast = "${opened.speakerType.badgeLabel}: ${opened.speakerName}"
              }
            }
          )

          RitualActionButton(
            icon = Icons.Default.Mic,
            label = "Voice",
            tint = MantraJade,
            tag = "open_voice_mantra_button",
            onClick = {
              soundEngine.playMantraChantPulse()
              voiceAndBossManager.openVoiceMantraOverlay()
            }
          )

          RitualActionButton(
            icon = Icons.Default.Shield,
            label = if (voiceAndBossManager.isBossBanished) "Purified" else "Exorcise",
            tint = if (voiceAndBossManager.isBossBanished) MantraJade else BloodAltarRed,
            tag = "open_boss_exorcism_button",
            onClick = {
              soundEngine.playTempleBell(528.0)
              voiceAndBossManager.openMahaAartiBossEncounter()
            }
          )

          RitualActionButton(
            icon = Icons.Default.Map,
            label = "Map (${fogOfWarManager.explorationPercent}%)",
            tint = MoltenBrass,
            tag = "open_fog_of_war_map_button",
            onClick = {
              fogOfWarManager.openExpandedMap()
            }
          )
        }
      }
    }

    // 4. Ancient Vedic Seal Puzzle Modal
    activePuzzleCoord?.let { coord ->
      VedicSealPuzzleDialog(
        sealIndex = solvedSealCoords.size,
        soundEngine = soundEngine,
        puzzleManager = puzzleManager,
        onSolved = {
          solvedSealCoords = solvedSealCoords + coord
          activePuzzleCoord = null
          sanityManager.recordEvent(MythologicalSanityEvent.VEDIC_SEAL_DECIPHERED)
          val newSeals = maxOf(solvedSealCoords.size, effectiveSealsSolved + 1)
          val unearthed = inventoryManager.collectNextUndiscoveredArtifact()
          val baseMsg = if (unearthed != null) {
            "Vedic Seal #$newSeals unlocked & unearthed ${unearthed.name}!"
          } else {
            "Vedic Seal #$newSeals unlocked! (${newSeals}/${temple.requiredSeals})"
          }
          val expected = ritualManager.nextRequiredStep
          if (expected?.actionType == RitualActionType.TRACE_YANTRA_SEAL) {
            val ritualOutcome = ritualManager.recordAction(
              actionType = RitualActionType.TRACE_YANTRA_SEAL,
              coord = coord,
              allowWildcardCoord = true
            )
            applyRitualOutcome(ritualOutcome, fallbackMessage = baseMsg)
          } else {
            eventToast = baseMsg
          }
          onPublishRitualEvent(
            "active",
            effectiveDiyasLit,
            newSeals,
            entityX,
            entityY,
            entityAlertLevel,
            eventToast
          )
        },
        onDismiss = {
          activePuzzleCoord = null
        }
      )
    }

    // 4b. Sacred Artifact Inventory & Relic Inspection Modal
    if (showInventoryDialog) {
      ArtifactInventoryDialog(
        inventoryManager = inventoryManager,
        soundManager = soundManager,
        onInvokeArtifact = { artifact ->
          prana = (prana + artifact.pranaBoost).coerceAtMost(100)
          sanityManager.restoreSanity(artifact.pranaBoost, "Invoked ${artifact.name}: ${artifact.ritualEffectSummary}")
          if (artifact.oilBoost > 0f) {
            torchManager.refillFuel(artifact.oilBoost, autoIgnite = true)
          }
          if (artifact.stunSeconds > 0) {
            encounterManager.stunEntity(ticks = artifact.stunSeconds * 10)
          }
          chantingMantra = true
          eventToast = "Invoked ${artifact.name}: ${artifact.ritualEffectSummary}"
          showInventoryDialog = false
        },
        onSearchNearbyReliquary = {
          val found = inventoryManager.collectNextUndiscoveredArtifact()
          if (found != null) {
            eventToast = "Unearthed ${found.name} (${found.sanskritTitle}) from temple alcove!"
          }
        },
        onDismiss = {
          showInventoryDialog = false
        }
      )
    }

    // 4c. Sacred Shrine Sanctuary Save & Restore Modal (Room Database)
    if (showSacredShrineDialog) {
      val shrineTitle = nearbyInteraction?.let { (coord, tileChar) ->
        when (tileChar) {
          '2' -> "Agni Deepa Shrine (${coord.first},${coord.second})"
          '3' -> "Surya Yantra Altar (${coord.first},${coord.second})"
          else -> "Swarga Dwara Threshold (${coord.first},${coord.second})"
        }
      } ?: "Mandapa Pillar Shrine (${playerX.toInt()},${playerY.toInt()})"

      SacredShrineDialog(
        temple = temple,
        shrineTitle = shrineTitle,
        currentPrana = prana,
        currentSanity = sanity,
        currentStamina = staminaManager.staminaPercent,
        currentDiyasLit = effectiveDiyasLit,
        currentSealsSolved = effectiveSealsSolved,
        collectedArtifactCount = inventoryManager.collectedArtifactIds.size,
        existingSave = existingShrineSave,
        onConsecrateAndSave = {
          soundEngine.playMantraChantPulse()
          val favorPrayerOutcome = godsFavorManager.performMoralAction(MoralActionType.PRAY_AT_SHRINE)
          sanityManager.restoreSanity(15, "Sacred Shrine consecrated! Progress, Relics & Sanity saved.")
          staminaManager.restoreStamina(40f)
          torchManager.refillFuel(30f, autoIgnite = true)
          val updatedSanity = sanityManager.sanityPercent
          onSaveAtSacredShrine(
            shrineTitle,
            playerX,
            playerY,
            playerAngle,
            prana,
            updatedSanity,
            staminaManager.staminaPercent,
            torchManager.fuelPercent,
            torchManager.isLit,
            litDiyaCoords,
            solvedSealCoords,
            inventoryManager.collectedArtifactIds.toList(),
            survivalSeconds
          )
          eventToast = "Progress, ${inventoryManager.collectedArtifactIds.size} Relics & Sanity ($updatedSanity%) saved at $shrineTitle!"
          showSacredShrineDialog = false
        },
        onRestoreFromShrine = { saved ->
          soundEngine.playTempleBell(528.0)
          playerX = saved.playerX
          playerY = saved.playerY
          playerAngle = saved.playerAngle
          prana = saved.pranaPercent
          sanityManager.reset(saved.sanityPercent.toFloat())
          staminaManager.reset(saved.staminaPercent.toFloat())
          torchManager.restoreState(saved.diyaOilPercent, saved.diyaLit)
          litDiyaCoords = saved.parseLitDiyaCoords()
          solvedSealCoords = saved.parseSolvedSealCoords()
          inventoryManager.replaceCollectedArtifacts(saved.parseCollectedArtifactIds())
          survivalSeconds = saved.survivalSeconds
          eventToast = "Restored checkpoint from ${saved.shrineTitle} (Sanity ${saved.sanityPercent}%)!"
          showSacredShrineDialog = false
        },
        onDismiss = {
          showSacredShrineDialog = false
        }
      )
    }

    // 4d. NPC Spirit & Ancient Inscription Narrative Dialogue Overlay
    if (dialogueManager.isDialogueOpen) {
      TempleDialogueOverlay(
        dialogueManager = dialogueManager,
        onChoiceSelected = { effect ->
          soundEngine.playMantraChantPulse()
          if (effect.sanityDelta > 0) {
            sanityManager.restoreSanity(effect.sanityDelta, effect.summaryMessage)
          } else if (effect.sanityDelta < 0) {
            sanityManager.recordEvent(MythologicalSanityEvent.CURSED_INSCRIPTION_WHISPER)
          }
          if (effect.pranaDelta != 0) {
            prana = (prana + effect.pranaDelta).coerceIn(1, 100)
          }
          if (effect.fuelDelta > 0f) {
            torchManager.refillFuel(effect.fuelDelta, autoIgnite = true)
          }
          if (effect.grantsRelicChance) {
            val unearthed = inventoryManager.collectNextUndiscoveredArtifact()
            if (unearthed != null) {
              eventToast = "${effect.summaryMessage} • Unearthed ${unearthed.name}!"
            } else {
              eventToast = effect.summaryMessage
            }
          } else {
            eventToast = effect.summaryMessage
          }
        },
        onCycleNextChronicle = {
          val allForTemple = dialogueManager.getEncountersForTemple(temple.id)
          if (allForTemple.isNotEmpty()) {
            val currentId = dialogueManager.activeEncounter?.id
            val currentIdx = allForTemple.indexOfFirst { it.id == currentId }
            val nextEnc = allForTemple[(currentIdx + 1).coerceAtLeast(0) % allForTemple.size]
            soundEngine.playStoneChakraGrind()
            dialogueManager.startDialogue(nextEnc)
          }
        },
        onDismiss = {
          dialogueManager.dismissDialogue()
        }
      )
    }

    // 4e. Sacred Ritual Sequence & Secret Temple Chamber Overlay
    if (ritualManager.isOverlayOpen) {
      RitualInteractionOverlay(
        ritualManager = ritualManager,
        playerX = playerX,
        playerY = playerY,
        onStepPerformed = { step, outcome ->
          when (step.actionType) {
            RitualActionType.IGNITE_DIRECTIONAL_LAMP -> {
              soundEngine.playDiyaIgnite()
              step.targetCoord?.let { coord ->
                litDiyaCoords = litDiyaCoords + coord
              }
            }
            RitualActionType.RING_SACRED_GHANTA -> {
              soundEngine.playTempleBell(528.0)
              screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
            }
            RitualActionType.CHANT_VEDIC_MANTRA -> {
              soundEngine.playMantraChantPulse()
              chantingMantra = true
            }
            RitualActionType.TRACE_YANTRA_SEAL -> {
              soundEngine.playStoneChakraGrind()
              step.targetCoord?.let { coord ->
                solvedSealCoords = solvedSealCoords + coord
              }
            }
          }
          applyRitualOutcome(outcome)
        },
        onClaimSanctuaryBlessing = {
          soundEngine.playTempleBell(528.0)
          val claimOutcome = ritualManager.claimSecretSanctuaryBlessing()
          applyRitualOutcome(claimOutcome)
          val relic = inventoryManager.collectNextUndiscoveredArtifact()
          if (relic != null) {
            eventToast = "${claimOutcome.message} • Found ${relic.name}!"
          }
        },
        onTeleportToSecretChamber = {
          soundEngine.playStoneChakraGrind()
          val door = ritualManager.activeRitual.secretDoorTileCoord
          playerX = door.first + 0.5
          playerY = door.second + 0.5
          ritualManager.closeOverlay()
          eventToast = "Stepped through the unsealed doorway into ${ritualManager.activeRitual.secretChamberName}!"
        },
        onDismiss = {
          ritualManager.closeOverlay()
        }
      )
    }

    // 4f. Expanded Vastu Mandala Fog-of-War Cartography Overlay
    if (fogOfWarManager.isExpandedMapOpen) {
      ExpandedFogOfWarMapDialog(
        templeName = temple.name,
        mapGrid = mapGrid,
        fogOfWarManager = fogOfWarManager,
        playerX = playerX,
        playerY = playerY,
        playerAngle = playerAngle,
        diyaLit = diyaLit,
        entityX = entityX,
        entityY = entityY,
        litDiyaCoords = litDiyaCoords,
        solvedSealCoords = solvedSealCoords,
        dialogueEncounters = templeDialogueEncounters,
        shadowCasterSystem = shadowCasterSystem,
        ritualManager = ritualManager,
        coOpPlayers = coOpPlayers,
        currentUserId = currentUserId,
        onDismiss = {
          fogOfWarManager.closeExpandedMap()
        }
      )
    }

    // 4g. Voice Mantra Recognition Overlay
    if (voiceAndBossManager.isVoiceOverlayOpen) {
      VoiceMantraOverlayDialog(
        manager = voiceAndBossManager,
        entityName = temple.entityName,
        onMantraRecognized = { mantra, msg ->
          soundEngine.playMantraChantPulse()
          soundEngine.playTempleBell(528.0)
          chantingMantra = true
          prana = (prana + mantra.pranaBoost).coerceAtMost(100)
          sanityManager.restoreSanity(mantra.sanityBoost, msg)
          torchManager.refillFuel(mantra.fuelBoost, autoIgnite = true)
          encounterManager.stunEntity(ticks = mantra.stunTicks)
          screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
          val expected = ritualManager.nextRequiredStep
          if (expected?.actionType == RitualActionType.CHANT_VEDIC_MANTRA) {
            val outcome = ritualManager.recordAction(RitualActionType.CHANT_VEDIC_MANTRA)
            applyRitualOutcome(outcome, fallbackMessage = msg)
          } else {
            eventToast = msg
          }
          voiceAndBossManager.closeVoiceMantraOverlay()
        },
        onDismiss = {
          voiceAndBossManager.closeVoiceMantraOverlay()
        }
      )
    }

    // 4h. Multi-Stage Maha-Aarti Final Exorcism Boss Fight Overlay
    if (voiceAndBossManager.isBossFightOverlayOpen) {
      MahaAartiBossExorcismDialog(
        manager = voiceAndBossManager,
        templeName = temple.name,
        entityName = temple.entityName,
        onStageCompleted = { phase, msg ->
          when (phase) {
            MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA -> {
              soundEngine.playDiyaIgnite()
              torchManager.refillFuel(35f, autoIgnite = true)
              screenShakeController.triggerShake(ScreenShakeTrigger.PROXIMITY_RESONANCE)
            }
            MahaAartiPhase.STAGE_3_PURNA_AHUTI_MANTRA -> {
              soundEngine.playTempleBell(528.0)
              encounterManager.stunEntity(ticks = 45)
              screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
            }
            MahaAartiPhase.BANISHED_VICTORY -> {
              soundEngine.playMantraChantPulse()
              soundEngine.playTempleBell(639.0)
              chantingMantra = true
              prana = 100
              sanityManager.restoreSanity(40, msg)
              encounterManager.stunEntity(ticks = 120)
              screenShakeController.triggerShake(ScreenShakeTrigger.GHANTA_SONIC_SHOCKWAVE)
            }
            else -> {}
          }
          eventToast = msg
        },
        onVictoryEscape = {
          voiceAndBossManager.closeMahaAartiBossEncounter()
          outcomeState = "escaped"
          onPublishRitualEvent(
            "escaped",
            effectiveDiyasLit,
            effectiveSealsSolved,
            entityX,
            entityY,
            0,
            "Maha-Aarti completed! ${temple.entityName} banished & Swarga Dwara unsealed!"
          )
          onExpeditionFinished(
            true,
            maxOf(1, litDiyaCoords.size),
            maxOf(1, solvedSealCoords.size),
            survivalSeconds,
            temple.id
          )
        },
        onDismiss = {
          voiceAndBossManager.closeMahaAartiBossEncounter()
        }
      )
    }

    // 4i. God's Favor (Deva Anugraha) Moral Choice & Hazard Immunity Overlay
    if (godsFavorManager.isOverlayOpen) {
      GodsFavorSanctuaryDialog(
        favorManager = godsFavorManager,
        isHindi = voiceAndBossManager.languageMode == SanctumLanguageMode.HINDI_DEVANAGARI,
        onPerformAction = { actionType, nodeId ->
          if (actionType.isRespectful) {
            soundEngine.playMantraChantPulse()
            soundEngine.playTempleBell(528.0)
          } else {
            soundEngine.playStoneChakraGrind()
            screenShakeController.triggerShake(ScreenShakeTrigger.PROXIMITY_RESONANCE)
          }
          val outcome = godsFavorManager.performMoralAction(actionType, nodeId)
          applyMoralOutcome(outcome)
        },
        onDismiss = {
          godsFavorManager.closeOverlay()
        }
      )
    }

    // 5. Expedition Outcome Overlay (Escaped or Consumed)
    outcomeState?.let { status ->
      val escaped = (status == "escaped")
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.88f))
          .padding(24.dp)
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = SanctumGranite),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 2.dp,
              color = if (escaped) MantraJade else BloodAltarRed,
              shape = RoundedCornerShape(24.dp)
            )
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (escaped) "GARBHAGRIHA CLEANSED" else "CONSUMED BY SHADOWS",
              style = MaterialTheme.typography.headlineMedium,
              color = if (escaped) MantraJade else BloodAltarRed,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (escaped) {
                "You aligned the Vedic Yantras, kept the sacred Diyas burning, and banished ${temple.entityName} from ${temple.name}!"
              } else {
                "Your Diya flickered out and ${temple.entityName} overwhelmed your Prana inside ${temple.name}."
              },
              style = MaterialTheme.typography.bodyLarge,
              color = ParchmentCream,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Survival Time: ${survivalSeconds}s • Diyas Lit: $effectiveDiyasLit • Seals Unlocked: $effectiveSealsSolved",
              style = MaterialTheme.typography.labelLarge,
              color = MoltenBrass
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = onExitToHub,
              colors = ButtonDefaults.buttonColors(
                containerColor = SaffronFlame,
                contentColor = Color.Black
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("return_to_sanctum_hub_button")
            ) {
              Text("RETURN TO TEMPLE HUB", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun findNearbyInteractiveTile(
  mapGrid: List<String>,
  px: Double,
  py: Double
): Pair<Pair<Int, Int>, Char>? {
  val rows = mapGrid.size
  val cols = mapGrid.firstOrNull()?.length ?: 12
  var best: Pair<Pair<Int, Int>, Char>? = null
  var bestDist = 1.65
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      val ch = mapGrid[r][c]
      if (ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6') {
        val d = hypot((c + 0.5) - px, (r + 0.5) - py)
        if (d < bestDist) {
          bestDist = d
          best = (c to r) to ch
        }
      }
    }
  }
  return best
}

@Composable
private fun ControlPadButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  tag: String,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = SacredCardSurface.copy(alpha = 0.92f),
      contentColor = MoltenBrass
    ),
    contentPadding = PaddingValues(0.dp),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .size(50.dp)
      .border(1.dp, MoltenBrass.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
      .testTag(tag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier = Modifier.size(24.dp)
    )
  }
}

@Composable
private fun RitualActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  tag: String,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = SanctumGranite.copy(alpha = 0.94f),
      contentColor = tint
    ),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    shape = CircleShape,
    modifier = Modifier
      .height(54.dp)
      .border(1.5.dp, tint, CircleShape)
      .testTag(tag)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge
      )
    }
  }
}
