package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PlayerPresence
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * First-Person 3D Raycasting Engine for Garbhagriha.
 * Renders carved ancient Hindu temple corridors, flickering brass Diya sconces,
 * Vedic Seal altars, the Swarga Dwara escape gate, co-op Sadhaka teammates,
 * and the hunting Brahmarakshasa / Chhaya Pret / Pishacha entity in real time.
 */
@Composable
fun FirstPersonSanctumViewport(
  mapGrid: List<String>,
  playerX: Double,
  playerY: Double,
  playerAngle: Double,
  diyaLit: Boolean,
  torchLightRadiusTiles: Float = if (diyaLit) 7.5f else 2.4f,
  torchLightRadiusNormalized: Float = if (diyaLit) 1.0f else 0.22f,
  torchFlickerMultiplier: Float = 1.0f,
  chantingMantra: Boolean,
  prana: Int,
  entityX: Double,
  entityY: Double,
  entityAlertLevel: Int,
  litDiyaCoords: Set<Pair<Int, Int>>,
  solvedSealCoords: Set<Pair<Int, Int>>,
  coOpPlayers: List<PlayerPresence>,
  currentUserId: String,
  animTick: Float,
  onLookDelta: (Float) -> Unit,
  effectManager: PostProcessEffectManager? = null,
  environmentalManager: EnvironmentalEffectManager? = null,
  screenShakeController: ScreenShakeController? = null,
  shadowCasterSystem: TorchShadowCasterSystem? = null,
  dialogueEncounters: List<TempleDialogueEncounter> = emptyList(),
  ritualManager: RitualInteractionManager? = null,
  fogOfWarManager: FogOfWarMiniMapManager? = null,
  modifier: Modifier = Modifier
) {
  val activeShadowSystem = shadowCasterSystem ?: androidx.compose.runtime.remember { TorchShadowCasterSystem() }
  val activeFogManager = fogOfWarManager ?: androidx.compose.runtime.remember { FogOfWarMiniMapManager() }
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF060201))
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          onLookDelta(dragAmount.x * 0.0055f)
        }
      }
      .testTag("first_person_viewport")
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .applyScreenShake(screenShakeController)
    ) {
      val w = size.width
      val h = size.height
      val halfH = h * 0.5f

      // Dynamic Torch / Diya illumination reach driven by TorchLightManager
      val lightReach = torchLightRadiusTiles.coerceIn(2.0f, 9.5f)

      // 1. Ceiling & Floor atmospheric gradients
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF140804),
            Color(0xFF090302),
            Color(0xFF030101)
          ),
          startY = 0f,
          endY = halfH
        ),
        topLeft = Offset.Zero,
        size = Size(w, halfH)
      )

      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF030101),
            Color(0xFF120805),
            Color(0xFF24110A)
          ),
          startY = halfH,
          endY = h
        ),
        topLeft = Offset(0f, halfH),
        size = Size(w, halfH)
      )

      // 1b. Update & Render Dynamic Torch Shadow Volumes (Floor & Ceiling Umbra/Penumbra from Pillars & Statues)
      activeShadowSystem.updateShadowCasters(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        playerAngle = playerAngle,
        diyaLit = diyaLit,
        torchLightRadiusTiles = lightReach,
        torchFlickerMultiplier = torchFlickerMultiplier,
        entityX = entityX,
        entityY = entityY,
        dialogueEncounters = dialogueEncounters,
        animTick = animTick
      )
      activeShadowSystem.renderFloorAndCeilingShadows(
        drawScope = this,
        diyaLit = diyaLit,
        torchLightRadiusNormalized = torchLightRadiusNormalized,
        torchFlickerMultiplier = torchFlickerMultiplier,
        animTick = animTick
      )

      // 2. Raycasting 3D Walls (96 vertical slices for crisp performance and carved texture detail)
      val numRays = 96
      val fov = (PI / 3.0).toFloat() // 60 degrees FOV
      val sliceWidth = (w / numRays) + 1.5f
      val zBuffer = FloatArray(numRays) { 30f }

      val rows = mapGrid.size
      val cols = mapGrid.firstOrNull()?.length ?: 12

      for (rayIdx in 0 until numRays) {
        val cameraX = 2.0 * rayIdx / numRays.toDouble() - 1.0
        val rayAngle = playerAngle + cameraX * (fov / 2.0)
        val rayDirX = cos(rayAngle).let { if (abs(it) < 1e-5) 1e-5 else it }
        val rayDirY = sin(rayAngle).let { if (abs(it) < 1e-5) 1e-5 else it }

        var mapX = playerX.toInt()
        var mapY = playerY.toInt()

        val deltaDistX = abs(1.0 / rayDirX)
        val deltaDistY = abs(1.0 / rayDirY)

        val stepX: Int
        var sideDistX: Double
        if (rayDirX < 0) {
          stepX = -1
          sideDistX = (playerX - mapX) * deltaDistX
        } else {
          stepX = 1
          sideDistX = (mapX + 1.0 - playerX) * deltaDistX
        }

        val stepY: Int
        var sideDistY: Double
        if (rayDirY < 0) {
          stepY = -1
          sideDistY = (playerY - mapY) * deltaDistY
        } else {
          stepY = 1
          sideDistY = (mapY + 1.0 - playerY) * deltaDistY
        }

        var hit = false
        var side = 0
        var hitTile = '1'
        var steps = 0

        while (!hit && steps < 28) {
          if (sideDistX < sideDistY) {
            sideDistX += deltaDistX
            mapX += stepX
            side = 0
          } else {
            sideDistY += deltaDistY
            mapY += stepY
            side = 1
          }
          if (mapY in 0 until rows && mapX in 0 until cols) {
            val cell = mapGrid[mapY][mapX]
            if (cell == '1' || cell == '4') {
              hit = true
              hitTile = cell
            }
          } else {
            hit = true
            hitTile = '1'
          }
          steps++
        }

        val perpWallDist = if (side == 0) {
          (sideDistX - deltaDistX).coerceAtLeast(0.12)
        } else {
          (sideDistY - deltaDistY).coerceAtLeast(0.12)
        }.toFloat()

        zBuffer[rayIdx] = perpWallDist

        val lineHeight = (h / perpWallDist).coerceAtMost(h * 1.6f)
        val drawStart = (halfH - lineHeight / 2f).coerceAtLeast(0f)
        val drawEnd = (halfH + lineHeight / 2f).coerceAtMost(h)
        val sliceX = rayIdx * (w / numRays)

        // Compute texture coordinate for temple pillar / stone relief bands
        val wallX = if (side == 0) {
          playerY + perpWallDist * rayDirY
        } else {
          playerX + perpWallDist * rayDirX
        }
        val texU = (wallX - kotlin.math.floor(wallX)).toFloat()

        val distFactor = (1f - (perpWallDist / lightReach)).coerceIn(0.05f, 1f)
        val sideShade = if (side == 1) 0.74f else 1.0f
        // Apply cast shadow occlusion from intervening pillars, Murti statues, and steles
        val castShadowFactor = activeShadowSystem.computeRayShadowFactor(
          rayIdx = rayIdx,
          numRays = numRays,
          wallPerpDist = perpWallDist
        )
        val brightness = (distFactor * sideShade * castShadowFactor).coerceIn(0.03f, 1f)

        val baseColor = if (hitTile == '4') {
          // Swarga Dwara Sanctum Exit Portal (Golden/Cyan mystical gate)
          Color(
            red = (0.95f * brightness).coerceIn(0f, 1f),
            green = (0.72f * brightness).coerceIn(0f, 1f),
            blue = (0.20f * brightness).coerceIn(0f, 1f)
          )
        } else {
          // Carved Chola granite with warm Diya saffron bounce
          val reliefBand = if (texU in 0.15f..0.25f || texU in 0.75f..0.85f) 1.22f else 1.0f
          Color(
            red = (0.36f * brightness * reliefBand).coerceIn(0f, 1f),
            green = (0.19f * brightness * reliefBand).coerceIn(0f, 1f),
            blue = (0.11f * brightness).coerceIn(0f, 1f)
          )
        }

        drawRect(
          color = baseColor,
          topLeft = Offset(sliceX, drawStart),
          size = Size(sliceWidth, (drawEnd - drawStart).coerceAtLeast(2f))
        )

        // Carved horizontal Vedic frieze band across temple pillars
        if (perpWallDist < lightReach && (rayIdx % 2 == 0)) {
          val friezeHeight = (lineHeight * 0.08f).coerceAtLeast(2f)
          val friezeColor = if (hitTile == '4') {
            Color(0xFFFFEA00).copy(alpha = brightness * 0.75f)
          } else {
            Color(0xFFFF8F00).copy(alpha = brightness * 0.32f)
          }
          drawRect(
            color = friezeColor,
            topLeft = Offset(sliceX, drawStart + lineHeight * 0.25f),
            size = Size(sliceWidth, friezeHeight)
          )
          drawRect(
            color = friezeColor,
            topLeft = Offset(sliceX, drawStart + lineHeight * 0.72f),
            size = Size(sliceWidth, friezeHeight)
          )
          // Hanging ancient cobweb filaments along upper wall cornices when close to walls
          if (perpWallDist < 2.2f && ((mapX + mapY + rayIdx) % 7 == 0)) {
            val webLength = (lineHeight * 0.18f * (1f - perpWallDist / 2.6f)).coerceAtLeast(4f)
            val webSway = sin(animTick * 2.2f + rayIdx * 0.4f) * 3.5f
            drawLine(
              color = Color(0xFFE0E0E0).copy(alpha = (brightness * 0.48f).coerceIn(0.08f, 0.55f)),
              start = Offset(sliceX + sliceWidth * 0.5f, drawStart),
              end = Offset(sliceX + sliceWidth * 0.5f + webSway, drawStart + webLength),
              strokeWidth = 1.3f
            )
          }
        }
      }

      // 3. Collect and depth-sort 3D Billboards (Diyas, Vedic Seals, Co-Op Teammates, and the Mythological Creature)
      val sprites = mutableListOf<BillboardSprite>()

      val nextRitualCoord = ritualManager?.nextRequiredStep?.targetCoord
      val secretDoorCoord = ritualManager?.activeRitual?.secretDoorTileCoord

      for (r in 0 until rows) {
        for (c in 0 until cols) {
          when (mapGrid[r][c]) {
            '2' -> {
              val isLit = litDiyaCoords.contains(c to r)
              sprites.add(
                BillboardSprite(
                  x = c + 0.5,
                  y = r + 0.5,
                  type = if (isLit) SpriteType.DIYA_LIT else SpriteType.DIYA_UNLIT,
                  isRitualNextTarget = (nextRitualCoord == (c to r))
                )
              )
            }
            '3' -> {
              val isSolved = solvedSealCoords.contains(c to r)
              sprites.add(
                BillboardSprite(
                  x = c + 0.5,
                  y = r + 0.5,
                  type = if (isSolved) SpriteType.SEAL_SOLVED else SpriteType.SEAL_LOCKED,
                  isRitualNextTarget = (nextRitualCoord == (c to r))
                )
              )
            }
            '5' -> {
              sprites.add(
                BillboardSprite(
                  x = c + 0.5,
                  y = r + 0.5,
                  type = SpriteType.SECRET_CHAMBER_PORTAL
                )
              )
            }
            '6' -> {
              sprites.add(
                BillboardSprite(
                  x = c + 0.5,
                  y = r + 0.5,
                  type = SpriteType.SECRET_CHAMBER_SANCTUARY
                )
              )
            }
          }
        }
      }

      if (secretDoorCoord != null && ritualManager.isSecretChamberUnlocked != true) {
        sprites.add(
          BillboardSprite(
            x = secretDoorCoord.first + 0.5,
            y = secretDoorCoord.second + 0.5,
            type = SpriteType.SECRET_DOOR_SEALED
          )
        )
      }

      // Co-op teammates
      for (mate in coOpPlayers) {
        if (mate.userId != currentUserId) {
          sprites.add(
            BillboardSprite(
              x = mate.x,
              y = mate.y,
              type = SpriteType.TEAMMATE,
              label = mate.displayName
            )
          )
        }
      }

      // NPC Spirits and Ancient Stone Inscriptions
      for (enc in dialogueEncounters) {
        sprites.add(
          BillboardSprite(
            x = enc.tileX + 0.5,
            y = enc.tileY + 0.5,
            type = if (enc.speakerType == DialogueSpeakerType.NPC_SPIRIT) {
              SpriteType.NPC_SPIRIT
            } else {
              SpriteType.ANCIENT_INSCRIPTION
            },
            label = enc.speakerName
          )
        )
      }

      // The Mythological Creature (Brahmarakshasa / Chhaya Pret / Pishacha)
      sprites.add(
        BillboardSprite(
          x = entityX,
          y = entityY,
          type = SpriteType.CREATURE
        )
      )

      sprites
        .map { sp ->
          val dx = sp.x - playerX
          val dy = sp.y - playerY
          val dist = hypot(dx, dy)
          sp to dist
        }
        .sortedByDescending { it.second }
        .forEach { (sp, dist) ->
          if (dist > 0.25 && dist < 12.0) {
            val dx = sp.x - playerX
            val dy = sp.y - playerY
            var relAngle = atan2(dy, dx) - playerAngle
            while (relAngle > PI) relAngle -= 2.0 * PI
            while (relAngle < -PI) relAngle += 2.0 * PI

            val halfFov = fov / 2.0
            if (abs(relAngle) < halfFov + 0.25) {
              val screenX = ((0.5 + (relAngle / fov)) * w).toFloat()
              val rayIndex = ((screenX / w) * numRays).toInt().coerceIn(0, numRays - 1)
              if (dist.toFloat() < zBuffer[rayIndex] + 0.4f) {
                val spriteSize = (h / dist.toFloat()).coerceAtMost(h * 0.95f)
                drawSpriteBillboard(
                  sp = sp,
                  screenX = screenX,
                  centerY = halfH,
                  spriteSize = spriteSize,
                  dist = dist.toFloat(),
                  animTick = animTick
                )
              }
            }
          }
        }

      // 4. First-Person Brass Diya Hand-Lamp at Bottom-Right & Protective Mantra Aura
      drawFirstPersonDiyaHand(
        w = w,
        h = h,
        diyaLit = diyaLit,
        lightRadiusNormalized = torchLightRadiusNormalized,
        flickerMultiplier = torchFlickerMultiplier,
        chantingMantra = chantingMantra,
        animTick = animTick
      )
    }

    // 4b. Local Environmental Disturbance Overlay (Falling Dust, Masonry Grit & Cobweb Brush)
    val activeEnvManager = environmentalManager ?: androidx.compose.runtime.remember { EnvironmentalEffectManager() }
    EnvironmentalDisturbanceOverlay(
      environmentalManager = activeEnvManager,
      diyaLit = diyaLit,
      animTick = animTick
    )

    // 5. Post-Processing Horror Overlay (Film Grain, Dynamic Vignette, Ash Embers & Distortion)
    val activeEffectManager = effectManager ?: androidx.compose.runtime.remember { PostProcessEffectManager() }
    val entityDist = hypot(entityX - playerX, entityY - playerY)
    HorrorPostProcessOverlay(
      effectManager = activeEffectManager,
      prana = prana,
      diyaLit = diyaLit,
      lightRadiusNormalized = torchLightRadiusNormalized,
      chantingMantra = chantingMantra,
      entityDistance = entityDist,
      animTick = animTick
    )

    // Top-Left Fog-of-War Mini Yantra Radar Map Overlay
    FogOfWarMiniMapOverlay(
      mapGrid = mapGrid,
      fogOfWarManager = activeFogManager,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      diyaLit = diyaLit,
      entityX = entityX,
      entityY = entityY,
      litDiyaCoords = litDiyaCoords,
      solvedSealCoords = solvedSealCoords,
      dialogueEncounters = dialogueEncounters,
      shadowCasterSystem = activeShadowSystem,
      ritualManager = ritualManager,
      coOpPlayers = coOpPlayers,
      currentUserId = currentUserId,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(12.dp)
    )
  }
}

