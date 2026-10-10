package com.example.ui

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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PlayerPresence
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Visibility states for each tile in the Fog-of-War temple layout grid.
 */
enum class TileFogVisibility(
  val hudBadge: String
) {
  UNEXPLORED("SHROUDED IN TAMAS"),
  DISCOVERED_MEMORY("MEMORY CARTOGRAPHY"),
  ACTIVELY_ILLUMINATED("TORCH ILLUMINATED")
}

/**
 * Overall exploration status of the current temple maze.
 */
enum class MapExplorationState(
  val hudLabel: String,
  val sanskritLabel: String
) {
  UNMAPPED_DARKNESS("UNMAPPED CRYPT", "अज्ञात गर्भगृह"),
  PARTIAL_CARTOGRAPHY("CHARTING MANDAPA", "मण्डप मानचित्रण"),
  SANCTUM_SURVEYED("SANCTUM CHARTED", "सम्पूर्ण दर्शन")
}

/**
 * Stateful manager for the Fog-of-War Mini-Map & Vastu Mandala Cartography system.
 *
 * Tracks:
 * - Permanently discovered tiles (`discoveredCoords`) as the player explores the maze
 * - Currently illuminated tiles (`activelyLitCoords`) within the player's dynamic torch radius
 *   and line-of-sight / forward FOV cone
 * - Exploration percentage (`explorationPercent`) across all tiles in the temple
 * - Breadcrumb trail (`breadcrumbTrail`) of recent corridor positions to prevent getting lost
 *   in complex maze-like temple areas
 */
