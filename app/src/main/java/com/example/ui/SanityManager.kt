package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/**
 * Spiritual & psychological stability states (Manas / Citta) experienced by the explorer
 * inside cursed Garbhagriha sanctums.
 */
enum class SanityMentalState(
  val statusLabel: String,
  val sanskritTerm: String,
  val description: String,
  val hallucinationIntensity: Float
) {
  KAVACH_PROTECTED(
    statusLabel = "KAVACH",
    sanskritTerm = "मन्त्र कवच • Mantra Shielded",
    description = "Protected by Vedic mantra resonance; immune to terror drain.",
    hallucinationIntensity = 0f
  ),
  LUCID(
    statusLabel = "LUCID",
    sanskritTerm = "सत्त्व • Sattvic Clarity",
    description = "Mind is steady and focused on the sacred Diyas.",
    hallucinationIntensity = 0.05f
  ),
  UNEASY(
    statusLabel = "UNEASY",
    sanskritTerm = "चञ्चल • Restless Manas",
    description = "Whispers echo from the granite pillars; shadow forms flicker.",
    hallucinationIntensity = 0.28f
  ),
  SHAKEN(
    statusLabel = "SHAKEN",
    sanskritTerm = "भय • Gripped by Bhaya",
    description = "Mythological dread distorts vision and amplifies temple echoes.",
    hallucinationIntensity = 0.62f
  ),
  UNRAVELING(
    statusLabel = "DREAD",
    sanskritTerm = "उन्माद • Unmada Collapse",
    description = "Sanity critically fractured; spiritual terror erodes physical Prana!",
    hallucinationIntensity = 1.0f
  )
}

/**
 * Discrete horror events and mythological encounters that shock or restore the player's sanity.
 */
enum class MythologicalSanityEvent(
  val title: String,
  val sanityDelta: Float,
  val whisperPrompt: String
) {
  ENTITY_GAZE_ENCOUNTER(
    title = "Direct Gaze of the Guardian",
    sanityDelta = -10f,
    whisperPrompt = "You locked eyes with the lurking temple guardian! Manas trembles!"
  ),
  ENTITY_SHADOW_STRIKE(
    title = "Mythological Ambush",
    sanityDelta = -18f,
    whisperPrompt = "Claws of shadow tore through your spiritual aura! (-18% Sanity)"
  ),
  DIYA_FLAME_EXTINGUISHED(
    title = "Tamasic Darkness Descends",
    sanityDelta = -12f,
    whisperPrompt = "Your brass Diya extinguished into pitch darkness! (-12% Sanity)"
  ),
  CURSED_INSCRIPTION_WHISPER(
    title = "Forbidden Tantric Echo",
    sanityDelta = -7f,
    whisperPrompt = "Unseen voices chant backwards from the Garbhagriha walls..."
  ),
  DIYA_SCONCE_IGNITED(
    title = "Agni Illumination",
    sanityDelta = +14f,
    whisperPrompt = "Sacred Diya flame banishes encroaching Tamas! (+14% Sanity)"
  ),
  VEDIC_SEAL_DECIPHERED(
    title = "Yantra Alignment",
    sanityDelta = +20f,
    whisperPrompt = "Ancient Vedic Yantra aligned! Cosmic order steadies your mind. (+20% Sanity)"
  ),
  MANTRA_KAVACH_CHANTED(
    title = "Maha Mrityunjaya Kavach",
    sanityDelta = +24f,
    whisperPrompt = "ॐ त्र्यम्बकं यजामहे • Sacred vibrations restore Sattvic clarity! (+24% Sanity)"
  ),
  GHANTA_BELL_RESONANCE(
    title = "528Hz Saptadhatu Ghanta",
    sanityDelta = +8f,
    whisperPrompt = "Pure bronze bell resonance clears shadow illusions! (+8% Sanity)"
  )
}

/**
 * Tracks and updates the player's mental state (`sanityPercent`, `mentalState`,
 * `hallucinationFactor`, and `activeWhisper`) in response to continuous environmental
 * horror telemetry (creature distance, line-of-sight, darkness, mantra shield) and
 * discrete mythological encounters.
 */