private enum class SpriteType {
  DIYA_UNLIT,
  DIYA_LIT,
  SEAL_LOCKED,
  SEAL_SOLVED,
  SECRET_DOOR_SEALED,
  SECRET_CHAMBER_PORTAL,
  SECRET_CHAMBER_SANCTUARY,
  NPC_SPIRIT,
  ANCIENT_INSCRIPTION,
  TEAMMATE,
  CREATURE
}

private data class BillboardSprite(
  val x: Double,
  val y: Double,
  val type: SpriteType,
  val label: String = "",
  val isRitualNextTarget: Boolean = false
)

private fun DrawScope.drawSpriteBillboard(
  sp: BillboardSprite,
  screenX: Float,
  centerY: Float,
  spriteSize: Float,
  dist: Float,
  animTick: Float
) {
  val fade = (1f - (dist / 11f)).coerceIn(0.15f, 1f)
  when (sp.type) {
    SpriteType.DIYA_LIT -> {
      val radius = spriteSize * 0.22f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFFEA00).copy(alpha = 0.85f * fade),
            Color(0xFFFF6D00).copy(alpha = 0.35f * fade),
            Color.Transparent
          ),
          center = Offset(screenX, centerY + spriteSize * 0.1f),
          radius = radius * 2.4f
        ),
        radius = radius * 2.4f,
        center = Offset(screenX, centerY + spriteSize * 0.1f)
      )
      // Brass pedestal
      drawRoundRect(
        color = Color(0xFFD4AF37).copy(alpha = fade),
        topLeft = Offset(screenX - radius * 0.6f, centerY + spriteSize * 0.14f),
        size = Size(radius * 1.2f, radius * 0.45f),
        cornerRadius = CornerRadius(6f, 6f)
      )
      // Flame
      drawCircle(
        color = Color(0xFFFFF59D),
        radius = radius * (0.35f + 0.06f * sin(animTick * 14f)),
        center = Offset(screenX, centerY + spriteSize * 0.06f)
      )
    }

    SpriteType.DIYA_UNLIT -> {
      val radius = spriteSize * 0.18f
      drawRoundRect(
        color = Color(0xFF795548).copy(alpha = fade),
        topLeft = Offset(screenX - radius * 0.6f, centerY + spriteSize * 0.14f),
        size = Size(radius * 1.2f, radius * 0.45f),
        cornerRadius = CornerRadius(6f, 6f)
      )
      drawCircle(
        color = if (sp.isRitualNextTarget) Color(0xFF00E676).copy(alpha = 0.85f * fade)
        else Color(0xFFFF8F00).copy(alpha = 0.35f * fade),
        radius = if (sp.isRitualNextTarget) radius * (0.95f + 0.18f * sin(animTick * 8f)) else radius * 0.7f,
        center = Offset(screenX, centerY + spriteSize * 0.16f),
        style = Stroke(width = if (sp.isRitualNextTarget) 3.5f else 2.5f)
      )
    }

    SpriteType.SEAL_LOCKED, SpriteType.SEAL_SOLVED -> {
      val ringRadius = spriteSize * 0.28f
      val sealColor = if (sp.type == SpriteType.SEAL_SOLVED) {
        Color(0xFF00E676).copy(alpha = fade)
      } else if (sp.isRitualNextTarget) {
        Color(0xFFFFEA00).copy(alpha = fade)
      } else {
        Color(0xFF00E5FF).copy(alpha = fade)
      }
      drawCircle(
        color = sealColor,
        radius = ringRadius,
        center = Offset(screenX, centerY),
        style = Stroke(width = 4f)
      )
      drawCircle(
        color = sealColor.copy(alpha = 0.45f * fade),
        radius = ringRadius * 0.65f,
        center = Offset(screenX, centerY),
        style = Stroke(width = 2.5f)
      )
    }

    SpriteType.SECRET_DOOR_SEALED -> {
      val archW = spriteSize * 0.36f
      val archH = spriteSize * 0.54f
      drawRoundRect(
        color = Color(0xFFFF8F00).copy(alpha = 0.48f * fade),
        topLeft = Offset(screenX - archW / 2f, centerY - archH / 2f),
        size = Size(archW, archH),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 3f)
      )
    }

    SpriteType.SECRET_CHAMBER_PORTAL -> {
      val archW = spriteSize * 0.42f
      val archH = spriteSize * 0.68f
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF00E676).copy(alpha = 0.78f * fade),
            Color(0xFF00BFA5).copy(alpha = 0.42f * fade),
            Color(0xFF004D40).copy(alpha = 0.25f * fade)
          )
        ),
        topLeft = Offset(screenX - archW / 2f, centerY - archH / 2f),
        size = Size(archW, archH),
        cornerRadius = CornerRadius(16f, 16f)
      )
      drawRoundRect(
        color = Color(0xFFFFD54F).copy(alpha = 0.9f * fade),
        topLeft = Offset(screenX - archW / 2f, centerY - archH / 2f),
        size = Size(archW, archH),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 3.5f)
      )
    }

    SpriteType.SECRET_CHAMBER_SANCTUARY -> {
      val hover = sin(animTick * 5f) * (spriteSize * 0.03f)
      val r = spriteSize * 0.28f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFFEA00).copy(alpha = 0.90f * fade),
            Color(0xFF00E676).copy(alpha = 0.45f * fade),
            Color.Transparent
          ),
          center = Offset(screenX, centerY + hover),
          radius = r * 2.3f
        ),
        radius = r * 2.3f,
        center = Offset(screenX, centerY + hover)
      )
      drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = fade),
        radius = r * 0.55f,
        center = Offset(screenX, centerY + hover)
      )
    }

    SpriteType.TEAMMATE -> {
      val r = spriteSize * 0.25f
      drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.75f * fade),
        radius = r,
        center = Offset(screenX, centerY)
      )
      drawCircle(
        color = Color(0xFFFFCA28).copy(alpha = fade),
        radius = r * 0.45f,
        center = Offset(screenX, centerY - r * 0.3f)
      )
    }

    SpriteType.NPC_SPIRIT -> {
      val hover = sin(animTick * 4.5f) * (spriteSize * 0.04f)
      val r = spriteSize * 0.26f
      val cy = centerY + hover
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF80DEEA).copy(alpha = 0.75f * fade),
            Color(0xFF00BCD4).copy(alpha = 0.28f * fade),
            Color.Transparent
          ),
          center = Offset(screenX, cy),
          radius = r * 2.1f
        ),
        radius = r * 2.1f,
        center = Offset(screenX, cy)
      )
      drawCircle(
        color = Color(0xFFE0F7FA).copy(alpha = 0.88f * fade),
        radius = r * 0.55f,
        center = Offset(screenX, cy - r * 0.25f)
      )
      drawRoundRect(
        color = Color(0xFF26C6DA).copy(alpha = 0.72f * fade),
        topLeft = Offset(screenX - r * 0.45f, cy - r * 0.05f),
        size = Size(r * 0.9f, r * 1.1f),
        cornerRadius = CornerRadius(r * 0.4f, r * 0.4f)
      )
    }

    SpriteType.ANCIENT_INSCRIPTION -> {
      val slabW = spriteSize * 0.34f
      val slabH = spriteSize * 0.48f
      drawRoundRect(
        color = Color(0xFF4E342E).copy(alpha = fade),
        topLeft = Offset(screenX - slabW / 2f, centerY - slabH / 2f),
        size = Size(slabW, slabH),
        cornerRadius = CornerRadius(8f, 8f)
      )
      drawRoundRect(
        color = Color(0xFFFFCA28).copy(alpha = 0.75f * fade),
        topLeft = Offset(screenX - slabW / 2f, centerY - slabH / 2f),
        size = Size(slabW, slabH),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2.5f)
      )
      for (lineIdx in -1..1) {
        val ly = centerY + lineIdx * (slabH * 0.2f)
        drawLine(
          color = Color(0xFFFF8F00).copy(alpha = 0.85f * fade),
          start = Offset(screenX - slabW * 0.3f, ly),
          end = Offset(screenX + slabW * 0.3f, ly),
          strokeWidth = 2.2f
        )
      }
    }

    SpriteType.CREATURE -> {
      // Towering Brahmarakshasa / Chhaya Pret shadow entity with glowing crimson-amber eyes and horns
      val bob = sin(animTick * 6f) * (spriteSize * 0.03f)
      val bodyW = spriteSize * 0.55f
      val bodyH = spriteSize * 0.88f
      val cy = centerY + bob

      // Dark smoke aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF1A0000).copy(alpha = 0.92f * fade),
            Color(0xFFD50000).copy(alpha = 0.35f * fade),
            Color.Transparent
          ),
          center = Offset(screenX, cy),
          radius = bodyH * 0.65f
        ),
        radius = bodyH * 0.65f,
        center = Offset(screenX, cy)
      )

      // Torso silhouette
      drawRoundRect(
        color = Color(0xFF0D0303).copy(alpha = 0.96f * fade),
        topLeft = Offset(screenX - bodyW / 2f, cy - bodyH * 0.38f),
        size = Size(bodyW, bodyH * 0.76f),
        cornerRadius = CornerRadius(bodyW * 0.3f, bodyW * 0.3f)
      )

      // Horns
      val hornPath = Path().apply {
        moveTo(screenX - bodyW * 0.22f, cy - bodyH * 0.35f)
        lineTo(screenX - bodyW * 0.42f, cy - bodyH * 0.56f)
        lineTo(screenX - bodyW * 0.10f, cy - bodyH * 0.40f)
        close()
        moveTo(screenX + bodyW * 0.22f, cy - bodyH * 0.35f)
        lineTo(screenX + bodyW * 0.42f, cy - bodyH * 0.56f)
        lineTo(screenX + bodyW * 0.10f, cy - bodyH * 0.40f)
        close()
      }
      drawPath(hornPath, color = Color(0xFF3E1C14).copy(alpha = fade))

      // Glowing eyes
      val eyeOffset = bodyW * 0.13f
      val eyeY = cy - bodyH * 0.24f
      val eyeRadius = (spriteSize * 0.035f).coerceAtLeast(3.5f)
      drawCircle(
        color = Color(0xFFFF3D00),
        radius = eyeRadius * 1.6f,
        center = Offset(screenX - eyeOffset, eyeY)
      )
      drawCircle(
        color = Color(0xFFFF3D00),
        radius = eyeRadius * 1.6f,
        center = Offset(screenX + eyeOffset, eyeY)
      )
      drawCircle(
        color = Color(0xFFFFEA00),
        radius = eyeRadius * 0.7f,
        center = Offset(screenX - eyeOffset, eyeY)
      )
      drawCircle(
        color = Color(0xFFFFEA00),
        radius = eyeRadius * 0.7f,
        center = Offset(screenX + eyeOffset, eyeY)
      )
    }
  }
}

