package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Types of sequential ritual actions that a player can perform in a [RitualSequenceDefinition].
 */
enum class RitualActionType(
  val label: String,
  val sanskritLabel: String
) {
  IGNITE_DIRECTIONAL_LAMP(
    label = "Ignite Directional Diya",
    sanskritLabel = "दिशा दीप प्रज्वलन"
  ),
  RING_SACRED_GHANTA(
    label = "Ring Saptadhatu Ghanta",
    sanskritLabel = "सप्तधातु घण्टा नाद"
  ),
  CHANT_VEDIC_MANTRA(
    label = "Chant Vedic Mantra",
    sanskritLabel = "वैदिक मन्त्र जप"
  ),
  TRACE_YANTRA_SEAL(
    label = "Consecrate Yantra Seal",
    sanskritLabel = "यन्त्र मुद्रा संस्कार"
  )
}

/**
 * Represents a single ordered step in a sacred sequence required to unseal a secret chamber.
 */
data class RitualSequenceStep(
  val stepIndex: Int,
  val actionType: RitualActionType,
  val title: String,
  val sanskritGlyph: String,
  val cardinalLabel: String,
  val targetCoord: Pair<Int, Int>? = null,
  val hintText: String
)

/**
 * Defines a temple-specific ritual sequence and the secret chamber (`Gupta Garbhagriha`) it unlocks.
 */
data class RitualSequenceDefinition(
  val id: String,
  val templeId: String,
  val ritualName: String,
  val sanskritName: String,
  val secretChamberName: String,
  val secretChamberSubtitle: String,
  val loreInscribedClue: String,
  val secretDoorTileCoord: Pair<Int, Int>,
  val secretChamberInteriorCoord: Pair<Int, Int>,
  val steps: List<RitualSequenceStep>,
  val pranaReward: Int = 30,
  val sanityReward: Int = 25,
  val fuelReward: Float = 45f,
  val stunTicksReward: Int = 45
)

/**
 * Summary state of the player's ritual progression for HUD telemetry.
 */
enum class RitualProgressState(
  val badgeLabel: String
) {
  DORMANT("CHAMBER SEALED"),
  IN_PROGRESS("RITUAL ACTIVE"),
  CHAMBER_UNLOCKED("CHAMBER UNSEALED"),
  SANCTUARY_CLAIMED("RELIC CLAIMED")
}

/**
 * Outcome returned whenever a ritual step is performed (either in the 3D world or via the Ritual Altar UI).
 */
data class RitualStepOutcome(
  val isStepCorrect: Boolean,
  val completedStepIndex: Int,
  val totalSteps: Int,
  val didUnlockSecretChamber: Boolean,
  val message: String,
  val sanityDelta: Int = 0,
  val pranaDelta: Int = 0,
  val fuelDelta: Float = 0f,
  val stunTicks: Int = 0,
  val grantsRelic: Boolean = false
)

/**
 * Catalog of temple-specific ordered Ritual Sequences that unseal hidden chambers (`Gupta Garbhagriha`).
 * Notice that the coordinates of the directional lamps (`'2'`) and seals (`'3'`) match each temple's `mapGrid`,
 * and `secretDoorTileCoord` targets an interior stone wall (`'1'`) that dynamically parts into a secret doorway!
 */
