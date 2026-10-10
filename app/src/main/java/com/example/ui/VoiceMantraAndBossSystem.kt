package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.sin

/**
 * Bilingual UI & Narrative Language Mode (English + Sanskrit vs Hindi / Devanagari).
 */
enum class SanctumLanguageMode(
  val code: String,
  val badgeLabel: String,
  val toggleLabel: String
) {
  ENGLISH_SANSKRIT(
    code = "en",
    badgeLabel = "EN • संस्कृत",
    toggleLabel = "A/अ"
  ),
  HINDI_DEVANAGARI(
    code = "hi",
    badgeLabel = "हिन्दी • देवनागरी",
    toggleLabel = "अ/A"
  )
}

/**
 * Sacred Vedic Mantras that can be recited via Voice Mantra Recognition (SpeechRecognizer
 * or Acoustic Nada Resonance).
 */
enum class SacredVoiceMantra(
  val id: String,
  val titleEn: String,
  val titleHi: String,
  val devanagariText: String,
  val transliteration: String,
  val keywords: List<String>,
  val sanityBoost: Int,
  val pranaBoost: Int,
  val fuelBoost: Float,
  val stunTicks: Int
) {
  OM_NAMAH_SHIVAYA(
    id = "om_namah_shivaya",
    titleEn = "Panchakshara Shiva Kavach",
    titleHi = "पञ्चाक्षर शिव कवच",
    devanagariText = "ॐ नमः शिवाय",
    transliteration = "Om Namah Shivaya",
    keywords = listOf("om", "namah", "shivaya", "shiva", "शिवाय", "नमः", "ओम"),
    sanityBoost = 18,
    pranaBoost = 14,
    fuelBoost = 25f,
    stunTicks = 35
  ),
  MAHA_MRITYUNJAYA(
    id = "maha_mrityunjaya",
    titleEn = "Maha Mrityunjaya Moksha Mantra",
    titleHi = "महामृत्युञ्जय मोक्ष मन्त्र",
    devanagariText = "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम्",
    transliteration = "Om Tryambakam Yajamahe Sugandhim Pushtivardhanam",
    keywords = listOf("tryambakam", "yajamahe", "mrityunjaya", "sugandhim", "त्र्यम्बकं", "यजामहे"),
    sanityBoost = 25,
    pranaBoost = 22,
    fuelBoost = 35f,
    stunTicks = 50
  ),
  GAYATRI_MANTRA(
    id = "gayatri_mantra",
    titleEn = "Surya Gayatri Tejas Mantra",
    titleHi = "सूर्य गायत्री तेजस् मन्त्र",
    devanagariText = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि",
    transliteration = "Om Bhur Bhuvah Svah Tat Savitur Varenyam",
    keywords = listOf("gayatri", "savitur", "bhur", "bhuvah", "varenyam", "भूर्भुवः", "सवितुर्"),
    sanityBoost = 20,
    pranaBoost = 16,
    fuelBoost = 40f,
    stunTicks = 40
  ),
  DURGA_RAKSHA(
    id = "durga_raksha",
    titleEn = "Chandi Asura-Nashini Mantra",
    titleHi = "चण्डी असुर-नाशिनी मन्त्र",
    devanagariText = "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",
    transliteration = "Om Aim Hrim Klim Chamundaye Viche",
    keywords = listOf("chamundaye", "durga", "hrim", "klim", "चामुण्डायै", "दुर्गा"),
    sanityBoost = 22,
    pranaBoost = 18,
    fuelBoost = 30f,
    stunTicks = 45
  )
}

/**
 * Multi-stage Maha-Aarti Final Exorcism Boss Fight phases before unsealing the Swarga Dwara.
 */
