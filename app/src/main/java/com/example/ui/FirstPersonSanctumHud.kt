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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.AtmosphereState
import com.example.audio.CreatureProximityZone
import com.example.audio.SpatialCueType
import com.example.audio.SurfaceMaterial
import com.example.data.TempleInfo
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
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Represents a live objective inside the first-person HUD tracker.
 */
data class SanctumObjectiveItem(
  val id: String,
  val title: String,
  val progressText: String,
  val isCompleted: Boolean,
  val isPrimary: Boolean = false
)

/**
 * Dedicated First-Person Heads-Up Display (HUD) for Garbhagriha:
 * - Vital Health (Prana) Bar with critical crimson pulse & numerical percentage
 * - Sanity (Manas / Citta) Radial & Linear Meter with psychological stability status
 * - Brass Diya Ghee Lamp Fuel Gauge
 * - Directional Compass Bearing Ribbon (N • NE • E • SE • S • SW • W • NW) with nearest target distance & bearing
 * - Active Objective Tracker Checklist (Diyas, Vedic Seals, and Swarga Dwara Extraction)
 */
@Composable
fun FirstPersonSanctumHud(
  temple: TempleInfo,
  roomCode: String?,
  coOpPlayerCount: Int,
  healthPercent: Int,
  sanityPercent: Int,
  staminaPercent: Int = 100,
  isSprinting: Boolean = false,
  isExhausted: Boolean = false,
  mentalState: SanityMentalState = SanityMentalState.LUCID,
  sanityWhisper: String? = null,
  creatureBehaviorState: CreatureBehaviorState = CreatureBehaviorState.DORMANT_PATROL,
  diyaOilPercent: Float,
  diyaLit: Boolean,
  torchFlameState: TorchFlameState = if (diyaLit) TorchFlameState.BLAZING_AGNI else TorchFlameState.EXTINGUISHED_DARKNESS,
  torchLightRadiusTiles: Float = if (diyaLit) 7.5f else 2.2f,
  chantingMantra: Boolean,
  diyasLitCount: Int,
  sealsSolvedCount: Int,
  playerX: Double,
  playerY: Double,
  playerAngle: Double,
  entityX: Double,
  entityY: Double,
  currentAtmosphere: AtmosphereState,
  currentSurfaceMaterial: SurfaceMaterial = SurfaceMaterial.STONE,
  environmentalEffectState: EnvironmentalEffectState = EnvironmentalEffectState.STILL_AIR,
  shakeIntensityState: ShakeIntensityState = ShakeIntensityState.STILL,
  shakePercent: Int = 0,
  isShakeEnabled: Boolean = true,
  torchShadowState: TorchShadowState = TorchShadowState.CLEAR_CORRIDOR,
  activeShadowCasters: Int = 0,
  ritualProgressState: RitualProgressState = RitualProgressState.DORMANT,
  ritualStepsCompleted: Int = 0,
  ritualTotalSteps: Int = 5,
  secretChamberName: String = "Gupta Garbhagriha",
  creatureProximityZone: CreatureProximityZone = CreatureProximityZone.OUT_OF_RANGE,
  lastCreatureAudioCue: SpatialCueType? = null,
  mapExplorationPercent: Int = 0,
  languageMode: SanctumLanguageMode = SanctumLanguageMode.ENGLISH_SANSKRIT,
  isBossBanished: Boolean = false,
  godsFavorScore: Int = 55,
  godsFavorStanding: GodsFavorStanding = GodsFavorStanding.NEUTRAL_SEEKER,
  activeFavorBuff: Pair<DivinePassiveBuff, Int>? = null,
  isMuted: Boolean,
  onOpenExpandedMap: () -> Unit = {},
  onOpenGodsFavor: () -> Unit = {},
  onToggleLanguage: () -> Unit = {},
  onToggleShake: () -> Unit = {},
  onCyclePostProcess: () -> Unit,
  onToggleMute: () -> Unit,
  onExitSanctum: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isHindi = languageMode == SanctumLanguageMode.HINDI_DEVANAGARI
  val infiniteTransition = rememberInfiniteTransition(label = "hud_vital_pulse")
  val criticalPulse by infiniteTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "critical_pulse_alpha"
  )

  val entityDistance = hypot(entityX - playerX, entityY - playerY)
  val canEscape = diyasLitCount >= temple.requiredDiyas || sealsSolvedCount >= temple.requiredSeals

  val objectives = listOf(
    SanctumObjectiveItem(
      id = "obj_diyas",
      title = if (isHindi) "पञ्च अष्टधातु दीप प्रज्वलित करें" else "Ignite Sacred Ashtadhatu Diyas",
      progressText = "$diyasLitCount / ${temple.requiredDiyas}",
      isCompleted = diyasLitCount >= temple.requiredDiyas,
      isPrimary = !canEscape
    ),
    SanctumObjectiveItem(
      id = "obj_seals",
      title = if (isHindi) "वैदिक सूर्य एवं घण्टा मुद्राएँ खोलें" else "Decipher Vedic Surya & Ghanta Seals",
      progressText = "$sealsSolvedCount / ${temple.requiredSeals}",
      isCompleted = sealsSolvedCount >= temple.requiredSeals,
      isPrimary = !canEscape && diyasLitCount < temple.requiredDiyas
    ),
    SanctumObjectiveItem(
      id = "obj_secret_ritual",
      title = if (ritualProgressState == RitualProgressState.CHAMBER_UNLOCKED || ritualProgressState == RitualProgressState.SANCTUARY_CLAIMED) {
        if (isHindi) "गुप्त कक्ष खुला: $secretChamberName" else "Secret Chamber Unsealed: $secretChamberName"
      } else {
        if (isHindi) "गुप्त कक्ष खोलने हेतु वैदिक क्रम पूर्ण करें" else "Perform Ritual Sequence to Unseal Secret Chamber"
      },
      progressText = if (ritualProgressState == RitualProgressState.CHAMBER_UNLOCKED || ritualProgressState == RitualProgressState.SANCTUARY_CLAIMED) {
        if (isHindi) "खुला" else "UNSEALED"
      } else {
        "$ritualStepsCompleted / $ritualTotalSteps"
      },
      isCompleted = ritualProgressState == RitualProgressState.CHAMBER_UNLOCKED || ritualProgressState == RitualProgressState.SANCTUARY_CLAIMED,
      isPrimary = ritualProgressState == RitualProgressState.IN_PROGRESS
    ),
    SanctumObjectiveItem(
      id = "obj_escape",
      title = if (canEscape) {
        if (isHindi) "महा-आरती पूर्ण कर स्वर्ग द्वार में प्रवेश करें!" else "Complete Maha-Aarti & Enter Swarga Dwara!"
      } else {
        if (isHindi) "स्वर्ग द्वार खोलें (दीप या मुद्रा पूर्ण करें)" else "Unseal Swarga Dwara Sanctum Gate"
      },
      progressText = if (isBossBanished) "PURIFIED" else if (canEscape) "OPEN" else "SEALED",
      isCompleted = isBossBanished,
      isPrimary = canEscape
    )
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("first_person_hud_root"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Top Vitals & Tactical Bar (Health, Sanity Meter, Diya Oil, Compass & Quick Toggles)
    Surface(
      color = SanctumGranite.copy(alpha = 0.90f),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = 1.2.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              if (healthPercent <= 35 || sanityPercent <= 35) BloodAltarRed.copy(alpha = criticalPulse)
              else MoltenBrass.copy(alpha = 0.55f),
              SaffronFlame.copy(alpha = 0.45f),
              MoltenBrass.copy(alpha = 0.55f)
            )
          ),
          shape = RoundedCornerShape(18.dp)
        )
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Header Row: Temple Title and System Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = temple.name.uppercase(),
              style = MaterialTheme.typography.labelLarge,
              color = MoltenBrass,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (roomCode != null) "CO-OP [$roomCode] ($coOpPlayerCount) • ${currentAtmosphere.label} • ${currentSurfaceMaterial.displayName}"
              else "${currentAtmosphere.label} • ${currentSurfaceMaterial.displayName} • Threat: ${"%.1f".format(entityDistance)}m",
              style = MaterialTheme.typography.bodyMedium,
              color = if (entityDistance < 3.6) BloodAltarRed else if (roomCode != null) SacredTeal else SandstoneMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onToggleLanguage,
              modifier = Modifier
                .size(32.dp)
                .testTag("toggle_language_button")
            ) {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Toggle Hindi / Sanskrit / English Language",
                tint = if (isHindi) MantraJade else MoltenBrass,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onToggleShake,
              modifier = Modifier
                .size(32.dp)
                .testTag("toggle_shake_button")
            ) {
              Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = "Toggle Screen Shake and Haptics",
                tint = if (isShakeEnabled) MoltenBrass else SandstoneMuted,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onCyclePostProcess,
              modifier = Modifier
                .size(32.dp)
                .testTag("cycle_post_process_button")
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Cycle Horror Post-Process FX Preset",
                tint = SaffronFlame,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onToggleMute,
              modifier = Modifier
                .size(32.dp)
                .testTag("toggle_audio_button")
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Toggle Spatial Sound",
                tint = MoltenBrass,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onExitSanctum,
              modifier = Modifier
                .size(32.dp)
                .testTag("exit_sanctum_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Exit Sanctum",
                tint = ParchmentCream,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Full-Width Tactical Status Chips Row
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          CompassBearingChip(playerAngle = playerAngle)
          FogOfWarCartographyChip(
            explorationPercent = mapExplorationPercent,
            isHindi = isHindi,
            onClick = onOpenExpandedMap
          )
          GodsFavorHudChip(
            favorScore = godsFavorScore,
            standing = godsFavorStanding,
            activeBuff = activeFavorBuff,
            isHindi = isHindi,
            onClick = onOpenGodsFavor
          )
          SurfaceMaterialChip(surfaceMaterial = currentSurfaceMaterial)
          if (environmentalEffectState != EnvironmentalEffectState.STILL_AIR) {
            EnvironmentalEffectChip(effectState = environmentalEffectState)
          }
          if (shakeIntensityState != ShakeIntensityState.STILL && isShakeEnabled) {
            ScreenShakeChip(
              shakeState = shakeIntensityState,
              shakePercent = shakePercent
            )
          }
          if (activeShadowCasters > 0 || torchShadowState == TorchShadowState.NO_TORCH_UMBRA) {
            TorchShadowChip(
              shadowState = torchShadowState,
              casterCount = activeShadowCasters
            )
          }
          if (ritualProgressState != RitualProgressState.DORMANT) {
            RitualProgressChip(
              progressState = ritualProgressState,
              stepsCompleted = ritualStepsCompleted,
              totalSteps = ritualTotalSteps
            )
          }
          if (creatureProximityZone != CreatureProximityZone.OUT_OF_RANGE) {
            CreatureAudioCueChip(
              proximityZone = creatureProximityZone,
              lastCue = lastCreatureAudioCue
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Vitals Row: Health (Prana), Sanity (Manas), Stamina (Sprint Energy), and Diya Oil
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // HEALTH (PRANA VITALITY) METER
          VitalMeterColumn(
            icon = Icons.Default.Favorite,
            label = "HEALTH",
            valueText = "$healthPercent%",
            progress = healthPercent / 100f,
            activeColor = if (healthPercent > 35) MantraJade else BloodAltarRed.copy(alpha = criticalPulse),
            testTag = "hud_health_meter",
            modifier = Modifier.weight(0.95f)
          )

          // SANITY (MANAS / CITTA) METER + RADIAL THIRD-EYE GAUGE
          SanityMeterWidget(
            sanityPercent = sanityPercent,
            mentalState = mentalState,
            chantingMantra = chantingMantra,
            criticalPulse = criticalPulse,
            modifier = Modifier
              .weight(1.1f)
              .testTag("hud_sanity_meter")
          )

          // STAMINA (SPRINT EVASION ENERGY) METER
          VitalMeterColumn(
            icon = Icons.Default.DirectionsRun,
            label = when {
              isExhausted -> "WINDED"
              isSprinting -> "SPRINT"
              else -> "STAMINA"
            },
            valueText = "$staminaPercent%",
            progress = (staminaPercent / 100f).coerceIn(0f, 1f),
            activeColor = when {
              isExhausted -> BloodAltarRed.copy(alpha = criticalPulse)
              isSprinting -> MoltenBrass
              else -> SacredTeal
            },
            testTag = "hud_stamina_meter",
            modifier = Modifier.weight(0.95f)
          )

          // DIYA TORCH FUEL & DYNAMIC LIGHT RADIUS METER
          VitalMeterColumn(
            icon = Icons.Default.LocalFireDepartment,
            label = torchFlameState.hudLabel,
            valueText = "${diyaOilPercent.roundToInt()}% (${"%.1f".format(torchLightRadiusTiles)}m)",
            progress = (diyaOilPercent / 100f).coerceIn(0f, 1f),
            activeColor = when (torchFlameState) {
              TorchFlameState.BLAZING_AGNI -> SaffronFlame
              TorchFlameState.STEADY_FLAME -> MoltenBrass
              TorchFlameState.SPUTTERING_EMBER -> BloodAltarRed.copy(alpha = criticalPulse)
              TorchFlameState.EXTINGUISHED_DARKNESS -> SandstoneMuted
            },
            testTag = "hud_diya_meter",
            modifier = Modifier.weight(0.95f)
          )
        }

        // Optional Psychological Whisper / Mental State Subtitle when sanity is affected
        if (!sanityWhisper.isNullOrBlank() || sanityPercent < 70) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = sanityWhisper ?: "${mentalState.sanskritTerm} — ${mentalState.description}",
            style = MaterialTheme.typography.labelLarge,
            color = if (sanityPercent < 35) BloodAltarRed else SaffronFlame,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("hud_sanity_whisper_banner")
          )
        }
      }
    }

    // 2. Active Objective Indicators Tracker Panel
    ActiveObjectivesTrackerCard(
      objectives = objectives,
      entityName = temple.entityName,
      entityDistance = entityDistance,
      creatureBehaviorState = creatureBehaviorState
    )
  }
}