class FogOfWarMiniMapManager(
  val templeId: String = "brihadeeswarar"
) {
  private val visibilityGrid = mutableStateMapOf<Pair<Int, Int>, TileFogVisibility>()

  var explorationPercent by mutableIntStateOf(0)
    private set

  var discoveredTileCount by mutableIntStateOf(0)
    private set

  var totalChartableTiles by mutableIntStateOf(144)
    private set

  var isExpandedMapOpen by mutableStateOf(false)
    private set

  private val recentBreadcrumbs = ArrayDeque<Pair<Int, Int>>()

  val explorationState: MapExplorationState
    get() = when {
      explorationPercent >= 75 -> MapExplorationState.SANCTUM_SURVEYED
      explorationPercent >= 25 -> MapExplorationState.PARTIAL_CARTOGRAPHY
      else -> MapExplorationState.UNMAPPED_DARKNESS
    }

  fun openExpandedMap() {
    isExpandedMapOpen = true
  }

  fun closeExpandedMap() {
    isExpandedMapOpen = false
  }

  fun toggleExpandedMap() {
    isExpandedMapOpen = !isExpandedMapOpen
  }

  /**
   * Updates the Fog-of-War grid based on the player's current position `(playerX, playerY)`,
   * facing angle `playerAngle`, and active torch state (`diyaLit`, `torchRadiusTiles`).
   */
  fun updateExploration(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    playerAngle: Double,
    diyaLit: Boolean,
    torchRadiusTiles: Float = if (diyaLit) 4.2f else 1.8f
  ) {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12
    totalChartableTiles = (rows * cols).coerceAtLeast(1)

    // 1. Demote previously actively illuminated tiles to DISCOVERED_MEMORY
    for ((coord, state) in visibilityGrid.entries.toList()) {
      if (state == TileFogVisibility.ACTIVELY_ILLUMINATED) {
        visibilityGrid[coord] = TileFogVisibility.DISCOVERED_MEMORY
      }
    }

    // Record player's current tile in breadcrumb trail
    val pCol = playerX.toInt().coerceIn(0, cols - 1)
    val pRow = playerY.toInt().coerceIn(0, rows - 1)
    val currentTile = pCol to pRow
    if (recentBreadcrumbs.lastOrNull() != currentTile) {
      recentBreadcrumbs.addLast(currentTile)
      while (recentBreadcrumbs.size > 18) {
        recentBreadcrumbs.removeFirst()
      }
    }

    // Effective reveal radius: immediate 360° aura + extended forward torch beam cone
    val omniRadius = if (diyaLit) {
      (torchRadiusTiles * 0.52f).coerceIn(1.85f, 3.6f)
    } else {
      1.55f
    }
    val coneRadius = if (diyaLit) {
      (torchRadiusTiles * 0.85f).coerceIn(2.8f, 5.4f)
    } else {
      2.1f
    }

    for (r in 0 until rows) {
      for (c in 0 until cols) {
        val tileCenterX = c + 0.5
        val tileCenterY = r + 0.5
        val dx = tileCenterX - playerX
        val dy = tileCenterY - playerY
        val dist = hypot(dx, dy)

        val withinOmni = dist <= omniRadius
        val withinForwardCone = if (dist <= coneRadius) {
          val angleToTile = atan2(dy, dx)
          var diff = angleToTile - playerAngle
          while (diff > PI) diff -= 2.0 * PI
          while (diff < -PI) diff += 2.0 * PI
          abs(diff) <= (PI / 2.6)
        } else {
          false
        }

        if (withinOmni || withinForwardCone) {
          if (hasClearOrAdjacentSightline(mapGrid, playerX, playerY, c, r)) {
            visibilityGrid[c to r] = TileFogVisibility.ACTIVELY_ILLUMINATED
          }
        }
      }
    }

    discoveredTileCount = visibilityGrid.count { it.value != TileFogVisibility.UNEXPLORED }
    explorationPercent = ((discoveredTileCount * 100f) / totalChartableTiles)
      .roundToInt()
      .coerceIn(0, 100)
  }

  /**
   * Checks whether a tile `(targetCol, targetRow)` is either directly visible from `(playerX, playerY)`
   * or is the first wall tile hit along the sightline (so corridor walls framing the player's path
   * are revealed on the mini-map).
   */
  private fun hasClearOrAdjacentSightline(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    targetCol: Int,
    targetRow: Int
  ): Boolean {
    val targetX = targetCol + 0.5
    val targetY = targetRow + 0.5
    val dist = hypot(targetX - playerX, targetY - playerY)
    if (dist <= 1.65) return true

    val steps = (dist * 3.0).toInt().coerceAtLeast(2)
    for (i in 1 until steps) {
      val t = i.toDouble() / steps.toDouble()
      val sampleX = playerX + (targetX - playerX) * t
      val sampleY = playerY + (targetY - playerY) * t
      val sCol = sampleX.toInt()
      val sRow = sampleY.toInt()
      if (sCol == targetCol && sRow == targetRow) return true
      if (sRow in mapGrid.indices && sCol in 0 until mapGrid[sRow].length) {
        if (mapGrid[sRow][sCol] == '1') {
          // Blocked by an intermediate stone wall before reaching target tile
          return false
        }
      }
    }
    return true
  }

  fun getTileVisibility(col: Int, row: Int): TileFogVisibility {
    return visibilityGrid[col to row] ?: TileFogVisibility.UNEXPLORED
  }

  fun isTileDiscovered(col: Int, row: Int): Boolean {
    return getTileVisibility(col, row) != TileFogVisibility.UNEXPLORED
  }

  fun getBreadcrumbTrail(): List<Pair<Int, Int>> = recentBreadcrumbs.toList()

  /**
   * Reveals a local cluster of tiles around a coordinate (e.g., when unlocking a secret chamber
   * or consulting an ancient architectural stele).
   */
  fun revealAreaAround(
    mapGrid: List<String>,
    centerCol: Int,
    centerRow: Int,
    radiusTiles: Int = 2
  ) {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12
    for (r in (centerRow - radiusTiles)..(centerRow + radiusTiles)) {
      for (c in (centerCol - radiusTiles)..(centerCol + radiusTiles)) {
        if (r in 0 until rows && c in 0 until cols) {
          if (visibilityGrid[c to r] == null) {
            visibilityGrid[c to r] = TileFogVisibility.DISCOVERED_MEMORY
          }
        }
      }
    }
    discoveredTileCount = visibilityGrid.count { it.value != TileFogVisibility.UNEXPLORED }
    totalChartableTiles = (rows * cols).coerceAtLeast(1)
    explorationPercent = ((discoveredTileCount * 100f) / totalChartableTiles)
      .roundToInt()
      .coerceIn(0, 100)
  }
}

/**
 * Compact HUD Fog-of-War Mini-Map Overlay (Top-Left of the First-Person Viewport).
 * Reveals parts of the temple layout as the player explores while keeping unvisited
 * corridors shrouded in dark Tamas fog. Tapping the mini-map opens the expanded
 * Vastu Mandala Cartography modal.
 */