private fun DrawScope.drawFirstPersonDiyaHand(
  w: Float,
  h: Float,
  diyaLit: Boolean,
  lightRadiusNormalized: Float,
  flickerMultiplier: Float,
  chantingMantra: Boolean,
  animTick: Float
) {
  val swayX = cos(animTick * 3.5f) * 10f
  val swayY = abs(sin(animTick * 3.5f)) * 8f
  val lampX = w * 0.78f + swayX
  val lampY = h * 0.84f + swayY

  if (chantingMantra) {
    // Golden Vedic Kavach ring around viewport
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFFCA28).copy(alpha = 0.28f),
          Color.Transparent
        ),
        center = Offset(w * 0.5f, h * 0.5f),
        radius = w * 0.55f
      ),
      radius = w * 0.55f,
      center = Offset(w * 0.5f, h * 0.5f)
    )
  }

  if (diyaLit) {
    val scale = (0.42f + 0.58f * lightRadiusNormalized.coerceIn(0.18f, 1.0f)) * flickerMultiplier
    val flameRadius = (82f * scale).coerceAtLeast(24f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFFF176).copy(alpha = (0.88f * scale).coerceIn(0.25f, 0.92f)),
          Color(0xFFFF8F00).copy(alpha = (0.48f * scale).coerceIn(0.15f, 0.55f)),
          Color.Transparent
        ),
        center = Offset(lampX, lampY - 28f),
        radius = flameRadius * 2.3f
      ),
      radius = flameRadius * 2.3f,
      center = Offset(lampX, lampY - 28f)
    )
    // Inner flame teardrop scaled by fuel & flicker
    drawCircle(
      color = Color(0xFFFFFDE7),
      radius = (17f * scale).coerceAtLeast(6f),
      center = Offset(lampX, lampY - 24f)
    )
  }

  // Brass Diya bowl in player's hand
  drawRoundRect(
    color = Color(0xFFB8860B),
    topLeft = Offset(lampX - 44f, lampY - 8f),
    size = Size(88f, 34f),
    cornerRadius = CornerRadius(18f, 18f)
  )
  drawRoundRect(
    color = Color(0xFFFFCA28),
    topLeft = Offset(lampX - 36f, lampY - 4f),
    size = Size(72f, 10f),
    cornerRadius = CornerRadius(8f, 8f)
  )
}