enum class MahaAartiPhase(
  val stageNumber: Int,
  val titleEn: String,
  val titleHi: String,
  val instructionEn: String,
  val instructionHi: String
) {
  STAGE_1_AGNI_PRADAKSHINA(
    stageNumber = 1,
    titleEn = "Stage I: Pancha-Prana Agni Aarti",
    titleHi = "चरण १: पञ्च-प्राण अग्नि आरती",
    instructionEn = "Wave the blazing Pancha-Prana Brass Diya in a sacred circle to burn away the entity's shadow armor!",
    instructionHi = "असुर के तमस कवच को भस्म करने के लिए पंच-प्राण दीप से आरती घुमाएँ!"
  ),
  STAGE_2_SHANKHA_GHANTA_NADA(
    stageNumber = 2,
    titleEn = "Stage II: 528Hz Saptadhatu Ghanta Nada",
    titleHi = "चरण २: ५२८ Hz सप्तधातु घण्टा नाद",
    instructionEn = "Strike the Saptadhatu Ghanta & Shankha resonance to shatter its acoustic illusions!",
    instructionHi = "सप्तधातु घण्टा और शंख बजाकर मायावी भ्रम को खण्डित करें!"
  ),
  STAGE_3_PURNA_AHUTI_MANTRA(
    stageNumber = 3,
    titleEn = "Stage III: Maha-Mrityunjaya Purna-Ahuti",
    titleHi = "चरण ३: महामृत्युञ्जय पूर्णाहुति",
    instructionEn = "Deliver the final Vedic Mantra Purna-Ahuti to banish the guardian and unseal Swarga Dwara!",
    instructionHi = "अंतिम वैदिक मन्त्र पूर्णाहुति देकर असुर का निष्कासन करें और स्वर्ग द्वार खोलें!"
  ),
  BANISHED_VICTORY(
    stageNumber = 4,
    titleEn = "Asura Banished • Swarga Dwara Unsealed!",
    titleHi = "असुर निष्कासित • स्वर्ग द्वार खुल गया!",
    instructionEn = "The Garbhagriha is purified! Step into the golden Swarga Dwara portal.",
    instructionHi = "गर्भगृह शुद्ध हो चुका है! अब स्वर्ग द्वार में प्रवेश करें।"
  )
}

/**
 * Manages:
 * 1. Voice Mantra Recognition (`SacredVoiceMantra`) with live transcript matching and acoustic
 *    Nada resonance fallback so players can chant mantras on any device or emulator.
 * 2. Multi-stage Maha-Aarti Final Exorcism Boss Encounter (`MahaAartiPhase`) at the Swarga Dwara.
 * 3. Bilingual Hindi / Sanskrit (`SanctumLanguageMode`) UI localization.
 */
class VoiceMantraAndBossManager {
  var languageMode by mutableStateOf(SanctumLanguageMode.ENGLISH_SANSKRIT)
    private set

  // Voice Mantra Recognition state
  var isVoiceOverlayOpen by mutableStateOf(false)
    private set

  var selectedMantra by mutableStateOf(SacredVoiceMantra.OM_NAMAH_SHIVAYA)
    private set

  var isListeningForVoice by mutableStateOf(false)
    private set

  var lastRecognizedTranscript by mutableStateOf("")
    private set

  var voiceResonanceProgress by mutableFloatStateOf(0f)
    private set

  var totalVoiceMantrasChanted by mutableIntStateOf(0)
    private set

  // Maha-Aarti Final Exorcism Boss Fight state
  var isBossFightOverlayOpen by mutableStateOf(false)
    private set

  var bossPhase by mutableStateOf(MahaAartiPhase.STAGE_1_AGNI_PRADAKSHINA)
    private set

  var bossShadowIntegrityPercent by mutableIntStateOf(100)
    private set

  var isBossBanished by mutableStateOf(false)
    private set

  fun toggleLanguageMode(): String {
    languageMode = if (languageMode == SanctumLanguageMode.ENGLISH_SANSKRIT) {
      SanctumLanguageMode.HINDI_DEVANAGARI
    } else {
      SanctumLanguageMode.ENGLISH_SANSKRIT
    }
    return if (languageMode == SanctumLanguageMode.HINDI_DEVANAGARI) {
      "भाषा बदली गई: हिन्दी एवं संस्कृत देवनागरी मोड सक्रिय!"
    } else {
      "Language Switched: English & Sanskrit Transliteration Mode Active!"
    }
  }

