package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlin.math.hypot

/**
 * Moral alignment tier inside the temple based on respectful vs. desecrating choices.
 */
enum class GodsFavorStanding(
  val id: String,
  val hudBadge: String,
  val sanskritTitle: String,
  val minScore: Int,
  val description: String
) {
  EXALTED_ANUGRAHA(
    id = "exalted",
    hudBadge = "EXALTED GRACE",
    sanskritTitle = "परम देव अनुग्रह (Parama Deva Anugraha)",
    minScore = 80,
    description = "Deities of the Garbhagriha shield your Prana, Sanity, and Diya flame from temple hazards."
  ),
  REVERENT_DEVOTEE(
    id = "reverent",
    hudBadge = "REVERENT",
    sanskritTitle = "श्रद्धावान् साधक (Shraddhavan Sadhaka)",
    minScore = 60,
    description = "Respectful conduct grants temporary passive blessings and hazard resistance."
  ),
  NEUTRAL_SEEKER(
    id = "neutral",
    hudBadge = "NEUTRAL",
    sanskritTitle = "समभाव पथिक (Samabhava Pathika)",
    minScore = 40,
    description = "Balanced karma. Perform prayers at shrines or spare sacred vessels to earn divine buffs."
  ),
  TRANSGRESSOR_DOSHA(
    id = "transgressor",
    hudBadge = "DEVA KOPA",
    sanskritTitle = "देव कोप दोष (Deva Kopa Dosha)",
    minScore = 0,
    description = "Desecrating temple vessels has angered the sanctum guardians, amplifying environmental hazards."
  )
}

/**
 * Environmental hazards inside the temple that can be neutralized by God's Favor passive buffs.
 */
enum class SanctumEnvironmentalHazard(
  val id: String,
  val displayName: String,
  val sanskritName: String,
  val description: String
) {
  ABYSSAL_DARKNESS_DRAIN(
    id = "abyssal_darkness",
    displayName = "Abyssal Darkness Drain",
    sanskritName = "तमस् प्राण क्षय (Tamas Prana Kshaya)",
    description = "Rapid Sanity & Prana erosion when the Diya flame flickers low or goes out."
  ),
  SUFFOCATING_MASONRY_SPORES(
    id = "masonry_spores",
    displayName = "Choking Masonry Dust & Spores",
    sanskritName = "अश्म धूलि विष (Ashma Dhuli Visha)",
    description = "Falling ceiling grit and ancient spores that sap Stamina and cloud vision."
  ),
  CURSED_COBWEB_SNARE(
    id = "cursed_cobwebs",
    displayName = "Cursed Cobweb & Whisper Snare",
    sanskritName = "शापित जाल पाश (Shapita Jala Pasha)",
    description = "Clinging temple cobwebs and cursed wall inscriptions that drain Sanity and slow movement."
  ),
  SHADOW_AURA_CORRUPTION(
    id = "shadow_corruption",
    displayName = "Guardian Shadow Aura",
    sanskritName = "छाया अभिशाप (Chhaya Abhishapa)",
    description = "Lethal Prana & Torch Oil drain when stalking entities draw near."
  )
}

/**
 * Temporary passive buff / immunity granted by earning God's Favor through respectful actions.
 */