val TEMPLE_RITUAL_CATALOG: List<RitualSequenceDefinition> = listOf(
  // 1. Brihadeeswarar ("brihadeeswarar"): Pradakshina Clockwise Lamp Ignition + Ghanta + Mantra
  RitualSequenceDefinition(
    id = "ritual_chola_pradakshina",
    templeId = "brihadeeswarar",
    ritualName = "Ashta-Dikpala Pradakshina Aarti",
    sanskritName = "अष्टदिक्पाल प्रदक्षिणा आरती",
    secretChamberName = "Rajaraja Gupta Koshagara (Chola Secret Treasury)",
    secretChamberSubtitle = "गुप्त कोषागार • Hidden Vault Beneath the Vimana",
    loreInscribedClue = "“Kindle the North-Central, West-Mandapa, and East-Mandapa Agni Diyas in sacred Pradakshina order, sound the 528Hz Saptadhatu Ghanta, and invoke the Maha Mrityunjaya Mantra to part the southern sanctum wall.”",
    secretDoorTileCoord = 5 to 6,
    secretChamberInteriorCoord = 5 to 7,
    steps = listOf(
      RitualSequenceStep(
        stepIndex = 0,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Someshwara (North) Sconce",
        sanskritGlyph = "उत्तर दीप (१)",
        cardinalLabel = "North Lamp (4,1)",
        targetCoord = 4 to 1,
        hintText = "First, light the North brass Diya sconce at (4,1)."
      ),
      RitualSequenceStep(
        stepIndex = 1,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Varuna (West-Central) Sconce",
        sanskritGlyph = "वरुण दीप (२)",
        cardinalLabel = "West Lamp (4,4)",
        targetCoord = 4 to 4,
        hintText = "Second, descend to the central Mandapa and kindle the West Diya at (4,4)."
      ),
      RitualSequenceStep(
        stepIndex = 2,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Agni (South-East) Sconce",
        sanskritGlyph = "अग्नि दीप (३)",
        cardinalLabel = "SE Lamp (9,7)",
        targetCoord = 9 to 7,
        hintText = "Third, cross the pillared hall and kindle the South-East Diya at (9,7)."
      ),
      RitualSequenceStep(
        stepIndex = 3,
        actionType = RitualActionType.RING_SACRED_GHANTA,
        title = "Resonate Saptadhatu Ghanta Bell",
        sanskritGlyph = "घण्टा नाद (४)",
        cardinalLabel = "528Hz Bell",
        targetCoord = null,
        hintText = "Fourth, ring the Saptadhatu Ghanta bell to awaken the stone resonance lock."
      ),
      RitualSequenceStep(
        stepIndex = 4,
        actionType = RitualActionType.CHANT_VEDIC_MANTRA,
        title = "Chant Mrityunjaya Kavach",
        sanskritGlyph = "मन्त्र सिद्धि (५)",
        cardinalLabel = "Vedic Chant",
        targetCoord = null,
        hintText = "Fifth, chant the sacred Mantra to unseal the Chola Secret Treasury at (5,6)!"
      )
    )
  ),

  // 2. Konark Sun Crypt ("konark"): Sapta-Ashwa Solar Chariot Sequence
  RitualSequenceDefinition(
    id = "ritual_konark_surya_kram",
    templeId = "konark",
    ritualName = "Sapta-Ashwa Surya Ratha Kram",
    sanskritName = "सप्ताश्व सूर्य रथ प्रज्वलन",
    secretChamberName = "Chumbaka Kalasha Chamber (Lodestone Crown Vault)",
    secretChamberSubtitle = "चुम्बक कलश कक्ष • Secret Magnetic Sanctuary of Arkakshetra",
    loreInscribedClue = "“Follow the rising sun across the Black Pagoda: kindle the Dawn (NW) and Zenith (NE) Solar Diyas, align the Eastern Surya Chakra Seal, sound the Solar Ghanta, and kindle the Dusk (SE) Diya.”",
    secretDoorTileCoord = 5 to 4,
    secretChamberInteriorCoord = 5 to 5,
    steps = listOf(
      RitualSequenceStep(
        stepIndex = 0,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Udaya (Dawn NW) Solar Lamp",
        sanskritGlyph = "उदय सूर्य दीप (१)",
        cardinalLabel = "NW Lamp (3,1)",
        targetCoord = 3 to 1,
        hintText = "First, ignite the Dawn (North-West) Solar Lamp at (3,1)."
      ),
      RitualSequenceStep(
        stepIndex = 1,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Madhyahna (Zenith NE) Lamp",
        sanskritGlyph = "मध्याह्न दीप (२)",
        cardinalLabel = "NE Lamp (9,1)",
        targetCoord = 9 to 1,
        hintText = "Second, ignite the Zenith (North-East) Solar Lamp at (9,1)."
      ),
      RitualSequenceStep(
        stepIndex = 2,
        actionType = RitualActionType.TRACE_YANTRA_SEAL,
        title = "Consecrate Lotus Wheel Seal",
        sanskritGlyph = "अर्क चक्र मुद्रा (३)",
        cardinalLabel = "East Seal (9,3)",
        targetCoord = 9 to 3,
        hintText = "Third, consecrate the Solar Wheel Yantra Seal at (9,3)."
      ),
      RitualSequenceStep(
        stepIndex = 3,
        actionType = RitualActionType.RING_SACRED_GHANTA,
        title = "Sound Arkakshetra Ghanta",
        sanskritGlyph = "सूर्य घण्टा (४)",
        cardinalLabel = "528Hz Bell",
        targetCoord = null,
        hintText = "Fourth, ring the sacred Ghanta bell to harmonize the magnetic lodestone."
      ),
      RitualSequenceStep(
        stepIndex = 4,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Sandhya (Dusk SE) Lamp",
        sanskritGlyph = "सन्ध्या दीप (५)",
        cardinalLabel = "SE Lamp (9,9)",
        targetCoord = 9 to 9,
        hintText = "Fifth, ignite the Dusk (South-East) Solar Lamp at (9,9) to open the Lodestone Vault at (5,4)!"
      )
    )
  ),

  // 3. Kailasa Monolith Abyss ("kailasa"): Bhairava Panchagni Monolith Sequence
  RitualSequenceDefinition(
    id = "ritual_kailasa_panchagni",
    templeId = "kailasa",
    ritualName = "Kailasa Panchagni Monolith Kram",
    sanskritName = "कैलास पञ्चाग्नि तान्त्रिक क्रम",
    secretChamberName = "Rashtrakuta Patala Guha (Subterranean Monolith Vault)",
    secretChamberSubtitle = "पाताल गुहा • Sealed Chamber Beneath Cave 16",
    loreInscribedClue = "“Carved top-down from living basalt: kindle the North-West and North-East cliff Diyas in sequence, unseal the North-West Monolith Yantra, strike the Saptadhatu Ghanta, and chant the Rudra Mantra to open the central monolith wall.”",
    secretDoorTileCoord = 5 to 4,
    secretChamberInteriorCoord = 5 to 5,
    steps = listOf(
      RitualSequenceStep(
        stepIndex = 0,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Vayavya Cliff Diya",
        sanskritGlyph = "वायव्य दीप (१)",
        cardinalLabel = "NW Lamp (2,1)",
        targetCoord = 2 to 1,
        hintText = "First, ignite the North-West Cliff Diya at (2,1)."
      ),
      RitualSequenceStep(
        stepIndex = 1,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Ishanya Cliff Diya",
        sanskritGlyph = "ईशान्य दीप (२)",
        cardinalLabel = "NE Lamp (9,1)",
        targetCoord = 9 to 1,
        hintText = "Second, ignite the North-East Cliff Diya at (9,1)."
      ),
      RitualSequenceStep(
        stepIndex = 2,
        actionType = RitualActionType.TRACE_YANTRA_SEAL,
        title = "Consecrate Kailasa Monolith Yantra",
        sanskritGlyph = "कैलास चक्र (३)",
        cardinalLabel = "NW Seal (3,3)",
        targetCoord = 3 to 3,
        hintText = "Third, consecrate the Monolith Yantra Seal at (3,3)."
      ),
      RitualSequenceStep(
        stepIndex = 3,
        actionType = RitualActionType.RING_SACRED_GHANTA,
        title = "Strike Dandapani Ghanta Bell",
        sanskritGlyph = "दण्डपाणि नाद (४)",
        cardinalLabel = "528Hz Bell",
        targetCoord = null,
        hintText = "Fourth, ring the Saptadhatu Ghanta to shatter the Vetala shadow ward."
      ),
      RitualSequenceStep(
        stepIndex = 4,
        actionType = RitualActionType.CHANT_VEDIC_MANTRA,
        title = "Intone Rudra-Abhisheka Mantra",
        sanskritGlyph = "रुद्र मन्त्र (५)",
        cardinalLabel = "Vedic Chant",
        targetCoord = null,
        hintText = "Fifth, chant the sacred Mantra to open the Subterranean Monolith Vault at (5,4)!"
      )
    )
  ),

  // 4. Meenakshi Thousand-Pillar Labyrinth ("meenakshi"): Sapta-Swara Musical Pillar Kram
  RitualSequenceDefinition(
    id = "ritual_meenakshi_swara_kram",
    templeId = "meenakshi",
    ritualName = "Ayiram-Kaal Sapta-Swara Aarti",
    sanskritName = "आइराम-काल सप्तस्वर प्रज्वलन",
    secretChamberName = "Porthamarai Kulam Gupta Mandapam (Golden Lotus Vault)",
    secretChamberSubtitle = "स्वर्ण कमल मण्डप • Subterranean Lotus Sanctuary of Madurai",
    loreInscribedClue = "“In the Hall of a Thousand Musical Pillars, kindle the North-West and North-East Lotus Diyas, consecrate the West Swara Yantra Seal, strike the 528Hz Ghanta, and chant the Panchakshara Mantra to part the central Mandapa wall.”",
    secretDoorTileCoord = 5 to 4,
    secretChamberInteriorCoord = 5 to 5,
    steps = listOf(
      RitualSequenceStep(
        stepIndex = 0,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Shadja (NW Lotus) Lamp",
        sanskritGlyph = "षड्ज दीप (१)",
        cardinalLabel = "NW Lamp (3,1)",
        targetCoord = 3 to 1,
        hintText = "First, ignite the North-West Lotus Lamp at (3,1)."
      ),
      RitualSequenceStep(
        stepIndex = 1,
        actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
        title = "Ignite Panchama (NE Lotus) Lamp",
        sanskritGlyph = "पञ्चम दीप (२)",
        cardinalLabel = "NE Lamp (9,1)",
        targetCoord = 9 to 1,
        hintText = "Second, ignite the North-East Lotus Lamp at (9,1)."
      ),
      RitualSequenceStep(
        stepIndex = 2,
        actionType = RitualActionType.TRACE_YANTRA_SEAL,
        title = "Consecrate Swara-Stambha Seal",
        sanskritGlyph = "स्वर चक्र (३)",
        cardinalLabel = "West Seal (4,3)",
        targetCoord = 4 to 3,
        hintText = "Third, consecrate the Musical Pillar Yantra Seal at (4,3)."
      ),
      RitualSequenceStep(
        stepIndex = 3,
        actionType = RitualActionType.RING_SACRED_GHANTA,
        title = "Resonate Sundareshwarar Ghanta",
        sanskritGlyph = "सुन्दरेश्वर नाद (४)",
        cardinalLabel = "528Hz Bell",
        targetCoord = null,
        hintText = "Fourth, ring the sacred Ghanta bell to harmonize the musical pillars."
      ),
      RitualSequenceStep(
        stepIndex = 4,
        actionType = RitualActionType.CHANT_VEDIC_MANTRA,
        title = "Chant Meenakshi-Sundareshwara Kavach",
        sanskritGlyph = "मीनाक्षी मन्त्र (५)",
        cardinalLabel = "Vedic Chant",
        targetCoord = null,
        hintText = "Fifth, chant the sacred Mantra to open the Golden Lotus Vault at (5,4)!"
      )
    )
  )
)

