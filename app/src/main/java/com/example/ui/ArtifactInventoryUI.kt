package com.example.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
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
import kotlin.math.sin

/**
 * Represents an ancient Sanatani artifact discovered inside a cursed temple sanctum.
 */
data class SacredArtifact(
  val id: String,
  val name: String,
  val sanskritTitle: String,
  val eraAndOrigin: String,
  val rarity: String,
  val inscription: String,
  val loreDescription: String,
  val ritualEffectSummary: String,
  val pranaBoost: Int,
  val oilBoost: Float,
  val stunSeconds: Int,
  val primaryColor: Color
)

object TempleArtifactCatalog {
  val allArtifacts = listOf(
    SacredArtifact(
      id = "rudraksha_mala",
      name = "Ek-Mukhi Rudraksha Mala",
      sanskritTitle = "एकमुखी रुद्राक्ष माला",
      eraAndOrigin = "Chola Sanctum Reliquary • 1010 CE",
      rarity = "Mythic Sivacharya Relic",
      inscription = "ॐ नमः शिवाय • Tears of Rudra",
      loreDescription = "A 108-bead rosary strung with consecrated silver wire around a rare single-faced Rudraksha seed. Worn by the high temple Tantrik to anchor the soul against Brahmarakshasa possession.",
      ritualEffectSummary = "Restores +35% Prana Vitality and grants a protective Vedic Kavach aura.",
      pranaBoost = 35,
      oilBoost = 10f,
      stunSeconds = 2,
      primaryColor = Color(0xFFFF8F00)
    ),
    SacredArtifact(
      id = "valampuri_shankha",
      name = "Valampuri Dakshinavarti Shankha",
      sanskritTitle = "दक्षिणावर्ती शंख",
      eraAndOrigin = "Puri Sea Shrine • 1250 CE",
      rarity = "Sacred Acoustic Conch",
      inscription = "पाञ्चजन्य नाद • Cosmic Ocean Spiral",
      loreDescription = "A clockwise-spiraling white conch shell mounted in embossed Chola bronze. Sounding its primordial Omkara blast shatters shadow illusions and paralyzes nocturnal Pretas.",
      ritualEffectSummary = "Stuns the hunting Guardian Entity for 5 seconds and restores +15% Prana.",
      pranaBoost = 15,
      oilBoost = 0f,
      stunSeconds = 5,
      primaryColor = Color(0xFF00E5FF)
    ),
    SacredArtifact(
      id = "surya_tamra_patra",
      name = "Surya Yantra Tamra Patra",
      sanskritTitle = "सूर्य यन्त्र ताम्रपत्र",
      eraAndOrigin = "Konark Inner Crypt • 1258 CE",
      rarity = "Solar Copper Plate",
      inscription = "ॐ घृणिः सूर्याय नमः",
      loreDescription = "A hammered copper talisman etched with the 9 Navagraha planetary squares and solar mantras. It radiates warm Agni energy that replenishes sacred ghee inside brass Diyas.",
      ritualEffectSummary = "Refills +60% Brass Diya Ghee Oil and re-ignites extinguished flames.",
      pranaBoost = 10,
      oilBoost = 60f,
      stunSeconds = 1,
      primaryColor = Color(0xFFFFCA28)
    ),
    SacredArtifact(
      id = "aghora_vibhuti_kalasha",
      name = "Vibhuti Bhasma Kalasha",
      sanskritTitle = "विभूति भस्म कलश",
      eraAndOrigin = "Kailasa Monolith Vault • 760 CE",
      rarity = "Sanctified Dhuni Urn",
      inscription = "भस्मनाङ्गानि संस्मरेत्",
      loreDescription = "An Ashtadhatu vessel containing sacred white ash from an eternal Vedic Homa fire. Scattering a pinch of Vibhuti forms an impenetrable barrier against Pishacha and Vetala spirits.",
      ritualEffectSummary = "Restores +25% Prana, +35% Diya Oil, and repels the entity for 4 seconds.",
      pranaBoost = 25,
      oilBoost = 35f,
      stunSeconds = 4,
      primaryColor = Color(0xFF00E676)
    ),
    SacredArtifact(
      id = "kirtimukha_darpana",
      name = "Aranmula Kirtimukha Mirror",
      sanskritTitle = "कीर्तिमुख दर्पण",
      eraAndOrigin = "Ancient Dravida Metallurgy • 985 CE",
      rarity = "Front-Surface Alloy Mirror",
      inscription = "सत्यं शिवं सुन्दरम्",
      loreDescription = "A hand-cast bell-metal mirror framed by the fierce Kirtimukha ('Face of Glory') guardian mask. Its distortion-free reflection reveals invisible Chhaya Pretas lurking behind pillars.",
      ritualEffectSummary = "Banishes shadow blindness, restores +20% Prana & +40% Diya Oil.",
      pranaBoost = 20,
      oilBoost = 40f,
      stunSeconds = 3,
      primaryColor = Color(0xFFFF5252)
    )
  )
}

