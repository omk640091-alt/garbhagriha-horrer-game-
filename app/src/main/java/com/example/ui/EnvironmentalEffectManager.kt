package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Classification of local environmental disturbances inside the ancient temple sanctum,
 * driven by the player's movement speed and proximity to carved stone walls.
 */
enum class EnvironmentalEffectState(
  val id: String,
  val hudLabel: String,
  val sanskritLabel: String,
  val description: String
) {
  STILL_AIR(
    id = "still",
    hudLabel = "STILL SANCTUM",
    sanskritLabel = "शान्त वायु (Shanta Vayu)",
    description = "Ancient dust motes hang motionless in the stagnant temple air"
  ),
  DRIFTING_SPORES(
    id = "spores",
    hudLabel = "DRIFTING DUST",
    sanskritLabel = "धूलि कण (Dhuli Kana)",
    description = "Footsteps stir sacred Vibhuti ash and centuries-old stone dust"
  ),
  COBWEB_BRUSH(
    id = "cobwebs",
    hudLabel = "COBWEB BRUSH",
    sanskritLabel = "जाल स्पर्श (Jala Sparsha)",
    description = "Brushing closely against carved granite walls snags dense silk cobwebs"
  ),
  FALLING_MASONRY_DUST(
    id = "falling_dust",
    hudLabel = "FALLING DUST",
    sanskritLabel = "अश्म पात (Ashma Pata)",
    description = "Rapid movement near temple pillars dislodges streams of ceiling dust and grit"
  )
}

/**
 * Represents a falling dust mote, stone grit speck, or ash flake in normalized screen space.
 */
data class FallingDustParticle(
  val xNorm: Float,
  val yNorm: Float,
  val vyNorm: Float,
  val vxNorm: Float,
  val radiusDp: Float,
  val alpha: Float,
  val isStoneGrit: Boolean
)

/**
 * Represents a gossamer cobweb strand clinging to the viewport periphery when the player
 * brushes close to ancient temple walls or alcoves.
 */
data class CobwebStrandCluster(
  val cornerIndex: Int, // 0 = Top-Left, 1 = Top-Right, 2 = Bottom-Left, 3 = Bottom-Right
  val reachFraction: Float,
  val tensionAlpha: Float,
  val swayPhase: Float
)

/**
 * Manages local environmental changes — such as falling ceiling dust, dislodged stone grit,
 * and clinging cobweb strands — triggered dynamically by the player's movement speed
 * and proximity to carved temple walls (`'1'`).
 */
class EnvironmentalEffectManager {

  var effectState by mutableStateOf(EnvironmentalEffectState.STILL_AIR)
    private set

  /** Normalized player movement speed (0.0 = stationary, ~0.55 = walking, 1.0 = sprinting). */
  var movementSpeedNormalized by mutableFloatStateOf(0f)
    private set

  /** Closest Euclidean distance (in map tiles) from the player's position to a solid temple wall ('1'). */
  var nearestWallDistance by mutableFloatStateOf(0.5f)
    private set

  /** Normalized wall proximity (0.0 = center of open corridor, 1.0 = brushing directly against stone wall). */
  var wallProximityFactor by mutableFloatStateOf(0f)
    private set

  /** Intensity of falling ceiling dust and stone grit streams (0.0 .. 1.0). */
  var fallingDustIntensity by mutableFloatStateOf(0.12f)
    private set

  /** Intensity of peripheral cobweb strands snagging across the viewport (0.0 .. 1.0). */
  var cobwebClingIntensity by mutableFloatStateOf(0f)
    private set

  /** Total environmental disturbance triggers (cobweb snags or dust cascades) during the expedition. */
  var disturbanceCount by mutableIntStateOf(0)
    private set

  val dustParticles = mutableStateListOf<FallingDustParticle>()
  val cobwebClusters = mutableStateListOf<CobwebStrandCluster>()

  private var lastPlayerX: Double = -1.0
  private var lastPlayerY: Double = -1.0
  private val rng = Random(108)

  init {
    seedInitialAmbientDust()
  }

  private fun seedInitialAnalyzerParticles(count: Int) {
    for (i in 0 until count) {
      dustParticles.add(
        FallingDustParticle(
          xNorm = rng.nextFloat(),
          yNorm = rng.nextFloat(),
          vyNorm = 0.015f + rng.nextFloat() * 0.025f,
          vxNorm = (rng.nextFloat() - 0.5f) * 0.008f,
          radiusDp = 1.6f + rng.nextFloat() * 2.4f,
          alpha = 0.22f + rng.nextFloat() * 0.35f,
          isStoneGrit = rng.nextBoolean()
        )
      )
    }
  }