enum class DivinePassiveBuff(
  val id: String,
  val title: String,
  val sanskritName: String,
  val protectedHazard: SanctumEnvironmentalHazard,
  val defaultDurationSeconds: Int,
  val summary: String
) {
  SURYA_TEJAS_AEGIS(
    id = "surya_tejas",
    title = "Surya Tejas Aegis",
    sanskritName = "सूर्य तेजस् कवच",
    protectedHazard = SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN,
    defaultDurationSeconds = 45,
    summary = "Immunity to Abyssal Darkness Sanity/Prana drain & 50% slower Diya oil burn."
  ),
  VAYU_PRANA_SHIELD(
    id = "vayu_prana",
    title = "Vayu Prana Purity",
    sanskritName = "वायु प्राण शुद्धि",
    protectedHazard = SanctumEnvironmentalHazard.SUFFOCATING_MASONRY_SPORES,
    defaultDurationSeconds = 45,
    summary = "Immunity to Choking Masonry Dust & Spores; +25% Stamina recovery while moving."
  ),
  SARASWATI_SMRITI_WARD(
    id = "saraswati_ward",
    title = "Saraswati Smriti Ward",
    sanskritName = "सरस्वती स्मृति रक्षा",
    protectedHazard = SanctumEnvironmentalHazard.CURSED_COBWEB_SNARE,
    defaultDurationSeconds = 50,
    summary = "Immunity to Cursed Cobweb Snares & Inscription Whispers; +15% Sanity stability."
  ),
  RUDRA_ABHAYA_MANTLE(
    id = "rudra_mantle",
    title = "Rudra Abhaya Mantle",
    sanskritName = "रुद्र अभय आवरण",
    protectedHazard = SanctumEnvironmentalHazard.SHADOW_AURA_CORRUPTION,
    defaultDurationSeconds = 40,
    summary = "Immunity to Guardian Shadow Aura Torch drain & 40% reduced Prana damage from strikes."
  )
}

/**
 * Moral action categories that shift God's Favor up (respectful) or down (desecrating).
 */
enum class MoralActionType(
  val id: String,
  val title: String,
  val sanskritLabel: String,
  val favorDelta: Int,
  val grantedBuff: DivinePassiveBuff?,
  val isRespectful: Boolean
) {
  PRAY_AT_SHRINE(
    id = "pray_at_shrine",
    title = "Offer Pranam & Prayer at Sacred Shrine",
    sanskritLabel = "मन्दिर प्रणाम एवं प्रार्थना",
    favorDelta = 18,
    grantedBuff = DivinePassiveBuff.SURYA_TEJAS_AEGIS,
    isRespectful = true
  ),
  SPARE_SACRED_KALASHA(
    id = "spare_kalasha",
    title = "Preserve & Venerate Ancient Kalasha Vessel",
    sanskritLabel = "पवित्र कलश रक्षण",
    favorDelta = 16,
    grantedBuff = DivinePassiveBuff.VAYU_PRANA_SHIELD,
    isRespectful = true
  ),
  ANOINT_GUARDIAN_MURTI(
    id = "anoint_murti",
    title = "Leave Sacred Vibhuti Offering at Murti",
    sanskritLabel = "विभूति अर्पण",
    favorDelta = 16,
    grantedBuff = DivinePassiveBuff.SARASWATI_SMRITI_WARD,
    isRespectful = true
  ),
  BOW_BEFORE_GARBHAGRIHA_PILLAR(
    id = "bow_pillar",
    title = "Perform Circumambulation (Pradakshina) Respectfully",
    sanskritLabel = "प्रदक्षिणा सम्मान",
    favorDelta = 20,
    grantedBuff = DivinePassiveBuff.RUDRA_ABHAYA_MANTLE,
    isRespectful = true
  ),
  SHATTER_VOTIVE_URN_FOR_LOOT(
    id = "shatter_urn",
    title = "Smash Consecrated Urn for Quick Ghee & Salvage",
    sanskritLabel = "कलश भञ्जन (लोभ)",
    favorDelta = -22,
    grantedBuff = null,
    isRespectful = false
  ),
  PLUNDER_ALTAR_OFFERINGS(
    id = "plunder_altar",
    title = "Plunder Shrine Offerings for Instant Vitality",
    sanskritLabel = "नैवेद्य हरण (अपमान)",
    favorDelta = -25,
    grantedBuff = null,
    isRespectful = false
  )
}

/**
 * Represents an interactive Sacred Reliquary / Votive Object in the temple where the player faces
 * a moral choice: venerate/preserve it for God's Favor & hazard immunity, or smash/plunder it
 * for immediate material gain at the cost of divine wrath (Deva Kopa).
 */
data class SacredMoralShrineNode(
  val id: String,
  val templeId: String,
  val name: String,
  val sanskritName: String,
  val tileX: Int,
  val tileY: Int,
  val lorePrompt: String,
  val respectfulAction: MoralActionType,
  val respectfulSummary: String,
  val desecrateAction: MoralActionType,
  val desecrateSummary: String,
  val desecrateFuelBonus: Float = 45f,
  val desecratePranaBonus: Int = 20
)

