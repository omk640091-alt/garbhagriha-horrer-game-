package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Discrete horror trauma & vibration triggers inside the temple sanctum.
 */
enum class ScreenShakeTrigger(
  val displayName: String,
  val sanskritTerm: String,
  val traumaBoost: Float,
  val vibrationDurationMs: Long,
  val vibrationAmplitude: Int
) {
  JUMPSCARE_STRIKE(
    displayName = "Entity Jumpscare Strike",
    sanskritTerm = "Bhayanaka Aghata",
    traumaBoost = 0.95f,
    vibrationDurationMs = 340L,
    vibrationAmplitude = 255
  ),
  PROCEDURAL_AMBUSH(
    displayName = "Shadow Ambush Manifestation",
    sanskritTerm = "Chhaya Avirbhava",
    traumaBoost = 0.78f,
    vibrationDurationMs = 240L,
    vibrationAmplitude = 215
  ),
  CHASE_FOOTSTEP_TREMOR(
    displayName = "Relentless Chase Tremor",
    sanskritTerm = "Rakshasa Padaghata",
    traumaBoost = 0.42f,
    vibrationDurationMs = 95L,
    vibrationAmplitude = 155
  ),
  PROXIMITY_RESONANCE(
    displayName = "Creature Proximity Tremor",
    sanskritTerm = "Samipya Kampana",
    traumaBoost = 0.26f,
    vibrationDurationMs = 60L,
    vibrationAmplitude = 110
  ),
  GHANTA_SONIC_SHOCKWAVE(
    displayName = "Ghanta Bell Resonance Wave",
    sanskritTerm = "Nada Brahma Prakampa",
    traumaBoost = 0.52f,
    vibrationDurationMs = 170L,
    vibrationAmplitude = 180
  )
}

/**
 * Qualitative shake intensity state exposed to the first-person HUD and telemetry.
 */
enum class ShakeIntensityState(
  val hudBadge: String,
  val description: String
) {
  STILL(
    hudBadge = "STEADY",
    description = "Sanctum foundations still"
  ),
  SUBTLE_TREMOR(
    hudBadge = "TREMOR",
    description = "Subtle stone vibration from nearby entity"
  ),
  CHASE_SHUDDER(
    hudBadge = "CHASE SHAKE",
    description = "Heavy corridor tremors during pursuit"
  ),
  VIOLENT_JUMPSCARE(
    hudBadge = "JUMPSCARE QUAKE",
    description = "Violent camera shock & haptic jolt"
  )
}

/**
 * Intensity-based Screen Shake & Device Haptic Vibration Controller for Garbhagriha.
 *
 * Uses a trauma-based non-linear shake curve (`shakeIntensity = trauma^2` blended with continuous
 * creature proximity and chase state) to drive:
 * - Horizontal (`offsetXPx`) and vertical (`offsetYPx`) first-person camera displacement
 * - Rotational camera roll (`rotationZDeg`) during violent jumpscares or shadow strikes
 * - Real Android `Vibrator` / `VibrationEffect` amplitude-scaled haptic pulses
 */