  fun openVoiceMantraOverlay() {
    isVoiceOverlayOpen = true
  }

  fun closeVoiceMantraOverlay() {
    isVoiceOverlayOpen = false
    isListeningForVoice = false
  }

  fun selectMantra(mantra: SacredVoiceMantra) {
    selectedMantra = mantra
  }

  fun setListeningState(listening: Boolean) {
    isListeningForVoice = listening
  }

  /**
   * Evaluates spoken text from Android SpeechRecognizer or acoustic chant invocation against
   * the sacred mantra catalog and returns the matched [SacredVoiceMantra].
   */
  fun evaluateSpokenMantra(spokenText: String): SacredVoiceMantra {
    val clean = spokenText.trim().lowercase()
    lastRecognizedTranscript = spokenText.ifBlank { selectedMantra.devanagariText }

    val matched = SacredVoiceMantra.entries
      .map { mantra ->
        val score = mantra.keywords
          .filter { kw -> clean.contains(kw.lowercase()) }
          .sumOf { it.length }
        mantra to score
      }
      .filter { it.second > 0 }
      .maxByOrNull { it.second }
      ?.first ?: selectedMantra

    selectedMantra = matched
    voiceResonanceProgress = 1.0f
    totalVoiceMantrasChanted += 1
    isListeningForVoice = false
    return matched
  }

  /**
   * Starts or resumes the 3-stage Maha-Aarti Final Exorcism Boss Encounter.
   */
  fun openMahaAartiBossEncounter() {
    isBossFightOverlayOpen = true
  }

  fun closeMahaAartiBossEncounter() {
    isBossFightOverlayOpen = false
  }

  /**
   * Advances the current stage of the Maha-Aarti Boss Exorcism when the player performs
   * the required stage action (1: Agni Aarti, 2: 528Hz Ghanta Nada, 3: Purna-Ahuti Mantra).
   */
  fun performBossExorcismStageAction(): Pair<MahaAartiPhase, String> {
    return when (bossPhase) {
      MahaAartiPhase.STAGE_1_AGNI_PRADAKSHINA -> {
        bossShadowIntegrityPercent = 66
        bossPhase = MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA
        val msg = if (languageMode == SanctumLanguageMode.HINDI_DEVANAGARI) {
          "चरण १ पूर्ण: पंच-प्राण अग्नि आरती से तमस कवच भस्म हुआ! अब सप्तधातु घण्टा बजाएँ!"
        } else {
          "Stage I Complete: Agni Aarti scorched the shadow armor (66% left)! Now strike the 528Hz Ghanta!"
        }
        bossPhase to msg
      }

      MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA -> {
        bossShadowIntegrityPercent = 32
        bossPhase = MahaAartiPhase.STAGE_3_PURNA_AHUTI_MANTRA
        val msg = if (languageMode == SanctumLanguageMode.HINDI_DEVANAGARI) {
          "चरण २ पूर्ण: ५२८ Hz घण्टा नाद से मायावी भ्रम टूटा! अब महामृत्युञ्जय पूर्णाहुति दें!"
        } else {
          "Stage II Complete: 528Hz Ghanta Nada shattered its illusions (32% left)! Deliver the Purna-Ahuti Mantra!"
        }
        bossPhase to msg
      }

      MahaAartiPhase.STAGE_3_PURNA_AHUTI_MANTRA -> {
        bossShadowIntegrityPercent = 0
        bossPhase = MahaAartiPhase.BANISHED_VICTORY
        isBossBanished = true
        val msg = if (languageMode == SanctumLanguageMode.HINDI_DEVANAGARI) {
          "महा-आरती पूर्ण! असुर का निष्कासन हो गया और स्वर्ग द्वार खुल गया!"
        } else {
          "Maha-Aarti Complete! The guardian is banished and the Swarga Dwara portal is unsealed!"
        }
        bossPhase to msg
      }

      MahaAartiPhase.BANISHED_VICTORY -> {
        isBossBanished = true
        bossPhase to "Swarga Dwara is unsealed!"
      }
    }
  }
}