  private fun seedInitialAmbientDust() {
    dustParticles.clear()
    seedInitialAnalyzerParticles(18)
    cobwebClusters.clear()
    for (corner in 0..3) {
      cobwebClusters.add(
        CobwebStrandCluster(
          cornerIndex = corner,
          reachFraction = 0.16f,
          tensionAlpha = 0f,
          swayPhase = corner * 1.4f
        )
      )
    }
  }

  /**
   * Computes the shortest distance from `(playerX, playerY)` to any solid wall tile (`'1'`)
   * in the 3x3 neighborhood around the player.
   */
  fun computeNearestWallDistance(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double
  ): Float {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12
    val baseCol = floor(playerX).toInt()
    val baseRow = floor(playerY).toInt()
    var minDist = 2.0

    for (dr in -1..1) {
      for (dc in -1..1) {
        val r = baseRow + dr
        val c = baseCol + dc
        val isWall = if (r in 0 until rows && c in 0 until cols) {
          mapGrid[r][c] == '1'
        } else {
          true
        }
        if (isWall) {
          // Closest point on the axis-aligned unit square [c, c+1] x [r, r+1]
          val closestX = playerX.coerceIn(c.toDouble(), (c + 1).toDouble())
          val closestY = playerY.coerceIn(r.toDouble(), (r + 1).toDouble())
          val d = hypot(playerX - closestX, playerY - closestY)
          if (d < minDist) {
            minDist = d
          }
        }
      }
    }
    return minDist.toFloat().coerceIn(0.05f, 2.0f)
  }

  /**
   * Called immediately when the player steps forward or backward in `movePlayer(...)`.
   * Evaluates step speed and wall proximity to spawn bursts of falling ceiling dust or snag cobwebs.
   * Returns an optional atmospheric toast if a prominent cobweb brush or masonry dust fall occurs.
   */
  fun onPlayerMoveStep(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    stepMagnitude: Double,
    isSprinting: Boolean
  ): String? {
    val wallDist = computeNearestWallDistance(mapGrid, playerX, playerY)
    nearestWallDistance = wallDist
    // In a 1-tile-wide corridor, center is 0.50 from walls; brushing near a wall is <= 0.34
    val wallProx = ((0.54f - wallDist) / 0.38f).coerceIn(0f, 1f)
    wallProximityFactor = wallProx

    val stepSpeedNorm = (abs(stepMagnitude).toFloat() / 0.55f).coerceIn(0.25f, 1.0f)
    movementSpeedNormalized = maxOf(movementSpeedNormalized, if (isSprinting) 1.0f else stepSpeedNorm)

    // Spawn falling dust particles from upper ceiling crevices proportional to speed & wall proximity
    val burstCount = when {
      isSprinting && wallProx > 0.35f -> 9
      isSprinting -> 6
      wallProx > 0.45f -> 5
      else -> 3
    }
    spawnFallingDustBurst(burstCount, isHeavyGrit = isSprinting || wallProx > 0.4f)

    var message: String? = null

    if (wallProx > 0.42f && isSprinting) {
      disturbanceCount++
      cobwebClingIntensity = (cobwebClingIntensity + 0.34f).coerceAtMost(1.0f)
      fallingDustIntensity = (fallingDustIntensity + 0.38f).coerceAtMost(1.0f)
      effectState = EnvironmentalEffectState.FALLING_MASONRY_DUST
      if (disturbanceCount % 3 == 1) {
        message = "Sprinting near carved granite pillars dislodged falling masonry dust and ancient cobwebs!"
      }
    } else if (wallProx > 0.48f) {
      disturbanceCount++
      cobwebClingIntensity = (cobwebClingIntensity + 0.28f).coerceAtMost(0.95f)
      effectState = EnvironmentalEffectState.COBWEB_BRUSH
      if (disturbanceCount % 4 == 1) {
        message = "You brushed closely against the sanctum wall — dense cobwebs snag across your shoulders!"
      }
    } else if (isSprinting) {
      fallingDustIntensity = (fallingDustIntensity + 0.26f).coerceAtMost(0.92f)
      effectState = EnvironmentalEffectState.FALLING_MASONRY_DUST
    } else {
      fallingDustIntensity = (fallingDustIntensity + 0.12f).coerceAtMost(0.65f)
      effectState = EnvironmentalEffectState.DRIFTING_SPORES
    }

    lastPlayerX = playerX
    lastPlayerY = playerY
    return message
  }

