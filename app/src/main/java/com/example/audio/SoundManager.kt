package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Background atmosphere mood states triggered dynamically by temple exploration,
 * Diya lamp state, and entity chase proximity.
 */
enum class AtmosphereState(val label: String, val droneFreqHz: Double, val tensionDepth: Double) {
  CALM_SANCTUM("Sanctum Drone (108Hz)", 108.0, 0.15),
  SHADOW_WHISPERS("Dark Corridor Tension", 81.0, 0.45),
  ENTITY_CHASE("Brahmarakshasa Pursuit", 54.0, 0.85),
  VEDIC_KAVACH("Sacred Mantra Aura", 136.1, 0.10)
}

/**
 * Spatial audio cue types positioned in 2D/3D temple space relative to the player's
 * coordinates and facing angle.
 */
enum class SpatialCueType(
  val displayLabel: String,
  val sanskritLabel: String
) {
  ENTITY_FOOTSTEP(
    displayLabel = "Heavy Stone Stalking Step",
    sanskritLabel = "पदचाप (Padachapa)"
  ),
  BRAHMARAKSHASA_GROWL(
    displayLabel = "Brahmarakshasa Sub-Bass Growl",
    sanskritLabel = "ब्रह्मराक्षस गर्जन"
  ),
  CHHAYA_PRET_RASP(
    displayLabel = "Chhaya Pret Hollow Breath Rasp",
    sanskritLabel = "छाया प्रेत श्वास"
  ),
  PISHACHA_GUTTURAL_SNARL(
    displayLabel = "Pishacha Bone-Grinding Snarl",
    sanskritLabel = "पिशाच घर्घर"
  ),
  VETALA_SPECTRAL_SHRIEK(
    displayLabel = "Vetala Dissonant Blood-Shriek",
    sanskritLabel = "वेताल चीत्कार"
  ),
  CREATURE_STALKING_WHISPER(
    displayLabel = "Distant Crypt Mantra Distortion",
    sanskritLabel = "तमस फुसफुसाहट"
  ),
  DISTANT_GHANTA_ECHO(
    displayLabel = "Distant Saptadhatu Ghanta Echo",
    sanskritLabel = "घण्टा प्रतिध्वनि"
  ),
  DIYA_FLICKER(
    displayLabel = "Brass Diya Ghee Crackle",
    sanskritLabel = "दीप स्फुरण"
  ),
  SEAL_RESONANCE(
    displayLabel = "Vedic Yantra Seal Harmonic",
    sanskritLabel = "यन्त्र नाद"
  )
}

/**
 * Distance-tiered proximity zones evaluated when `EncounterManager` detects a mythological
 * creature within range of the player.
 */
enum class CreatureProximityZone(
  val maxDistanceTiles: Double,
  val hudBadge: String,
  val defaultCadenceTicks: Int
) {
  OUT_OF_RANGE(
    maxDistanceTiles = Double.MAX_VALUE,
    hudBadge = "SILENT",
    defaultCadenceTicks = Int.MAX_VALUE
  ),
  DISTANT_OMEN(
    maxDistanceTiles = 9.0,
    hudBadge = "OMEN WHISPER",
    defaultCadenceTicks = 22
  ),
  STALKING_GROWL(
    maxDistanceTiles = 6.2,
    hudBadge = "STALKING GROWL",
    defaultCadenceTicks = 14
  ),
  IMMINENT_TERROR_ROAR(
    maxDistanceTiles = 3.4,
    hudBadge = "TERROR ROAR",
    defaultCadenceTicks = 8
  )
}

/**
 * Telemetry snapshot returned when [SoundManager.triggerEncounterProximityAudio] evaluates
 * the creature's distance and triggers a spatial growl or terrifying vocalization.
 */
data class EncounterAudioTriggerResult(
  val proximityZone: CreatureProximityZone,
  val triggeredCue: SpatialCueType?,
  val leftEarGain: Float,
  val rightEarGain: Float,
  val warningBanner: String? = null
)