/**
 * Stateful manager for collecting, inspecting, and invoking ancient Sanatani artifacts
 * during first-person temple exploration.
 */
class ArtifactInventoryManager {
  val collectedArtifactIds = mutableStateListOf<String>()
  var selectedArtifact by mutableStateOf<SacredArtifact>(TempleArtifactCatalog.allArtifacts.first())
    private set

  init {
    // Start explorer with the consecrated Rudraksha Mala in their satchel
    collectedArtifactIds.add("rudraksha_mala")
  }

  fun isCollected(artifactId: String): Boolean = collectedArtifactIds.contains(artifactId)

  fun selectForInspection(artifact: SacredArtifact) {
    selectedArtifact = artifact
  }

  /**
   * Unlocks the next undiscovered artifact from the catalog when the player searches a shrine,
   * lights a Diya sconce, or solves a Vedic seal.
   */
  fun collectNextUndiscoveredArtifact(): SacredArtifact? {
    val next = TempleArtifactCatalog.allArtifacts.firstOrNull { !collectedArtifactIds.contains(it.id) }
    if (next != null) {
      collectedArtifactIds.add(next.id)
      selectedArtifact = next
    }
    return next
  }

  fun collectSpecificArtifact(artifactId: String): SacredArtifact? {
    val found = TempleArtifactCatalog.allArtifacts.find { it.id == artifactId } ?: return null
    if (!collectedArtifactIds.contains(found.id)) {
      collectedArtifactIds.add(found.id)
    }
    selectedArtifact = found
    return found
  }

  /**
   * Restores the player's collected artifact satchel from a Sacred Shrine Room save.
   */
  fun replaceCollectedArtifacts(savedIds: Collection<String>) {
    collectedArtifactIds.clear()
    val validIds = savedIds.filter { id ->
      TempleArtifactCatalog.allArtifacts.any { it.id == id }
    }
    if (validIds.isEmpty()) {
      collectedArtifactIds.add("rudraksha_mala")
    } else {
      collectedArtifactIds.addAll(validIds.distinct())
    }
    val firstUnlocked = TempleArtifactCatalog.allArtifacts.firstOrNull {
      collectedArtifactIds.contains(it.id)
    } ?: TempleArtifactCatalog.allArtifacts.first()
    selectedArtifact = firstUnlocked
  }
}

/**
 * Interactive Inventory & Relic Inspection Modal Dialog (`ArtifactInventoryDialog`).
 * Allows players to browse collected artifacts, inspect a rotating 3D-style Canvas render
 * of the relic with its Sanskrit inscription, or invoke its Vedic power during gameplay.
 */
