package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Architectural classification of shadow-casting geometry inside the temple sanctum.
 */
enum class ShadowOccluderType(
  val displayName: String,
  val baseRadiusTiles: Float,
  val heightScale: Float
) {
  MANDAPA_PILLAR(
    displayName = "Carved Mandapa Pillar",
    baseRadiusTiles = 0.46f,
    heightScale = 1.0f
  ),
  MURTI_STATUE(
    displayName = "Deity Murti Statue",
    baseRadiusTiles = 0.28f,
    heightScale = 0.72f
  ),
  INSCRIPTION_STELE(
    displayName = "Stone Shila-Lekha Stele",
    baseRadiusTiles = 0.24f,
    heightScale = 0.64f
  ),
  CREATURE_SILHOUETTE(
    displayName = "Stalking Creature Shadow",
    baseRadiusTiles = 0.34f,
    heightScale = 0.94f
  )
}

/**
 * Projected 2D/3D shadow volume cast by a temple pillar, statue, stele, or creature
 * from the player's handheld flickering Diya torch light.
 */
data class ProjectedShadowVolume(
  val occluderType: ShadowOccluderType,
  val worldX: Double,
  val worldY: Double,
  val distanceToTorch: Float,
  val centerAngleRad: Float,
  val angularHalfWidthRad: Float,
  val screenCenterXNorm: Float,
  val screenHalfWidthNorm: Float,
  val shadowReachNorm: Float,
  val umbraAlpha: Float,
  val penumbraAlpha: Float,
  val torchLateralSkewNorm: Float
)

/**
 * Qualitative state of dynamic shadow casting for HUD display and telemetry.
 */
enum class TorchShadowState(
  val hudLabel: String,
  val description: String
) {
  NO_TORCH_UMBRA(
    hudLabel = "DARK UMBRAL VOID",
    description = "Torch unlit; temple swallowed in solid darkness"
  ),
  CLEAR_CORRIDOR(
    hudLabel = "OPEN LIGHT CONE",
    description = "Torch light unobstructed in open corridor"
  ),
  PILLAR_PENUMBRA(
    hudLabel = "PILLAR SHADOWS",
    description = "Mandapa pillars casting flickering penumbra wedges"
  ),
  STATUE_SILHOUETTE(
    hudLabel = "STATUE SHADOWS",
    description = "Temple statues & steles casting elongated silhouettes"
  )
}

/**
 * Dynamic Shadow Caster System for the player's handheld flickering Diya torch light.
 *
 * Computes real-time 2D/3D shadow occlusion from:
 * - Interlocking granite temple pillars & wall corners (`mapGrid` `'1'` tiles with exposed edges)
 * - Ancient Murti statues, Vedic Seal pedestals (`'3'`), Diya sconces (`'2'`), and stone steles
 * - The stalking mythological entity (`entityX`, `entityY`)
 *
 * Uses Compose `Canvas` (`DrawScope`) to render:
 * 1. Floor & ceiling projected trapezoidal umbra and soft penumbra shadow wedges that sway
 *    dynamically with the right-hand Diya torch position and flame flicker.
 * 2. Ray-slice shadow occlusion multipliers (`computeRayShadowFactor`) so walls and corridors
 *    standing behind pillars or statues receive realistic cast shadow darkening.
 * 3. Top-down 2D visibility/shadow polygon wedges on the Yantra Mini-Map radar.
 */
class TorchShadowCasterSystem {
  var shadowState by mutableStateOf(TorchShadowState.CLEAR_CORRIDOR)
    private set

  var activeCasterCount by mutableIntStateOf(0)
    private set

  var pillarShadowCount by mutableIntStateOf(0)
    private set

  var statueShadowCount by mutableIntStateOf(0)
    private set

  /** Real-time lateral sway of the handheld torch in normalized screen coordinates (-0.08f..+0.08f). */
  var torchSwayShiftNorm by mutableFloatStateOf(0f)
    private set