/**
 * Physical floor surface materials inside the temple sanctum, resolved from environment tags
 * and tile coordinates when the player moves.
 */
enum class SurfaceMaterial(
  val id: String,
  val displayName: String,
  val environmentTag: String,
  val sanskritName: String,
  val acousticDescription: String
) {
  STONE(
    id = "stone",
    displayName = "Carved Granite Slab",
    environmentTag = "ENV_SURFACE_STONE",
    sanskritName = "शिला (Shila)",
    acousticDescription = "Crisp granite heel echo across pillared Mandapa corridors"
  ),
  DIRT(
    id = "dirt",
    displayName = "Subterranean Earth & Ash",
    environmentTag = "ENV_SURFACE_DIRT",
    sanskritName = "मृत्तिका (Mrittika)",
    acousticDescription = "Muffled earthen crunch and sacred Vibhuti ash underfoot"
  ),
  METAL(
    id = "metal",
    displayName = "Ashtadhatu Ritual Grate",
    environmentTag = "ENV_SURFACE_METAL",
    sanskritName = "अष्टधातु (Ashtadhatu)",
    acousticDescription = "Resonant bronze and iron yantra plate vibrations"
  )
}

/**
 * SoundManager handles both:
 * 1. True Stereo 3D/2D Spatial Audio Cues (calculating distance attenuation and left/right ear
 *    azimuth panning from `(listenerX, listenerY, listenerAngle)` to `(sourceX, sourceY)`).
 * 2. Continuous & Triggered Background Atmosphere States (`AtmosphereState`) that shift
 *    between calm Vedic sanctum resonance, dark corridor whispers, and intense chase drones.
 */
