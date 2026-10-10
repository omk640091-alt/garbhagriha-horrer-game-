package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/**
 * Represents the player's physical breath and exertion state (Pranayama / Ojas)
 * while evading mythological guardians in the temple corridors.
 */
enum class StaminaExertionState(
  val badgeLabel: String,
  val sanskritTerm: String,
  val speedMultiplier: Double
) {
  STEADY_BREATH(
    badgeLabel = "STEADY",
    sanskritTerm = "सम प्राण • Balanced Breath",
    speedMultiplier = 1.0
  ),
  SPRINTING_SURGE(
    badgeLabel = "SPRINTING",
    sanskritTerm = "वेग धावन • Rapid Evasion",
    speedMultiplier = 1.62
  ),
  WINDED(
    badgeLabel = "WINDED",
    sanskritTerm = "श्रम • Heavy Breathing",
    speedMultiplier = 0.88
  ),
  EXHAUSTED_LOCKOUT(
    badgeLabel = "EXHAUSTED",
    sanskritTerm = "निःश्वास • Breath Depleted",
    speedMultiplier = 0.65
  )
}

/**
 * Manages the player's physical stamina (`staminaPercent`, `isSprinting`, `isExhausted`,
 * and `exertionState`) to limit sprinting during intense chase sequences with mythological
 * creatures. Forces players to balance burst evasion against exhaustion lockouts.
 */
class StaminaManager(
  initialStamina: Float = 100f
) {
  private var rawStamina by mutableFloatStateOf(initialStamina.coerceIn(0f, 100f))

  var staminaPercent by mutableIntStateOf(initialStamina.roundToInt().coerceIn(0, 100))
    private set

  var isSprinting by mutableStateOf(false)
    private set

  var isExhausted by mutableStateOf(false)
    private set

  var exertionState by mutableStateOf(StaminaExertionState.STEADY_BREATH)
    private set

  private var ticksSinceLastMove = 0

  /**
   * Toggles sprint mode on/off if the player is not currently in an exhaustion lockout.
   * Returns a status message for the HUD banner.
   */
  fun toggleSprint(): String {
    if (isExhausted) {
      isSprinting = false
      updateDerivedState()
      return "Breath exhausted! Recover Stamina to 30% before sprinting again!"
    }
    if (!isSprinting && rawStamina < 10f) {
      return "Stamina too low to initiate sprint!"
    }
    isSprinting = !isSprinting
    updateDerivedState()
    return if (isSprinting) {
      "Sprint Mode ENGAGED — Moving at 1.6x evasion speed (drains Stamina per step)!"
    } else {
      "Sprint Mode OFF — Conserving breath for steady Pranayama recovery."
    }
  }

  fun setSprintEnabled(enabled: Boolean) {
    if (enabled && !isExhausted && rawStamina >= 10f) {
      isSprinting = true
    } else if (!enabled) {
      isSprinting = false
    }
    updateDerivedState()
  }

  /**
   * Consumes stamina when the player steps forward or backward and returns the effective
   * step distance multiplier (1.62x when sprinting, 1.0x normal, or 0.65x when exhausted).
   */
  fun consumeStepStamina(isChaseActive: Boolean = false): Double {
    ticksSinceLastMove = 0

    if (isExhausted) {
      isSprinting = false
      updateDerivedState()
      return StaminaExertionState.EXHAUSTED_LOCKOUT.speedMultiplier
    }

    if (isSprinting) {
      // Sprinting during an active creature chase costs slightly more adrenaline/stamina
      val drain = if (isChaseActive) 11.5f else 9.5f
      applyDelta(-drain)
      if (rawStamina <= 0f) {
        rawStamina = 0f
        staminaPercent = 0
        isSprinting = false
        isExhausted = true
        updateDerivedState()
        return StaminaExertionState.EXHAUSTED_LOCKOUT.speedMultiplier
      }
      updateDerivedState()
      return StaminaExertionState.SPRINTING_SURGE.speedMultiplier
    }

    updateDerivedState()
    return exertionState.speedMultiplier
  }

  /**
   * Evaluates 100ms (10Hz) stamina regeneration and exhaustion recovery.
   * Regeneration is faster when standing still or chanting a Vedic Mantra, and slightly
   * hindered when Sanity is critically low.
   */
  fun onTick(
    sanityPercent: Int,
    chantingMantra: Boolean,
    isChaseActive: Boolean
  ) {
    ticksSinceLastMove++

    // Passive regeneration when not actively stepping while sprinting
    val isResting = !isSprinting || ticksSinceLastMove >= 4
    if (isResting && rawStamina < 100f) {
      var regenPerTick = if (ticksSinceLastMove >= 8) 1.35f else 0.75f
      if (chantingMantra) {
        regenPerTick += 1.4f
      }
      if (sanityPercent < 30) {
        // Panic breathing slows stamina recovery during low sanity
        regenPerTick *= 0.68f
      }
      if (isChaseActive && !chantingMantra) {
        regenPerTick *= 0.85f
      }
      applyDelta(regenPerTick)
    }

    // Clear exhaustion lockout once stamina recovers to at least 30%
    if (isExhausted && rawStamina >= 30f) {
      isExhausted = false
    }

    updateDerivedState()
  }

  /**
   * Restores stamina directly (e.g., when chanting a Vedic Mantra or invoking a sacred relic).
   */
  fun restoreStamina(amount: Float) {
    if (amount <= 0f) return
    applyDelta(amount)
    if (rawStamina >= 30f) {
      isExhausted = false
    }
    updateDerivedState()
  }

  fun reset(initialValue: Float = 100f) {
    rawStamina = initialValue.coerceIn(0f, 100f)
    staminaPercent = rawStamina.roundToInt()
    isSprinting = false
    isExhausted = false
    ticksSinceLastMove = 0
    updateDerivedState()
  }

  private fun applyDelta(delta: Float) {
    rawStamina = (rawStamina + delta).coerceIn(0f, 100f)
    staminaPercent = rawStamina.roundToInt().coerceIn(0, 100)
  }

  private fun updateDerivedState() {
    exertionState = when {
      isExhausted -> StaminaExertionState.EXHAUSTED_LOCKOUT
      isSprinting && ticksSinceLastMove < 6 -> StaminaExertionState.SPRINTING_SURGE
      staminaPercent < 32 -> StaminaExertionState.WINDED
      else -> StaminaExertionState.STEADY_BREATH
    }
  }
}