class ScreenShakeController(
  private val context: Context? = null
) {
  /** Acute trauma accumulator in `0f..1f` that spikes on jumpscares/attacks and decays over time. */
  var trauma by mutableFloatStateOf(0f)
    private set

  /** Continuous baseline tremor in `0f..1f` driven by creature proximity and chase state. */
  var continuousTremor by mutableFloatStateOf(0f)
    private set

  /** Combined effective shake intensity in `0f..1f` (combining acute trauma and proximity tremor). */
  var currentIntensity by mutableFloatStateOf(0f)
    private set

  /** Current horizontal camera shake offset in pixels. */
  var offsetXPx by mutableFloatStateOf(0f)
    private set

  /** Current vertical camera shake offset in pixels. */
  var offsetYPx by mutableFloatStateOf(0f)
    private set

  /** Current camera roll angle in degrees during high-trauma jumpscares. */
  var rotationZDeg by mutableFloatStateOf(0f)
    private set

  /** Qualitative shake state for HUD telemetry badges. */
  var intensityState by mutableStateOf(ShakeIntensityState.STILL)
    private set

  /** Whether screen shake & haptic vibrations are enabled by the player. */
  var isHapticsAndShakeEnabled by mutableStateOf(true)
    private set

  /** Last triggered shake event label for telemetry/HUD feedback. */
  var lastTriggeredEventLabel by mutableStateOf<String?>(null)
    private set

  private var tickCounter by mutableIntStateOf(0)
  private var lastHapticPulseTick by mutableIntStateOf(-20)

  val shakePercent: Int
    get() = (currentIntensity * 100f).roundToInt().coerceIn(0, 100)

  fun toggleEnabled(): String {
    isHapticsAndShakeEnabled = !isHapticsAndShakeEnabled
    if (!isHapticsAndShakeEnabled) {
      reset()
      return "Screen Shake & Haptic Vibrations: OFF"
    }
    return "Screen Shake & Haptic Vibrations: ON"
  }

  fun reset() {
    trauma = 0f
    continuousTremor = 0f
    currentIntensity = 0f
    offsetXPx = 0f
    offsetYPx = 0f
    rotationZDeg = 0f
    intensityState = ShakeIntensityState.STILL
  }

  /**
   * Triggers an immediate trauma spike and amplitude-scaled Android haptic vibration pulse
   * for jumpscares, ambushes, creature attacks, or Ghanta bell shockwaves.
   */
  fun triggerShake(
    trigger: ScreenShakeTrigger,
    customIntensityMultiplier: Float = 1.0f
  ) {
    if (!isHapticsAndShakeEnabled) return

    val scaledBoost = (trigger.traumaBoost * customIntensityMultiplier).coerceIn(0.08f, 1.0f)
    trauma = (trauma + scaledBoost).coerceIn(0f, 1.0f)
    lastTriggeredEventLabel = "${trigger.displayName} (${trigger.sanskritTerm})"

    updateCombinedOffsets(phaseSeed = tickCounter * 0.37f + trauma * 9.1f)

    val amp = (trigger.vibrationAmplitude * customIntensityMultiplier.coerceIn(0.35f, 1.25f))
      .roundToInt()
      .coerceIn(1, 255)
    vibrateDevice(
      durationMs = trigger.vibrationDurationMs,
      amplitude = amp,
      isJumpscarePattern = trigger == ScreenShakeTrigger.JUMPSCARE_STRIKE ||
        trigger == ScreenShakeTrigger.PROCEDURAL_AMBUSH
    )
    lastHapticPulseTick = tickCounter
  }

  /**
   * Called every 100ms (10Hz game loop) to decay acute jumpscare trauma, compute continuous
   * proximity/chase tremor when a mythological creature is nearby, update camera shake offsets,
   * and emit rhythmic haptic footstep/heartbeat vibrations during close chases.
   */
  fun onTick(
    entityDistance: Double,
    behaviorState: CreatureBehaviorState,
    sanityPercent: Int,
    chantingMantra: Boolean,
    animTick: Float
  ) {
    tickCounter++

    if (!isHapticsAndShakeEnabled) {
      reset()
      return
    }

    // 1. Decay acute jumpscare trauma smoothly (faster decay when chanting protective Mantra Kavach)
    val decayRate = if (chantingMantra) 0.14f else 0.085f
    trauma = (trauma - decayRate).coerceAtLeast(0f)

    // 2. Compute continuous proximity & chase tremor
    val dist = entityDistance.toFloat()
    val proximityFactor = when {
      dist < 5.8f -> ((5.8f - dist) / 5.8f).coerceIn(0f, 1f)
      else -> 0f
    }

    val behaviorBonus = when (behaviorState) {
      CreatureBehaviorState.FRENZIED_CHASE -> 0.36f
      CreatureBehaviorState.SANITY_AMBUSH -> 0.44f
      CreatureBehaviorState.SHADOW_STALKING -> if (dist < 4.5f) 0.16f else 0.05f
      CreatureBehaviorState.STUNNED_BY_NADA -> 0f
      CreatureBehaviorState.DORMANT_PATROL -> 0f
    }

    val lowSanityTremor = if (sanityPercent < 35) ((35 - sanityPercent) / 140f) else 0f
    val kavachDampening = if (chantingMantra) 0.45f else 1.0f

    continuousTremor = ((proximityFactor * 0.58f + behaviorBonus + lowSanityTremor) * kavachDampening)
      .coerceIn(0f, 0.85f)

    updateCombinedOffsets(phaseSeed = animTick * 24.0f + tickCounter * 1.9f)

    // 3. Emit periodic intensity-scaled haptic thuds when creature is chasing or very close
    val isChase = behaviorState == CreatureBehaviorState.FRENZIED_CHASE ||
      behaviorState == CreatureBehaviorState.SANITY_AMBUSH
    val pulseIntervalTicks = when {
      dist < 2.4f || isChase -> 7 // Every 0.7s during active chase or extreme proximity
      dist < 4.4f -> 12           // Every 1.2s when creature stalks nearby
      else -> 0
    }

    if (pulseIntervalTicks > 0 &&
      currentIntensity >= 0.18f &&
      (tickCounter - lastHapticPulseTick) >= pulseIntervalTicks
    ) {
      val pulseAmp = (currentIntensity * 220f).roundToInt().coerceIn(45, 235)
      val pulseDuration = if (isChase) 85L else 55L
      vibrateDevice(
        durationMs = pulseDuration,
        amplitude = pulseAmp,
        isJumpscarePattern = false
      )
      lastHapticPulseTick = tickCounter
    }
  }

  private fun updateCombinedOffsets(phaseSeed: Float) {
    // Non-linear quadratic trauma curve feels punchy on jumpscares while preserving subtle proximity rumble
    val nonlinearTrauma = trauma * trauma
    val combined = maxOf(nonlinearTrauma, continuousTremor * 0.78f + nonlinearTrauma * 0.45f)
      .coerceIn(0f, 1.0f)

    currentIntensity = combined

    if (combined < 0.03f) {
      offsetXPx = 0f
      offsetYPx = 0f
      rotationZDeg = 0f
      intensityState = ShakeIntensityState.STILL
      return
    }

    val maxTranslationPx = 28.0f
    val maxRollDeg = 3.2f

    // Multi-frequency pseudo-harmonic oscillation for organic horror camera jolt
    val waveX = sin(phaseSeed * 1.7f) * 0.65f + cos(phaseSeed * 3.9f) * 0.35f
    val waveY = cos(phaseSeed * 2.3f) * 0.60f + sin(phaseSeed * 4.7f) * 0.40f
    val waveRoll = sin(phaseSeed * 2.9f + 1.1f)

    offsetXPx = waveX * maxTranslationPx * combined
    offsetYPx = waveY * maxTranslationPx * combined
    rotationZDeg = if (combined > 0.28f) waveRoll * maxRollDeg * combined else 0f

    intensityState = when {
      combined >= 0.62f -> ShakeIntensityState.VIOLENT_JUMPSCARE
      combined >= 0.30f -> ShakeIntensityState.CHASE_SHUDDER
      combined >= 0.04f -> ShakeIntensityState.SUBTLE_TREMOR
      else -> ShakeIntensityState.STILL
    }
  }

  private fun vibrateDevice(
    durationMs: Long,
    amplitude: Int,
    isJumpscarePattern: Boolean
  ) {
    val ctx = context ?: return
    try {
      val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      if (vibrator == null || !vibrator.hasVibrator()) return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (isJumpscarePattern) {
          // Double-thud heartbeat jolt pattern for jumpscares & ambushes
          val timings = longArrayOf(0L, durationMs / 2, 45L, durationMs)
          val amplitudes = intArrayOf(
            0,
            amplitude.coerceIn(1, 255),
            0,
            (amplitude * 0.85f).roundToInt().coerceIn(1, 255)
          )
          vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
          vibrator.vibrate(
            VibrationEffect.createOneShot(
              durationMs.coerceAtLeast(25L),
              amplitude.coerceIn(1, 255)
            )
          )
        }
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.coerceAtLeast(25L))
      }
    } catch (_: Throwable) {
      // Gracefully ignore vibration errors on emulators or devices without vibrator hardware
    }
  }
}

/**
 * Remembers a [ScreenShakeController] bound to the current Android [LocalContext] so it can
 * drive both Compose viewport transformations and device haptic vibrations.
 */
@Composable
fun rememberScreenShakeController(key: Any? = Unit): ScreenShakeController {
  val context = LocalContext.current
  return remember(key, context) {
    ScreenShakeController(context.applicationContext)
  }
}

/**
 * Modifier extension that applies the [ScreenShakeController]'s real-time translation and
 * rotational camera roll directly to any Compose viewport container.
 */
fun Modifier.applyScreenShake(controller: ScreenShakeController?): Modifier {
  if (controller == null) return this
  return this.graphicsLayer {
    translationX = controller.offsetXPx
    translationY = controller.offsetYPx
    rotationZ = controller.rotationZDeg
  }
}