  private var cachedShadowVolumes: List<ProjectedShadowVolume> = emptyList()

  val projectedShadowVolumes: List<ProjectedShadowVolume>
    get() = cachedShadowVolumes

  /**
   * Recomputes all active pillar and statue shadow volumes within the player's current
   * torch light radius and 60° first-person field of view.
   */
  fun updateShadowCasters(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    playerAngle: Double,
    diyaLit: Boolean,
    torchLightRadiusTiles: Float,
    torchFlickerMultiplier: Float,
    entityX: Double,
    entityY: Double,
    dialogueEncounters: List<TempleDialogueEncounter>,
    animTick: Float
  ) {
    if (!diyaLit) {
      cachedShadowVolumes = emptyList()
      activeCasterCount = 0
      pillarShadowCount = 0
      statueShadowCount = 0
      shadowState = TorchShadowState.NO_TORCH_UMBRA
      return
    }

    val fov = (PI / 3.0).toFloat() // 60-degree FOV matching FirstPersonSanctumViewport
    val halfFov = fov * 0.5f

    // Right-hand held Diya torch introduces a subtle parallax offset & organic walking sway
    val handSway = cos(animTick * 3.5f) * 0.022f + sin(animTick * 11.2f) * 0.008f * (2f - torchFlickerMultiplier)
    torchSwayShiftNorm = (0.032f + handSway).coerceIn(-0.08f, 0.08f)

    // Virtual light origin slightly to the right-front of the player's camera
    val rightAngle = playerAngle + PI / 2.0
    val torchOriginX = playerX + cos(playerAngle) * 0.14 + cos(rightAngle) * 0.16
    val torchOriginY = playerY + sin(playerAngle) * 0.14 + sin(rightAngle) * 0.16

    val maxReach = torchLightRadiusTiles.coerceIn(2.4f, 9.2f)
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12

    val minRow = (playerY.toInt() - 7).coerceAtLeast(0)
    val maxRow = (playerY.toInt() + 7).coerceAtMost(rows - 1)
    val minCol = (playerX.toInt() - 7).coerceAtLeast(0)
    val maxCol = (playerX.toInt() + 7).coerceAtMost(cols - 1)

    val volumes = mutableListOf<ProjectedShadowVolume>()
    var pillars = 0
    var statues = 0

    // 1. Scan nearby temple pillars (interior/protruding '1' wall blocks) & sanctum statues/altars ('2', '3')
    for (r in minRow..maxRow) {
      for (c in minCol..maxCol) {
        val tile = mapGrid[r][c]
        val cx = c + 0.5
        val cy = r + 0.5
        val dist = hypot(cx - torchOriginX, cy - torchOriginY).toFloat()
        if (dist < 0.35f || dist > maxReach) continue

        val occluderType: ShadowOccluderType? = when (tile) {
          '1' -> {
            // Protruding temple pillars or corner monoliths (excluding outer boundary ring)
            val isInnerPillar = r in 1 until (rows - 1) && c in 1 until (cols - 1) &&
              hasAdjacentOpenCorridor(mapGrid, r, c, rows, cols)
            if (isInnerPillar) ShadowOccluderType.MANDAPA_PILLAR else null
          }
          '2', '3' -> ShadowOccluderType.MURTI_STATUE
          else -> null
        }

        if (occluderType != null) {
          val vol = buildShadowVolume(
            occluderType = occluderType,
            worldX = cx,
            worldY = cy,
            torchOriginX = torchOriginX,
            torchOriginY = torchOriginY,
            playerAngle = playerAngle,
            fov = fov,
            halfFov = halfFov,
            maxReach = maxReach,
            torchFlickerMultiplier = torchFlickerMultiplier
          )
          if (vol != null) {
            volumes.add(vol)
            if (occluderType == ShadowOccluderType.MANDAPA_PILLAR) pillars++ else statues++
          }
        }
      }
    }

    // 2. Add carved stone inscription steles & spectral shrines as statue shadow casters
    for (enc in dialogueEncounters) {
      val sx = enc.tileX + 0.5
      val sy = enc.tileY + 0.5
      val vol = buildShadowVolume(
        occluderType = ShadowOccluderType.INSCRIPTION_STELE,
        worldX = sx,
        worldY = sy,
        torchOriginX = torchOriginX,
        torchOriginY = torchOriginY,
        playerAngle = playerAngle,
        fov = fov,
        halfFov = halfFov,
        maxReach = maxReach,
        torchFlickerMultiplier = torchFlickerMultiplier
      )
      if (vol != null) {
        volumes.add(vol)
        statues++
      }
    }

    // 3. Add stalking mythological creature silhouette shadow when within torch reach
    val creatureVol = buildShadowVolume(
      occluderType = ShadowOccluderType.CREATURE_SILHOUETTE,
      worldX = entityX,
      worldY = entityY,
      torchOriginX = torchOriginX,
      torchOriginY = torchOriginY,
      playerAngle = playerAngle,
      fov = fov,
      halfFov = halfFov,
      maxReach = maxReach,
      torchFlickerMultiplier = torchFlickerMultiplier
    )
    if (creatureVol != null) {
      volumes.add(creatureVol)
      statues++
    }

    // Sort front-to-back by distance and cap to top 14 casters for crisp 60fps Canvas rendering
    val sorted = volumes
      .sortedBy { it.distanceToTorch }
      .take(14)

    cachedShadowVolumes = sorted
    activeCasterCount = sorted.size
    pillarShadowCount = pillars
    statueShadowCount = statues

    shadowState = when {
      statues > 0 -> TorchShadowState.STATUE_SILHOUETTE
      pillars > 0 -> TorchShadowState.PILLAR_PENUMBRA
      else -> TorchShadowState.CLEAR_CORRIDOR
    }
  }