@Composable
fun ArtifactInventoryDialog(
  inventoryManager: ArtifactInventoryManager,
  soundManager: SoundManager,
  onInvokeArtifact: (SacredArtifact) -> Unit,
  onSearchNearbyReliquary: () -> Unit,
  onDismiss: () -> Unit
) {
  val selected = inventoryManager.selectedArtifact
  val isSelectedCollected = inventoryManager.isCollected(selected.id)

  val infiniteTransition = rememberInfiniteTransition(label = "artifact_inspection_spin")
  val haloRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 14000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "relic_halo_rotation"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, MoltenBrass, RoundedCornerShape(24.dp))
        .testTag("artifact_inventory_dialog")
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "SADHAKA RELIC SATCHEL",
              style = MaterialTheme.typography.titleLarge,
              color = MoltenBrass
            )
            Text(
              text = "Collected Artifacts: ${inventoryManager.collectedArtifactIds.size} / ${TempleArtifactCatalog.allArtifacts.size}",
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_inventory_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Inventory", tint = ParchmentCream)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Artifact Slot Strip
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(TempleArtifactCatalog.allArtifacts, key = { it.id }) { artifact ->
            val unlocked = inventoryManager.isCollected(artifact.id)
            val isFocused = artifact.id == selected.id
            Surface(
              color = if (isFocused) SacredCardSurface else ObsidianVoid.copy(alpha = 0.75f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .size(64.dp)
                .border(
                  width = if (isFocused) 2.dp else 1.dp,
                  color = if (isFocused) MoltenBrass else if (unlocked) artifact.primaryColor.copy(alpha = 0.6f) else SandstoneMuted.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(14.dp)
                )
                .clickable {
                  soundManager.playStoneChakraGrind()
                  inventoryManager.selectForInspection(artifact)
                }
                .testTag("artifact_slot_${artifact.id}")
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (unlocked) {
                  Canvas(modifier = Modifier.size(40.dp)) {
                    drawArtifactEmblem(artifact.id, artifact.primaryColor)
                  }
                } else {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Undiscovered Artifact",
                    tint = SandstoneMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3D-Style Interactive Relic Inspection Stage
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(ObsidianVoid)
            .border(1.5.dp, selected.primaryColor.copy(alpha = 0.65f), CircleShape)
        ) {
          Canvas(modifier = Modifier.size(140.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Rotating Sacred Yantra Halo behind the inspected artifact
            rotate(degrees = haloRotation, pivot = Offset(cx, cy)) {
              drawCircle(
                color = selected.primaryColor.copy(alpha = 0.28f),
                radius = size.minDimension * 0.44f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
              )
              for (i in 0 until 8) {
                val a = (i * 45.0) * PI / 180.0
                val sx = cx + cos(a).toFloat() * (size.minDimension * 0.36f)
                val sy = cy + sin(a).toFloat() * (size.minDimension * 0.36f)
                val ex = cx + cos(a).toFloat() * (size.minDimension * 0.46f)
                val ey = cy + sin(a).toFloat() * (size.minDimension * 0.46f)
                drawLine(
                  color = MoltenBrass.copy(alpha = 0.45f),
                  start = Offset(sx, sy),
                  end = Offset(ex, ey),
                  strokeWidth = 2.5f
                )
              }
            }

            if (isSelectedCollected) {
              drawArtifactEmblem(selected.id, selected.primaryColor)
            } else {
              drawCircle(
                color = Color(0xFF3E2723),
                radius = size.minDimension * 0.25f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Detailed Inspection Card
        Surface(
          color = SacredCardSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MoltenBrass.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isSelectedCollected) selected.name else "Undiscovered Temple Relic",
              style = MaterialTheme.typography.titleMedium,
              color = MoltenBrass,
              textAlign = TextAlign.Center
            )
            Text(
              text = if (isSelectedCollected) "${selected.sanskritTitle} • ${selected.eraAndOrigin}"
              else "Hidden inside a dark temple alcove or sconce",
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isSelectedCollected) {
              Text(
                text = "“${selected.inscription}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = SacredTeal,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = selected.loreDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "⚡ ${selected.ritualEffectSummary}",
                style = MaterialTheme.typography.labelLarge,
                color = MantraJade,
                textAlign = TextAlign.Center
              )
            } else {
              Text(
                text = "Explore the Garbhagriha corridors, ignite sacred Diyas, or search nearby stone alcoves to unearth this ancient Sanatani artifact.",
                style = MaterialTheme.typography.bodyMedium,
                color = SandstoneMuted,
                textAlign = TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Invoke Selected Relic OR Search Nearby Reliquary
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              soundManager.playMantraChantPulse(900)
              onInvokeArtifact(selected)
            },
            enabled = isSelectedCollected,
            colors = ButtonDefaults.buttonColors(
              containerColor = MantraJade,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("invoke_artifact_button")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("INVOKE RELIC", fontWeight = FontWeight.Bold)
          }

          if (inventoryManager.collectedArtifactIds.size < TempleArtifactCatalog.allArtifacts.size) {
            Button(
              onClick = {
                soundManager.playDiyaIgnite()
                onSearchNearbyReliquary()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = SaffronFlame,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("collect_nearby_artifact_button")
            ) {
              Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("UNEARTH RELIC", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun DrawScope.drawArtifactEmblem(artifactId: String, primaryColor: Color) {
  val cx = size.width / 2f
  val cy = size.height / 2f
  val r = size.minDimension * 0.32f

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        primaryColor.copy(alpha = 0.45f),
        Color.Transparent
      ),
      center = Offset(cx, cy),
      radius = r * 1.4f
    ),
    radius = r * 1.4f,
    center = Offset(cx, cy)
  )

  when (artifactId) {
    "rudraksha_mala" -> {
      // Circle of 12 Rudraksha beads + central Ek-Mukhi pendant
      for (i in 0 until 12) {
        val a = (i * 30.0) * PI / 180.0
        val bx = cx + cos(a).toFloat() * (r * 0.85f)
        val by = cy + sin(a).toFloat() * (r * 0.85f)
        drawCircle(
          color = Color(0xFFD84315),
          radius = r * 0.16f,
          center = Offset(bx, by)
        )
      }
      drawCircle(
        color = MoltenBrass,
        radius = r * 0.34f,
        center = Offset(cx, cy)
      )
    }

    "valampuri_shankha" -> {
      // Sacred Conch Shell silhouette
      val conchPath = Path().apply {
        moveTo(cx, cy - r)
        cubicTo(cx + r * 0.9f, cy - r * 0.4f, cx + r * 0.8f, cy + r * 0.6f, cx, cy + r)
        cubicTo(cx - r * 0.8f, cy + r * 0.6f, cx - r * 0.9f, cy - r * 0.4f, cx, cy - r)
        close()
      }
      drawPath(conchPath, color = Color(0xFFE0F7FA))
      drawPath(conchPath, color = primaryColor, style = Stroke(width = 3.5f))
    }

    "surya_tamra_patra" -> {
      // Copper Yantra plate with 9 Navagraha grid cells
      val plateSize = r * 1.5f
      drawRoundRect(
        color = Color(0xFFD87A2B),
        topLeft = Offset(cx - plateSize / 2f, cy - plateSize / 2f),
        size = Size(plateSize, plateSize),
        cornerRadius = CornerRadius(8f, 8f)
      )
      drawRoundRect(
        color = MoltenBrass,
        topLeft = Offset(cx - plateSize / 2f, cy - plateSize / 2f),
        size = Size(plateSize, plateSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 3f)
      )
      drawCircle(
        color = Color(0xFFFFF59D),
        radius = r * 0.35f,
        center = Offset(cx, cy)
      )
    }

    "aghora_vibhuti_kalasha" -> {
      // Sacred Urn with Tripundra (3 horizontal Shiva ash lines)
      drawCircle(
        color = Color(0xFFB8860B),
        radius = r * 0.78f,
        center = Offset(cx, cy + r * 0.1f)
      )
      for (line in -1..1) {
        val ly = cy + r * 0.1f + line * (r * 0.22f)
        drawLine(
          color = Color.White,
          start = Offset(cx - r * 0.48f, ly),
          end = Offset(cx + r * 0.48f, ly),
          strokeWidth = 4f
        )
      }
    }

    else -> {
      // Aranmula Kirtimukha Mirror
      drawCircle(
        color = MoltenBrass,
        radius = r * 0.85f,
        center = Offset(cx, cy)
      )
      drawCircle(
        color = Color(0xFFB2EBF2),
        radius = r * 0.62f,
        center = Offset(cx, cy)
      )
    }
  }
}
