package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay

/**
 * Classification of narrative encounter inside the ancient Hindu temple sanctum.
 */
enum class DialogueSpeakerType(
  val badgeLabel: String,
  val actionVerb: String
) {
  NPC_SPIRIT(
    badgeLabel = "BOUND TEMPLE SPIRIT",
    actionVerb = "COMMUNE WITH SPIRIT"
  ),
  ANCIENT_INSCRIPTION(
    badgeLabel = "SHILA-LEKHA INSCRIPTION",
    actionVerb = "READ ANCIENT INSCRIPTION"
  )
}

/**
 * Interactive player response option on a dialogue node that can branch to another node
 * or conclude the encounter while granting gameplay blessings or curses.
 */
data class DialogueChoice(
  val id: String,
  val label: String,
  val sanskritIncantation: String? = null,
  val nextNodeId: String? = null, // null ends the conversation
  val sanityDelta: Int = 0,
  val pranaDelta: Int = 0,
  val fuelDelta: Float = 0f,
  val grantsRelicChance: Boolean = false,
  val outcomeSummary: String = ""
)

/**
 * Single narrative step in a conversation with an NPC Spirit or Ancient Stone Inscription.
 */
data class DialogueNode(
  val id: String,
  val sanskritVerse: String,
  val transliteration: String,
  val narrativeText: String,
  val riddleHint: String? = null,
  val choices: List<DialogueChoice> = emptyList()
)

/**
 * World-positioned NPC Spirit or Ancient Inscription inside a temple map grid.
 */
data class TempleDialogueEncounter(
  val id: String,
  val templeId: String,
  val speakerName: String,
  val speakerTitle: String,
  val sanskritHonorific: String,
  val speakerType: DialogueSpeakerType,
  val tileX: Int,
  val tileY: Int,
  val startNodeId: String,
  val nodes: Map<String, DialogueNode>
)

/**
 * Reward/penalty payload returned when the player chooses a dialogue action or finishes an inscription.
 */
data class DialogueOutcomeEffect(
  val encounterId: String,
  val speakerName: String,
  val sanityDelta: Int,
  val pranaDelta: Int,
  val fuelDelta: Float,
  val grantsRelicChance: Boolean,
  val summaryMessage: String,
  val dialogueClosed: Boolean
)

/**
 * Manages narrative interactions, dialogue tree progression, typewriter text reveal,
 * and spatial proximity detection for NPC Spirits and Ancient Temple Inscriptions.
 */
