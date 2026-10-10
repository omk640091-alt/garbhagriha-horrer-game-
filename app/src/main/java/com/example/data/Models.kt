package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Sadhaka Explorer",
  val sanctumsCleansed: Long = 0L,
  val diyasLit: Long = 0L,
  val puzzlesSolved: Long = 0L,
  val highestSurvivalSec: Long = 0L,
  val unlockedCodexIds: List<String> = listOf("brahmarakshasa", "brihadeeswarar"),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpeditionRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val templeId: String = "brihadeeswarar",
  val templeName: String = "Brihadeeswarar Sanctum",
  val entityName: String = "Brahmarakshasa",
  val hostUid: String = "",
  val hostName: String = "Sadhaka",
  val status: String = "waiting",
  val diyasLitCount: Long = 0L,
  val sealsUnlocked: Long = 0L,
  val entityX: Double = 7.5,
  val entityY: Double = 7.5,
  val entityAlertLevel: Long = 10L,
  val lastRitualEvent: String = "Sanctum entered",
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlayerPresence(
  @DocumentId val id: String = "",
  val userId: String = "",
  val roomCode: String = "",
  val displayName: String = "Sadhaka",
  val x: Double = 1.5,
  val y: Double = 1.5,
  val angle: Double = 0.0,
  val prana: Long = 100L,
  val diyaLit: Boolean = true,
  val chantingMantra: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class TempleInfo(
  val id: String,
  val name: String,
  val dynastyAndEra: String,
  val location: String,
  val entityName: String,
  val entityTitle: String,
  val curseLore: String,
  val difficultyLabel: String,
  val requiredDiyas: Int,
  val requiredSeals: Int,
  val mapGrid: List<String>
)

data class CodexEntry(
  val id: String,
  val title: String,
  val sanskritName: String,
  val category: String, // "Mythological Creature", "Sacred Temple", "Vedic Ritual"
  val threatLevel: String,
  val description: String,
  val counterRitual: String
)

object SanataniLoreCatalog {
  val temples = listOf(
    TempleInfo(
      id = "brihadeeswarar",
      name = "Brihadeeswarar Sanctum",
      dynastyAndEra = "Chola Dynasty • 1010 CE",
      location = "Thanjavur, Tamil Nadu",
      entityName = "Brahmarakshasa",
      entityTitle = "The Fallen Vedic Scholar",
      curseLore = "Beneath the 216-foot Vimana tower lies a sealed subterranean Garbhagriha where a proud scholar corrupted the Atharva chants. His shadow stalks anyone whose Diya flickers out.",
      difficultyLabel = "Rahasya (Medium)",
      requiredDiyas = 5,
      requiredSeals = 3,
      mapGrid = listOf(
        "111111111111",
        "100020001001",
        "101110101001",
        "101000100031",
        "101021111011",
        "100000000001",
        "111011011101",
        "103010010201",
        "101010000101",
        "101000110001",
        "102010003041",
        "111111111111"
      )
    ),
    TempleInfo(
      id = "konark",
      name = "Konark Sun Crypt",
      dynastyAndEra = "Eastern Ganga Dynasty • 1250 CE",
      location = "Puri Coast, Odisha",
      entityName = "Chhaya Pret",
      entityTitle = "The Eclipse Devourer",
      curseLore = "Inside the collapsed Natamandira chariot wheels, an ancient lunar eclipse trapped a shadow entity that feeds on sound and extinguishes brass lamps from afar.",
      difficultyLabel = "Bhayanak (Hard)",
      requiredDiyas = 5,
      requiredSeals = 3,
      mapGrid = listOf(
        "111111111111",
        "100201000201",
        "101101011101",
        "100000010301",
        "111011010101",
        "103010000001",
        "101110111101",
        "100020100001",
        "101011101101",
        "101000301201",
        "102010000041",
        "111111111111"
      )
    ),
    TempleInfo(
      id = "kailasa",
      name = "Kailasa Monolith Abyss",
      dynastyAndEra = "Rashtrakuta Dynasty • 760 CE",
      location = "Ellora Caves, Maharashtra",
      entityName = "Pishacha Vetala",
      entityTitle = "The Monolith Stalker",
      curseLore = "Carved top-down from a single basalt cliff, the lower sacrificial galleries echo with phantom temple bells. Only synchronized Vedic mantras and Surya Yantra alignment can break its curse.",
      difficultyLabel = "Pralaya (Nightmare)",
      requiredDiyas = 5,
      requiredSeals = 3,
      mapGrid = listOf(
        "111111111111",
        "102000100201",
        "101110101101",
        "101300000101",
        "101011110101",
        "100010000301",
        "101010111101",
        "101200100201",
        "101110101101",
        "103000000001",
        "101110120041",
        "111111111111"
      )
    ),
    TempleInfo(
      id = "meenakshi",
      name = "Meenakshi Thousand-Pillar Labyrinth",
      dynastyAndEra = "Pandya & Nayak Dynasty • 1190 CE",
      location = "Madurai, Tamil Nadu",
      entityName = "Mayavi Vetala",
      entityTitle = "The Musical Pillar Phantom",
      curseLore = "Beneath the Ayiram Kaal Mandapam (Hall of a Thousand Musical Pillars) lies a flooded subterranean lotus sanctum where each granite column rings a different carnatic swara—and a Mayavi Vetala mimics your footsteps in the fog.",
      difficultyLabel = "Maha-Rahasya (Expert)",
      requiredDiyas = 5,
      requiredSeals = 3,
      mapGrid = listOf(
        "111111111111",
        "100200100201",
        "101110101101",
        "100030003001",
        "111011111011",
        "102000000021",
        "101110101101",
        "100010101001",
        "101010001011",
        "103000100201",
        "100010000041",
        "111111111111"
      )
    )
  )

  val codexEntries = listOf(
    CodexEntry(
      id = "brahmarakshasa",
      title = "Brahmarakshasa",
      sanskritName = "ब्रह्मराक्षस",
      category = "Mythological Creature",
      threatLevel = "Extreme • Sound & Darkness Hunter",
      description = "According to ancient Puranic texts, a Brahmarakshasa is the cursed spirit of a learned scholar who misused sacred Vedic knowledge in life. Towering in stature, draped in charred Yajnopavita threads and Rudraksha beads, it patrols the inner sanctum chanting inverted mantras.",
      counterRitual = "Keep your brass Diya lit to preserve Prana. When its footsteps echo closely, ring a sacred Ghanta (temple bell) or chant the Gayatri/Maha Mrityunjaya kavach to force it back into the shadows."
    ),
    CodexEntry(
      id = "chhaya_pret",
      title = "Chhaya Pret",
      sanskritName = "छाया प्रेत",
      category = "Mythological Creature",
      threatLevel = "High • Light Drainer",
      description = "Born during a total solar eclipse inside the Konark Sun Crypt, the Chhaya Pret moves silently along carved stone reliefs. It merges with pillar shadows and drains the oil from nearby Diyas.",
      counterRitual = "Align the Surya Yantra stone rings quickly and avoid sprinting in dark corridors without a lit Diya."
    ),
    CodexEntry(
      id = "pishacha_vetala",
      title = "Pishacha & Vetala",
      sanskritName = "पिशाच वेताल",
      category = "Mythological Creature",
      threatLevel = "Lethal • Ambush Predator",
      description = "Flesh-eating nocturnal spirits and guardian Vetalas that hang from the monolithic ceilings of Kailasa Cave 16. They test intruders with riddles of Dharma and cosmic order.",
      counterRitual = "Solve the Navagraha and Swara Nada bell sequences without error; wrong strikes immediately summon the Vetala to your coordinates."
    ),
    CodexEntry(
      id = "brihadeeswarar",
      title = "Brihadeeswarar Garbhagriha",
      sanskritName = "बृहदीश्वर गर्भगृह",
      category = "Sacred Temple",
      threatLevel = "Sanctum Depth • Level I",
      description = "Constructed by Emperor Rajaraja Chola I entirely out of interlocking granite without mortar. Its shadow-free Vimana conceals acoustic chambers tuned to 108 Hz.",
      counterRitual = "Light all 5 Ashtadhatu Diyas and unlock 3 Vedic Seals to unseal the Swarga Dwara exit portal."
    ),
    CodexEntry(
      id = "surya_yantra",
      title = "Surya Chakra & Navagraha Yantra",
      sanskritName = "सूर्य यन्त्र",
      category = "Vedic Ritual",
      threatLevel = "Ancient Mechanism",
      description = "Concentric stone rings engraved with solar and planetary mantras. Rotating the inner and outer stone wheels into cosmic alignment channels solar Prana through the dark sanctum.",
      counterRitual = "Align all three concentric chakra rings to 0° (North/East Surya axis) before the guardian breaches the chamber."
    ),
    CodexEntry(
      id = "nada_brahma_bells",
      title = "Nada Brahma Ghanta Ritual",
      sanskritName = "नाद ब्रह्म घण्टा",
      category = "Vedic Ritual",
      threatLevel = "Acoustic Seal",
      description = "Ancient bronze temple bells cast from seven sacred metals (Saptadhatu). When struck in the prescribed Vedic Swara sequence, their overtones shatter demonic illusions.",
      counterRitual = "Memorize the glowing Sanskrit syllable sequence (OM • HRIM • SHRIM • KLIM) and strike the corresponding bells in exact order."
    )
  )
}