  /**
   * Computes a real-time shadow occlusion factor (`0.32f..1.0f`) for a vertical wall ray slice
   * at [rayIdx] out of [numRays] with wall hit distance [wallPerpDist].
   *
   * When a temple pillar, statue, stele, or creature stands between the player's torch and a
   * more distant wall slice, it casts a dark umbra core and soft flickering penumbra edge onto that wall.
   */
  fun computeRayShadowFactor(
    rayIdx: Int,
    numRays: Int,
    wallPerpDist: Float
  ): Float {
    if (cachedShadowVolumes.isEmpty()) return 1.0f

    val screenXNorm = (rayIdx.toFloat() + 0.5f) / numRays.toFloat()
    var lightFactor = 1.0f

    for (vol in cachedShadowVolumes) {
      // Only cast shadow onto surfaces behind the occluder
      if (wallPerpDist > vol.distanceToTorch + 0.28f) {
        val shiftedCenter = vol.screenCenterXNorm - torchSwayShiftNorm * (1.2f / vol.distanceToTorch.coerceAtLeast(0.7f))
        val deltaX = abs(screenXNorm - shiftedCenter)
        val penumbraHalfWidth = vol.screenHalfWidthNorm * 1.45f
        if (deltaX < penumbraHalfWidth) {
          val normalizedEdge = (deltaX / penumbraHalfWidth).coerceIn(0f, 1f)
          // Smooth cubic penumbra falloff from deep umbra center to soft lit edge
          val smoothFalloff = 1f - (normalizedEdge * normalizedEdge * (3f - 2f * normalizedEdge))
          val distStretch = ((wallPerpDist - vol.distanceToTorch) / 3.5f).coerceIn(0.25f, 1.0f)
          val shadowStrength = (vol.umbraAlpha * 0.68f * smoothFalloff * distStretch).coerceIn(0f, 0.68f)
          lightFactor *= (1f - shadowStrength)
        }
      }
    }

    return lightFactor.coerceIn(0.28f, 1.0f)
  }