class DialogueManager(
  initialDiscoveredIds: Collection<String> = emptyList()
) {
  private val _discoveredEncounterIds = mutableStateListOf<String>().apply {
    addAll(initialDiscoveredIds.distinct())
  }
  val discoveredEncounterIds: List<String> get() = _discoveredEncounterIds

  var activeEncounter by mutableStateOf<TempleDialogueEncounter?>(null)
    private set

  var currentNode by mutableStateOf<DialogueNode?>(null)
    private set

  var nodeStepIndex by mutableIntStateOf(0)
    private set

  var lastOutcomeNotice by mutableStateOf<String?>(null)
    private set

  val isDialogueOpen: Boolean
    get() = activeEncounter != null && currentNode != null

  /**
   * Returns all NPC Spirits and Ancient Inscriptions located in the specified temple.
   */
  fun getEncountersForTemple(templeId: String): List<TempleDialogueEncounter> {
    return TEMPLE_DIALOGUE_CATALOG.filter { it.templeId == templeId }
  }

  /**
   * Finds the closest NPC Spirit or Ancient Inscription within [maxDistanceTiles] of the player.
   */
  fun findNearbyEncounter(
    templeId: String,
    playerX: Double,
    playerY: Double,
    maxDistanceTiles: Double = 1.85
  ): TempleDialogueEncounter? {
    val encounters = getEncountersForTemple(templeId)
    var closest: TempleDialogueEncounter? = null
    var bestDist = maxDistanceTiles
    for (enc in encounters) {
      val d = hypot((enc.tileX + 0.5) - playerX, (enc.tileY + 0.5) - playerY)
      if (d <= bestDist) {
        bestDist = d
        closest = enc
      }
    }
    return closest
  }

  /**
   * Starts a narrative dialogue with the given [encounter], or opens the nearest/next
   * unread chronicle in the current temple if triggered from the HUD action bar.
   */
  fun startDialogue(encounter: TempleDialogueEncounter) {
    val startNode = encounter.nodes[encounter.startNodeId] ?: encounter.nodes.values.firstOrNull()
    activeEncounter = encounter
    currentNode = startNode
    nodeStepIndex = 1
    if (!_discoveredEncounterIds.contains(encounter.id)) {
      _discoveredEncounterIds.add(encounter.id)
    }
  }

  /**
   * Opens either the nearby encounter or cycles through the temple's inscriptions & spirits
   * so players can always inspect discovered lore and commune with temple spirits.
   */
  fun openNearbyOrNextEncounter(
    templeId: String,
    playerX: Double,
    playerY: Double
  ): TempleDialogueEncounter? {
    val nearby = findNearbyEncounter(templeId, playerX, playerY, maxDistanceTiles = 2.2)
    if (nearby != null) {
      startDialogue(nearby)
      return nearby
    }
    val allForTemple = getEncountersForTemple(templeId)
    if (allForTemple.isEmpty()) return null
    val nextUndiscovered = allForTemple.firstOrNull { !_discoveredEncounterIds.contains(it.id) }
      ?: allForTemple.first()
    startDialogue(nextUndiscovered)
    return nextUndiscovered
  }

  /**
   * Selects a player choice on the active [DialogueNode], advancing to the target node
   * or completing the dialogue and returning a [DialogueOutcomeEffect].
   */
  fun selectChoice(choice: DialogueChoice): DialogueOutcomeEffect? {
    val enc = activeEncounter ?: return null
    val nextId = choice.nextNodeId
    val nextNode = if (nextId != null) enc.nodes[nextId] else null

    val summary = choice.outcomeSummary.ifBlank {
      "${enc.speakerName}: ${choice.label}"
    }
    lastOutcomeNotice = summary

    return if (nextNode != null) {
      currentNode = nextNode
      nodeStepIndex += 1
      DialogueOutcomeEffect(
        encounterId = enc.id,
        speakerName = enc.speakerName,
        sanityDelta = choice.sanityDelta,
        pranaDelta = choice.pranaDelta,
        fuelDelta = choice.fuelDelta,
        grantsRelicChance = choice.grantsRelicChance,
        summaryMessage = summary,
        dialogueClosed = false
      )
    } else {
      dismissDialogue()
      DialogueOutcomeEffect(
        encounterId = enc.id,
        speakerName = enc.speakerName,
        sanityDelta = choice.sanityDelta,
        pranaDelta = choice.pranaDelta,
        fuelDelta = choice.fuelDelta,
        grantsRelicChance = choice.grantsRelicChance,
        summaryMessage = summary,
        dialogueClosed = true
      )
    }
  }

  fun dismissDialogue() {
    activeEncounter = null
    currentNode = null
    nodeStepIndex = 0
  }

  fun hasDiscovered(encounterId: String): Boolean {
    return _discoveredEncounterIds.contains(encounterId)
  }

  companion object {
    val TEMPLE_DIALOGUE_CATALOG: List<TempleDialogueEncounter> = listOf(
      // ==================== BRIHADEESWARAR SANCTUM ====================
      TempleDialogueEncounter(
        id = "brihadeeswarar_sthapati_spirit",
        templeId = "brihadeeswarar",
        speakerName = "Acharya Kunjara Mallan",
        speakerTitle = "Spectral Chief Sthapati (Architect of the Vimana)",
        sanskritHonorific = "वास्तु-आचार्य कुञ्जर मल्लन • आत्मन्",
        speakerType = DialogueSpeakerType.NPC_SPIRIT,
        tileX = 2,
        tileY = 1,
        startNodeId = "spirit_intro",
        nodes = mapOf(
          "spirit_intro" to DialogueNode(
            id = "spirit_intro",
            sanskritVerse = "असतो मा सद्गमय । तमसो मा ज्योतिर्गमय ॥",
            transliteration = "Asato mā sad gamaya • Tamaso mā jyotir gamaya",
            narrativeText = "A translucent figure draped in Chola silk and holding a bronze measuring rod materializes beside the granite pillar. \"Sadhaka... my mason-guilds interlocked eighty-ton capstones to resonate at 108 Hz. Before the Brahmarakshasa twisted the inner sanctum, we hid the counter-mantra within the Surya Yantras.\"",
            riddleHint = "Architectural Clue: The Saptadhatu Ghanta bell's 528 Hz overtone stuns the fallen scholar when he charges.",
            choices = listOf(
              DialogueChoice(
                id = "ask_secret_passage",
                label = "Ask how to unseal the Swarga Dwara portal",
                sanskritIncantation = "ॐ विश्वकर्मणे नमः",
                nextNodeId = "spirit_guidance",
                sanityDelta = 8,
                pranaDelta = 5,
                outcomeSummary = "Acharya Kunjara's spectral mudra steadies your Manas (+8% Sanity, +5% Prana)."
              ),
              DialogueChoice(
                id = "offer_diya_flame",
                label = "Share a drop of consecrated Diya ghee with his spirit",
                sanskritIncantation = "ॐ अग्नये स्वाहा",
                nextNodeId = "spirit_blessing",
                sanityDelta = 14,
                pranaDelta = 10,
                fuelDelta = -6f,
                grantsRelicChance = true,
                outcomeSummary = "Offered Diya flame to Acharya Kunjara (+14% Sanity, +10% Prana, Relic blessing)."
              ),
              DialogueChoice(
                id = "depart_respectfully",
                label = "Bow in Pranama and return to the dark corridor",
                nextNodeId = null,
                sanityDelta = 4,
                outcomeSummary = "You bowed to the Sthapati spirit (+4% Sanity)."
              )
            )
          ),
          "spirit_guidance" to DialogueNode(
            id = "spirit_guidance",
            sanskritVerse = "पञ्च दीपाः प्रज्वाल्याः त्रयः मुद्राः विमोचयेत् ॥",
            transliteration = "Pañca dīpāḥ prajvālyāḥ trayaḥ mudrāḥ vimocayet",
            narrativeText = "\"Listen closely to the stone underfoot, traveler. When your footsteps echo on hollow bronze grates, a Vedic Seal or relic cache lies nearby. Ignite five Ashtadhatu Diyas or align three Vedic Seals—then the golden Swarga Dwara in the southeast corner shall open.\"",
            riddleHint = "Puzzle Hint: Pressure plates follow the Pancha-Bhuta order: Prithvi -> Apas -> Tejas -> Vayu -> Akasha.",
            choices = listOf(
              DialogueChoice(
                id = "accept_architect_wisdom",
                label = "Commit the Pancha-Bhuta sequence to memory",
                sanskritIncantation = "ॐ भूर्भुवः स्वः",
                nextNodeId = null,
                sanityDelta = 10,
                fuelDelta = 15f,
                outcomeSummary = "Memorized the Sthapati's Pancha-Bhuta key (+10% Sanity, +15% Diya Oil)."
              )
            )
          ),
          "spirit_blessing" to DialogueNode(
            id = "spirit_blessing",
            sanskritVerse = "तेजोऽसि तेजो मयि धेहि । वीर्यमसि वीर्यं मयि धेहि ॥",
            transliteration = "Tejo'si tejo mayi dhehi • Vīryam asi vīryaṃ mayi dhehi",
            narrativeText = "The architect's spirit inhales the warm aroma of sacred ghee. His spectral hands trace a protective Vibhuti Rekha across your brow, refilling your lamp with undying temple oil and revealing a hidden alcove in the stonework.",
            riddleHint = "Blessing Granted: Your Diya lamp surges with solar Agni and your mind resists the Brahmarakshasa's whispers.",
            choices = listOf(
              DialogueChoice(
                id = "receive_vibhuti_grace",
                label = "Receive the Chola Sthapati's consecration",
                nextNodeId = null,
                sanityDelta = 12,
                pranaDelta = 8,
                fuelDelta = 28f,
                outcomeSummary = "Consecrated by Acharya Kunjara (+12% Sanity, +8% Prana, +28% Diya Oil)!"
              )
            )
          )
        )
      ),
      TempleDialogueEncounter(
        id = "brihadeeswarar_chola_edict",
        templeId = "brihadeeswarar",
        speakerName = "Rajaraja Chola Copper-Stone Edict",
        speakerTitle = "Carved Grantha & Sanskrit Wall Inscription (1010 CE)",
        sanskritHonorific = "श्री राजराजेश्वर शिलालेख",
        speakerType = DialogueSpeakerType.ANCIENT_INSCRIPTION,
        tileX = 5,
        tileY = 5,
        startNodeId = "edict_root",
        nodes = mapOf(
          "edict_root" to DialogueNode(
            id = "edict_root",
            sanskritVerse = "शिवपादशेखरः राजराजः इदं गर्भगृहं रक्षतु ॥",
            transliteration = "Śivapādaśekharaḥ Rājarājaḥ idaṃ garbhagṛhaṃ rakṣatu",
            narrativeText = "Deep chiselled grooves glow with faint saffron vermilion along the central Mandapa wall: \"Let no scholar recite inverted Atharva verses within the shadowless Vimana. Should the Brahmarakshasa awaken, align the three Dandaka Levers to URDDHVA (Up), MADHYA (Center), and ADHAS (Down).\"",
            riddleHint = "Inscription Deciphered: Dandaka Lever Code -> Lever I: UP • Lever II: CENTER • Lever III: DOWN.",
            choices = listOf(
              DialogueChoice(
                id = "trace_sanskrit_glyphs",
                label = "Trace the vermilion Grantha glyphs and chant the seal verse",
                sanskritIncantation = "ॐ नमः शिवाय",
                nextNodeId = "edict_resonance",
                sanityDelta = 10,
                fuelDelta = 12f,
                outcomeSummary = "Chanted the Grantha inscription verse (+10% Sanity, +12% Diya Oil)."
              ),
              DialogueChoice(
                id = "read_cursed_margin",
                label = "Read the scratched blood-red warning at the bottom",
                nextNodeId = null,
                sanityDelta = -6,
                pranaDelta = 5,
                grantsRelicChance = true,
                outcomeSummary = "Read the cursed scholar's warning (-6% Sanity, +5% Prana, unearthed hidden niche)."
              )
            )
          ),
          "edict_resonance" to DialogueNode(
            id = "edict_resonance",
            sanskritVerse = "नादब्रह्मणा पाशं भिन्दन्ति साधकाः ॥",
            transliteration = "Nādabrahmaṇā pāśaṃ bhindanti sādhakāḥ",
            narrativeText = "As your fingers complete the final akshara, the granite slab hums at 108 Hz. Dust cascades from the ceiling cornice, exposing an ancient oil reservoir carved into the pillar base.",
            riddleHint = "Vedic Pattern Key: Select OM (ॐ), SRI (श्रीं), HRIM (ह्रीं), and KSHAM (क्षं) while avoiding cursed Tamas sigils.",
            choices = listOf(
              DialogueChoice(
                id = "collect_pillar_oil",
                label = "Anoint your Diya and step away from the stele",
                nextNodeId = null,
                sanityDelta = 8,
                fuelDelta = 20f,
                outcomeSummary = "Anointed Diya from the Chola inscription reservoir (+8% Sanity, +20% Diya Oil)."
              )
            )
          )
        )
      ),

      // ==================== KONARK SUN CRYPT ====================
      TempleDialogueEncounter(
        id = "konark_surya_priestess",
        templeId = "konark",
        speakerName = "Devi Bhanumati",
        speakerTitle = "Guardian Spirit of the Konark Sun Chariot",
        sanskritHonorific = "सूर्य-उपासिका भानुमती • दिव्यात्मा",
        speakerType = DialogueSpeakerType.NPC_SPIRIT,
        tileX = 2,
        tileY = 3,
        startNodeId = "konark_spirit_intro",
        nodes = mapOf(
          "konark_spirit_intro" to DialogueNode(
            id = "konark_spirit_intro",
            sanskritVerse = "आ कृष्णेन रजसा वर्तमानो निवेशयन्नमृतं मर्त्यं च ॥",
            transliteration = "Ā kṛṣṇena rajasā vartamāno niveśayann amṛtaṃ martyaṃ ca",
            narrativeText = "A luminous silhouette crowned with a twelve-spoked solar halo hovers above the shattered chariot wheel. \"The Chhaya Pret was born when Rahu swallowed the midday sun over Puri. It drinks the ghee from your brass lamp whenever you linger in unlit alcoves.\"",
            riddleHint = "Spirit Counsel: Keep your Torch above 25% oil so the Chhaya Pret cannot ambush from the pillar shadows.",
            choices = listOf(
              DialogueChoice(
                id = "invoke_aditya_hridayam",
                label = "Recite the Aditya Hridayam hymn with Devi Bhanumati",
                sanskritIncantation = "ॐ घृणिः सूर्याय आदित्य श्रीं",
                nextNodeId = "konark_solar_blessing",
                sanityDelta = 15,
                pranaDelta = 10,
                fuelDelta = 25f,
                outcomeSummary = "Recited Aditya Hridayam with Devi Bhanumati (+15% Sanity, +10% Prana, +25% Fuel)!"
              ),
              DialogueChoice(
                id = "ask_wheel_alignment",
                label = "Ask how the twelve Konark sundial wheels unlock the crypt",
                nextNodeId = null,
                sanityDelta = 8,
                grantsRelicChance = true,
                outcomeSummary = "Devi Bhanumati revealed the sundial chakra alignment (+8% Sanity)."
              )
            )
          ),
          "konark_solar_blessing" to DialogueNode(
            id = "konark_solar_blessing",
            sanskritVerse = "जपाकुसुमसंकाशं काश्यपेयं महाद्युतिम् । तमोऽरिं सर्वपापघ्नं प्रणतोऽस्मि दिवाकरम् ॥",
            transliteration = "Japākusuma-saṃkāśaṃ kāśyapeyaṃ mahādyutim",
            narrativeText = "Golden solar sparks swirl from her anklets into your brass Diya. \"Go forth, Sadhaka. The Chhaya Pret recoils from pure solar mantra and the four sacred Bija syllables.\"",
            choices = listOf(
              DialogueChoice(
                id = "accept_solar_flame",
                label = "Carry the Solar Agni deeper into the Sun Crypt",
                nextNodeId = null,
                sanityDelta = 10,
                fuelDelta = 20f,
                outcomeSummary = "Solar Agni infused your Diya (+10% Sanity, +20% Diya Oil)."
              )
            )
          )
        )
      ),
      TempleDialogueEncounter(
        id = "konark_sundial_inscription",
        templeId = "konark",
        speakerName = "Konark Sundial Chakra Stele",
        speakerTitle = "Carved Khondalite Solar Wheel Inscription (1250 CE)",
        sanskritHonorific = "कोणार्क कालचक्र शिलालेख",
        speakerType = DialogueSpeakerType.ANCIENT_INSCRIPTION,
        tileX = 5,
        tileY = 5,
        startNodeId = "sundial_root",
        nodes = mapOf(
          "sundial_root" to DialogueNode(
            id = "sundial_root",
            sanskritVerse = "द्वादश अराः कालचक्रस्य सूर्यतेजसा भ्रमन्ति ॥",
            transliteration = "Dvādaśa arāḥ kālacakrasya sūryatejasā bhramanti",
            narrativeText = "Carved into the red khondalite stone axle of a giant chariot wheel are eight major and आठ minor spokes: \"When shadow eclipses brass, step upon the five Pancha-Bhuta flagstones from Earth to Ether, and set the three iron levers to Sky, Horizon, and Abyss.\"",
            riddleHint = "Sundial Key: 1. Prithvi • 2. Apas • 3. Tejas • 4. Vayu • 5. Akasha.",
            choices = listOf(
              DialogueChoice(
                id = "align_sundial_gnomon",
                label = "Touch the bronze sundial gnomon and absorb its warmth",
                sanskritIncantation = "ॐ सूर्याय नमः",
                nextNodeId = null,
                sanityDelta = 12,
                pranaDelta = 6,
                fuelDelta = 18f,
                outcomeSummary = "Absorbed solar warmth from the Konark Sundial (+12% Sanity, +6% Prana, +18% Oil)."
              )
            )
          )
        )
      ),

      // ==================== KAILASA MONOLITH ABYSS ====================
      TempleDialogueEncounter(
        id = "kailasa_rishi_spirit",
        templeId = "kailasa",
        speakerName = "Tapasvi Kokasa",
        speakerTitle = "Spectral Master Sculptor of Kailasa Cave XVI",
        sanskritHonorific = "कैलास-शिल्पी कोकस • सिद्धात्मा",
        speakerType = DialogueSpeakerType.NPC_SPIRIT,
        tileX = 3,
        tileY = 1,
        startNodeId = "kailasa_spirit_intro",
        nodes = mapOf(
          "kailasa_spirit_intro" to DialogueNode(
            id = "kailasa_spirit_intro",
            sanskritVerse = "ऊर्ध्वाधः शिलां छित्त्वा कैलासं निर्मापितम् ॥",
            transliteration = "Ūrdhvādhaḥ śilāṃ chittvā Kailāsaṃ nirmāpitam",
            narrativeText = "From the monolithic basalt gallery steps a bearded ascetic holding a glowing iron chisel. \"We carved two hundred thousand tons of volcanic cliff from the summit downward! Beware the Pishacha Vetala—it clings to the high cornices where cobwebs hang thickest.\"",
            riddleHint = "Survival Tip: Avoid sprinting blindly against ancient walls; falling basalt dust betrays your position.",
            choices = listOf(
              DialogueChoice(
                id = "chant_rudram_with_rishi",
                label = "Chant the Sri Rudram kavach alongside Tapasvi Kokasa",
                sanskritIncantation = "ॐ नमो भगवते रुद्राय",
                nextNodeId = "kailasa_rudra_grace",
                sanityDelta = 16,
                pranaDelta = 14,
                fuelDelta = 22f,
                grantsRelicChance = true,
                outcomeSummary = "Chanted Sri Rudram with Tapasvi Kokasa (+16% Sanity, +14% Prana, +22% Oil)!"
              ),
              DialogueChoice(
                id = "inquire_vetala_riddle",
                label = "Ask how to break the Vetala's acoustic illusion",
                nextNodeId = null,
                sanityDelta = 10,
                outcomeSummary = "Learned the Vetala counter-ritual from Tapasvi Kokasa (+10% Sanity)."
              )
            )
          ),
          "kailasa_rudra_grace" to DialogueNode(
            id = "kailasa_rudra_grace",
            sanskritVerse = "त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम् ॥",
            transliteration = "Tryambakaṃ yajāmahe sugandhiṃ puṣṭivardhanam",
            narrativeText = "The sculptor strikes his iron chisel against the monolithic floor, sending a wave of emerald-gold Nada vibrations down the corridor that drives back the lurking Vetala.",
            choices = listOf(
              DialogueChoice(
                id = "thank_kailasa_sculptor",
                label = "Accept the Rudra blessing and press onward",
                nextNodeId = null,
                sanityDelta = 10,
                pranaDelta = 8,
                outcomeSummary = "Blessed by the Kailasa Monolith Sculptor (+10% Sanity, +8% Prana)."
              )
            )
          )
        )
      ),
      TempleDialogueEncounter(
        id = "kailasa_ravana_stele",
        templeId = "kailasa",
        speakerName = "Ravana-Anugraha Basalt Relief",
        speakerTitle = "Monolithic Sanskrit Frieze Inscription (760 CE)",
        sanskritHonorific = "कैलास-शिखर शिलालेख",
        speakerType = DialogueSpeakerType.ANCIENT_INSCRIPTION,
        tileX = 4,
        tileY = 5,
        startNodeId = "kailasa_stele_root",
        nodes = mapOf(
          "kailasa_stele_root" to DialogueNode(
            id = "kailasa_stele_root",
            sanskritVerse = "अहंकारेण यः शैलम् चालयति सः पाताले पतति ॥",
            transliteration = "Ahaṃkāreṇa yaḥ śailam cālayati saḥ pātāle patati",
            narrativeText = "Carved into the towering black basalt wall is the mythic relief of Mount Kailasa trembling: \"Pride shakes the mountain, yet a single toe of Shiva pins the ten-headed king below. Only those who align the четыре sacred Bija mantras (OM • SRI • HRIM • KSHAM) may cross the abyss.\"",
            riddleHint = "Bija Mandala Solution: Activate ॐ (Om), श्रीं (Srim), ह्रीं (Hrim), and क्षं (Ksham).",
            choices = listOf(
              DialogueChoice(
                id = "meditate_on_relief",
                label = "Meditate upon the basalt relief to steady your breath",
                sanskritIncantation = "ॐ शान्तिः शान्तिः शान्तिः",
                nextNodeId = null,
                sanityDelta = 14,
                pranaDelta = 10,
                fuelDelta = 15f,
                outcomeSummary = "Meditated before the Kailasa Basalt Relief (+14% Sanity, +10% Prana, +15% Oil)."
              )
            )
          )
        )
      ),

      // ==================== MEENAKSHI THOUSAND-PILLAR LABYRINTH ====================
      TempleDialogueEncounter(
        id = "meenakshi_nadavidwan_spirit",
        templeId = "meenakshi",
        speakerName = "Vidwan Muthuswami",
        speakerTitle = "Spectral Nada-Vidwan of the Thousand Musical Pillars",
        sanskritHonorific = "नाद-विद्वान् मुत्तुस्वामी • दिव्य गन्धर्व",
        speakerType = DialogueSpeakerType.NPC_SPIRIT,
        tileX = 2,
        tileY = 1,
        startNodeId = "meenakshi_spirit_intro",
        nodes = mapOf(
          "meenakshi_spirit_intro" to DialogueNode(
            id = "meenakshi_spirit_intro",
            sanskritVerse = "सप्तस्वराः स्तम्भेषु नदन्ति मीनाक्षी-मण्डपे ॥",
            transliteration = "Saptasvarāḥ stambheṣu nadanti Mīnākṣī-maṇḍape",
            narrativeText = "A luminous musician holding a veena of carved granite stands beside a resonant musical column. \"Sadhaka! In this Thousand-Pillar Labyrinth, the Mayavi Vetala shrouds unvisited corridors in thick fog. Watch your Vastu Mandala mini-map and follow the golden breadcrumb trail of your Diya!\"",
            riddleHint = "Labyrinth Clue: Kindle the NW (3,1) and NE (9,1) Lotus Lamps first to unseal the Golden Lotus Vault at (5,4).",
            choices = listOf(
              DialogueChoice(
                id = "harmonize_swara_pillars",
                label = "Strike the Shadja-Panchama musical pillar chord with him",
                sanskritIncantation = "ॐ नादब्रह्मणे नमः",
                nextNodeId = null,
                sanityDelta = 16,
                pranaDelta = 12,
                fuelDelta = 24f,
                grantsRelicChance = true,
                outcomeSummary = "Harmonized the Musical Pillars with Vidwan Muthuswami (+16% Sanity, +12% Prana, +24% Oil)!"
              )
            )
          )
        )
      ),
      TempleDialogueEncounter(
        id = "meenakshi_lotus_edict",
        templeId = "meenakshi",
        speakerName = "Porthamarai Golden Lotus Stele",
        speakerTitle = "Carved Pandya Tamil-Sanskrit Pillar Edict (1190 CE)",
        sanskritHonorific = "स्वर्ण-पद्म शिलालेख",
        speakerType = DialogueSpeakerType.ANCIENT_INSCRIPTION,
        tileX = 5,
        tileY = 5,
        startNodeId = "meenakshi_edict_root",
        nodes = mapOf(
          "meenakshi_edict_root" to DialogueNode(
            id = "meenakshi_edict_root",
            sanskritVerse = "ह्रीं श्रीं मीनाक्षी-सुन्दरेश्वराभ्यां नमः ॥",
            transliteration = "Hrīṃ Śrīṃ Mīnākṣī-Sundareśvarābhyāṃ namaḥ",
            narrativeText = "Inscribed upon the central lotus pedestal: \"Whoever recites the sacred Mantra with pure voice and completes the Threefold Maha-Aarti at the Swarga Dwara shall banish the Mayavi Vetala forever.\"",
            riddleHint = "Maha-Aarti Key: Stage 1 Agni Aarti -> Stage 2 528Hz Ghanta Nada -> Stage 3 Maha-Mrityunjaya Purna-Ahuti.",
            choices = listOf(
              DialogueChoice(
                id = "anoint_lotus_stele",
                label = "Touch the golden lotus engraving and refill your Diya",
                sanskritIncantation = "ॐ नमः शिवाय",
                nextNodeId = null,
                sanityDelta = 14,
                pranaDelta = 10,
                fuelDelta = 22f,
                outcomeSummary = "Blessed at the Golden Lotus Stele (+14% Sanity, +10% Prana, +22% Diya Oil)."
              )
            )
          )
        )
      )
    )
  }
}