@Composable
fun FogOfWarMiniMapOverlay(
  mapGrid: List<String>,
  fogOfWarManager: FogOfWarMiniMapManager,
  playerX: Double,
  playerY: Double,
  playerAngle: Double,
  diyaLit: Boolean,
  entityX: Double,
  entityY: Double,
  litDiyaCoords: Set<Pair<Int, Int>>,
  solvedSealCoords: Set<Pair<Int, Int>>,
  dialogueEncounters: List<TempleDialogueEncounter>,
  shadowCasterSystem: TorchShadowCasterSystem,
  ritualManager: RitualInteractionManager? = null,
  coOpPlayers: List<PlayerPresence>,
  currentUserId: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .width(120.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xE6100604))
      .border(1.2.dp, MoltenBrass.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
      .testTag("fog_of_war_minimap_overlay")
      .clickable { fogOfWarManager.openExpandedMap() }
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Mini-Map Header Bar: Title + Exploration %
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "🗺 MANDALA",
        style = MaterialTheme.typography.labelSmall,
        color = MoltenBrass,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "${fogOfWarManager.explorationPercent}%",
        style = MaterialTheme.typography.labelSmall,
        color = if (fogOfWarManager.explorationPercent >= 60) MantraJade else SaffronFlame,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("minimap_exploration_percent_text")
      )
    }

    Box(
      modifier = Modifier
        .size(104.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF050201))
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawFogOfWarMapCanvas(
          mapGrid = mapGrid,
          fogOfWarManager = fogOfWarManager,
          playerX = playerX,
          playerY = playerY,
          playerAngle = playerAngle,
          diyaLit = diyaLit,
          entityX = entityX,
          entityY = entityY,
          litDiyaCoords = litDiyaCoords,
          solvedSealCoords = solvedSealCoords,
          dialogueEncounters = dialogueEncounters,
          shadowCasterSystem = shadowCasterSystem,
          ritualManager = ritualManager,
          coOpPlayers = coOpPlayers,
          currentUserId = currentUserId
        )
      }
    }
  }
}

/**
 * Full-screen interactive Vastu Mandala Cartography Overlay when the player taps the
 * mini-map or the HUD Cartography chip. Displays the fog-of-war temple layout at high
 * resolution with a legend for discovered Diyas, Vedic Seals, Secret Chambers, and
 * breadcrumb pathing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedFogOfWarMapDialog(
  templeName: String,
  mapGrid: List<String>,
  fogOfWarManager: FogOfWarMiniMapManager,
  playerX: Double,
  playerY: Double,
  playerAngle: Double,
  diyaLit: Boolean,
  entityX: Double,
  entityY: Double,
  litDiyaCoords: Set<Pair<Int, Int>>,
  solvedSealCoords: Set<Pair<Int, Int>>,
  dialogueEncounters: List<TempleDialogueEncounter>,
  shadowCasterSystem: TorchShadowCasterSystem,
  ritualManager: RitualInteractionManager? = null,
  coOpPlayers: List<PlayerPresence>,
  currentUserId: String,
  onDismiss: () -> Unit
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid.copy(alpha = 0.86f))
      .clickable(enabled = true, onClick = onDismiss)
      .padding(16.dp)
      .testTag("expanded_fog_of_war_map_dialog")
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SanctumGranite),
      shape = RoundedCornerShape(22.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.8.dp, MoltenBrass, RoundedCornerShape(22.dp))
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
              imageVector = Icons.Default.Map,
              contentDescription = null,
              tint = MoltenBrass,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "VASTU MANDALA CARTOGRAPHY",
                style = MaterialTheme.typography.titleMedium,
                color = MoltenBrass,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "$templeName • ${fogOfWarManager.explorationState.hudLabel} (${fogOfWarManager.explorationPercent}% Explored)",
                style = MaterialTheme.typography.bodySmall,
                color = SaffronFlame
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_expanded_map_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Map",
              tint = ParchmentCream
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High-Resolution Fog-of-War Temple Maze Canvas
        Box(
          modifier = Modifier
            .size(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF050201))
            .border(1.2.dp, MoltenBrass.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(8.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawFogOfWarMapCanvas(
              mapGrid = mapGrid,
              fogOfWarManager = fogOfWarManager,
              playerX = playerX,
              playerY = playerY,
              playerAngle = playerAngle,
              diyaLit = diyaLit,
              entityX = entityX,
              entityY = entityY,
              litDiyaCoords = litDiyaCoords,
              solvedSealCoords = solvedSealCoords,
              dialogueEncounters = dialogueEncounters,
              shadowCasterSystem = shadowCasterSystem,
              ritualManager = ritualManager,
              coOpPlayers = coOpPlayers,
              currentUserId = currentUserId
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend for Fog-of-War & Sacred Landmarks
        Surface(
          color = SacredCardSurface,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MapLegendDot(color = Color.White, label = "You + Torch Cone")
            MapLegendDot(color = SaffronFlame, label = "Breadcrumb Trail")
            MapLegendDot(color = MoltenBrass, label = "Sacred Diya")
            MapLegendDot(color = SacredTeal, label = "Vedic Seal")
            MapLegendDot(color = MantraJade, label = "Ritual / Secret Door")
            MapLegendDot(color = BloodAltarRed, label = "Sensed Entity")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(
            containerColor = MoltenBrass,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dismiss_expanded_map_cta")
        ) {
          Icon(Icons.Default.Explore, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("RETURN TO FIRST-PERSON EXPLORATION", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun MapLegendDot(
  color: Color,
  label: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = ParchmentCream
    )
  }
}

/**
 * Shared Canvas renderer for both the HUD Fog-of-War Mini-Map and the Expanded Cartography Modal.
 */