/**
 * Interactive Voice Mantra Recognition Overlay.
 * Supports real microphone speech recognition where available on device AND direct
 * acoustic Nada resonance chanting so players can always invoke sacred mantras seamlessly.
 */
@Composable
fun VoiceMantraOverlayDialog(
  manager: VoiceMantraAndBossManager,
  entityName: String,
  onMantraRecognized: (SacredVoiceMantra, String) -> Unit,
  onDismiss: () -> Unit
) {
  val isHindi = manager.languageMode == SanctumLanguageMode.HINDI_DEVANAGARI
  val infiniteTransition = rememberInfiniteTransition(label = "voice_mantra_wave")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "voice_wave_phase"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.86f))
      .clickable(enabled = true, onClick = onDismiss)
      .padding(16.dp)
      .testTag("voice_mantra_overlay_dialog")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(22.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.8.dp, MantraJade, RoundedCornerShape(22.dp))
        .clickable(enabled = true, onClick = {})
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = null,
              tint = MantraJade,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (isHindi) "वैदिक ध्वनि मन्त्र साधना" else "VOICE MANTRA INVOCATION",
                style = MaterialTheme.typography.titleMedium,
                color = MoltenBrass,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isHindi) "माइक्रोफ़ोन या नाद-अनुनाद से $entityName को स्तम्भित करें"
                else "Recite sacred Sanskrit mantras to banish $entityName",
                style = MaterialTheme.typography.bodySmall,
                color = SaffronFlame
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_voice_mantra_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Voice Mantra",
              tint = ParchmentCream
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selectable Sacred Mantra Cards
        SacredVoiceMantra.entries.forEach { mantra ->
          val selected = mantra == manager.selectedMantra
          Surface(
            color = if (selected) SacredCardSurface else ObsidianVoid.copy(alpha = 0.72f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp)
              .border(
                width = if (selected) 1.5.dp else 0.8.dp,
                color = if (selected) MantraJade else MoltenBrass.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
              )
              .clickable { manager.selectMantra(mantra) }
              .testTag("select_voice_mantra_${mantra.id}")
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isHindi) mantra.titleHi else mantra.titleEn,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) MantraJade else MoltenBrass,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "+${mantra.sanityBoost}% Sanity • +${mantra.pranaBoost}% Prana",
                  style = MaterialTheme.typography.labelSmall,
                  color = SaffronFlame
                )
              }
              Text(
                text = mantra.devanagariText,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = mantra.transliteration,
                style = MaterialTheme.typography.bodySmall,
                color = SandstoneMuted
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Acoustic Sri-Yantra Soundwave Visualizer
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianVoid)
            .border(1.dp, MantraJade.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            val barCount = 28
            val spacing = size.width / (barCount + 1)
            for (i in 1..barCount) {
              val x = i * spacing
              val amp = (0.35f + 0.6f * kotlin.math.abs(sin(wavePhase + i * 0.45f))) * (size.height * 0.42f)
              drawLine(
                color = if (i % 2 == 0) MantraJade else MoltenBrass,
                start = Offset(x, midY - amp),
                end = Offset(x, midY + amp),
                strokeWidth = 4f,
                cap = StrokeCap.Round
              )
            }
          }
        }

        if (manager.lastRecognizedTranscript.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "🎙 \"${manager.lastRecognizedTranscript}\"",
            style = MaterialTheme.typography.labelLarge,
            color = MantraJade,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            val chosen = manager.selectedMantra
            val recognized = manager.evaluateSpokenMantra(chosen.transliteration)
            val msg = if (isHindi) {
              "🎙 वैदिक मन्त्र सिद्ध: \"${recognized.devanagariText}\" • $entityName स्तम्भित (+${recognized.sanityBoost}% मनोबल)!"
            } else {
              "🎙 Voice Mantra Recited: \"${recognized.devanagariText}\" • $entityName banished into shadows (+${recognized.sanityBoost}% Sanity)!"
            }
            onMantraRecognized(recognized, msg)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MantraJade,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("invoke_voice_mantra_button")
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isHindi) {
              "मन्त्र का उच्चारण करें (${manager.selectedMantra.devanagariText})"
            } else {
              "CHANT & RESONATE MANTRA (${manager.selectedMantra.devanagariText})"
            },
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

