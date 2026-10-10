package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.SacredShrineSaveEntity
import com.example.data.TempleInfo
import com.example.ui.theme.MantraJade
import com.example.ui.theme.MoltenBrass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.SacredCardSurface
import com.example.ui.theme.SacredTeal
import com.example.ui.theme.SaffronFlame
import com.example.ui.theme.SandstoneMuted
import com.example.ui.theme.SanctumGranite

/**
 * Interactive Sacred Shrine Altar Dialog (`SacredShrineDialog`).
 * Allows players inside a temple sanctum to save their current expedition progress,
 * collected inventory artifacts, and live Sanity/Health/Stamina levels to the local Room
 * database, or restore from an existing Sacred Shrine checkpoint.
 */
@Composable
fun SacredShrineDialog(
  temple: TempleInfo,
  shrineTitle: String,
  currentPrana: Int,
  currentSanity: Int,
  currentStamina: Int,
  currentDiyasLit: Int,
  currentSealsSolved: Int,
  collectedArtifactCount: Int,
  existingSave: SacredShrineSaveEntity?,
  onConsecrateAndSave: () -> Unit,
  onRestoreFromShrine: (SacredShrineSaveEntity) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, MoltenBrass, RoundedCornerShape(24.dp))
        .testTag("sacred_shrine_dialog")
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
              text = "SACRED SHRINE SANCTUARY",
              style = MaterialTheme.typography.titleLarge,
              color = MoltenBrass
            )
            Text(
              text = "$shrineTitle • ${temple.name}",
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_shrine_dialog_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Sacred Shrine", tint = ParchmentCream)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Offer an Agni संकल्प (Sankalpa) at this consecrated stone shrine to preserve your Sadhaka's current temple progress, Relic Satchel inventory, and Sanity state in local storage.",
          style = MaterialTheme.typography.bodyMedium,
          color = ParchmentCream,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live State Snapshot to be saved
        Surface(
          color = SacredCardSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MoltenBrass.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "CURRENT SADHAKA STATE TO RECORD",
              style = MaterialTheme.typography.labelLarge,
              color = MoltenBrass
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "♥ Health: $currentPrana%",
                style = MaterialTheme.typography.bodyMedium,
                color = MantraJade
              )
              Text(
                text = "👁 Sanity: $currentSanity%",
                style = MaterialTheme.typography.bodyMedium,
                color = SacredTeal
              )
              Text(
                text = "⚡ Stamina: $currentStamina%",
                style = MaterialTheme.typography.bodyMedium,
                color = SaffronFlame
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "🪔 Diyas: $currentDiyasLit/${temple.requiredDiyas} • ☸ Seals: $currentSealsSolved/${temple.requiredSeals} • 🏺 Relics: $collectedArtifactCount",
              style = MaterialTheme.typography.labelLarge,
              color = ParchmentCream
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Existing Room Shrine Checkpoint Preview (if any)
        if (existingSave != null) {
          val savedDiyas = existingSave.parseLitDiyaCoords().size
          val savedSeals = existingSave.parseSolvedSealCoords().size
          val savedRelics = existingSave.parseCollectedArtifactIds().size
          Surface(
            color = ObsidianVoid.copy(alpha = 0.78f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, SacredTeal.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
              .testTag("existing_shrine_save_card")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "SAVED SHRINE CHECKPOINT (${existingSave.shrineTitle.uppercase()})",
                style = MaterialTheme.typography.labelLarge,
                color = SacredTeal
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sanity: ${existingSave.sanityPercent}% • Health: ${existingSave.pranaPercent}% • Diyas: $savedDiyas • Seals: $savedSeals • Relics: $savedRelics",
                style = MaterialTheme.typography.bodyMedium,
                color = SandstoneMuted
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Primary Save Action Button
        Button(
          onClick = onConsecrateAndSave,
          colors = ButtonDefaults.buttonColors(
            containerColor = MantraJade,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_shrine_save_button")
        ) {
          Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CONSECRATE & SAVE AT SHRINE (+15% SANITY)",
            fontWeight = FontWeight.Bold
          )
        }

        if (existingSave != null) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = { onRestoreFromShrine(existingSave) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("restore_shrine_save_button")
          ) {
            Icon(Icons.Default.Restore, contentDescription = null, tint = SacredTeal, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "RESTORE SAVED SHRINE STATE",
              color = SacredTeal,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
