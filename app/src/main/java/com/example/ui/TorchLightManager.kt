package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sin

/**
 * Illumination states of the player's handheld brass Diya lantern / Agni torch.
 */
enum class TorchFlameState(
  val hudLabel: String,
  val sanskritName: String,
  val baseRadiusTiles: Float
) {
  BLAZING_AGNI(
    hudLabel = "BLAZING",
    sanskritName = "प्रज्वलित दीप • Radiant Agni",
    baseRadiusTiles = 8.4f
  ),
  STEADY_FLAME(
    hudLabel = "DIYA LIT",
    sanskritName = "स्थिर ज्योति • Steady Flame",
    baseRadiusTiles = 6.8f
  ),
  SPUTTERING_EMBER(
    hudLabel = "FLICKERING",
    sanskritName = "कम्पित शिखा • Sputtering Wick",
    baseRadiusTiles = 4.3f
  ),
  EXTINGUISHED_DARKNESS(
    hudLabel = "UNLIT",
    sanskritName = "तमस् • Extinguished",
    baseRadiusTiles = 2.2f
  )
}

/**
 * Manages the player's handheld flickering brass Diya lantern / torch:
 * - Tracks `fuelPercent` (0f..100f) and `isLit`
 * - Computes multi-harmonic organic flame flicker (`flickerIntensity`) and dynamic
 *   raycast illumination radius (`currentLightRadiusTiles`, `lightRadiusNormalized`)
 * - Consumes fuel over time (accelerated when sprinting or when supernatural entity drafts blow)
 * - Supports manual extinguishing/re-igniting to conserve fuel, sconce refills, and flare surges
 */
class TorchLightManager(
  initialFuel: Float = 100f,
  initiallyLit: Boolean = true
) {
  var fuelPercent by mutableFloatStateOf(initialFuel.coerceIn(0f, 100f))
    private set

  var isLit by mutableStateOf(initiallyLit && initialFuel > 0f)
    private set

  var flameState by mutableStateOf(TorchFlameState.BLAZING_AGNI)
    private set

  /**
   * Real-time light reach in map tiles (ranges from ~2.2f in darkness up to ~8.8f at full blaze).
   */
  var currentLightRadiusTiles by mutableFloatStateOf(8.0f)
    private set

  /**
   * Normalized light radius ratio (0.18f..1.0f) used by the 3D raycaster, hand-lamp renderer,
   * and post-processing vignette overlay.
   */
  var lightRadiusNormalized by mutableFloatStateOf(1.0f)
    private set

  /**
   * Instantaneous flame flicker oscillation factor (0.55f..1.15f).
   */
  var flickerMultiplier by mutableFloatStateOf(1.0f)
    private set

  init {
    recalculateLightRadius(animTick = 0f, entityDistance = 8.0)
  }

  /**
   * Advances torch fuel consumption and dynamic flicker physics on each 100ms (10Hz) game tick.
   * Returns `true` on the exact tick the flame sputters out due to fuel depletion.
   */
  fun onTick(
    animTick: Float,
    isSprinting: Boolean,
    chantingMantra: Boolean,
    entityDistance: Double
  ): Boolean {
    var extinguishedNow = false

    if (isLit) {
      // Base burn rate: ~0.12% per 100ms (1.2% per second).
      // Sprinting wind draft or close supernatural entity presence increases fuel consumption.
      var burnRate = 0.115f
      if (isSprinting) {
        burnRate += 0.045f
      }
      if (entityDistance < 3.6) {
        burnRate += 0.035f
      }
      if (chantingMantra) {
        // Mantra Kavach shields the flame from draining
        burnRate = 0.02f
      }

      fuelPercent = (fuelPercent - burnRate).coerceIn(0f, 100f)
      if (fuelPercent <= 0f) {
        fuelPercent = 0f
        isLit = false
        extinguishedNow = true
      }
    }

    recalculateLightRadius(animTick = animTick, entityDistance = entityDistance)
    return extinguishedNow
  }

  /**
   * Toggles the handheld Diya torch on or off so players can strategically conserve ghee fuel
   * or re-ignite in dark corridors.
   */
  fun toggleFlame(): String {
    if (!isLit) {
      if (fuelPercent <= 1f) {
        return "Torch reservoir is dry! Refill sacred ghee at a Temple Sconce or invoke a Solar Relic!"
      }
      isLit = true
      recalculateLightRadius(animTick = 0f, entityDistance = 6.0)
      return "Brass Diya Lantern ignited (${fuelPercent.toInt()}% fuel remaining • Light Radius ${"%.1f".format(currentLightRadiusTiles)}m)."
    } else {
      isLit = false
      recalculateLightRadius(animTick = 0f, entityDistance = 6.0)
      return "Diya Lantern shuttered to conserve fuel—beware the encroaching Tamasic darkness!"
    }
  }

  /**
   * Refills fuel by [amount] (e.g., +100% at a temple sconce or +35%..60% from a relic)
   * and optionally re-ignites the flame.
   */
  fun refillFuel(amount: Float, autoIgnite: Boolean = true) {
    if (amount <= 0f) return
    fuelPercent = (fuelPercent + amount).coerceIn(0f, 100f)
    if (autoIgnite && fuelPercent > 0f) {
      isLit = true
    }
    recalculateLightRadius(animTick = 0f, entityDistance = 8.0)
  }

  fun restoreState(savedFuel: Float, savedLit: Boolean) {
    fuelPercent = savedFuel.coerceIn(0f, 100f)
    isLit = savedLit && fuelPercent > 0f
    recalculateLightRadius(animTick = 0f, entityDistance = 8.0)
  }

  private fun recalculateLightRadius(animTick: Float, entityDistance: Double) {
    if (!isLit || fuelPercent <= 0f) {
      flameState = TorchFlameState.EXTINGUISHED_DARKNESS
      flickerMultiplier = 0.75f
      currentLightRadiusTiles = TorchFlameState.EXTINGUISHED_DARKNESS.baseRadiusTiles
      lightRadiusNormalized = 0.22f
      return
    }

    flameState = when {
      fuelPercent >= 65f -> TorchFlameState.BLAZING_AGNI
      fuelPercent >= 28f -> TorchFlameState.STEADY_FLAME
      else -> TorchFlameState.SPUTTERING_EMBER
    }

    val fuelRatio = (fuelPercent / 100f).coerceIn(0.08f, 1.0f)

    // Multi-harmonic organic flame turbulence: stronger sputtering when fuel is low (< 28%)
    // or when a mythological creature's cold aura is nearby (< 4.5m).
    val lowFuelTurbulence = if (fuelPercent < 28f) 0.22f else 0.08f
    val entityDraftTurbulence = if (entityDistance < 4.5) 0.14f else 0.0f
    val amp = lowFuelTurbulence + entityDraftTurbulence

    val wave1 = sin(animTick * 7.5f)
    val wave2 = sin(animTick * 19.3f + 1.2f) * 0.6f
    val wave3 = sin(animTick * 31.7f + 2.7f) * 0.35f
    val combinedWave = (wave1 + wave2 + wave3) / 1.95f

    flickerMultiplier = (1.0f + combinedWave * amp).coerceIn(0.58f, 1.18f)

    // Dynamic light radius scales smoothly with remaining fuel and real-time flicker
    val baseReach = 3.2f + (5.4f * (0.35f + 0.65f * fuelRatio))
    currentLightRadiusTiles = (baseReach * flickerMultiplier).coerceIn(2.4f, 9.2f)
    lightRadiusNormalized = ((currentLightRadiusTiles - 2.2f) / 6.8f).coerceIn(0.18f, 1.0f)
  }
}