@Composable
private fun YantraMiniMap(
  mapGrid: List<String>,
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
  Box(
    modifier = modifier
      .size(116.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xDD120705))
      .padding(6.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val rows = mapGrid.size
      val cols = mapGrid.firstOrNull()?.length ?: 12
      val cellW = size.width / cols
      val cellH = size.height / rows

      // Render 2D top-down pillar & statue shadow wedges cast from the player's torch
      shadowCasterSystem.renderMiniMapShadowCones(
        drawScope = this,
        playerX = playerX,
        playerY = playerY,
        cols = cols,
        rows = rows,
        diyaLit = diyaLit
      )

      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val ch = mapGrid[r][c]
          val topLeft = Offset(c * cellW, r * cellH)
          when (ch) {
            '1' -> drawRect(
              color = Color(0xFF3E2217),
              topLeft = topLeft,
              size = Size(cellW + 0.5f, cellH + 0.5f)
            )
            '2' -> {
              val lit = litDiyaCoords.contains(c to r)
              drawCircle(
                color = if (lit) Color(0xFFFFCA28) else Color(0xFF8D6E63),
                radius = cellW * 0.35f,
                center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
              )
            }
            '3' -> {
              val solved = solvedSealCoords.contains(c to r)
              drawCircle(
                color = if (solved) Color(0xFF00E676) else Color(0xFF00E5FF),
                radius = cellW * 0.38f,
                center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
              )
            }
            '4' -> drawRect(
              color = Color(0xFFFF8F00),
              topLeft = topLeft,
              size = Size(cellW, cellH)
            )
            '5' -> drawRect(
              color = Color(0xFF00E676),
              topLeft = topLeft,
              size = Size(cellW, cellH)
            )
            '6' -> drawCircle(
              color = Color(0xFFFFEA00),
              radius = cellW * 0.44f,
              center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
            )
          }
        }
      }

      // Highlight next required ritual target on the mini-map
      ritualManager?.nextRequiredStep?.targetCoord?.let { (tx, ty) ->
        drawCircle(
          color = Color(0xFF00E676),
          radius = cellW * 0.52f,
          center = Offset((tx + 0.5f) * cellW, (ty + 0.5f) * cellH),
          style = Stroke(width = 2f)
        )
      }

      // Co-op teammates
      for (mate in coOpPlayers) {
        if (mate.userId != currentUserId) {
          drawCircle(
            color = Color(0xFF00E5FF),
            radius = cellW * 0.42f,
            center = Offset(mate.x.toFloat() * cellW, mate.y.toFloat() * cellH)
          )
        }
      }

      // NPC Spirits and Ancient Inscriptions on Mini-Map
      for (enc in dialogueEncounters) {
        drawCircle(
          color = if (enc.speakerType == DialogueSpeakerType.NPC_SPIRIT) Color(0xFF80DEEA) else Color(0xFFFFD54F),
          radius = cellW * 0.34f,
          center = Offset((enc.tileX + 0.5f) * cellW, (enc.tileY + 0.5f) * cellH)
        )
      }

      // Creature radar pulse
      drawCircle(
        color = Color(0xFFFF1744),
        radius = cellW * 0.48f,
        center = Offset(entityX.toFloat() * cellW, entityY.toFloat() * cellH)
      )

      // Local player dot + facing cone
      val px = playerX.toFloat() * cellW
      val py = playerY.toFloat() * cellH
      drawLine(
        color = Color(0xFFFFEA00),
        start = Offset(px, py),
        end = Offset(
          px + cos(playerAngle).toFloat() * cellW * 1.6f,
          py + sin(playerAngle).toFloat() * cellH * 1.6f
        ),
        strokeWidth = 2.5f
      )
      drawCircle(
        color = Color(0xFFFFFFFF),
        radius = cellW * 0.42f,
        center = Offset(px, py)
      )
    }
  }
}
