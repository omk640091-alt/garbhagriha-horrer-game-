package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Preset horror post-processing visual profiles for Garbhagriha.
 */
enum class PostProcessPreset(
  val label: String,
  val baseGrainDensity: Float,
  val baseVignetteStrength: Float,
  val scanlineIntensity: Float,
  val emberParticlesEnabled: Boolean
) {
  SUBTLE_SANCTUM(
    label = "Sanctum Grit",
    baseGrainDensity = 0.25f,
    baseVignetteStrength = 0.22f,
    scanlineIntensity = 0.08f,
    emberParticlesEnabled = true
  ),
  CURSED_VHS(
    label = "Cursed Prana Film",
    baseGrainDensity = 0.50f,
    baseVignetteStrength = 0.36f,
    scanlineIntensity = 0.20f,
    emberParticlesEnabled = true
  ),
  ABYSSAL_DARKNESS(
    label = "Abyssal Eclipse",
    baseGrainDensity = 0.35f,
    baseVignetteStrength = 0.52f,
    scanlineIntensity = 0.12f,
    emberParticlesEnabled = false
  )
}

/**
 * Manages real-time post-processing visual parameters (film grain, multi-layer vignette,
 * low-Prana chromatic distortion bands, and floating sacred ash/ember motes) that react
 * to gameplay telemetry (entity proximity, Prana vitality, Diya lamp state, and Vedic Kavach).
 */