@Composable
private fun VitalMeterColumn(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  valueText: String,
  progress: Float,
  activeColor: Color,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(ObsidianVoid.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = activeColor,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = ParchmentCream
        )
      }
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelLarge,
        color = activeColor,
        fontWeight = FontWeight.Bold
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { progress.coerceIn(0f, 1f) },
      color = activeColor,
      trackColor = SacredCardSurface,
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(4.dp))
    )
  }
}

@Composable
private fun SanityMeterWidget(
  sanityPercent: Int,
  mentalState: SanityMentalState,
  chantingMantra: Boolean,
  criticalPulse: Float,
  modifier: Modifier = Modifier
) {
  val sanityColor = when (mentalState) {
    SanityMentalState.KAVACH_PROTECTED -> MoltenBrass
    SanityMentalState.LUCID -> SacredTeal
    SanityMentalState.UNEASY -> SaffronFlame
    SanityMentalState.SHAKEN -> SaffronFlame
    SanityMentalState.UNRAVELING -> BloodAltarRed.copy(alpha = criticalPulse)
  }

  val sanityStatus = if (chantingMantra) "KAVACH" else mentalState.statusLabel

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .background(ObsidianVoid.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
      .border(
        width = 1.dp,
        color = sanityColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
      )
      .padding(horizontal = 8.dp, vertical = 5.dp)
  ) {
    // Radial Ajna (Third-Eye) Sanity Arc Gauge
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(30.dp)
    ) {
      Canvas(modifier = Modifier.size(28.dp)) {
        val strokeW = 3.5.dp.toPx()
        drawArc(
          color = SacredCardSurface,
          startAngle = 135f,
          sweepAngle = 270f,
          useCenter = false,
          style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawArc(
          color = sanityColor,
          startAngle = 135f,
          sweepAngle = 270f * (sanityPercent / 100f).coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Center Third-Eye Bindu dot
        drawCircle(
          color = sanityColor,
          radius = 3.5f,
          center = Offset(size.width / 2f, size.height / 2f)
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "Sanity Meter",
            tint = sanityColor,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = sanityStatus,
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentCream
          )
        }
        Text(
          text = "$sanityPercent%",
          style = MaterialTheme.typography.labelLarge,
          color = sanityColor,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(3.dp))

      LinearProgressIndicator(
        progress = { (sanityPercent / 100f).coerceIn(0f, 1f) },
        color = sanityColor,
        trackColor = SacredCardSurface,
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
      )
    }
  }
}

@Composable
private fun ActiveObjectivesTrackerCard(
  objectives: List<SanctumObjectiveItem>,
  entityName: String,
  entityDistance: Double,
  creatureBehaviorState: CreatureBehaviorState
) {
  Surface(
    color = SanctumGranite.copy(alpha = 0.84f),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MoltenBrass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
      .testTag("hud_active_objectives_card")
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ACTIVE SANCTUM OBJECTIVES",
          style = MaterialTheme.typography.labelLarge,
          color = MoltenBrass
        )
        Text(
          text = if (entityDistance < 3.8 || creatureBehaviorState == CreatureBehaviorState.FRENZIED_CHASE) {
            "⚠ ${entityName.uppercase()} [${creatureBehaviorState.badgeLabel}]"
          } else {
            "👁 ${creatureBehaviorState.badgeLabel} • ${"%.1f".format(entityDistance)}m"
          },
          style = MaterialTheme.typography.labelLarge,
          color = when (creatureBehaviorState) {
            CreatureBehaviorState.FRENZIED_CHASE, CreatureBehaviorState.SANITY_AMBUSH -> BloodAltarRed
            CreatureBehaviorState.STUNNED_BY_NADA -> MantraJade
            CreatureBehaviorState.SHADOW_STALKING -> SaffronFlame
            CreatureBehaviorState.DORMANT_PATROL -> SandstoneMuted
          },
          modifier = Modifier.testTag("hud_entity_behavior_badge")
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      objectives.forEach { obj ->
        val rowColor = when {
          obj.isCompleted -> MantraJade
          obj.isPrimary -> SaffronFlame
          else -> ParchmentCream.copy(alpha = 0.85f)
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("objective_indicator_${obj.id}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (obj.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
              contentDescription = obj.title,
              tint = rowColor,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = obj.title,
              style = MaterialTheme.typography.bodyMedium,
              color = rowColor,
              fontWeight = if (obj.isPrimary || obj.isCompleted) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            color = ObsidianVoid.copy(alpha = 0.75f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = obj.progressText,
              style = MaterialTheme.typography.labelLarge,
              color = rowColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompassBearingChip(playerAngle: Double) {
  var deg = ((playerAngle * 180.0 / PI) % 360.0)
  if (deg < 0) deg += 360.0
  val directions = listOf("E", "SE", "S", "SW", "W", "NW", "N", "NE")
  val idx = (((deg + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)
  val cardinal = directions[idx]

  Surface(
    color = SacredCardSurface,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, MoltenBrass.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
      .testTag("hud_compass_chip")
  ) {
    Text(
      text = "🧭 $cardinal ${deg.roundToInt()}°",
      style = MaterialTheme.typography.labelLarge,
      color = ParchmentCream,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun SurfaceMaterialChip(surfaceMaterial: SurfaceMaterial) {
  val chipColor = when (surfaceMaterial) {
    SurfaceMaterial.STONE -> ParchmentCream
    SurfaceMaterial.DIRT -> SaffronFlame
    SurfaceMaterial.METAL -> MoltenBrass
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.78f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
      .testTag("hud_surface_material_chip")
  ) {
    Text(
      text = "👣 ${surfaceMaterial.id.uppercase()}",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun EnvironmentalEffectChip(effectState: EnvironmentalEffectState) {
  val chipColor = when (effectState) {
    EnvironmentalEffectState.FALLING_MASONRY_DUST -> SaffronFlame
    EnvironmentalEffectState.COBWEB_BRUSH -> ParchmentCream
    EnvironmentalEffectState.DRIFTING_SPORES -> MoltenBrass
    EnvironmentalEffectState.STILL_AIR -> SandstoneMuted
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.78f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
      .testTag("hud_environmental_effect_chip")
  ) {
    Text(
      text = "🕸 ${effectState.hudLabel}",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun ScreenShakeChip(
  shakeState: ShakeIntensityState,
  shakePercent: Int
) {
  val chipColor = when (shakeState) {
    ShakeIntensityState.VIOLENT_JUMPSCARE -> BloodAltarRed
    ShakeIntensityState.CHASE_SHUDDER -> SaffronFlame
    ShakeIntensityState.SUBTLE_TREMOR -> MoltenBrass
    ShakeIntensityState.STILL -> SandstoneMuted
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.82f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
      .testTag("hud_screen_shake_chip")
  ) {
    Text(
      text = "📳 ${shakeState.hudBadge} $shakePercent%",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun TorchShadowChip(
  shadowState: TorchShadowState,
  casterCount: Int
) {
  val chipColor = when (shadowState) {
    TorchShadowState.STATUE_SILHOUETTE -> SaffronFlame
    TorchShadowState.PILLAR_PENUMBRA -> MoltenBrass
    TorchShadowState.CLEAR_CORRIDOR -> ParchmentCream
    TorchShadowState.NO_TORCH_UMBRA -> SandstoneMuted
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.80f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
      .testTag("hud_torch_shadow_chip")
  ) {
    Text(
      text = if (casterCount > 0) "🕯 ${shadowState.hudLabel} ($casterCount)" else "🕯 ${shadowState.hudLabel}",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun RitualProgressChip(
  progressState: RitualProgressState,
  stepsCompleted: Int,
  totalSteps: Int
) {
  val chipColor = when (progressState) {
    RitualProgressState.SANCTUARY_CLAIMED, RitualProgressState.CHAMBER_UNLOCKED -> MantraJade
    RitualProgressState.IN_PROGRESS -> SaffronFlame
    RitualProgressState.DORMANT -> SandstoneMuted
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.82f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
      .testTag("hud_ritual_progress_chip")
  ) {
    Text(
      text = if (progressState == RitualProgressState.IN_PROGRESS) {
        "🕉 RITUAL $stepsCompleted/$totalSteps"
      } else {
        "🕉 ${progressState.badgeLabel}"
      },
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun CreatureAudioCueChip(
  proximityZone: CreatureProximityZone,
  lastCue: SpatialCueType?
) {
  val chipColor = when (proximityZone) {
    CreatureProximityZone.IMMINENT_TERROR_ROAR -> BloodAltarRed
    CreatureProximityZone.STALKING_GROWL -> SaffronFlame
    CreatureProximityZone.DISTANT_OMEN -> MoltenBrass
    CreatureProximityZone.OUT_OF_RANGE -> SandstoneMuted
  }
  val cueLabel = lastCue?.displayLabel ?: proximityZone.hudBadge
  Surface(
    color = ObsidianVoid.copy(alpha = 0.84f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
      .testTag("hud_creature_audio_cue_chip")
  ) {
    Text(
      text = "🔊 ${proximityZone.hudBadge} • $cueLabel",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun FogOfWarCartographyChip(
  explorationPercent: Int,
  isHindi: Boolean,
  onClick: () -> Unit
) {
  val chipColor = when {
    explorationPercent >= 65 -> MantraJade
    explorationPercent >= 25 -> MoltenBrass
    else -> SaffronFlame
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.82f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .testTag("hud_fog_of_war_map_chip")
  ) {
    Text(
      text = if (isHindi) "🗺 मानचित्र $explorationPercent%" else "🗺 MAP $explorationPercent%",
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

@Composable
private fun GodsFavorHudChip(
  favorScore: Int,
  standing: GodsFavorStanding,
  activeBuff: Pair<DivinePassiveBuff, Int>?,
  isHindi: Boolean,
  onClick: () -> Unit
) {
  val chipColor = when (standing) {
    GodsFavorStanding.EXALTED_ANUGRAHA -> MantraJade
    GodsFavorStanding.REVERENT_DEVOTEE -> MoltenBrass
    GodsFavorStanding.NEUTRAL_SEEKER -> SacredTeal
    GodsFavorStanding.TRANSGRESSOR_DOSHA -> BloodAltarRed
  }
  val labelText = if (activeBuff != null) {
    val (buff, sec) = activeBuff
    if (isHindi) "🙏 ${buff.sanskritName} (${sec}s)"
    else "🙏 ${buff.title.substringBefore(" ").uppercase()} ${sec}s"
  } else {
    if (isHindi) "🙏 अनुग्रह $favorScore%" else "🙏 FAVOR $favorScore%"
  }
  Surface(
    color = ObsidianVoid.copy(alpha = 0.84f),
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .border(0.8.dp, chipColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .testTag("hud_gods_favor_chip")
  ) {
    Text(
      text = labelText,
      style = MaterialTheme.typography.labelLarge,
      color = chipColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}