class SanityManager(
  initialSanity: Float = 100f
) {
  private var rawSanity by mutableFloatStateOf(initialSanity.coerceIn(0f, 100f))

  var sanityPercent by mutableIntStateOf(initialSanity.roundToInt().coerceIn(0, 100))
    private set

  var mentalState by mutableStateOf(SanityMentalState.LUCID)
    private set

  var hallucinationFactor by mutableFloatStateOf(0f)
    private set

  var activeWhisper by mutableStateOf<String?>(null)
    private set

  var totalMythologicalEncounters by mutableIntStateOf(0)
    private set

  private var encounterCooldownTicks = 0

  /**
   * Evaluates continuous 1-second horror telemetry inside the temple sanctum:
   * - Proximity to mythological creatures (Brahmarakshasa, Chhaya Pret, Pishacha)
   * - Whether the player is looking directly toward the creature (`isLookingAtEntity`)
   * - Darkness penalty when the brass Diya oil lamp goes out
   * - Recovery when chanting a Vedic Mantra (`chantingMantra`) or standing in safe light
   *
   * Returns the Prana health penalty (0 or positive integer) caused by severe sanity collapse.
   */
  fun onTelemetryTick(
    entityDistance: Double,
    isLookingAtEntity: Boolean,
    diyaLit: Boolean,
    chantingMantra: Boolean,
    entityName: String
  ): Int {
    if (encounterCooldownTicks > 0) {
      encounterCooldownTicks--
    }

    if (chantingMantra) {
      applyDelta(+5.5f)
      updateDerivedState(chantingMantra = true)
      return 0
    }

    var delta = 0f

    // 1. Creature proximity dread
    when {
      entityDistance < 2.8 -> {
        delta -= 4.5f
        if (encounterCooldownTicks == 0) {
          totalMythologicalEncounters++
          encounterCooldownTicks = 6
          activeWhisper = "$entityName breathes down your neck! Chant Mantra or ring Ghanta!"
        }
      }
      entityDistance < 4.5 -> {
        delta -= 2.6f
      }
      entityDistance < 6.5 -> {
        delta -= 1.0f
      }
    }

    // 2. Direct gaze at mythological entity amplifies psychological shock
    if (isLookingAtEntity && entityDistance < 5.8) {
      delta -= 1.8f
      if (encounterCooldownTicks == 0 && entityDistance < 4.2) {
        totalMythologicalEncounters++
        encounterCooldownTicks = 5
        activeWhisper = "Direct sight of $entityName fractures your Manas!"
      }
    }

    // 3. Darkness dread when brass Diya lamp is unlit
    if (!diyaLit) {
      delta -= 3.0f
      if (activeWhisper == null && sanityPercent < 60) {
        activeWhisper = "Pitch Tamasic darkness erodes your sanity—re-ignite your Diya!"
      }
    }

    // 4. Slow Sattvic recovery when Diya is lit and guardian is far away
    if (diyaLit && entityDistance >= 6.8 && delta == 0f) {
      delta += 1.4f
      if (sanityPercent > 75) {
        activeWhisper = null
      }
    }

    applyDelta(delta)
    updateDerivedState(chantingMantra = false)

    // When sanity drops into UNRAVELING (< 25%) or Diya is out, spiritual terror drains Prana
    return when {
      sanityPercent <= 15 -> 3
      sanityPercent < 25 || !diyaLit -> 2
      else -> 0
    }
  }

  /**
   * Triggers a discrete horror or ritual event (e.g., creature ambush, Diya extinguishing,
   * igniting a sconce, unlocking a Vedic seal, or invoking a relic).
   */
  fun recordEvent(event: MythologicalSanityEvent, customDelta: Float? = null): String {
    val delta = customDelta ?: event.sanityDelta
    if (delta < 0f) {
      totalMythologicalEncounters++
    }
    applyDelta(delta)
    activeWhisper = event.whisperPrompt
    updateDerivedState(chantingMantra = event == MythologicalSanityEvent.MANTRA_KAVACH_CHANTED)
    return event.whisperPrompt
  }

  /**
   * Applies a custom sanity boost (e.g. from invoking a consecrated Sanatani artifact).
   */
  fun restoreSanity(amount: Int, reason: String? = null) {
    if (amount <= 0) return
    applyDelta(amount.toFloat())
    if (reason != null) {
      activeWhisper = reason
    }
    updateDerivedState(chantingMantra = false)
  }

  fun reset(initialValue: Float = 100f) {
    rawSanity = initialValue.coerceIn(0f, 100f)
    sanityPercent = rawSanity.roundToInt()
    mentalState = SanityMentalState.LUCID
    hallucinationFactor = 0f
    activeWhisper = null
    totalMythologicalEncounters = 0
    encounterCooldownTicks = 0
  }

  private fun applyDelta(delta: Float) {
    rawSanity = (rawSanity + delta).coerceIn(0f, 100f)
    sanityPercent = rawSanity.roundToInt().coerceIn(0, 100)
  }

  private fun updateDerivedState(chantingMantra: Boolean) {
    mentalState = when {
      chantingMantra -> SanityMentalState.KAVACH_PROTECTED
      sanityPercent >= 70 -> SanityMentalState.LUCID
      sanityPercent >= 48 -> SanityMentalState.UNEASY
      sanityPercent >= 25 -> SanityMentalState.SHAKEN
      else -> SanityMentalState.UNRAVELING
    }

    val rawStress = ((100f - rawSanity) / 100f).coerceIn(0f, 1f)
    hallucinationFactor = if (chantingMantra) {
      0f
    } else {
      (rawStress * 0.7f + mentalState.hallucinationIntensity * 0.3f).coerceIn(0f, 1f)
    }
  }
}