class PostProcessEffectManager(
  initialPreset: PostProcessPreset = PostProcessPreset.SUBTLE_SANCTUM
) {
  var currentPreset by mutableStateOf(initialPreset)
    private set

  var isEnabled by mutableStateOf(true)
    private set

  var computedVignetteRadiusRatio by mutableFloatStateOf(0.82f)
    private set

  var computedCrimsonPulseAlpha by mutableFloatStateOf(0f)
    private set

  var computedShadowVignetteAlpha by mutableFloatStateOf(0.25f)
    private set

  var computedGrainIntensity by mutableFloatStateOf(0.25f)
    private set

  var computedDistortionGlitch by mutableFloatStateOf(0f)
    private set

  fun cyclePreset(): PostProcessPreset {
    val values = PostProcessPreset.entries
    val nextIndex = (currentPreset.ordinal + 1) % values.size
    currentPreset = values[nextIndex]
    return currentPreset
  }

  fun setEffectEnabled(enabled: Boolean) {
    isEnabled = enabled
  }

  /**
   * Updates post-processing shader/canvas weights based on live player, creature, and torch light radius telemetry.
   */
  fun updateFromTelemetry(
    prana: Int,
    diyaLit: Boolean,
    lightRadiusNormalized: Float = if (diyaLit) 1.0f else 0.22f,
    chantingMantra: Boolean,
    entityDistance: Double,
    animTick: Float
  ) {
    val preset = currentPreset
    val proximityThreat = ((5.2f - entityDistance.toFloat()) / 5.2f).coerceIn(0f, 1f)
    val lowPranaStress = ((50f - prana.toFloat()) / 50f).coerceIn(0f, 1f)
    // Dynamic darkness penalty scales continuously as torch fuel & light radius shrink
    val darknessPenalty = ((1.0f - lightRadiusNormalized.coerceIn(0.18f, 1.0f)) * 0.24f)

    val heartbeatWave = 0.78f + 0.22f * sin(animTick * 11.5f)

    computedVignetteRadiusRatio = (0.88f - proximityThreat * 0.18f - darknessPenalty * 0.35f)
      .coerceIn(0.45f, 0.90f)

    computedShadowVignetteAlpha = (preset.baseVignetteStrength + darknessPenalty + proximityThreat * 0.18f)
      .coerceIn(0.15f, 0.72f)

    computedCrimsonPulseAlpha = if (chantingMantra) {
      0f
    } else {
      ((proximityThreat * 0.58f + lowPranaStress * 0.35f) * heartbeatWave).coerceIn(0f, 0.78f)
    }

    computedGrainIntensity = (preset.baseGrainDensity + proximityThreat * 0.35f + lowPranaStress * 0.25f + darknessPenalty * 0.2f)
      .coerceIn(0.15f, 1.0f)

    computedDistortionGlitch = if (proximityThreat > 0.45f || prana < 30) {
      (proximityThreat * 0.6f + lowPranaStress * 0.4f).coerceIn(0f, 1f)
    } else {
      0f
    }
  }

  /**
   * Renders the multi-stage horror post-processing pipeline directly onto a Compose [DrawScope].
   */
  fun renderPostProcessOverlay(
    drawScope: DrawScope,
    animTick: Float,
    diyaLit: Boolean,
    chantingMantra: Boolean
  ) = with(drawScope) {
    if (!isEnabled) return

    val w = size.width
    val h = size.height
    val maxDim = maxOf(w, h)
    val center = Offset(w * 0.5f, h * 0.5f)

    // 1. Subtle Obsidian Edge Vignette (keeps center 3D viewport and characters bright and clear)
    drawRect(
      brush = Brush.radialGradient(
        colorStops = arrayOf(
          0.0f to Color.Transparent,
          (computedVignetteRadiusRatio * 0.78f).coerceIn(0.35f, 0.85f) to Color.Transparent,
          1.0f to Color(0xFF040100).copy(alpha = computedShadowVignetteAlpha)
        ),
        center = center,
        radius = maxDim * computedVignetteRadiusRatio
      ),
      size = size
    )

    // 2. Crimson Blood-Prana & Entity Proximity Vignette Pulse OR Golden Vedic Kavach Halo
    if (chantingMantra) {
      val mantraPulse = 0.75f + 0.25f * sin(animTick * 8f)
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.Transparent,
            Color(0xFFFFCA28).copy(alpha = 0.26f * mantraPulse),
            Color(0xFFFF8F00).copy(alpha = 0.38f * mantraPulse)
          ),
          center = center,
          radius = maxDim * 0.68f
        ),
        size = size
      )
    } else if (computedCrimsonPulseAlpha > 0.03f) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.Transparent,
            Color(0xFF8E0000).copy(alpha = computedCrimsonPulseAlpha * 0.55f),
            Color(0xFFD50000).copy(alpha = computedCrimsonPulseAlpha)
          ),
          center = center,
          radius = maxDim * 0.62f
        ),
        size = size
      )
    }

    // 3. Deterministic Fast Pseudo-Random Film Grain Specks (Light & Shadow Grit)
    val grainCount = (110 * computedGrainIntensity).toInt().coerceIn(24, 130)
    val seedBase = (animTick * 37f).toInt()
    for (i in 0 until grainCount) {
      val h1 = hashInt(seedBase + i * 7919)
      val h2 = hashInt(seedBase + i * 104729 + 31)
      val gx = (abs(h1 % 1000) / 1000f) * w
      val gy = (abs(h2 % 1000) / 1000f) * h
      val isLightSpeck = (h1 and 1) == 0
      val speckAlpha = (0.04f + (abs(h2 % 100) / 1000f) * computedGrainIntensity).coerceIn(0.02f, 0.16f)
      val speckColor = if (isLightSpeck) {
        Color(0xFFFDF3E7).copy(alpha = speckAlpha)
      } else {
        Color(0xFF090302).copy(alpha = speckAlpha * 1.4f)
      }
      val speckRadius = 1.4f + (abs(h1 % 3)) * 0.9f
      drawCircle(
        color = speckColor,
        radius = speckRadius,
        center = Offset(gx, gy)
      )
    }

    // 4. Floating Temple Vibhuti (Sacred Ash) & Diya Ember Motes
    if (currentPreset.emberParticlesEnabled) {
      val emberCount = if (diyaLit) 18 else 9
      for (e in 0 until emberCount) {
        val phase = e * 1.73f
        val ex = ((0.1f + (e * 0.053f) % 0.85f) + 0.04f * sin(animTick * 0.9f + phase)) * w
        val rawY = ((1.0f - ((animTick * 0.06f + e * 0.11f) % 1.0f))) * h
        val emberColor = if (diyaLit && e % 2 == 0) {
          Color(0xFFFFAB00).copy(alpha = 0.24f)
        } else {
          Color(0xFFD7B899).copy(alpha = 0.16f)
        }
        drawCircle(
          color = emberColor,
          radius = if (diyaLit && e % 2 == 0) 2.8f else 2.1f,
          center = Offset(ex, rawY)
        )
      }
    }

    // 5. Subtle Atmospheric Scanlines & Creature Proximity Distortion Tear Lines
    val scanAlpha = currentPreset.scanlineIntensity * 0.14f
    if (scanAlpha > 0.01f) {
      val lineStep = 14f
      var y = (animTick * 12f) % lineStep
      while (y < h) {
        drawLine(
          color = Color.Black.copy(alpha = scanAlpha),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.5f
        )
        y += lineStep
      }
    }

    if (computedDistortionGlitch > 0.05f) {
      val sliceCount = (computedDistortionGlitch * 5f).toInt().coerceIn(1, 5)
      for (s in 0 until sliceCount) {
        val sliceHash = hashInt(seedBase + s * 4999)
        val sy = (abs(sliceHash % 1000) / 1000f) * h
        val sh = 3f + (abs(sliceHash % 12))
        // Chromatic crimson/cyan spiritual interference bands when Brahmarakshasa is near
        drawRect(
          color = Color(0xFFFF1744).copy(alpha = 0.14f * computedDistortionGlitch),
          topLeft = Offset(0f, sy),
          size = Size(w, sh)
        )
        drawRect(
          color = Color(0xFF00E5FF).copy(alpha = 0.09f * computedDistortionGlitch),
          topLeft = Offset(0f, (sy + 4f).coerceAtMost(h)),
          size = Size(w, sh * 0.6f)
        )
      }
    }
  }

  private fun hashInt(x: Int): Int {
    var v = x
    v = ((v ushr 16) xor v) * 0x45d9f3b
    v = ((v ushr 16) xor v) * 0x45d9f3b
    v = (v ushr 16) xor v
    return v
  }
}

/**
 * Standalone Compose Canvas overlay component backed by [PostProcessEffectManager].
 * Layer this over the 3D viewport to render real-time screen grain, vignette,
 * floating temple ash motes, and entity proximity chromatic distortion.
 */
@Composable
fun HorrorPostProcessOverlay(
  effectManager: PostProcessEffectManager,
  prana: Int,
  diyaLit: Boolean,
  lightRadiusNormalized: Float = if (diyaLit) 1.0f else 0.22f,
  chantingMantra: Boolean,
  entityDistance: Double,
  animTick: Float,
  modifier: Modifier = Modifier
) {
  effectManager.updateFromTelemetry(
    prana = prana,
    diyaLit = diyaLit,
    lightRadiusNormalized = lightRadiusNormalized,
    chantingMantra = chantingMantra,
    entityDistance = entityDistance,
    animTick = animTick
  )

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("horror_post_process_canvas")
  ) {
    effectManager.renderPostProcessOverlay(
      drawScope = this,
      animTick = animTick,
      diyaLit = diyaLit,
      chantingMantra = chantingMantra
    )
  }
}