  /**
   * Updates particle physics, velocity decay, cobweb tension, and environmental state at 10Hz (every 100ms).
   */
  fun onTick(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    isSprinting: Boolean,
    animTick: Float
  ) {
    val wallDist = computeNearestWallDistance(mapGrid, playerX, playerY)
    nearestWallDistance = wallDist
    val wallProx = ((0.54f - wallDist) / 0.38f).coerceIn(0f, 1f)
    wallProximityFactor = wallProx

    val movedDelta = if (lastPlayerX >= 0.0 && lastPlayerY >= 0.0) {
      hypot(playerX - lastPlayerX, playerY - lastPlayerY).toFloat()
    } else {
      0f
    }
    lastPlayerX = playerX
    lastPlayerY = playerY

    // Smoothly decay movement speed when player stops stepping
    movementSpeedNormalized = if (movedDelta > 0.01f) {
      val target = if (isSprinting) 1.0f else (movedDelta / 0.36f).coerceIn(0.35f, 0.85f)
      (movementSpeedNormalized * 0.55f + target * 0.45f).coerceIn(0f, 1f)
    } else {
      (movementSpeedNormalized * 0.80f).let { if (it < 0.04f) 0f else it }
    }

    // Target dust & cobweb intensities driven by speed and wall proximity
    val targetDust = (0.14f + movementSpeedNormalized * 0.56f + wallProx * 0.30f).coerceIn(0.12f, 1.0f)
    fallingDustIntensity = (fallingDustIntensity * 0.82f + targetDust * 0.18f).coerceIn(0.10f, 1.0f)

    val targetCobweb = if (wallProx > 0.25f) {
      (wallProx * 0.78f + movementSpeedNormalized * 0.22f).coerceIn(0f, 1.0f)
    } else {
      // Cobwebs gradually clear as the player moves away from walls
      (cobwebClingIntensity * 0.86f).let { if (it < 0.03f) 0f else it }
    }
    cobwebClingIntensity = (cobwebClingIntensity * 0.78f + targetCobweb * 0.22f).coerceIn(0f, 1.0f)

    // Update active state classification
    effectState = when {
      movementSpeedNormalized > 0.62f && wallProx > 0.28f -> EnvironmentalEffectState.FALLING_MASONRY_DUST
      cobwebClingIntensity > 0.36f -> EnvironmentalEffectState.COBWEB_BRUSH
      movementSpeedNormalized > 0.52f -> EnvironmentalEffectState.FALLING_MASONRY_DUST
      movementSpeedNormalized > 0.12f || fallingDustIntensity > 0.28f -> EnvironmentalEffectState.DRIFTING_SPORES
      else -> EnvironmentalEffectState.STILL_AIR
    }

    // Advance falling dust particles downward
    val speedBoost = 1.0f + movementSpeedNormalized * 1.85f + wallProx * 0.65f
    for (i in dustParticles.indices) {
      val p = dustParticles[i]
      val nextY = p.yNorm + p.vyNorm * speedBoost
      val flutterX = sin(animTick * 2.4f + i * 0.7f) * 0.004f
      var nextX = (p.xNorm + p.vxNorm + flutterX) % 1.0f
      if (nextX < 0f) nextX += 1.0f

      if (nextY > 1.04f) {
        // Respawn at ceiling (concentrated near left/right wall edges when wallProximityFactor is high)
        val spawnNearWallEdge = wallProx > 0.35f && (i % 2 == 0)
        val respawnX = if (spawnNearWallEdge) {
          if (i % 4 == 0) rng.nextFloat() * 0.24f else 0.76f + rng.nextFloat() * 0.24f
        } else {
          rng.nextFloat()
        }
        dustParticles[i] = p.copy(
          xNorm = respawnX,
          yNorm = -0.02f,
          vyNorm = 0.016f + rng.nextFloat() * 0.032f,
          alpha = (0.20f + fallingDustIntensity * 0.60f).coerceIn(0.18f, 0.88f)
        )
      } else {
        dustParticles[i] = p.copy(
          xNorm = nextX,
          yNorm = nextY
        )
      }
    }

    // Update corner cobweb strand reach and opacity
    for (c in cobwebClusters.indices) {
      val cluster = cobwebClusters[c]
      val cornerReach = (0.14f + cobwebClingIntensity * 0.28f + wallProx * 0.10f).coerceIn(0.12f, 0.46f)
      cobwebClusters[c] = cluster.copy(
        reachFraction = cornerReach,
        tensionAlpha = cobwebClingIntensity,
        swayPhase = animTick * (1.8f + movementSpeedNormalized * 2.2f) + c * 1.3f
      )
    }
  }