/**
 * Result of performing a moral action or praying at a shrine.
 */
data class GodsFavorOutcome(
  val action: MoralActionType,
  val updatedFavorScore: Int,
  val standing: GodsFavorStanding,
  val activatedBuff: DivinePassiveBuff?,
  val buffDurationSeconds: Int,
  val pranaDelta: Int,
  val sanityDelta: Int,
  val fuelDelta: Float,
  val message: String
)

/**
 * Manages the 'God's Favor' (Deva Anugraha) moral choice & passive hazard immunity system.
 *
 * Players earn God's Favor by performing respectful actions (praying at shrines, sparing consecrated
 * Kalasha vessels, leaving offerings at Murtis) which grant temporary passive buffs and immunity
 * against specific environmental hazards. Conversely, smashing sacred vessels or plundering offerings
 * yields quick resources (oil/Prana) but strips active blessings and incurs Deva Kopa.
 */
class GodsFavorManager(
  val templeId: String = "konark",
  initialFavor: Int = 55
) {
  /** Favor score from 0 (Deva Kopa) to 100 (Parama Anugraha). Starts at 55 (Neutral/Reverent threshold). */
  var favorScore by mutableIntStateOf(initialFavor.coerceIn(0, 100))
    private set

  /** Current moral standing derived from [favorScore]. */
  val standing: GodsFavorStanding
    get() = when {
      favorScore >= GodsFavorStanding.EXALTED_ANUGRAHA.minScore -> GodsFavorStanding.EXALTED_ANUGRAHA
      favorScore >= GodsFavorStanding.REVERENT_DEVOTEE.minScore -> GodsFavorStanding.REVERENT_DEVOTEE
      favorScore >= GodsFavorStanding.NEUTRAL_SEEKER.minScore -> GodsFavorStanding.NEUTRAL_SEEKER
      else -> GodsFavorStanding.TRANSGRESSOR_DOSHA
    }

  /** Map of currently active DivinePassiveBuffs to their remaining duration in seconds. */
  val activeBuffSeconds = mutableStateMapOf<DivinePassiveBuff, Int>()

  /** Count of respectful actions performed during this expedition. */
  var respectfulActionsCount by mutableIntStateOf(0)
    private set

  /** Count of desecrations committed during this expedition. */
  var desecrationsCount by mutableIntStateOf(0)
    private set

  /** Total environmental hazard instances neutralized by active God's Favor buffs. */
  var hazardsNeutralizedCount by mutableIntStateOf(0)
    private set

  /** Set of SacredMoralShrineNode IDs that have already been resolved (spared or desecrated). */
  val resolvedNodeIds = mutableStateListOf<String>()

  /** Controls visibility of the God's Favor & Moral Sanctuary Dialog. */
  var isOverlayOpen by mutableStateOf(false)
    private set

  /** Currently focused SacredMoralShrineNode in the dialog. */
  var focusedNode by mutableStateOf<SacredMoralShrineNode?>(null)
    private set

  /** Last status message from God's Favor actions. */
  var lastFavorMessage by mutableStateOf(
    "Deva Anugraha (${initialFavor}%): Pray at shrines or spare sacred vessels to earn hazard immunity."
  )
    private set

  val templeNodes: List<SacredMoralShrineNode> =
    TEMPLE_MORAL_NODES[templeId] ?: TEMPLE_MORAL_NODES["konark"].orEmpty()

  fun openOverlay(node: SacredMoralShrineNode? = null) {
    focusedNode = node ?: templeNodes.firstOrNull { !resolvedNodeIds.contains(it.id) } ?: templeNodes.firstOrNull()
    isOverlayOpen = true
  }

  fun closeOverlay() {
    isOverlayOpen = false
  }

  /**
   * Finds a nearby unresolved SacredMoralShrineNode within 1.85 tiles of the player.
   */
  fun findNearbyMoralNode(playerX: Double, playerY: Double): SacredMoralShrineNode? {
    return templeNodes
      .filter { !resolvedNodeIds.contains(it.id) }
      .minByOrNull { hypot((it.tileX + 0.5) - playerX, (it.tileY + 0.5) - playerY) }
      ?.takeIf { hypot((it.tileX + 0.5) - playerX, (it.tileY + 0.5) - playerY) <= 1.85 }
  }

  /**
   * Checks whether the player currently has passive immunity against [hazard].
   */
  fun hasHazardImmunity(hazard: SanctumEnvironmentalHazard): Boolean {
    if (favorScore >= 90) return true // Exalted pinnacle grace shields against all temple hazards
    return activeBuffSeconds.entries.any { (buff, remainingSec) ->
      remainingSec > 0 && buff.protectedHazard == hazard
    }
  }

  /**
   * Checks whether a specific [buff] is currently active.
   */
  fun isBuffActive(buff: DivinePassiveBuff): Boolean {
    return (activeBuffSeconds[buff] ?: 0) > 0
  }

  /**
   * Primary active buff for HUD display (longest remaining duration).
   */
  val primaryActiveBuff: Pair<DivinePassiveBuff, Int>?
    get() = activeBuffSeconds.entries
      .filter { it.value > 0 }
      .maxByOrNull { it.value }
      ?.let { it.key to it.value }

  /**
   * Performs a respectful or desecrating action and updates God's Favor & active passive buffs.
   */
  fun performMoralAction(
    action: MoralActionType,
    nodeId: String? = null
  ): GodsFavorOutcome {
    if (nodeId != null && !resolvedNodeIds.contains(nodeId)) {
      resolvedNodeIds.add(nodeId)
    }

    favorScore = (favorScore + action.favorDelta).coerceIn(0, 100)

    if (action.isRespectful) {
      respectfulActionsCount++
      val buff = action.grantedBuff ?: DivinePassiveBuff.SURYA_TEJAS_AEGIS
      val bonusSec = if (standing == GodsFavorStanding.EXALTED_ANUGRAHA) 15 else 0
      val totalDuration = (activeBuffSeconds[buff] ?: 0) + buff.defaultDurationSeconds + bonusSec
      activeBuffSeconds[buff] = totalDuration.coerceAtMost(120)

      val msg = "🙏 ${action.title} (+${action.favorDelta} Favor) • Granted [${buff.title}] (${activeBuffSeconds[buff]}s immunity to ${buff.protectedHazard.displayName})!"
      lastFavorMessage = msg
      return GodsFavorOutcome(
        action = action,
        updatedFavorScore = favorScore,
        standing = standing,
        activatedBuff = buff,
        buffDurationSeconds = activeBuffSeconds[buff] ?: totalDuration,
        pranaDelta = 8,
        sanityDelta = 10,
        fuelDelta = 12f,
        message = msg
      )
    } else {
      desecrationsCount++
      // Desecrating temple objects strips active divine buffs and angers the sanctum
      activeBuffSeconds.clear()
      val pranaGain = if (action == MoralActionType.PLUNDER_ALTAR_OFFERINGS) 22 else 10
      val fuelGain = if (action == MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT) 45f else 25f
      val msg = "⚠ ${action.title} (${action.favorDelta} Favor) • Active divine blessings lost! Deva Kopa intensifies hazards!"
      lastFavorMessage = msg
      return GodsFavorOutcome(
        action = action,
        updatedFavorScore = favorScore,
        standing = standing,
        activatedBuff = null,
        buffDurationSeconds = 0,
        pranaDelta = pranaGain,
        sanityDelta = -14,
        fuelDelta = fuelGain,
        message = msg
      )
    }
  }

  /**
   * Called once per second in the main game loop to tick down active buff durations
   * and evaluate passive hazard protection.
   */
  fun onSecondTick(
    diyaLit: Boolean,
    environmentalState: EnvironmentalEffectState,
    entityDistance: Double
  ): Int {
    // Decrement active buff timers
    val currentKeys = activeBuffSeconds.keys.toList()
    for (buff in currentKeys) {
      val rem = (activeBuffSeconds[buff] ?: 0) - 1
      if (rem <= 0) {
        activeBuffSeconds.remove(buff)
      } else {
        activeBuffSeconds[buff] = rem
      }
    }

    // Track neutralized hazards
    if (!diyaLit && hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN)) {
      hazardsNeutralizedCount++
    }
    if (environmentalState == EnvironmentalEffectState.FALLING_MASONRY_DUST &&
      hasHazardImmunity(SanctumEnvironmentalHazard.SUFFOCATING_MASONRY_SPORES)
    ) {
      hazardsNeutralizedCount++
    }
    if (environmentalState == EnvironmentalEffectState.COBWEB_BRUSH &&
      hasHazardImmunity(SanctumEnvironmentalHazard.CURSED_COBWEB_SNARE)
    ) {
      hazardsNeutralizedCount++
    }
    if (entityDistance < 5.0 &&
      hasHazardImmunity(SanctumEnvironmentalHazard.SHADOW_AURA_CORRUPTION)
    ) {
      hazardsNeutralizedCount++
    }

    // Passive Prana regeneration when in Exalted Grace, or Prana penalty if in Deva Kopa near hazards
    return when {
      standing == GodsFavorStanding.EXALTED_ANUGRAHA -> 2
      standing == GodsFavorStanding.REVERENT_DEVOTEE && activeBuffSeconds.isNotEmpty() -> 1
      standing == GodsFavorStanding.TRANSGRESSOR_DOSHA &&
        environmentalState != EnvironmentalEffectState.STILL_AIR -> -2
      else -> 0
    }
  }

  /**
   * Multiplier on Diya torch fuel consumption based on God's Favor & Surya Tejas Aegis.
   */
  val torchFuelDrainMultiplier: Float
    get() = when {
      hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN) -> 0.5f
      standing == GodsFavorStanding.TRANSGRESSOR_DOSHA -> 1.35f
      else -> 1.0f
    }

  /**
   * Multiplier on creature shadow strike damage based on Rudra Abhaya Mantle & standing.
   */
  val entityStrikeDamageMultiplier: Float
    get() = when {
      hasHazardImmunity(SanctumEnvironmentalHazard.SHADOW_AURA_CORRUPTION) -> 0.55f
      standing == GodsFavorStanding.EXALTED_ANUGRAHA -> 0.75f
      standing == GodsFavorStanding.TRANSGRESSOR_DOSHA -> 1.25f
      else -> 1.0f
    }
}

