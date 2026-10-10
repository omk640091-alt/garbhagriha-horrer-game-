package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.audio.TempleSoundEngine
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
 * Ancient Vedic Seal Puzzle Modal powered by [PuzzleManager]:
 * Supports:
 * 1. Pressure Plate Sequences (`PRESSURE_PLATE_SEQUENCE`)
 * 2. Coupled Lever Arrangements (`LEVER_ARRANGEMENT`)
 * 3. 3x3 Navagraha Pattern Matching (`PATTERN_MATCHING`)
 * 4. Surya Chakra Concentric Rings (`SURYA_CHAKRA_RINGS`)
 */
@Composable
fun VedicSealPuzzleDialog(
  sealIndex: Int,
  soundEngine: TempleSoundEngine,
  puzzleManager: PuzzleManager = remember { PuzzleManager() },
  onSolved: () -> Unit,
  onDismiss: () -> Unit
) {
  LaunchedEffect(sealIndex) {
    puzzleManager.loadPuzzleByIndex(sealIndex)
  }

  val activeType = puzzleManager.activePuzzleType
  val isSolved = puzzleManager.isCurrentPuzzleSolved

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, MoltenBrass, RoundedCornerShape(24.dp))
        .testTag("vedic_puzzle_dialog")
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeType.title,
              style = MaterialTheme.typography.titleLarge,
              color = MoltenBrass
            )
            Text(
              text = activeType.sanskritSubtitle,
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_puzzle_dialog_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Puzzle", tint = ParchmentCream)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Puzzle Mechanism Selector Strip (allows testing or switching between Pressure Plates, Levers, Pattern Match, and Surya Rings)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(AncientPuzzleType.entries, key = { it.name }) { type ->
            val selected = type == activeType
            Surface(
              color = if (selected) SaffronFlame else SacredCardSurface,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .clickable {
                  soundEngine.playStoneChakraGrind()
                  puzzleManager.selectPuzzleType(type)
                }
                .testTag("puzzle_tab_${type.name.lowercase()}")
            ) {
              Text(
                text = when (type) {
                  AncientPuzzleType.PRESSURE_PLATE_SEQUENCE -> "Plates"
                  AncientPuzzleType.LEVER_ARRANGEMENT -> "Levers"
                  AncientPuzzleType.PATTERN_MATCHING -> "Pattern"
                  AncientPuzzleType.SURYA_CHAKRA_RINGS -> "Chakra"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.Black else ParchmentCream,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = activeType.instructionText,
          style = MaterialTheme.typography.bodyMedium,
          color = SandstoneMuted,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Feedback Banner
        Surface(
          color = ObsidianVoid.copy(alpha = 0.78f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = puzzleManager.statusMessage,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSolved) MantraJade else SaffronFlame,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("puzzle_status_message")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Puzzle Body
        when (activeType) {
          AncientPuzzleType.PRESSURE_PLATE_SEQUENCE -> {
            PressurePlateSequenceView(
              puzzleManager = puzzleManager,
              soundEngine = soundEngine
            )
          }

          AncientPuzzleType.LEVER_ARRANGEMENT -> {
            LeverArrangementView(
              puzzleManager = puzzleManager,
              soundEngine = soundEngine
            )
          }

          AncientPuzzleType.PATTERN_MATCHING -> {
            PatternMatchingGridView(
              puzzleManager = puzzleManager,
              soundEngine = soundEngine
            )
          }

          AncientPuzzleType.SURYA_CHAKRA_RINGS -> {
            SuryaChakraRingsView(
              puzzleManager = puzzleManager,
              soundEngine = soundEngine
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            soundEngine.playMantraChantPulse()
            onSolved()
          },
          enabled = isSolved,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isSolved) MantraJade else SacredCardSurface,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("unlock_seal_button")
        ) {
          Text(
            text = if (isSolved) "CHANT & UNSEAL ANCIENT MECHANISM" else "SOLVE TEMPLE MECHANISM TO UNSEAL",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun PressurePlateSequenceView(
  puzzleManager: PuzzleManager,
  soundEngine: TempleSoundEngine
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      for (plate in puzzleManager.pressurePlates.take(2)) {
        PressurePlateButton(
          plate = plate,
          isPressed = puzzleManager.pressedPlateSequence.contains(plate.id),
          stepOrder = puzzleManager.pressedPlateSequence.indexOf(plate.id) + 1,
          onClick = {
            val ok = puzzleManager.stepOnPressurePlate(plate.id)
            if (ok) {
              soundEngine.playTempleBell(plate.resonanceHz)
            } else {
              soundEngine.playStoneChakraGrind()
            }
          },
          modifier = Modifier.weight(1f)
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      for (plate in puzzleManager.pressurePlates.drop(2)) {
        PressurePlateButton(
          plate = plate,
          isPressed = puzzleManager.pressedPlateSequence.contains(plate.id),
          stepOrder = puzzleManager.pressedPlateSequence.indexOf(plate.id) + 1,
          onClick = {
            val ok = puzzleManager.stepOnPressurePlate(plate.id)
            if (ok) {
              soundEngine.playTempleBell(plate.resonanceHz)
            } else {
              soundEngine.playStoneChakraGrind()
            }
          },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun PressurePlateButton(
  plate: PressurePlateTile,
  isPressed: Boolean,
  stepOrder: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (isPressed) MantraJade.copy(alpha = 0.22f) else SacredCardSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .height(78.dp)
      .border(
        width = if (isPressed) 2.dp else 1.dp,
        color = if (isPressed) MantraJade else MoltenBrass.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
      )
      .clickable { onClick() }
      .testTag("pressure_plate_${plate.id}")
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(8.dp)
    ) {
      Text(
        text = if (isPressed) "#$stepOrder • ${plate.sanskritGlyph}" else plate.sanskritGlyph,
        style = MaterialTheme.typography.titleMedium,
        color = if (isPressed) MantraJade else MoltenBrass,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = plate.elementName,
        style = MaterialTheme.typography.labelLarge,
        color = ParchmentCream
      )
    }
  }
}

@Composable
private fun LeverArrangementView(
  puzzleManager: PuzzleManager,
  soundEngine: TempleSoundEngine
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    for (idx in 0 until puzzleManager.leverStates.size) {
      val isUp = puzzleManager.leverStates[idx]
      Surface(
        color = if (isUp) MantraJade.copy(alpha = 0.20f) else SacredCardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .weight(1f)
          .height(120.dp)
          .border(
            width = if (isUp) 2.dp else 1.dp,
            color = if (isUp) MantraJade else SaffronFlame.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp)
          )
          .clickable {
            soundEngine.playStoneChakraGrind()
            puzzleManager.pullLever(idx)
          }
          .testTag("temple_lever_$idx")
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(6.dp)
        ) {
          Icon(
            imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = "Lever ${idx + 1}",
            tint = if (isUp) MantraJade else BloodAltarRed,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "LEVER ${idx + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentCream
          )
          Text(
            text = if (isUp) "UP (उर्ध्व)" else "DOWN (अधः)",
            style = MaterialTheme.typography.labelLarge,
            color = if (isUp) MantraJade else SaffronFlame,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun PatternMatchingGridView(
  puzzleManager: PuzzleManager,
  soundEngine: TempleSoundEngine
) {
  val sanskritNumerals = listOf("१", "२", "३", "४", "ॐ", "६", "७", "८", "९")
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    for (row in 0 until 3) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (col in 0 until 3) {
          val index = row * 3 + col
          val isLit = puzzleManager.currentPatternGrid[index]
          val isTarget = puzzleManager.targetPatternGrid[index]
          Surface(
            color = if (isLit) SaffronFlame else ObsidianVoid,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .size(62.dp)
              .border(
                width = if (isTarget) 1.8.dp else 1.dp,
                color = if (isLit) MoltenBrass else if (isTarget) SacredTeal.copy(alpha = 0.55f) else SandstoneMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
              )
              .clickable {
                soundEngine.playTempleBell(528.0)
                puzzleManager.togglePatternCell(index)
              }
              .testTag("pattern_cell_$index")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = sanskritNumerals[index],
                style = MaterialTheme.typography.titleMedium,
                color = if (isLit) Color.Black else ParchmentCream,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SuryaChakraRingsView(
  puzzleManager: PuzzleManager,
  soundEngine: TempleSoundEngine
) {
  val outerDeg = puzzleManager.outerRingDeg
  val middleDeg = puzzleManager.middleRingDeg
  val innerDeg = puzzleManager.innerRingDeg

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(185.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_puzzle_surya_chakra_1791611685548),
        contentDescription = "Surya Chakra Stone Wheel",
        modifier = Modifier
          .size(175.dp)
          .clip(CircleShape),
        contentScale = ContentScale.Crop,
        alpha = 0.35f
      )

      Canvas(modifier = Modifier.size(175.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawCircle(
          color = Color(0xFFFFEA00),
          radius = 7f,
          center = Offset(cx, 7f)
        )

        val rings = listOf(
          Triple(72f, outerDeg, Color(0xFFFF8F00)),
          Triple(50f, middleDeg, Color(0xFFFFCA28)),
          Triple(30f, innerDeg, Color(0xFF00E5FF))
        )

        for ((radius, deg, color) in rings) {
          val aligned = (deg % 360 == 0)
          val ringColor = if (aligned) MantraJade else color
          drawCircle(
            color = ringColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 7f)
          )
          val rad = ((deg - 90) * PI / 180.0).toFloat()
          val mx = cx + cos(rad) * radius
          val my = cy + sin(rad) * radius
          drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(mx, my)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = {
          soundEngine.playStoneChakraGrind()
          puzzleManager.rotateSuryaRing(0)
        },
        modifier = Modifier
          .weight(1f)
          .testTag("rotate_outer_ring")
      ) {
        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("Outer (${outerDeg}°)", style = MaterialTheme.typography.labelLarge)
      }

      OutlinedButton(
        onClick = {
          soundEngine.playStoneChakraGrind()
          puzzleManager.rotateSuryaRing(1)
        },
        modifier = Modifier
          .weight(1f)
          .testTag("rotate_middle_ring")
      ) {
        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("Mid (${middleDeg}°)", style = MaterialTheme.typography.labelLarge)
      }

      OutlinedButton(
        onClick = {
          soundEngine.playStoneChakraGrind()
          puzzleManager.rotateSuryaRing(2)
        },
        modifier = Modifier
          .weight(1f)
          .testTag("rotate_inner_ring")
      ) {
        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("Inner (${innerDeg}°)", style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