private fun DrawScope.drawFogOfWarMapCanvas(
  mapGrid: List<String>,
  fogOfWarManager: FogOfWarMiniMapManager,
  playerX: Double,
  playerY: Double,
  playerAngle: Double,
  diyaLit: Boolean,
  entityX: Double,
  entityY: Double,
  litDiyaCoords: Set<Pair<Int, Int>>,
  solvedSealCoords: Set<Pair<Int, Int>>,
  dialogueEncounters: List<TempleDialogueEncounter>,
  shadowCasterSystem: TorchShadowCasterSystem,
  ritualManager: RitualInteractionManager?,
  coOpPlayers: List<PlayerPresence>,
  currentUserId: String
) {
  val rows = mapGrid.size
  val cols = mapGrid.firstOrNull()?.length ?: 12
  val cellW = size.width / cols
  val cellH = size.height / rows

  // 1. Render discovered & actively illuminated tiles; keep unexplored tiles shrouded in Fog-of-War
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      val visibility = fogOfWarManager.getTileVisibility(c, r)
      val topLeft = Offset(c * cellW, r * cellH)

      if (visibility == TileFogVisibility.UNEXPLORED) {
        // Shrouded in dark Tamas fog with subtle stone grid hatching
        drawRect(
          color = Color(0xFF060201),
          topLeft = topLeft,
          size = Size(cellW + 0.5f, cellH + 0.5f)
        )
        drawRect(
          color = Color(0xFF1A0D08).copy(alpha = 0.32f),
          topLeft = topLeft,
          size = Size(cellW, cellH),
          style = Stroke(width = 0.6f)
        )
        continue
      }

      val isActivelyLit = visibility == TileFogVisibility.ACTIVELY_ILLUMINATED
      val alphaScale = if (isActivelyLit) 1.0f else 0.46f
      val ch = mapGrid[r][c]

      // Floor base for explored corridor tiles
      if (ch != '1') {
        drawRect(
          color = if (isActivelyLit) {
            Color(0xFF2B170E).copy(alpha = 0.85f)
          } else {
            Color(0xFF180C07).copy(alpha = 0.72f)
          },
          topLeft = topLeft,
          size = Size(cellW + 0.4f, cellH + 0.4f)
        )
      }

      when (ch) {
        '1' -> drawRect(
          color = Color(0xFF4A2A1C).copy(alpha = alphaScale),
          topLeft = topLeft,
          size = Size(cellW + 0.5f, cellH + 0.5f)
        )
        '2' -> {
          val lit = litDiyaCoords.contains(c to r)
          drawCircle(
            color = (if (lit) Color(0xFFFFCA28) else Color(0xFFB08568)).copy(alpha = alphaScale),
            radius = cellW * 0.36f,
            center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
          )
        }
        '3' -> {
          val solved = solvedSealCoords.contains(c to r)
          drawCircle(
            color = (if (solved) Color(0xFF00E676) else Color(0xFF00E5FF)).copy(alpha = alphaScale),
            radius = cellW * 0.38f,
            center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
          )
        }
        '4' -> drawRect(
          color = Color(0xFFFF8F00).copy(alpha = alphaScale),
          topLeft = topLeft,
          size = Size(cellW, cellH)
        )
        '5' -> drawRect(
          color = Color(0xFF00E676).copy(alpha = alphaScale),
          topLeft = topLeft,
          size = Size(cellW, cellH)
        )
        '6' -> drawCircle(
          color = Color(0xFFFFEA00).copy(alpha = alphaScale),
          radius = cellW * 0.44f,
          center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
        )
      }
    }
  }

  // 2. Render 2D top-down pillar & statue shadow wedges cast from the player's torch
  shadowCasterSystem.renderMiniMapShadowCones(
    drawScope = this,
    playerX = playerX,
    playerY = playerY,
    cols = cols,
    rows = rows,
    diyaLit = diyaLit
  )

  // 3. Render player exploration breadcrumb trail across discovered corridors
  val breadcrumbs = fogOfWarManager.getBreadcrumbTrail()
  if (breadcrumbs.size >= 2) {
    for (i in 0 until breadcrumbs.size - 1) {
      val (c1, r1) = breadcrumbs[i]
      val (c2, r2) = breadcrumbs[i + 1]
      if (hypot((c2 - c1).toDouble(), (r2 - r1).toDouble()) <= 1.6) {
        val trailAlpha = ((i + 1).toFloat() / breadcrumbs.size.toFloat()) * 0.55f
        drawLine(
          color = Color(0xFFFF9E2C).copy(alpha = trailAlpha),
          start = Offset((c1 + 0.5f) * cellW, (r1 + 0.5f) * cellH),
          end = Offset((c2 + 0.5f) * cellW, (r2 + 0.5f) * cellH),
          strokeWidth = (cellW * 0.16f).coerceAtLeast(1.5f)
        )
      }
    }
  }

  // 4. Highlight next required ritual target on the mini-map if discovered or within 5 tiles
  ritualManager?.nextRequiredStep?.targetCoord?.let { (tx, ty) ->
    val distToTarget = hypot((tx + 0.5) - playerX, (ty + 0.5) - playerY)
    if (fogOfWarManager.isTileDiscovered(tx, ty) || distToTarget <= 5.5) {
      drawCircle(
        color = Color(0xFF00E676),
        radius = cellW * 0.52f,
        center = Offset((tx + 0.5f) * cellW, (ty + 0.5f) * cellH),
        style = Stroke(width = 2f)
      )
    }
  }

  // 5. Co-op teammates (always visible via shared Sadhaka resonance)
  for (mate in coOpPlayers) {
    if (mate.userId != currentUserId) {
      drawCircle(
        color = Color(0xFF00E5FF),
        radius = cellW * 0.42f,
        center = Offset(mate.x.toFloat() * cellW, mate.y.toFloat() * cellH)
      )
    }
  }

  // 6. NPC Spirits and Ancient Inscriptions (shown when their tile is discovered)
  for (enc in dialogueEncounters) {
    if (fogOfWarManager.isTileDiscovered(enc.tileX, enc.tileY)) {
      drawCircle(
        color = if (enc.speakerType == DialogueSpeakerType.NPC_SPIRIT) Color(0xFF80DEEA) else Color(0xFFFFD54F),
        radius = cellW * 0.34f,
        center = Offset((enc.tileX + 0.5f) * cellW, (enc.tileY + 0.5f) * cellH)
      )
    }
  }

  // 7. Creature radar pulse (visible when on an actively lit tile or within acoustic threat sense range)
  val eCol = entityX.toInt().coerceIn(0, cols - 1)
  val eRow = entityY.toInt().coerceIn(0, rows - 1)
  val entityDist = hypot(entityX - playerX, entityY - playerY)
  val entityTileVis = fogOfWarManager.getTileVisibility(eCol, eRow)
  if (entityTileVis == TileFogVisibility.ACTIVELY_ILLUMINATED || entityDist <= 5.8) {
    drawCircle(
      color = Color(0xFFFF1744),
      radius = cellW * 0.48f,
      center = Offset(entityX.toFloat() * cellW, entityY.toFloat() * cellH)
    )
  }

  // 8. Local player dot + golden FOV cone
  val px = playerX.toFloat() * cellW
  val py = playerY.toFloat() * cellH
  val coneLength = if (diyaLit) cellW * 2.2f else cellW * 1.2f
  val leftConeAngle = (playerAngle - PI / 6.0).toFloat()
  val rightConeAngle = (playerAngle + PI / 6.0).toFloat()

  val fovPath = Path().apply {
    moveTo(px, py)
    lineTo(px + cos(leftConeAngle) * coneLength, py + sin(leftConeAngle) * coneLength)
    lineTo(px + cos(rightConeAngle) * coneLength, py + sin(rightConeAngle) * coneLength)
    close()
  }
  drawPath(
    path = fovPath,
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFFFFEA00).copy(alpha = if (diyaLit) 0.42f else 0.18f),
        Color.Transparent
      ),
      center = Offset(px, py),
      radius = coneLength
    )
  )

  drawLine(
    color = Color(0xFFFFEA00),
    start = Offset(px, py),
    end = Offset(
      px + cos(playerAngle).toFloat() * cellW * 1.6f,
      py + sin(playerAngle).toFloat() * cellH * 1.6f
    ),
    strokeWidth = 2.2f
  )
  drawCircle(
    color = Color(0xFFFFFFFF),
    radius = cellW * 0.42f,
    center = Offset(px, py)
  )
}