private val TEMPLE_MORAL_NODES: Map<String, List<SacredMoralShrineNode>> = mapOf(
  "konark" to listOf(
    SacredMoralShrineNode(
      id = "konark_surya_kalasha",
      templeId = "konark",
      name = "Gilded Surya Kalasha Pedestal",
      sanskritName = "स्वर्ण सूर्य कलश पीठ",
      tileX = 3,
      tileY = 1,
      lorePrompt = "An untouched 13th-century copper Kalasha brimming with consecrated sesame oil stands beside the Sun Chariot pillar. You can bow in reverence to receive Vayu's breath shield, or smash the seal to siphon its oil.",
      respectfulAction = MoralActionType.SPARE_SACRED_KALASHA,
      respectfulSummary = "Preserve the Kalasha (+16 Favor, Vayu Prana Purity: Immunity to Choking Masonry Dust)",
      desecrateAction = MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT,
      desecrateSummary = "Smash the vessel (+45% Torch Oil immediately, -22 Favor & strips divine buffs)"
    ),
    SacredMoralShrineNode(
      id = "konark_navagraha_shrine",
      templeId = "konark",
      name = "Navagraha Sanctuary Shrine",
      sanskritName = "नवग्रह पूजा वेदी",
      tileX = 6,
      tileY = 3,
      lorePrompt = "Nine planetary deities are carved in black chlorite above an incense brazier. Offering a silent Pranam aligns your aura with Surya's light; plundering the silver offerings restores Prana but invites Deva Kopa.",
      respectfulAction = MoralActionType.PRAY_AT_SHRINE,
      respectfulSummary = "Pray at the Navagraha Altar (+18 Favor, Surya Tejas Aegis: Immunity to Darkness Drain)",
      desecrateAction = MoralActionType.PLUNDER_ALTAR_OFFERINGS,
      desecrateSummary = "Plunder the votive offerings (+22% Prana immediately, -25 Favor & incurs Deva Kopa)"
    ),
    SacredMoralShrineNode(
      id = "konark_nata_mandira_murti",
      templeId = "konark",
      name = "Nata-Mandira Dancer Relief",
      sanskritName = "नटमन्दिर शिल्प मूर्ति",
      tileX = 8,
      tileY = 7,
      lorePrompt = "Silk cobwebs drape a celestial Apsara relief. Gently anointing its forehead with Vibhuti ash awakens Saraswati's ward against cursed cobwebs.",
      respectfulAction = MoralActionType.ANOINT_GUARDIAN_MURTI,
      respectfulSummary = "Anoint the Murti (+16 Favor, Saraswati Smriti Ward: Immunity to Cursed Cobweb Snares)",
      desecrateAction = MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT,
      desecrateSummary = "Break the alcove urn (+45% Torch Oil, -22 Favor)"
    )
  ),
  "kailasa" to listOf(
    SacredMoralShrineNode(
      id = "kailasa_nandi_mandapa",
      templeId = "kailasa",
      name = "Monolithic Nandi Shrine",
      sanskritName = "एकाश्म नन्दी मण्डप",
      tileX = 2,
      tileY = 2,
      lorePrompt = "The sacred bull Nandi gazes eternally toward the Garbhagriha. Performing a respectful Pranam grants Rudra's mantle against stalking shadow entities.",
      respectfulAction = MoralActionType.BOW_BEFORE_GARBHAGRIHA_PILLAR,
      respectfulSummary = "Perform Pradakshina (+20 Favor, Rudra Abhaya Mantle: Immunity to Shadow Aura Corruption)",
      desecrateAction = MoralActionType.PLUNDER_ALTAR_OFFERINGS,
      desecrateSummary = "Loot the brass offering tray (+22% Prana, -25 Favor)"
    ),
    SacredMoralShrineNode(
      id = "kailasa_bhasma_urn",
      templeId = "kailasa",
      name = "Shaiva Vibhuti Kalasha",
      sanskritName = "शैव भस्म कलश",
      tileX = 7,
      tileY = 5,
      lorePrompt = "A sealed basalt urn of sacred Vibhuti ash rests in an alcove. Preserving its sanctity wards off choking rock-cut dust.",
      respectfulAction = MoralActionType.SPARE_SACRED_KALASHA,
      respectfulSummary = "Venerate the Vibhuti Urn (+16 Favor, Vayu Prana Purity: Immunity to Falling Masonry Dust)",
      desecrateAction = MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT,
      desecrateSummary = "Shatter the urn for lamp oil (+45% Torch Oil, -22 Favor)"
    )
  ),
  "brihadeeswarar" to listOf(
    SacredMoralShrineNode(
      id = "chola_dakshinamurti_shrine",
      templeId = "brihadeeswarar",
      name = "Chola Dakshinamurti Alcove",
      sanskritName = "दक्षिणामूर्ति ज्ञान पीठ",
      tileX = 3,
      tileY = 2,
      lorePrompt = "An ancient Chola bronze lamp burns faintly before Dakshinamurti. Praying in stillness shields your mind from cursed inscriptions and cobweb snares.",
      respectfulAction = MoralActionType.ANOINT_GUARDIAN_MURTI,
      respectfulSummary = "Anoint & Pray (+16 Favor, Saraswati Smriti Ward: Immunity to Cursed Cobwebs & Whispers)",
      desecrateAction = MoralActionType.PLUNDER_ALTAR_OFFERINGS,
      desecrateSummary = "Seize the Chola offerings (+22% Prana, -25 Favor)"
    ),
    SacredMoralShrineNode(
      id = "chola_vimana_kalasha",
      templeId = "brihadeeswarar",
      name = "Consecrated Brihat Kalasha",
      sanskritName = "बृहत् ताम्र कलश",
      tileX = 8,
      tileY = 5,
      lorePrompt = "A royal Raja Raja Chola ceremonial vessel stands unblemished. Sparing it bestows Surya Tejas immunity against darkness drain.",
      respectfulAction = MoralActionType.PRAY_AT_SHRINE,
      respectfulSummary = "Offer Pranam (+18 Favor, Surya Tejas Aegis: Immunity to Darkness Drain)",
      desecrateAction = MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT,
      desecrateSummary = "Smash the copper vessel (+45% Torch Oil, -22 Favor)"
    )
  ),
  "meenakshi" to listOf(
    SacredMoralShrineNode(
      id = "meenakshi_golden_lotus_shrine",
      templeId = "meenakshi",
      name = "Porthamarai Kulam Lotus Altar",
      sanskritName = "स्वर्ण कमल तीर्थ वेदी",
      tileX = 3,
      tileY = 3,
      lorePrompt = "A golden lotus effigy gleams beside the subterranean tank. Praying respectfully grants immunity against darkness drain and slows Diya fuel consumption.",
      respectfulAction = MoralActionType.PRAY_AT_SHRINE,
      respectfulSummary = "Pray at the Golden Lotus (+18 Favor, Surya Tejas Aegis: Immunity to Darkness Drain)",
      desecrateAction = MoralActionType.PLUNDER_ALTAR_OFFERINGS,
      desecrateSummary = "Plunder the lotus pearls (+22% Prana, -25 Favor)"
    ),
    SacredMoralShrineNode(
      id = "meenakshi_sundareshwarar_pillar",
      templeId = "meenakshi",
      name = "Musical Yali Pillar Shrine",
      sanskritName = "सङ्गीत याली स्तम्भ",
      tileX = 9,
      tileY = 5,
      lorePrompt = "Carved mythical Yali guardians watch over a row of clay oil vessels. Leaving them intact invokes Rudra's protection against the Vetala's shadow aura.",
      respectfulAction = MoralActionType.BOW_BEFORE_GARBHAGRIHA_PILLAR,
      respectfulSummary = "Bow before the Yali Pillar (+20 Favor, Rudra Abhaya Mantle: Immunity to Shadow Aura)",
      desecrateAction = MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT,
      desecrateSummary = "Smash the clay vessels for ghee (+45% Torch Oil, -22 Favor)"
    )
  )
)