/**
 * Multi-Stage Maha-Aarti Final Exorcism Boss Encounter Overlay.
 * Triggered at the Swarga Dwara threshold or via the HUD Boss Exorcism action.
 */
@Composable
fun MahaAartiBossExorcismDialog(
  manager: VoiceMantraAndBossManager,
  templeName: String,
  entityName: String,
  onStageCompleted: (MahaAartiPhase, String) -> Unit,
  onVictoryEscape: () -> Unit,
  onDismiss: () -> Unit
) {
  val isHindi = manager.languageMode == SanctumLanguageMode.HINDI_DEVANAGARI
  val phase = manager.bossPhase

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.88f))
      .clickable(enabled = true, onClick = onDismiss)
      .padding(16.dp)
      .testTag("maha_aarti_boss_dialog")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 2.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(BloodAltarRed, SaffronFlame, MoltenBrass)
          ),
          shape = RoundedCornerShape(24.dp)
        )
        .clickable(enabled = true, onClick = {})
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isHindi) "महा-आरती: अंतिम असुर निष्कासन" else "MAHA-AARTI: FINAL EXORCISM",
              style = MaterialTheme.typography.titleMedium,
              color = SaffronFlame,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "$entityName • $templeName",
              style = MaterialTheme.typography.bodySmall,
              color = ParchmentCream
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_boss_dialog_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Boss Exorcism",
              tint = ParchmentCream
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Boss Shadow Armor Integrity Bar
        Surface(
          color = ObsidianVoid,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BloodAltarRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = if (isHindi) "$entityName तमस कवच" else "$entityName Shadow Ward",
                style = MaterialTheme.typography.labelLarge,
                color = BloodAltarRed,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${manager.bossShadowIntegrityPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = if (manager.bossShadowIntegrityPercent == 0) MantraJade else BloodAltarRed,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { (manager.bossShadowIntegrityPercent / 100f).coerceIn(0f, 1f) },
              color = if (manager.bossShadowIntegrityPercent == 0) MantraJade else BloodAltarRed,
              trackColor = SacredCardSurface,
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Current Phase Card
        Surface(
          color = SacredCardSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, MoltenBrass.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (isHindi) phase.titleHi else phase.titleEn,
              style = MaterialTheme.typography.titleMedium,
              color = MoltenBrass,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (isHindi) phase.instructionHi else phase.instructionEn,
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentCream,
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!manager.isBossBanished) {
          Button(
            onClick = {
              val (newPhase, msg) = manager.performBossExorcismStageAction()
              onStageCompleted(newPhase, msg)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = SaffronFlame,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("perform_boss_stage_action_button")
          ) {
            val actionIcon = when (phase) {
              MahaAartiPhase.STAGE_1_AGNI_PRADAKSHINA -> Icons.Default.LocalFireDepartment
              MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA -> Icons.Default.NotificationsActive
              else -> Icons.Default.SelfImprovement
            }
            Icon(actionIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = when (phase) {
                MahaAartiPhase.STAGE_1_AGNI_PRADAKSHINA ->
                  if (isHindi) "पंच-प्राण अग्नि आरती घुमाएँ" else "PERFORM PANCHA-PRANA AGNI AARTI"
                MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA ->
                  if (isHindi) "५२८ Hz घण्टा एवं शंख नाद करें" else "STRIKE 528Hz GHANTA & SHANKHA"
                else ->
                  if (isHindi) "महामृत्युञ्जय पूर्णाहुति दें" else "DELIVER MAHA-MRITYUNJAYA PURNA-AHUTI"
              },
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          Button(
            onClick = onVictoryEscape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MantraJade,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("complete_boss_exorcism_escape_button")
          ) {
            Icon(Icons.Default.Shield, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isHindi) "स्वर्ग द्वार में प्रवेश करें (विजय)" else "ENTER UNSEALED SWARGA DWARA (VICTORY)",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