/**
 * Stateful manager for the `RitualInteraction` system:
 * - Tracks ordered sequences of player actions (lighting specific lamps in order, ringing the Ghanta,
 *   chanting Vedic mantras, consecrating Yantra seals).
 * - Dynamically modifies the active temple `mapGrid` when a sequence is completed, converting the
 *   secret stone wall tile (`'1'`) into a passable Secret Chamber Doorway (`'5'`) and placing a
 *   Secret Chamber Altar (`'6'`) inside!
 * - Allows players to either perform the steps naturally in the 3D world OR perform/inspect the
 *   ritual steps directly inside the interactive `RitualInteractionOverlay`.
 */
class RitualInteractionManager(
  private val templeId: String
) {
  val activeRitual: RitualSequenceDefinition = TEMPLE_RITUAL_CATALOG.find {
    it.templeId.equals(templeId, ignoreCase = true) ||
      templeId.contains(it.templeId, ignoreCase = true)
  } ?: TEMPLE_RITUAL_CATALOG.first()

  var currentStepIndex by mutableIntStateOf(0)
    private set

  var isSecretChamberUnlocked by mutableStateOf(false)
    private set

  var isSecretSanctuaryClaimed by mutableStateOf(false)
    private set

  var isOverlayOpen by mutableStateOf(false)
    private set

  var mistakeCount by mutableIntStateOf(0)
    private set

  val completedStepIndices = mutableStateListOf<Int>()
  val stepTimestamps = mutableStateMapOf<Int, Long>()

  var statusBanner by mutableStateOf(
    "Step 1/${activeRitual.steps.size}: ${activeRitual.steps.first().hintText}"
  )
    private set

  val progressState: RitualProgressState
    get() = when {
      isSecretSanctuaryClaimed -> RitualProgressState.SANCTUARY_CLAIMED
      isSecretChamberUnlocked -> RitualProgressState.CHAMBER_UNLOCKED
      currentStepIndex > 0 -> RitualProgressState.IN_PROGRESS
      else -> RitualProgressState.DORMANT
    }

  val nextRequiredStep: RitualSequenceStep?
    get() = if (isSecretChamberUnlocked) null else activeRitual.steps.getOrNull(currentStepIndex)

  fun openOverlay() {
    isOverlayOpen = true
  }

  fun closeOverlay() {
    isOverlayOpen = false
  }

  /**
   * Produces a live modified `mapGrid` where:
   * - Once `isSecretChamberUnlocked` is true, `secretDoorTileCoord` is transformed from wall `'1'`
   *   to `'5'` (Open Secret Chamber Portal) so the player can walk directly through the parted wall,
   *   and `secretChamberInteriorCoord` is marked as `'6'` (Secret Chamber Relic Sanctuary).
   */
  fun applySecretChamberToMapGrid(baseGrid: List<String>): List<String> {
    if (!isSecretChamberUnlocked) return baseGrid
    val doorCoord = activeRitual.secretDoorTileCoord
    val interiorCoord = activeRitual.secretChamberInteriorCoord
    return baseGrid.mapIndexed { r, rowStr ->
      val chars = rowStr.toCharArray()
      if (r == doorCoord.second && doorCoord.first in chars.indices) {
        chars[doorCoord.first] = '5'
      }
      if (r == interiorCoord.second && interiorCoord.first in chars.indices) {
        if (chars[interiorCoord.first] == '0' || chars[interiorCoord.first] == '1') {
          chars[interiorCoord.first] = '6'
        }
      }
      String(chars)
    }
  }

  /**
   * Checks whether the player is standing near the Secret Chamber Door (`secretDoorTileCoord`) or
   * inside the Secret Chamber Sanctuary (`secretChamberInteriorCoord`).
   */
  fun isPlayerNearSecretChamber(playerX: Double, playerY: Double): Boolean {
    val door = activeRitual.secretDoorTileCoord
    val interior = activeRitual.secretChamberInteriorCoord
    val dDoor = hypot((door.first + 0.5) - playerX, (door.second + 0.5) - playerY)
    val dInt = hypot((interior.first + 0.5) - playerX, (interior.second + 0.5) - playerY)
    return dDoor < 1.75 || dInt < 1.75
  }

  /**
   * Called when the player performs an action in the 3D world or inside the Ritual Krama Altar UI.
   * Validates whether the action matches the current expected step in the ritual sequence.
   * - If it matches: advances `currentStepIndex` (and unlocks the secret temple chamber on the final step).
   * - If a wrong directional lamp is lit out of order: resets the ritual sequence with a feedback message
   *   so the player must follow the true Vedic sequence.
   */
  fun recordAction(
    actionType: RitualActionType,
    coord: Pair<Int, Int>? = null,
    allowWildcardCoord: Boolean = false
  ): RitualStepOutcome {
    if (isSecretChamberUnlocked) {
      return RitualStepOutcome(
        isStepCorrect = true,
        completedStepIndex = activeRitual.steps.size,
        totalSteps = activeRitual.steps.size,
        didUnlockSecretChamber = false,
        message = "${activeRitual.secretChamberName} is already unsealed at (${activeRitual.secretDoorTileCoord.first},${activeRitual.secretDoorTileCoord.second})!"
      )
    }

    val expected = activeRitual.steps.getOrNull(currentStepIndex)
      ?: return RitualStepOutcome(
        isStepCorrect = false,
        completedStepIndex = currentStepIndex,
        totalSteps = activeRitual.steps.size,
        didUnlockSecretChamber = false,
        message = "No active ritual step."
      )

    val typeMatches = expected.actionType == actionType
    val coordMatches = expected.targetCoord == null ||
      allowWildcardCoord ||
      expected.targetCoord == coord

    if (typeMatches && coordMatches) {
      completedStepIndices.add(currentStepIndex)
      stepTimestamps[currentStepIndex] = System.currentTimeMillis()
      currentStepIndex++

      if (currentStepIndex >= activeRitual.steps.size) {
        isSecretChamberUnlocked = true
        val unlockMsg =
          "RITUAL COMPLETE! ${activeRitual.secretChamberName} unsealed at (${activeRitual.secretDoorTileCoord.first},${activeRitual.secretDoorTileCoord.second})!"
        statusBanner = unlockMsg
        return RitualStepOutcome(
          isStepCorrect = true,
          completedStepIndex = currentStepIndex,
          totalSteps = activeRitual.steps.size,
          didUnlockSecretChamber = true,
          message = unlockMsg,
          sanityDelta = activeRitual.sanityReward,
          pranaDelta = activeRitual.pranaReward,
          fuelDelta = activeRitual.fuelReward,
          stunTicks = activeRitual.stunTicksReward,
          grantsRelic = true
        )
      } else {
        val nextStep = activeRitual.steps[currentStepIndex]
        val stepMsg =
          "Ritual Step $currentStepIndex/${activeRitual.steps.size} [${expected.title}] consecrated! Next: ${nextStep.title} (${nextStep.cardinalLabel})"
        statusBanner = stepMsg
        return RitualStepOutcome(
          isStepCorrect = true,
          completedStepIndex = currentStepIndex,
          totalSteps = activeRitual.steps.size,
          didUnlockSecretChamber = false,
          message = stepMsg,
          sanityDelta = 6,
          pranaDelta = 5,
          fuelDelta = 10f
        )
      }
    } else {
      // Only reset sequence if the player interacted with the wrong lamp/seal in a strict sequence
      mistakeCount++
      currentStepIndex = 0
      completedStepIndices.clear()
      stepTimestamps.clear()
      val firstStep = activeRitual.steps.first()
      val resetMsg =
        "Ritual sequence broken! Performed ${actionType.label}${coord?.let { " at (${it.first},${it.second})" } ?: ""} out of order. Restart with Step 1: ${firstStep.title} (${firstStep.cardinalLabel})."
      statusBanner = resetMsg
      return RitualStepOutcome(
        isStepCorrect = false,
        completedStepIndex = 0,
        totalSteps = activeRitual.steps.size,
        didUnlockSecretChamber = false,
        message = resetMsg,
        sanityDelta = -4
      )
    }
  }

  /**
   * Executes the next required step directly from the Ritual Altar UI (useful when testing or
   * consecrating the step via the ceremonial mandala board) and returns both the step and outcome.
   */
  fun performStepByIndexFromAltar(stepIndex: Int): Pair<RitualSequenceStep, RitualStepOutcome> {
    val clickedStep = activeRitual.steps.getOrElse(stepIndex) { activeRitual.steps.first() }
    val expected = activeRitual.steps.getOrNull(currentStepIndex)
    val outcome = if (expected != null && stepIndex == currentStepIndex) {
      recordAction(
        actionType = clickedStep.actionType,
        coord = clickedStep.targetCoord,
        allowWildcardCoord = true
      )
    } else {
      recordAction(
        actionType = clickedStep.actionType,
        coord = -1 to -1, // Deliberate mismatch to trigger sequence reset feedback
        allowWildcardCoord = false
      )
    }
    return clickedStep to outcome
  }

  /**
   * Claims the ancient relic & blessing inside the newly unsealed Secret Temple Chamber.
   */
  fun claimSecretSanctuaryBlessing(): RitualStepOutcome {
    if (!isSecretChamberUnlocked || isSecretSanctuaryClaimed) {
      return RitualStepOutcome(
        isStepCorrect = false,
        completedStepIndex = currentStepIndex,
        totalSteps = activeRitual.steps.size,
        didUnlockSecretChamber = false,
        message = "Secret Sanctuary blessing already claimed."
      )
    }
    isSecretSanctuaryClaimed = true
    val msg =
      "Claimed the Sacred Siddhi of ${activeRitual.secretChamberName}! Prana, Sanity & Diya Oil fully blessed!"
    statusBanner = msg
    return RitualStepOutcome(
      isStepCorrect = true,
      completedStepIndex = activeRitual.steps.size,
      totalSteps = activeRitual.steps.size,
      didUnlockSecretChamber = false,
      message = msg,
      sanityDelta = 30,
      pranaDelta = 35,
      fuelDelta = 60f,
      stunTicks = 50,
      grantsRelic = true
    )
  }

  fun resetRitual() {
    currentStepIndex = 0
    completedStepIndices.clear()
    stepTimestamps.clear()
    statusBanner = "Ritual reset. Step 1/${activeRitual.steps.size}: ${activeRitual.steps.first().hintText}"
  }
}