/**
 * Interactive Modal for the God's Favor (Deva Anugraha) Moral Choice & Passive Buff System.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GodsFavorSanctuaryDialog(
  favorManager: GodsFavorManager,
  isHindi: Boolean = false,
  onPerformAction: (MoralActionType, String?) -> Unit,
  onDismiss: () -> Unit
) {
  val standing = favorManager.standing
  val standingColor = when (standing) {
    GodsFavorStanding.EXALTED_ANUGRAHA -> MantraJade
    GodsFavorStanding.REVERENT_DEVOTEE -> MoltenBrass
    GodsFavorStanding.NEUTRAL_SEEKER -> SacredTeal
    GodsFavorStanding.TRANSGRESSOR_DOSHA -> BloodAltarRed
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .padding(16.dp)
      .testTag("gods_favor_dialog")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(22.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, standingColor, RoundedCornerShape(22.dp))
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isHindi) "देव अनुग्रह एवं धर्म निर्णय (GOD'S FAVOR)" else "GOD'S FAVOR • DEVA ANUGRAHA",
              style = MaterialTheme.typography.titleMedium,
              color = standingColor,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${standing.sanskritTitle} • Favor Score: ${favorManager.favorScore}%",
              style = MaterialTheme.typography.labelLarge,
              color = ParchmentCream
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_gods_favor_dialog_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close God's Favor Dialog",
              tint = ParchmentCream
            )
          }
        }

        // Favor Progress Bar & Moral Summary
        Surface(
          color = ObsidianVoid.copy(alpha = 0.72f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "MORAL STANDING: ${standing.hudBadge}",
                style = MaterialTheme.typography.labelLarge,
                color = standingColor,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Reverent: ${favorManager.respectfulActionsCount} • Desecrated: ${favorManager.desecrationsCount}",
                style = MaterialTheme.typography.labelLarge,
                color = SandstoneMuted
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { (favorManager.favorScore / 100f).coerceIn(0f, 1f) },
              color = standingColor,
              trackColor = SacredCardSurface,
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("gods_favor_progress_bar")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = standing.description,
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentCream
            )
          }
        }

        // Active Divine Passive Buffs & Hazard Immunities Section
        Surface(
          color = SacredCardSurface.copy(alpha = 0.85f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MoltenBrass.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "ACTIVE PASSIVE BUFFS & HAZARD IMMUNITIES",
                style = MaterialTheme.typography.labelLarge,
                color = MoltenBrass,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Blocked: ${favorManager.hazardsNeutralizedCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MantraJade
              )
            }

            DivinePassiveBuff.entries.forEach { buff ->
              val remaining = favorManager.activeBuffSeconds[buff] ?: 0
              val isImmune = favorManager.hasHazardImmunity(buff.protectedHazard)
              val rowColor = if (isImmune) MantraJade else SandstoneMuted
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(ObsidianVoid.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("buff_status_row_${buff.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${buff.title} (${buff.sanskritName})",
                    style = MaterialTheme.typography.labelLarge,
                    color = rowColor,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Shields: ${buff.protectedHazard.displayName} — ${buff.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentCream.copy(alpha = 0.85f)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = if (isImmune) MantraJade.copy(alpha = 0.2f) else ObsidianVoid,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = if (remaining > 0) "${remaining}s" else if (isImmune) "EXALTED" else "DORMANT",
                    style = MaterialTheme.typography.labelLarge,
                    color = rowColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }
          }
        }

        // Quick Devotional Prayer Actions (Always Available)
        Text(
          text = "DEVOTIONAL SHRINE PRAYERS & VOWS (GRANT PASSIVE IMMUNITY)",
          style = MaterialTheme.typography.labelLarge,
          color = SaffronFlame,
          fontWeight = FontWeight.Bold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onPerformAction(MoralActionType.PRAY_AT_SHRINE, null) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MantraJade,
              contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("pray_at_shrine_action_button")
          ) {
            Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "PRAY AT SHRINE (+SURYA AEGIS)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Button(
            onClick = { onPerformAction(MoralActionType.BOW_BEFORE_GARBHAGRIHA_PILLAR, null) },
            colors = ButtonDefaults.buttonColors(
              containerColor = SacredTeal,
              contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("pradakshina_vow_action_button")
          ) {
            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "PRADAKSHINA (+RUDRA MANTLE)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Moral Dilemma Reliquary Nodes in Current Temple
        val activeNode = favorManager.focusedNode ?: favorManager.templeNodes.firstOrNull()
        activeNode?.let { node ->
          val isResolved = favorManager.resolvedNodeIds.contains(node.id)
          Surface(
            color = ObsidianVoid.copy(alpha = 0.80f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, SaffronFlame.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "SANCTUM MORAL DILEMMA: ${node.name.uppercase()}",
                  style = MaterialTheme.typography.labelLarge,
                  color = MoltenBrass,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (isResolved) "RESOLVED" else "TILE (${node.tileX},${node.tileY})",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (isResolved) MantraJade else SaffronFlame
                )
              }

              Text(
                text = node.lorePrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream
              )

              // Choice 1: Respectful Preservation / Prayer
              Button(
                onClick = { onPerformAction(node.respectfulAction, node.id) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MantraJade,
                  contentColor = Color.Black
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("moral_choice_respectful_button")
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = node.respectfulSummary,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
              }

              // Choice 2: Desecrate / Smash for Immediate Gain
              Button(
                onClick = { onPerformAction(node.desecrateAction, node.id) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = BloodAltarRed,
                  contentColor = ParchmentCream
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("moral_choice_desecrate_button")
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = node.desecrateSummary,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
      }
    }
  }
}