  private fun spawnFallingDustBurst(count: Int, isHeavyGrit: Boolean) {
    val maxParticles = 34
    for (n in 0 until count) {
      val nearEdge = rng.nextBoolean()
      val x = if (nearEdge) {
        if (rng.nextBoolean()) rng.nextFloat() * 0.28f else 0.72f + rng.nextFloat() * 0.28f
      } else {
        rng.nextFloat()
      }
      val newParticle = FallingDustParticle(
        xNorm = x,
        yNorm = rng.nextFloat() * 0.22f,
        vyNorm = if (isHeavyGrit) 0.028f + rng.nextFloat() * 0.038f else 0.016f + rng.nextFloat() * 0.024f,
        vxNorm = (rng.nextFloat() - 0.5f) * 0.012f,
        radiusDp = if (isHeavyGrit) 2.2f + rng.nextFloat() * 3.0f else 1.5f + rng.nextFloat() * 2.0f,
        alpha = if (isHeavyGrit) 0.55f + rng.nextFloat() * 0.35f else 0.30f + rng.nextFloat() * 0.30f,
        isStoneGrit = isHeavyGrit
      )
      if (dustParticles.size < maxParticles) {
        dustParticles.add(newParticle)
      } else {
        val replaceIdx = rng.nextInt(dustParticles.size)
        dustParticles[replaceIdx] = newParticle
      }
    }
  }
}

/**
 * First-Person Canvas overlay that renders local environmental effects:
 * 1. Falling ceiling dust streams, dislodged Chola granite grit, and drifting Vibhuti spores
 *    that accelerate when sprinting or brushing near stone walls.
 * 2. Gossamer cobweb filament networks across the viewport corners and edges that snag and
 *    sway when the player moves closely past temple walls.
 */
@Composable
fun EnvironmentalDisturbanceOverlay(
  environmentalManager: EnvironmentalEffectManager,
  diyaLit: Boolean,
  animTick: Float,
  modifier: Modifier = Modifier
) {
  val dustIntensity = environmentalManager.fallingDustIntensity
  val cobwebIntensity = environmentalManager.cobwebClingIntensity
  val wallProximity = environmentalManager.wallProximityFactor
  val speedNorm = environmentalManager.movementSpeedNormalized
  val particles = environmentalManager.dustParticles
  val cobwebs = environmentalManager.cobwebClusters

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("environmental_disturbance_overlay")
  ) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    // 1. Ceiling Masonry Dust Cascades when moving rapidly or brushing near walls
    if (dustIntensity > 0.22f) {
      val cascadeAlpha = ((dustIntensity - 0.20f) * 0.42f).coerceIn(0f, 0.36f)
      val columns = 5
      for (col in 0 until columns) {
        val phase = animTick * (1.6f + speedNorm * 1.8f) + col * 1.37f
        val xCenter = w * (0.12f + 0.19f * col + sin(phase * 0.5f) * 0.03f)
        val streakWidth = w * (0.045f + 0.025f * wallProximity)
        val streakHeight = h * (0.28f + 0.35f * dustIntensity)
        val yOffset = ((phase * 95f) % (h * 0.65f)) - streakHeight * 0.35f

        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              if (diyaLit) Color(0xFFD7CCC8).copy(alpha = cascadeAlpha)
              else Color(0xFF9E9E9E).copy(alpha = cascadeAlpha * 0.75f),
              Color.Transparent
            ),
            startY = yOffset,
            endY = yOffset + streakHeight
          ),
          topLeft = Offset(xCenter - streakWidth * 0.5f, yOffset.coerceAtLeast(0f)),
          size = Size(streakWidth, streakHeight)
        )
      }
    }

    // 2. Individual Falling Dust Motes & Dislodged Stone Grit Particles
    for (i in particles.indices) {
      val p = particles[i]
      val px = (p.xNorm * w).coerceIn(0f, w)
      val py = (p.yNorm * h).coerceIn(0f, h)
      val effectiveAlpha = (p.alpha * (0.45f + 0.55f * dustIntensity)).coerceIn(0.08f, 0.88f)
      val particleColor = when {
        p.isStoneGrit && diyaLit -> Color(0xFFD7B899).copy(alpha = effectiveAlpha)
        diyaLit -> Color(0xFFFFE0B2).copy(alpha = effectiveAlpha * 0.85f)
        else -> Color(0xFFBDBDBD).copy(alpha = effectiveAlpha * 0.65f)
      }

      val rPx = p.radiusDp * (0.85f + 0.45f * dustIntensity)
      drawCircle(
        color = particleColor,
        radius = rPx,
        center = Offset(px, py)
      )

      // Draw motion-blur falling streak when sprinting or heavy grit is dislodged
      if (speedNorm > 0.45f || p.isStoneGrit) {
        val tailLen = (8f + speedNorm * 22f + wallProximity * 12f)
        drawLine(
          color = particleColor.copy(alpha = effectiveAlpha * 0.55f),
          start = Offset(px, (py - tailLen).coerceAtLeast(0f)),
          end = Offset(px, py),
          strokeWidth = (rPx * 0.75f).coerceAtLeast(1.2f),
          cap = StrokeCap.Round
        )
      }
    }

    // 3. Peripheral & Corner Cobweb Filaments when brushing near temple walls
    if (cobwebIntensity > 0.04f) {
      for (cluster in cobwebs) {
        drawCornerCobweb(
          w = w,
          h = h,
          cornerIndex = cluster.cornerIndex,
          reachFraction = cluster.reachFraction,
          alpha = (cluster.tensionAlpha * 0.72f).coerceIn(0.05f, 0.75f),
          swayPhase = cluster.swayPhase,
          diyaLit = diyaLit
        )
      }
    }
  }
}