/**
 * Interactive Jetpack Compose modal for inspecting and performing the ordered `RitualInteraction`
 * sequence (`Agama Vidhi Krama`) to unlock the temple's secret chamber (`Gupta Garbhagriha`).
 */
@Composable
fun RitualInteractionOverlay(
  ritualManager: RitualInteractionManager,
  playerX: Double,
  playerY: Double,
  onStepPerformed: (RitualSequenceStep, RitualStepOutcome) -> Unit,
  onClaimSanctuaryBlessing: () -> Unit,
  onTeleportToSecretChamber: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val ritual = ritualManager.activeRitual
  val isUnlocked = ritualManager.isSecretChamberUnlocked
  val isClaimed = ritualManager.isSecretSanctuaryClaimed
  val currentStepIdx = ritualManager.currentStepIndex

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .padding(16.dp)
      .testTag("ritual_interaction_overlay_root")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 2.dp,
          brush = Brush.linearGradient(
            colors = if (isUnlocked) {
              listOf(MantraJade, MoltenBrass, MantraJade)
            } else {
              listOf(MoltenBrass, SaffronFlame, SacredTeal)
            }
          ),
          shape = RoundedCornerShape(24.dp)
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = if (isUnlocked) MantraJade.copy(alpha = 0.2f) else SaffronFlame.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.border(
                  width = 1.dp,
                  color = if (isUnlocked) MantraJade else SaffronFlame,
                  shape = RoundedCornerShape(6.dp)
                )
              ) {
                Text(
                  text = ritualManager.progressState.badgeLabel,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (isUnlocked) MantraJade else SaffronFlame,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("ritual_progress_state_badge")
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = ritual.sanskritName,
                style = MaterialTheme.typography.labelLarge,
                color = SacredTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = ritual.ritualName.uppercase(),
              style = MaterialTheme.typography.titleMedium,
              color = MoltenBrass,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Target Vault: ${ritual.secretChamberName} (${ritual.secretDoorTileCoord.first},${ritual.secretDoorTileCoord.second})",
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentCream.copy(alpha = 0.88f)
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isUnlocked && currentStepIdx > 0) {
              IconButton(
                onClick = { ritualManager.resetRitual() },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("reset_ritual_sequence_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Reset Ritual Sequence",
                  tint = MoltenBrass
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(36.dp)
                .testTag("close_ritual_overlay_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Ritual Altar",
                tint = ParchmentCream
              )
            }
          }
        }

        // 2. Ceremonial Yantra Sequence Mandala & Secret Door Status Banner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .border(1.dp, MoltenBrass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          RitualMandalaWheelCanvas(
            totalSteps = ritual.steps.size,
            completedSteps = currentStepIdx,
            isUnlocked = isUnlocked,
            modifier = Modifier.size(72.dp)
          )

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = ritual.loreInscribedClue,
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentCream,
              fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = ritualManager.statusBanner,
              style = MaterialTheme.typography.labelLarge,
              color = if (isUnlocked) MantraJade else SaffronFlame,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("ritual_status_banner_text")
            )
          }
        }

        // 3. Ordered Sequence Steps List (Players can tap steps in order or perform them in the 3D world)
        Text(
          text = "SACRED ACTION SEQUENCE (PERFORM IN EXACT ORDER: $currentStepIdx / ${ritual.steps.size})",
          style = MaterialTheme.typography.labelLarge,
          color = MoltenBrass
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .testTag("ritual_steps_list"),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(ritual.steps) { index, step ->
            val isCompleted = index < currentStepIdx || isUnlocked
            val isCurrentTarget = index == currentStepIdx && !isUnlocked
            val distToTarget = step.targetCoord?.let { (tx, ty) ->
              hypot((tx + 0.5) - playerX, (ty + 0.5) - playerY)
            }

            RitualStepRowCard(
              step = step,
              isCompleted = isCompleted,
              isCurrentTarget = isCurrentTarget,
              distanceTiles = distToTarget,
              onPerformStep = {
                val (performedStep, outcome) = ritualManager.performStepByIndexFromAltar(index)
                onStepPerformed(performedStep, outcome)
              }
            )
          }
        }

        // 4. Secret Chamber Unlock / Claim Controls
        if (isUnlocked) {
          Surface(
            color = MantraJade.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.2.dp, MantraJade, RoundedCornerShape(14.dp))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "🔓 ${ritual.secretChamberName.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MantraJade,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = ritual.secretChamberSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentCream
                  )
                }
                Icon(
                  imageVector = Icons.Default.LockOpen,
                  contentDescription = "Chamber Unsealed",
                  tint = MantraJade,
                  modifier = Modifier.size(24.dp)
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = onTeleportToSecretChamber,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = SacredTeal,
                    contentColor = Color.Black
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("enter_secret_chamber_button")
                ) {
                  Text(
                    text = "STEP INTO CHAMBER",
                    fontWeight = FontWeight.Bold
                  )
                }

                if (!isClaimed) {
                  Button(
                    onClick = onClaimSanctuaryBlessing,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MoltenBrass,
                      contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("claim_secret_chamber_blessing_button")
                  ) {
                    Text(
                      text = "CLAIM SIDDHI RELIC",
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RitualStepRowCard(
  step: RitualSequenceStep,
  isCompleted: Boolean,
  isCurrentTarget: Boolean,
  distanceTiles: Double?,
  onPerformStep: () -> Unit
) {
  val borderColor = when {
    isCompleted -> MantraJade
    isCurrentTarget -> SaffronFlame
    else -> SandstoneMuted.copy(alpha = 0.45f)
  }
  val containerColor = when {
    isCompleted -> MantraJade.copy(alpha = 0.12f)
    isCurrentTarget -> SacredCardSurface
    else -> ObsidianVoid.copy(alpha = 0.58f)
  }

  val actionIcon = when (step.actionType) {
    RitualActionType.IGNITE_DIRECTIONAL_LAMP -> Icons.Default.LocalFireDepartment
    RitualActionType.RING_SACRED_GHANTA -> Icons.Default.NotificationsActive
    RitualActionType.CHANT_VEDIC_MANTRA -> Icons.Default.SelfImprovement
    RitualActionType.TRACE_YANTRA_SEAL -> Icons.Default.AutoAwesome
  }

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isCurrentTarget) 1.5.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable(enabled = !isCompleted) { onPerformStep() }
      .testTag("ritual_step_card_${step.stepIndex}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(borderColor.copy(alpha = 0.2f))
        ) {
          Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else actionIcon,
            contentDescription = step.title,
            tint = borderColor,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${step.stepIndex + 1}. ${step.title}",
              style = MaterialTheme.typography.bodyMedium,
              color = if (isCompleted) MantraJade else ParchmentCream,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = "${step.sanskritGlyph} • ${step.cardinalLabel}" +
              (distanceTiles?.let { " • ${"%.1f".format(it)}m away" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = if (isCurrentTarget) MoltenBrass else SandstoneMuted
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onPerformStep,
        enabled = !isCompleted,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isCurrentTarget) SaffronFlame else SacredCardSurface,
          contentColor = if (isCurrentTarget) Color.Black else ParchmentCream,
          disabledContainerColor = MantraJade.copy(alpha = 0.25f),
          disabledContentColor = MantraJade
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .height(38.dp)
          .testTag("perform_ritual_step_button_${step.stepIndex}")
      ) {
        Text(
          text = when {
            isCompleted -> "DONE"
            isCurrentTarget -> "PERFORM"
            else -> "TRY"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun RitualMandalaWheelCanvas(
  totalSteps: Int,
  completedSteps: Int,
  isUnlocked: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.44f

    val ringColor = if (isUnlocked) MantraJade else MoltenBrass
    drawCircle(
      color = ringColor.copy(alpha = 0.25f),
      radius = r * 1.08f,
      center = Offset(cx, cy)
    )
    drawCircle(
      color = ringColor,
      radius = r,
      center = Offset(cx, cy),
      style = Stroke(width = 3f)
    )
    drawCircle(
      color = SacredTeal.copy(alpha = 0.65f),
      radius = r * 0.62f,
      center = Offset(cx, cy),
      style = Stroke(width = 2f)
    )

    val count = totalSteps.coerceAtLeast(1)
    for (i in 0 until count) {
      val angle = (2.0 * PI * i / count) - (PI / 2.0)
      val px = cx + (r * cos(angle)).toFloat()
      val py = cy + (r * sin(angle)).toFloat()
      val nodeDone = i < completedSteps || isUnlocked
      val nodeCurrent = i == completedSteps && !isUnlocked

      drawCircle(
        color = when {
          nodeDone -> MantraJade
          nodeCurrent -> SaffronFlame
          else -> SandstoneMuted
        },
        radius = if (nodeCurrent) 7.5f else 6f,
        center = Offset(px, py)
      )
    }

    // Center Bindu flame/lock glyph
    drawRoundRect(
      color = if (isUnlocked) MantraJade else SaffronFlame,
      topLeft = Offset(cx - r * 0.22f, cy - r * 0.22f),
      size = Size(r * 0.44f, r * 0.44f),
      cornerRadius = CornerRadius(6f, 6f)
    )
  }
}