class SoundManager(
  private val soundEngine: TempleSoundEngine = TempleSoundEngine()
) {
  private val sampleRate = 22050
  private val isMuted = AtomicBoolean(false)
  private val atmosphereActive = AtomicBoolean(false)
  private val currentAtmosphere = AtomicReference(AtmosphereState.CALM_SANCTUM)
  private val currentSurfaceMaterial = AtomicReference(SurfaceMaterial.STONE)
  private val currentEnvironmentTag = AtomicReference(SurfaceMaterial.STONE.environmentTag)
  private val activeProximityZone = AtomicReference(CreatureProximityZone.OUT_OF_RANGE)
  private val lastTriggeredCreatureCue = AtomicReference<SpatialCueType?>(null)
  private var lastCreatureCueTick: Int = -100
  private var atmosphereThread: Thread? = null

  fun setMuted(muted: Boolean) {
    isMuted.set(muted)
    soundEngine.setMuted(muted)
    if (muted) {
      stopAtmosphere()
    }
  }

  fun isAudioMuted(): Boolean = isMuted.get()

  fun getCurrentAtmosphere(): AtmosphereState = currentAtmosphere.get()

  fun getCurrentSurfaceMaterial(): SurfaceMaterial = currentSurfaceMaterial.get()

  fun getCurrentEnvironmentTag(): String = currentEnvironmentTag.get()

  fun getActiveProximityZone(): CreatureProximityZone = activeProximityZone.get()

  fun getLastTriggeredCreatureCue(): SpatialCueType? = lastTriggeredCreatureCue.get()

  /**
   * Starts or transitions the background temple atmosphere trigger loop.
   */
  fun triggerAtmosphere(state: AtmosphereState) {
    currentAtmosphere.set(state)
    if (isMuted.get()) return
    if (atmosphereActive.compareAndSet(false, true)) {
      atmosphereThread = thread(start = true, isDaemon = true, name = "SanctumAtmosphereThread") {
        runAtmosphereLoop()
      }
    }
  }

  /**
   * Evaluates game telemetry (entity distance, Diya state, mantra shield) and automatically
   * triggers the matching background atmosphere state.
   */
  fun updateAtmosphereFromTelemetry(
    entityDistance: Double,
    diyaLit: Boolean,
    chantingMantra: Boolean
  ): AtmosphereState {
    val nextState = when {
      chantingMantra -> AtmosphereState.VEDIC_KAVACH
      entityDistance < 3.8 -> AtmosphereState.ENTITY_CHASE
      !diyaLit || entityDistance < 6.2 -> AtmosphereState.SHADOW_WHISPERS
      else -> AtmosphereState.CALM_SANCTUM
    }
    triggerAtmosphere(nextState)
    return nextState
  }

  /**
   * Stops the continuous background atmosphere loop cleanly when exiting the sanctum.
   */
  fun stopAtmosphere() {
    atmosphereActive.set(false)
    atmosphereThread?.interrupt()
    atmosphereThread = null
  }

  /**
   * Computes stereo left/right gains (`Pair<Float, Float>`) and distance attenuation for a sound
   * source at `(sourceX, sourceY)` relative to a listener at `(listenerX, listenerY, listenerAngle)`.
   */
  fun computeStereoSpatialGains(
    listenerX: Double,
    listenerY: Double,
    listenerAngle: Double,
    sourceX: Double,
    sourceY: Double,
    maxHearingDistance: Double = 12.0
  ): Pair<Float, Float> {
    val dx = sourceX - listenerX
    val dy = sourceY - listenerY
    val dist = hypot(dx, dy)
    if (dist >= maxHearingDistance) return 0f to 0f

    val attenuation = (1.0 - (dist / maxHearingDistance)).coerceIn(0.05, 1.0)
    val worldAngle = atan2(dy, dx)
    var relAngle = worldAngle - listenerAngle
    while (relAngle > PI) relAngle -= 2.0 * PI
    while (relAngle < -PI) relAngle += 2.0 * PI

    // sin(relAngle): -1.0 is hard left, 0.0 is dead center, +1.0 is hard right
    val pan = sin(relAngle).coerceIn(-1.0, 1.0)
    val leftGain = (attenuation * ((1.0 - pan) * 0.5 + 0.25)).coerceIn(0.0, 1.0).toFloat()
    val rightGain = (attenuation * ((1.0 + pan) * 0.5 + 0.25)).coerceIn(0.0, 1.0).toFloat()
    return leftGain to rightGain
  }

  /**
   * Plays a 3D positioned spatial audio cue in stereo (interleaved L/R PCM) based on the
   * source coordinates relative to the player's position and facing angle inside the temple.
   */
  fun playSpatialCue(
    cueType: SpatialCueType,
    listenerX: Double,
    listenerY: Double,
    listenerAngle: Double,
    sourceX: Double,
    sourceY: Double
  ) {
    if (isMuted.get()) return
    val (leftGain, rightGain) = computeStereoSpatialGains(
      listenerX = listenerX,
      listenerY = listenerY,
      listenerAngle = listenerAngle,
      sourceX = sourceX,
      sourceY = sourceY
    )
    if (leftGain <= 0.01f && rightGain <= 0.01f) return

    thread(start = true, isDaemon = true) {
      val monoSamples = synthesizeCueMono(cueType)
      val stereoInterleaved = ShortArray(monoSamples.size * 2)
      for (i in monoSamples.indices) {
        val raw = monoSamples[i].toInt()
        stereoInterleaved[i * 2] = (raw * leftGain).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        stereoInterleaved[i * 2 + 1] = (raw * rightGain).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playStereoBuffer(stereoInterleaved)
    }
  }

  // Delegate direct non-spatial ritual cues to TempleSoundEngine
  fun playTempleBell(baseFreqHz: Double = 528.0) = soundEngine.playTempleBell(baseFreqHz)
  fun playMantraChantPulse(durationMs: Int = 1100) = soundEngine.playMantraChantPulse(durationMs)
  fun playStoneChakraGrind() = soundEngine.playStoneChakraGrind()
  fun playDiyaIgnite() = soundEngine.playDiyaIgnite()
  fun playProximityHeartbeat(intensity: Float) = soundEngine.playProximityHeartbeat(intensity)
  fun playEntityStrikeAlert() = soundEngine.playEntityStrikeAlert()

  /**
   * Resolves the distance-based [CreatureProximityZone] from the creature's distance in tiles.
   */
  fun resolveCreatureProximityZone(distanceTiles: Double): CreatureProximityZone {
    return when {
      distanceTiles <= CreatureProximityZone.IMMINENT_TERROR_ROAR.maxDistanceTiles ->
        CreatureProximityZone.IMMINENT_TERROR_ROAR
      distanceTiles <= CreatureProximityZone.STALKING_GROWL.maxDistanceTiles ->
        CreatureProximityZone.STALKING_GROWL
      distanceTiles <= CreatureProximityZone.DISTANT_OMEN.maxDistanceTiles ->
        CreatureProximityZone.DISTANT_OMEN
      else ->
        CreatureProximityZone.OUT_OF_RANGE
    }
  }

  /**
   * Selects a distinct, terrifying mythological creature vocalization or growl tailored to
   * the temple's guardian entity (`entityName` / `templeId`), its proximity zone, and whether
   * it is actively hunting or ambushing the player.
   */
  fun selectCreatureVocalizationCue(
    entityName: String,
    templeId: String = "",
    proximityZone: CreatureProximityZone,
    isAggressiveHunt: Boolean
  ): SpatialCueType {
    val upperName = entityName.uppercase()
    val upperTemple = templeId.uppercase()

    return when (proximityZone) {
      CreatureProximityZone.IMMINENT_TERROR_ROAR -> {
        when {
          upperName.contains("VETALA") || upperName.contains("TANTRA") || upperTemple.contains("BHANGARH") ->
            SpatialCueType.VETALA_SPECTRAL_SHRIEK
          upperName.contains("PISHACHA") || upperName.contains("CHHAYA") || upperTemple.contains("KONARK") ->
            SpatialCueType.PISHACHA_GUTTURAL_SNARL
          else ->
            SpatialCueType.BRAHMARAKSHASA_GROWL
        }
      }

      CreatureProximityZone.STALKING_GROWL -> {
        if (isAggressiveHunt) {
          when {
            upperName.contains("CHHAYA") || upperName.contains("PRET") || upperTemple.contains("KALLARA") ->
              SpatialCueType.CHHAYA_PRET_RASP
            upperName.contains("PISHACHA") || upperTemple.contains("BHANGARH") ->
              SpatialCueType.PISHACHA_GUTTURAL_SNARL
            else ->
              SpatialCueType.BRAHMARAKSHASA_GROWL
          }
        } else {
          when {
            upperName.contains("CHHAYA") || upperName.contains("PRET") ->
              SpatialCueType.CHHAYA_PRET_RASP
            else ->
              SpatialCueType.ENTITY_FOOTSTEP
          }
        }
      }

      CreatureProximityZone.DISTANT_OMEN -> {
        if (isAggressiveHunt) {
          SpatialCueType.CHHAYA_PRET_RASP
        } else {
          SpatialCueType.CREATURE_STALKING_WHISPER
        }
      }

      CreatureProximityZone.OUT_OF_RANGE -> SpatialCueType.ENTITY_FOOTSTEP
    }
  }

  /**
   * Evaluates the creature's distance and behavior detected by `EncounterManager` and plays
   * distinct, stereo-panned terrifying growls, spectral shrieks, hollow rasps, and sub-bass
   * heartbeats when the creature enters or stalks within `CreatureProximityZone` thresholds.
   *
   * Also triggers an immediate vocalization whenever the creature crosses inward into a closer,
   * more dangerous proximity tier (e.g., from `STALKING_GROWL` into `IMMINENT_TERROR_ROAR`).
   */
  fun triggerEncounterProximityAudio(
    tickCount: Int,
    distanceTiles: Double,
    listenerX: Double,
    listenerY: Double,
    listenerAngle: Double,
    sourceX: Double,
    sourceY: Double,
    entityName: String,
    templeId: String = "",
    isAggressiveHunt: Boolean = false,
    isStunned: Boolean = false,
    forceImmediateRoar: Boolean = false
  ): EncounterAudioTriggerResult {
    val previousZone = activeProximityZone.get()
    val currentZone = if (isStunned) {
      CreatureProximityZone.OUT_OF_RANGE
    } else {
      resolveCreatureProximityZone(distanceTiles)
    }
    activeProximityZone.set(currentZone)

    if (currentZone == CreatureProximityZone.OUT_OF_RANGE) {
      return EncounterAudioTriggerResult(
        proximityZone = currentZone,
        triggeredCue = null,
        leftEarGain = 0f,
        rightEarGain = 0f
      )
    }

    val escalatedZone = currentZone.maxDistanceTiles < previousZone.maxDistanceTiles
    val ticksSinceLast = tickCount - lastCreatureCueTick
    val cadence = if (isAggressiveHunt) {
      (currentZone.defaultCadenceTicks * 0.75).toInt().coerceAtLeast(6)
    } else {
      currentZone.defaultCadenceTicks
    }

    val shouldFireVocalization = forceImmediateRoar || escalatedZone || ticksSinceLast >= cadence

    // Trigger spatial heartbeat pulse when within 4.8 tiles
    if (!isStunned && distanceTiles < 4.8 && tickCount % 9 == 0) {
      val hbIntensity = ((4.8 - distanceTiles) / 4.8).toFloat().coerceIn(0.22f, 1.0f)
      playProximityHeartbeat(hbIntensity)
    }

    val (leftGain, rightGain) = computeStereoSpatialGains(
      listenerX = listenerX,
      listenerY = listenerY,
      listenerAngle = listenerAngle,
      sourceX = sourceX,
      sourceY = sourceY
    )

    if (!shouldFireVocalization) {
      return EncounterAudioTriggerResult(
        proximityZone = currentZone,
        triggeredCue = null,
        leftEarGain = leftGain,
        rightEarGain = rightGain
      )
    }

    lastCreatureCueTick = tickCount
    val selectedCue = selectCreatureVocalizationCue(
      entityName = entityName,
      templeId = templeId,
      proximityZone = currentZone,
      isAggressiveHunt = isAggressiveHunt || escalatedZone
    )
    lastTriggeredCreatureCue.set(selectedCue)

    playSpatialCue(
      cueType = selectedCue,
      listenerX = listenerX,
      listenerY = listenerY,
      listenerAngle = listenerAngle,
      sourceX = sourceX,
      sourceY = sourceY
    )

    val directionHint = when {
      leftGain > rightGain + 0.14f -> "from the LEFT corridor"
      rightGain > leftGain + 0.14f -> "from the RIGHT corridor"
      else -> "dead ahead in the shadows"
    }

    val banner = if (escalatedZone || currentZone == CreatureProximityZone.IMMINENT_TERROR_ROAR || forceImmediateRoar) {
      "🔊 ${selectedCue.displayLabel} (${selectedCue.sanskritLabel}) echoes $directionHint (${"%.1f".format(distanceTiles)}m)!"
    } else {
      null
    }

    return EncounterAudioTriggerResult(
      proximityZone = currentZone,
      triggeredCue = selectedCue,
      leftEarGain = leftGain,
      rightEarGain = rightGain,
      warningBanner = banner
    )
  }

  /**
   * Inspects the temple map grid tile and spatial environment tags at `(playerX, playerY)`
   * to determine the active floor surface material (`STONE`, `DIRT`, or `METAL`).
   *
   * Environment tag mapping rules:
   * - Explicit override tag containing `"METAL"`, `"GRATE"`, `"BRONZE"`, or `"SEAL"` -> [SurfaceMaterial.METAL]
   * - Explicit override tag containing `"DIRT"`, `"EARTH"`, `"ASH"`, or `"CRYPT"` -> [SurfaceMaterial.DIRT]
   * - Explicit override tag containing `"STONE"`, `"GRANITE"`, or `"SLAB"` -> [SurfaceMaterial.STONE]
   * - Map tile `'3'` (Vedic Seal / Surya Yantra plate), `'4'` (Swarga Dwara bronze threshold),
   *   or adjacent to a mechanical seal plate -> [SurfaceMaterial.METAL]
   * - Map tile `'2'` (Sacred Diya earthen/ash alcove) or subterranean crypt chambers
   *   (`(tileX + tileY) % 3 == 0` in outer/crypt corridors) -> [SurfaceMaterial.DIRT]
   * - Standard carved temple pillared corridors (`'0'`) -> [SurfaceMaterial.STONE]
   */
  fun resolveSurfaceMaterialFromEnvironment(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    templeId: String = "",
    environmentTagOverride: String? = null
  ): SurfaceMaterial {
    if (!environmentTagOverride.isNullOrBlank()) {
      val upper = environmentTagOverride.uppercase()
      when {
        upper.contains("METAL") || upper.contains("GRATE") || upper.contains("BRONZE") || upper.contains("SEAL") -> {
          currentSurfaceMaterial.set(SurfaceMaterial.METAL)
          currentEnvironmentTag.set(SurfaceMaterial.METAL.environmentTag)
          return SurfaceMaterial.METAL
        }
        upper.contains("DIRT") || upper.contains("EARTH") || upper.contains("ASH") || upper.contains("CRYPT") -> {
          currentSurfaceMaterial.set(SurfaceMaterial.DIRT)
          currentEnvironmentTag.set(SurfaceMaterial.DIRT.environmentTag)
          return SurfaceMaterial.DIRT
        }
        upper.contains("STONE") || upper.contains("GRANITE") || upper.contains("SLAB") -> {
          currentSurfaceMaterial.set(SurfaceMaterial.STONE)
          currentEnvironmentTag.set(SurfaceMaterial.STONE.environmentTag)
          return SurfaceMaterial.STONE
        }
      }
    }

    val row = playerY.toInt()
    val col = playerX.toInt()
    val tileChar = if (row in mapGrid.indices && col in (0 until (mapGrid.getOrNull(row)?.length ?: 0))) {
      mapGrid[row][col]
    } else {
      '0'
    }

    // Check immediate tile or adjacent ritual grates/alcoves
    val resolved = when {
      tileChar == '3' || tileChar == '4' -> SurfaceMaterial.METAL
      tileChar == '2' -> SurfaceMaterial.DIRT
      hasAdjacentTileChar(mapGrid, col, row, '3') || hasAdjacentTileChar(mapGrid, col, row, '4') -> {
        SurfaceMaterial.METAL
      }
      templeId == "kailasa" && (row + col) % 2 == 1 -> SurfaceMaterial.DIRT
      (row + col) % 4 == 0 -> SurfaceMaterial.DIRT
      else -> SurfaceMaterial.STONE
    }

    currentSurfaceMaterial.set(resolved)
    currentEnvironmentTag.set(resolved.environmentTag)
    return resolved
  }

  private fun hasAdjacentTileChar(mapGrid: List<String>, col: Int, row: Int, target: Char): Boolean {
    val offsets = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    for ((dx, dy) in offsets) {
      val r = row + dy
      val c = col + dx
      if (r in mapGrid.indices && c in 0 until mapGrid[r].length) {
        if (mapGrid[r][c] == target) return true
      }
    }
    return false
  }

  /**
   * Checks the environment tags at the player's new coordinates and plays a dynamic
   * surface-appropriate footstep sound (Stone, Dirt, or Metal).
   */
  fun playPlayerFootstepForPosition(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    templeId: String = "",
    isSprinting: Boolean = false,
    environmentTagOverride: String? = null
  ): SurfaceMaterial {
    val surface = resolveSurfaceMaterialFromEnvironment(
      mapGrid = mapGrid,
      playerX = playerX,
      playerY = playerY,
      templeId = templeId,
      environmentTagOverride = environmentTagOverride
    )
    soundEngine.playSurfaceFootstep(surface = surface, isSprinting = isSprinting)
    return surface
  }

  private fun synthesizeCueMono(cueType: SpatialCueType): ShortArray {
    return when (cueType) {
      SpatialCueType.ENTITY_FOOTSTEP -> {
        val durationMs = 220
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        var filteredNoise = 0.0
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = exp(-14.0 * t)
          val noise = Random.nextDouble() * 2.0 - 1.0
          filteredNoise = 0.85 * filteredNoise + 0.15 * noise
          val thud = 0.7 * sin(2.0 * PI * 62.0 * t) + 0.3 * filteredNoise
          buf[i] = (thud * env * Short.MAX_VALUE * 0.85).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.BRAHMARAKSHASA_GROWL -> {
        // Deep sub-bass demonic chest growl with vocal-tract formant modulation & tritone subharmonics
        val durationMs = 640
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        var lowPassNoise = 0.0
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = sin(PI * (i.toDouble() / count)) * (0.78 + 0.22 * sin(2.0 * PI * 11.0 * t))
          val fm = sin(2.0 * PI * 19.0 * t) * 16.0
          val rawNoise = Random.nextDouble() * 2.0 - 1.0
          lowPassNoise = 0.86 * lowPassNoise + 0.14 * rawNoise
          val subChest = 0.48 * sin(2.0 * PI * (58.0 + fm * 0.5) * t)
          val throatFormant = 0.32 * sin(2.0 * PI * (87.0 - fm) * t) +
            0.18 * sin(2.0 * PI * (123.0 + fm) * t)
          val wave = subChest + throatFormant + 0.22 * lowPassNoise
          buf[i] = (wave * env * Short.MAX_VALUE * 0.88).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.CHHAYA_PRET_RASP -> {
        // Hollow spectral inhalation/exhalation rasp with sweeping wind-tunnel resonances
        val durationMs = 580
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        var bandState = 0.0
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = sin(PI * (i.toDouble() / count))
          val noise = Random.nextDouble() * 2.0 - 1.0
          bandState = 0.78 * bandState + 0.22 * noise
          val hollowCarrier = sin(2.0 * PI * (168.0 - 42.0 * t) * t)
          val minorSecond = sin(2.0 * PI * (179.0 - 38.0 * t) * t)
          val rasp = 0.45 * bandState * (0.6 + 0.4 * sin(2.0 * PI * 24.0 * t)) +
            0.32 * hollowCarrier +
            0.23 * minorSecond
          buf[i] = (rasp * env * Short.MAX_VALUE * 0.82).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.PISHACHA_GUTTURAL_SNARL -> {
        // Aggressive jagged bone-grinding snarl with rapid amplitude flutter & downward pitch snarl
        val durationMs = 540
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        var gritFilter = 0.0
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val attackDecay = exp(-2.4 * t) * (t / 0.04).coerceAtMost(1.0)
          val flutter = 0.55 + 0.45 * cos(2.0 * PI * 29.0 * t)
          val noise = Random.nextDouble() * 2.0 - 1.0
          gritFilter = 0.68 * gritFilter + 0.32 * noise
          val baseFreq = 92.0 - 28.0 * (i.toDouble() / count)
          val snarl = 0.46 * sin(2.0 * PI * baseFreq * t) +
            0.28 * sin(2.0 * PI * (baseFreq * 1.414) * t) +
            0.26 * gritFilter
          buf[i] = (snarl * attackDecay * flutter * Short.MAX_VALUE * 0.90).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.VETALA_SPECTRAL_SHRIEK -> {
        // Piercing dissonant metallic-spectral shriek at close range (< 3.4m)
        val durationMs = 600
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = exp(-3.1 * t) * (t / 0.025).coerceAtMost(1.0)
          val vibrato = sin(2.0 * PI * 23.0 * t) * 32.0
          val shriek = 0.38 * sin(2.0 * PI * (580.0 + vibrato) * t) +
            0.32 * sin(2.0 * PI * (615.0 - vibrato) * t) +
            0.18 * sin(2.0 * PI * (820.0 + vibrato * 1.3) * t) +
            0.12 * (Random.nextDouble() * 2.0 - 1.0)
          buf[i] = (shriek * env * Short.MAX_VALUE * 0.86).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.CREATURE_STALKING_WHISPER -> {
        // Eerie low-level reversed-chant whisper & sub-bass omen pulse when creature is 6.2m..9.0m away
        val durationMs = 490
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        var whisperLP = 0.0
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          // Reverse-swell envelope (crescendo then sudden cut)
          val progress = i.toDouble() / count
          val env = progress * progress * sin(PI * progress)
          val raw = Random.nextDouble() * 2.0 - 1.0
          whisperLP = 0.84 * whisperLP + 0.16 * raw
          val whisper = 0.52 * whisperLP * (0.5 + 0.5 * sin(2.0 * PI * 14.0 * t)) +
            0.48 * sin(2.0 * PI * 68.0 * t)
          buf[i] = (whisper * env * Short.MAX_VALUE * 0.76).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.DISTANT_GHANTA_ECHO -> {
        val durationMs = 680
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = exp(-3.8 * t)
          val wave = 0.6 * sin(2.0 * PI * 528.0 * t) +
            0.25 * sin(2.0 * PI * 1056.0 * t) +
            0.15 * sin(2.0 * PI * 1457.0 * t)
          buf[i] = (wave * env * Short.MAX_VALUE * 0.75).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.DIYA_FLICKER -> {
        val durationMs = 180
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = exp(-12.0 * t)
          val crackle = (Random.nextDouble() * 2.0 - 1.0) * 0.5 +
            0.5 * sin(2.0 * PI * 440.0 * t)
          buf[i] = (crackle * env * Short.MAX_VALUE * 0.6).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }

      SpatialCueType.SEAL_RESONANCE -> {
        val durationMs = 480
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buf = ShortArray(count)
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val env = exp(-3.2 * t)
          val wave = 0.5 * sin(2.0 * PI * 432.0 * t) + 0.5 * sin(2.0 * PI * 648.0 * t)
          buf[i] = (wave * env * Short.MAX_VALUE * 0.75).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buf
      }
    }
  }

  private fun runAtmosphereLoop() {
    while (atmosphereActive.get() && !isMuted.get()) {
      try {
        val state = currentAtmosphere.get()
        val durationMs = 1400
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val stereoBuf = ShortArray(numSamples * 2)
        val f0 = state.droneFreqHz
        val tension = state.tensionDepth

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          // Smooth fade-in and fade-out window to avoid clicks
          val env = sin(PI * (i.toDouble() / numSamples))
          val slowLfo = 0.5 + 0.5 * cos(2.0 * PI * 1.5 * t)
          val baseDrone = 0.55 * sin(2.0 * PI * f0 * t) +
            0.25 * sin(2.0 * PI * (f0 * 1.5) * t) +
            tension * 0.20 * sin(2.0 * PI * (f0 * 1.414) * t) // tritone tension harmonic

          val leftSample = (baseDrone * env * (0.7 + 0.3 * slowLfo) * Short.MAX_VALUE * 0.22).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
          val rightSample = (baseDrone * env * (1.0 - 0.3 * slowLfo) * Short.MAX_VALUE * 0.22).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

          stereoBuf[i * 2] = leftSample
          stereoBuf[i * 2 + 1] = rightSample
        }
        playStereoBuffer(stereoBuf)
        Thread.sleep(900L)
      } catch (_: InterruptedException) {
        break
      } catch (_: Exception) {
        break
      }
    }
  }

  private fun playStereoBuffer(stereoInterleaved: ShortArray) {
    var track: AudioTrack? = null
    try {
      val byteCount = stereoInterleaved.size * 2
      track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setBufferSizeInBytes(maxOf(byteCount, 8192))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(stereoInterleaved, 0, stereoInterleaved.size)
      track.play()
      val frames = stereoInterleaved.size / 2
      val sleepMs = (frames * 1000L / sampleRate) + 60L
      Thread.sleep(sleepMs)
    } catch (_: Exception) {
      // Ignore AudioTrack hardware exceptions in headless/test environments
    } finally {
      try {
        track?.stop()
        track?.release()
      } catch (_: Exception) {
      }
    }
  }
}