private fun DrawScope.drawCornerCobweb(
  w: Float,
  h: Float,
  cornerIndex: Int,
  reachFraction: Float,
  alpha: Float,
  swayPhase: Float,
  diyaLit: Boolean
) {
  val originX = if (cornerIndex % 2 == 0) 0f else w
  val originY = if (cornerIndex < 2) 0f else h
  val dirX = if (cornerIndex % 2 == 0) 1f else -1f
  val dirY = if (cornerIndex < 2) 1f else -1f

  val maxSpanX = w * reachFraction
  val maxSpanY = h * reachFraction
  val webColor = if (diyaLit) {
    Color(0xFFECE0D1).copy(alpha = alpha)
  } else {
    Color(0xFFB0BEC5).copy(alpha = alpha * 0.82f)
  }

  // Primary radial silk anchor threads from the corner
  val radialAngles = listOf(0.12f, 0.34f, 0.58f, 0.82f, 1.08f, 1.36f)
  val anchorPoints = mutableListOf<List<Offset>>()

  for ((idx, angleFrac) in radialAngles.withIndex()) {
    val theta = angleFrac * (PI.toFloat() * 0.5f)
    val sway = sin(swayPhase + idx * 0.9f) * 9f
    val endX = originX + dirX * cos(theta) * maxSpanX + sway
    val endY = originY + dirY * sin(theta) * maxSpanY + sway * 0.6f

    drawLine(
      color = webColor,
      start = Offset(originX, originY),
      end = Offset(endX, endY),
      strokeWidth = 1.6f,
      cap = StrokeCap.Round
    )

    // Store intermediate rib points along this radial thread for concentric web rings
    val ringFractions = listOf(0.32f, 0.58f, 0.84f)
    val pts = ringFractions.map { rf ->
      val sag = sin(swayPhase * 1.2f + rf * 4f) * 4.5f
      Offset(
        x = originX + (endX - originX) * rf + sag * dirX,
        y = originY + (endY - originY) * rf + sag * dirY
      )
    }
    anchorPoints.add(pts)
  }

  // Draw curved concentric silk cross-strands connecting adjacent radial threads
  for (ringIdx in 0..2) {
    val path = Path()
    for (rIdx in 0 until anchorPoints.size) {
      val pt = anchorPoints[rIdx][ringIdx]
      if (rIdx == 0) {
        path.moveTo(pt.x, pt.y)
      } else {
        val prev = anchorPoints[rIdx - 1][ringIdx]
        val midX = (prev.x + pt.x) * 0.5f - dirX * 6f
        val midY = (prev.y + pt.y) * 0.5f - dirY * 6f
        path.quadraticTo(midX, midY, pt.x, pt.y)
      }
    }
    drawPath(
      path = path,
      color = webColor.copy(alpha = (alpha * (0.85f - ringIdx * 0.18f)).coerceAtLeast(0.05f)),
      style = Stroke(width = 1.2f)
    )
  }
}