  /**
   * Renders projected floor and ceiling shadow volumes cast by temple pillars, Murti statues,
   * stone steles, and the mythological entity onto the first-person Compose [DrawScope].
   */
  fun renderFloorAndCeilingShadows(
    drawScope: DrawScope,
    diyaLit: Boolean,
    torchLightRadiusNormalized: Float,
    torchFlickerMultiplier: Float,
    animTick: Float
  ) = with(drawScope) {
    if (!diyaLit || cachedShadowVolumes.isEmpty()) return

    val w = size.width
    val h = size.height
    val halfH = h * 0.5f

    // 1. Warm radial floor light pool from the right-hand Diya lamp before carving out shadow wedges
    val torchFloorCenterX = w * (0.54f + torchSwayShiftNorm)
    val torchFloorCenterY = h * 0.86f
    val poolRadius = w * (0.48f + 0.34f * torchLightRadiusNormalized) * torchFlickerMultiplier

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFFB300).copy(alpha = (0.19f * torchLightRadiusNormalized).coerceIn(0.05f, 0.24f)),
          Color(0xFFE65100).copy(alpha = (0.09f * torchLightRadiusNormalized).coerceIn(0.02f, 0.12f)),
          Color.Transparent
        ),
        center = Offset(torchFloorCenterX, torchFloorCenterY),
        radius = poolRadius.coerceAtLeast(80f)
      ),
      radius = poolRadius.coerceAtLeast(80f),
      center = Offset(torchFloorCenterX, torchFloorCenterY)
    )

    // 2. Render each projected shadow volume from farthest to nearest
    val backToFront = cachedShadowVolumes.asReversed()
    for (vol in backToFront) {
      val baseScreenX = vol.screenCenterXNorm * w
      val halfBaseW = (vol.screenHalfWidthNorm * w).coerceIn(8f, w * 0.32f)

      // Perspective floor Y where the pillar/statue base meets the stone floor
      val perspectiveRatio = (1.0f / vol.distanceToTorch.coerceAtLeast(0.55f)).coerceIn(0.08f, 0.92f)
      val baseFloorY = (halfH + halfH * perspectiveRatio * 0.78f).coerceIn(halfH + 6f, h - 8f)

      // Shadow projects away from the right-hand Diya lamp toward the horizon & opposite side
      val shadowLengthPx = (halfH * perspectiveRatio * (0.55f + 0.45f * vol.shadowReachNorm)).coerceIn(18f, halfH * 0.85f)
      val farFloorY = (baseFloorY - shadowLengthPx * 0.52f).coerceIn(halfH + 2f, baseFloorY - 6f)

      // Skew angle driven by right-hand torch offset + dynamic flame flicker
      val flickerJitter = sin(animTick * 17.5f + vol.worldX.toFloat() * 3.1f) * 6.5f * (2f - torchFlickerMultiplier)
      val skewPx = vol.torchLateralSkewNorm * w * 0.22f + flickerJitter
      val farHalfW = halfBaseW * (1.55f + 0.35f * vol.shadowReachNorm)

      // Soft outer Penumbra trapezoid on the temple floor
      val penumbraPath = Path().apply {
        moveTo(baseScreenX - halfBaseW * 1.25f, baseFloorY)
        lineTo(baseScreenX + halfBaseW * 1.25f, baseFloorY)
        lineTo(baseScreenX + skewPx + farHalfW * 1.35f, farFloorY)
        lineTo(baseScreenX + skewPx - farHalfW * 1.35f, farFloorY)
        close()
      }
      drawPath(
        path = penumbraPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF030100).copy(alpha = vol.penumbraAlpha * 0.45f),
            Color(0xFF030100).copy(alpha = vol.penumbraAlpha)
          ),
          startY = farFloorY,
          endY = baseFloorY
        )
      )

      // Deep inner Umbra core trapezoid on the temple floor
      val umbraPath = Path().apply {
        moveTo(baseScreenX - halfBaseW * 0.82f, baseFloorY)
        lineTo(baseScreenX + halfBaseW * 0.82f, baseFloorY)
        lineTo(baseScreenX + skewPx + farHalfW * 0.78f, farFloorY)
        lineTo(baseScreenX + skewPx - farHalfW * 0.78f, farFloorY)
        close()
      }
      drawPath(
        path = umbraPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF020000).copy(alpha = vol.umbraAlpha * 0.55f),
            Color(0xFF020000).copy(alpha = vol.umbraAlpha)
          ),
          startY = farFloorY,
          endY = baseFloorY
        )
      )

      // For tall Mandapa pillars and creatures, also cast an upper cornice shadow wedge on the ceiling
      if (vol.occluderType == ShadowOccluderType.MANDAPA_PILLAR ||
        vol.occluderType == ShadowOccluderType.CREATURE_SILHOUETTE
      ) {
        val baseCeilY = (halfH - halfH * perspectiveRatio * 0.76f).coerceIn(8f, halfH - 6f)
        val farCeilY = (baseCeilY + shadowLengthPx * 0.42f).coerceIn(baseCeilY + 4f, halfH - 2f)
        val ceilPath = Path().apply {
          moveTo(baseScreenX - halfBaseW * 0.95f, baseCeilY)
          lineTo(baseScreenX + halfBaseW * 0.95f, baseCeilY)
          lineTo(baseScreenX + skewPx + farHalfW * 1.05f, farCeilY)
          lineTo(baseScreenX + skewPx - farHalfW * 1.05f, farCeilY)
          close()
        }
        drawPath(
          path = ceilPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF020000).copy(alpha = vol.umbraAlpha * 0.75f),
              Color(0xFF020000).copy(alpha = vol.penumbraAlpha * 0.35f)
            ),
            startY = baseCeilY,
            endY = farCeilY
          )
        )
      }

      // Ambient contact shadow ellipse directly beneath statues, steles, and sconces
      if (vol.occluderType != ShadowOccluderType.MANDAPA_PILLAR) {
        val contactW = halfBaseW * 2.1f
        val contactH = (halfBaseW * 0.55f).coerceAtLeast(5f)
        drawOval(
          color = Color(0xFF020000).copy(alpha = (vol.umbraAlpha * 1.1f).coerceIn(0.2f, 0.88f)),
          topLeft = Offset(baseScreenX - contactW * 0.5f, baseFloorY - contactH * 0.5f),
          size = Size(contactW, contactH)
        )
      }
    }
  }

  /**
   * Renders 2D top-down shadow wedges cast by pillars and statues inside the Yantra Mini-Map radar.
   */
  fun renderMiniMapShadowCones(
    drawScope: DrawScope,
    playerX: Double,
    playerY: Double,
    cols: Int,
    rows: Int,
    diyaLit: Boolean
  ) = with(drawScope) {
    if (!diyaLit || cachedShadowVolumes.isEmpty()) return

    val cellW = size.width / cols
    val cellH = size.height / rows
    val px = playerX.toFloat() * cellW
    val py = playerY.toFloat() * cellH

    for (vol in cachedShadowVolumes.take(8)) {
      val ox = vol.worldX.toFloat() * cellW
      val oy = vol.worldY.toFloat() * cellH
      val angle = atan2(oy - py, ox - px)
      val spread = vol.angularHalfWidthRad.coerceIn(0.08f, 0.42f)
      val shadowExtend = cellW * (2.2f + vol.shadowReachNorm * 1.8f)

      val p1x = ox + cos(angle - spread) * (cellW * 0.3f)
      val p1y = oy + sin(angle - spread) * (cellH * 0.3f)
      val p2x = ox + cos(angle + spread) * (cellW * 0.3f)
      val p2y = oy + sin(angle + spread) * (cellH * 0.3f)
      val f1x = ox + cos(angle - spread * 1.35f) * shadowExtend
      val f1y = oy + sin(angle - spread * 1.35f) * shadowExtend
      val f2x = ox + cos(angle + spread * 1.35f) * shadowExtend
      val f2y = oy + sin(angle + spread * 1.35f) * shadowExtend

      val wedge = Path().apply {
        moveTo(p1x, p1y)
        lineTo(p2x, p2y)
        lineTo(f2x, f2y)
        lineTo(f1x, f1y)
        close()
      }
      drawPath(
        path = wedge,
        color = Color(0xAA040100)
      )
    }
  }

  private fun buildShadowVolume(
    occluderType: ShadowOccluderType,
    worldX: Double,
    worldY: Double,
    torchOriginX: Double,
    torchOriginY: Double,
    playerAngle: Double,
    fov: Float,
    halfFov: Float,
    maxReach: Float,
    torchFlickerMultiplier: Float
  ): ProjectedShadowVolume? {
    val dx = worldX - torchOriginX
    val dy = worldY - torchOriginY
    val dist = hypot(dx, dy).toFloat()
    if (dist < 0.32f || dist > maxReach) return null

    var relAngle = (atan2(dy, dx) - playerAngle).toFloat()
    while (relAngle > PI.toFloat()) relAngle -= (2.0 * PI).toFloat()
    while (relAngle < -PI.toFloat()) relAngle += (2.0 * PI).toFloat()

    val angularHalfWidth = atan2(occluderType.baseRadiusTiles, dist).coerceIn(0.035f, 0.48f)
    if (abs(relAngle) > halfFov + angularHalfWidth + 0.22f) return null

    val screenCenterXNorm = 0.5f + (relAngle / fov)
    val screenHalfWidthNorm = (angularHalfWidth / fov).coerceIn(0.02f, 0.28f)
    val reachNorm = (1f - (dist / maxReach)).coerceIn(0.08f, 1.0f)

    // Stronger contrast when flame blazes bright and occluder is close to the torch
    val flickerBoost = torchFlickerMultiplier.coerceIn(0.65f, 1.18f)
    val umbraAlpha = (0.76f * reachNorm * occluderType.heightScale * flickerBoost).coerceIn(0.14f, 0.86f)
    val penumbraAlpha = (umbraAlpha * 0.62f).coerceIn(0.08f, 0.58f)

    // Lateral skew away from the right-hand torch position (screen X ~ 0.62)
    val lateralSkew = ((screenCenterXNorm - (0.56f + torchSwayShiftNorm)) * 0.85f).coerceIn(-0.45f, 0.45f)

    return ProjectedShadowVolume(
      occluderType = occluderType,
      worldX = worldX,
      worldY = worldY,
      distanceToTorch = dist,
      centerAngleRad = relAngle,
      angularHalfWidthRad = angularHalfWidth,
      screenCenterXNorm = screenCenterXNorm,
      screenHalfWidthNorm = screenHalfWidthNorm,
      shadowReachNorm = reachNorm,
      umbraAlpha = umbraAlpha,
      penumbraAlpha = penumbraAlpha,
      torchLateralSkewNorm = lateralSkew
    )
  }

  private fun hasAdjacentOpenCorridor(
    mapGrid: List<String>,
    r: Int,
    c: Int,
    rows: Int,
    cols: Int
  ): Boolean {
    var openNeighbors = 0
    if (r > 0 && mapGrid[r - 1][c] != '1') openNeighbors++
    if (r < rows - 1 && mapGrid[r + 1][c] != '1') openNeighbors++
    if (c > 0 && mapGrid[r][c - 1] != '1') openNeighbors++
    if (c < cols - 1 && mapGrid[r][c + 1] != '1') openNeighbors++
    return openNeighbors >= 2
  }
}