/**
 * Jetpack Compose UI Overlay for displaying narrative text, Sanskrit shlokas, typewriter
 * inscription reveal, and interactive branching dialogue choices with NPC spirits or
 * ancient stone inscriptions inside the temple.
 */
@Composable
fun TempleDialogueOverlay(
  dialogueManager: DialogueManager,
  onChoiceSelected: (DialogueOutcomeEffect) -> Unit,
  onCycleNextChronicle: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val encounter = dialogueManager.activeEncounter ?: return
  val node = dialogueManager.currentNode ?: return

  // Typewriter reveal animation for atmospheric narrative immersion
  var revealedChars by remember(encounter.id, node.id) { mutableIntStateOf(0) }
  val fullText = node.narrativeText

  LaunchedEffect(encounter.id, node.id, fullText) {
    revealedChars = 0
    while (revealedChars < fullText.length) {
      delay(14L)
      revealedChars = (revealedChars + 3).coerceAtMost(fullText.length)
    }
  }

  val isSpirit = encounter.speakerType == DialogueSpeakerType.NPC_SPIRIT
  val accentColor = if (isSpirit) SacredTeal else MoltenBrass

  Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.72f))
      .clickable(enabled = true, onClick = {}) // Consume background taps
      .padding(horizontal = 16.dp, vertical = 18.dp)
      .testTag("dialogue_overlay_root")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite.copy(alpha = 0.97f)),
      shape = RoundedCornerShape(22.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.8.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              accentColor,
              SaffronFlame.copy(alpha = 0.85f),
              accentColor
            )
          ),
          shape = RoundedCornerShape(22.dp)
        )
        .testTag("dialogue_overlay_card")
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Header: Portrait Emblem, Speaker Name, Sanskrit Honorific, and Close/Skip Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            DialogueSpeakerEmblem(
              speakerType = encounter.speakerType,
              accentColor = accentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = accentColor.copy(alpha = 0.18f),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.border(0.8.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                ) {
                  Text(
                    text = encounter.speakerType.badgeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = " Coordinates (${encounter.tileX}, ${encounter.tileY})",
                  style = MaterialTheme.typography.labelLarge,
                  color = SandstoneMuted
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = encounter.speakerName,
                style = MaterialTheme.typography.titleMedium,
                color = ParchmentCream,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dialogue_speaker_name")
              )
              Text(
                text = "${encounter.speakerTitle} • ${encounter.sanskritHonorific}",
                style = MaterialTheme.typography.bodyMedium,
                color = MoltenBrass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = onCycleNextChronicle,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("dialogue_cycle_lore_button")
            ) {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = "Next Temple Inscription or Spirit",
                tint = MoltenBrass,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Next Lore",
                style = MaterialTheme.typography.labelLarge,
                color = MoltenBrass
              )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(36.dp)
                .testTag("dialogue_close_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Dialogue Overlay",
                tint = ParchmentCream
              )
            }
          }
        }

        // 2. Engraved Sanskrit Shloka & Transliteration Banner
        Surface(
          color = ObsidianVoid.copy(alpha = 0.82f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MoltenBrass.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .testTag("dialogue_sanskrit_banner")
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
              text = node.sanskritVerse,
              style = MaterialTheme.typography.bodyLarge,
              color = SaffronFlame,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = node.transliteration,
              style = MaterialTheme.typography.bodyMedium,
              color = SandstoneMuted,
              fontStyle = FontStyle.Italic
            )
          }
        }

        // 3. Narrative Text Body with Tap-to-Complete Typewriter Effect
        Surface(
          color = SacredCardSurface.copy(alpha = 0.90f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (revealedChars < fullText.length) {
                revealedChars = fullText.length
              }
            }
            .testTag("dialogue_narrative_body")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = fullText.take(revealedChars),
              style = MaterialTheme.typography.bodyLarge,
              color = ParchmentCream
            )

            if (node.riddleHint != null) {
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = MoltenBrass.copy(alpha = 0.25f))
              Spacer(modifier = Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = MantraJade,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = node.riddleHint,
                  style = MaterialTheme.typography.labelLarge,
                  color = MantraJade,
                  modifier = Modifier.testTag("dialogue_riddle_hint")
                )
              }
            }
          }
        }

        // 4. Interactive Dialogue Choices / Responses
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          node.choices.forEachIndexed { idx, choice ->
            Button(
              onClick = {
                val effect = dialogueManager.selectChoice(choice)
                if (effect != null) {
                  onChoiceSelected(effect)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = ObsidianVoid.copy(alpha = 0.9f),
                contentColor = ParchmentCream
              ),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = 1.2.dp,
                  color = if (choice.sanityDelta >= 0) accentColor.copy(alpha = 0.65f)
                  else BloodAltarRed.copy(alpha = 0.65f),
                  shape = RoundedCornerShape(12.dp)
                )
                .testTag("dialogue_choice_button_$idx")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = choice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentCream,
                    fontWeight = FontWeight.Bold
                  )
                  if (!choice.sanskritIncantation.isNullOrBlank()) {
                    Text(
                      text = "Chant: ${choice.sanskritIncantation}",
                      style = MaterialTheme.typography.labelLarge,
                      color = SaffronFlame
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Effect preview chip (Sanity / Prana / Oil boost)
                val badgeText = buildList {
                  if (choice.sanityDelta != 0) {
                    val sign = if (choice.sanityDelta > 0) "+" else ""
                    add("${sign}${choice.sanityDelta}% Sanity")
                  }
                  if (choice.pranaDelta != 0) {
                    val sign = if (choice.pranaDelta > 0) "+" else ""
                    add("${sign}${choice.pranaDelta}% Prana")
                  }
                  if (choice.fuelDelta > 0f) {
                    add("+${choice.fuelDelta.toInt()}% Oil")
                  }
                }.joinToString(" • ")

                if (badgeText.isNotEmpty()) {
                  Surface(
                    color = SacredCardSurface,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = if (choice.sanityDelta >= 0) MantraJade else BloodAltarRed,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (choice.sanityDelta >= 0) MantraJade else BloodAltarRed
                      )
                    }
                  }
                } else {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
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

/**
 * Custom Canvas emblem representing either an ethereal NPC Temple Spirit or a carved Shila-Lekha Stone Stele.
 */
@Composable
private fun DialogueSpeakerEmblem(
  speakerType: DialogueSpeakerType,
  accentColor: Color
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(54.dp)
      .clip(CircleShape)
      .background(ObsidianVoid)
      .border(1.5.dp, accentColor, CircleShape)
  ) {
    Canvas(modifier = Modifier.size(44.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      if (speakerType == DialogueSpeakerType.NPC_SPIRIT) {
        // Spectral aura halo + third-eye bindu
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              accentColor.copy(alpha = 0.55f),
              Color.Transparent
            )
          ),
          radius = size.minDimension * 0.48f,
          center = Offset(cx, cy)
        )
        drawCircle(
          color = accentColor,
          radius = size.minDimension * 0.32f,
          center = Offset(cx, cy),
          style = Stroke(width = 2.5f)
        )
        drawCircle(
          color = SaffronFlame,
          radius = 4.5f,
          center = Offset(cx, cy - 3f)
        )
      } else {
        // Carved stone stele tablet with engraved horizontal Sanskrit lines
        val path = Path().apply {
          moveTo(cx - 12f, cy + 14f)
          lineTo(cx - 12f, cy - 8f)
          quadraticTo(cx, cy - 16f, cx + 12f, cy - 8f)
          lineTo(cx + 12f, cy + 14f)
          close()
        }
        drawPath(path = path, color = accentColor.copy(alpha = 0.28f))
        drawPath(path = path, color = accentColor, style = Stroke(width = 2.4f))
        for (row in -1..1) {
          val y = cy + row * 5.5f
          drawLine(
            color = SaffronFlame,
            start = Offset(cx - 7f, y),
            end = Offset(cx + 7f, y),
            strokeWidth = 2f
          )
        }
      }
    }
  }
}
