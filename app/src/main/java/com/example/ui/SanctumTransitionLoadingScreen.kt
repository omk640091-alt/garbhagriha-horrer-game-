package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.TempleSoundEngine
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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Immersive Sanatani Hindu horror transition screen between the Main Sanctum Hub
 * and the 3D First-Person Temple Exploration scene.
 *
 * Features:
 * - Counter-rotating sacred Sri Yantra / Surya Mandala geometric rings
 * - Sequential ignition of 5 Ashtadhatu Brass Diya flames around the mandala
 * - Pulsating golden-saffron Diya flame & Trishul silhouette in the center
 * - Phased Vedic ritual loading chant & survival warning for the selected temple's guardian entity
 */
@Composable
fun SanctumTransitionLoadingScreen(
  temple: TempleInfo,
  roomCode: String?,
  soundEngine: TempleSoundEngine,
  onTransitionComplete: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "yantra_mandala_transition")

  val outerRingAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "outer_ring_rotation"
  )

  val innerRingAngle by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "inner_ring_rotation"
  )

  val flamePulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.18f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "diya_flame_pulse"
  )

  var progress by remember { mutableFloatStateOf(0.05f) }
  var activeDiyaCount by remember { mutableIntStateOf(1) }
  var ritualStageIndex by remember { mutableIntStateOf(0) }

  val ritualStages = remember(temple.name, temple.entityName) {
    listOf(
      "ॐ भूर्भुवः स्वः • Invoking Sacred Kavach...",
      "Unsealing interlocking granite doors of ${temple.name}...",
      "Igniting Ashtadhatu Brass Diyas against the dark...",
      "Warning: ${temple.entityName} stirs in the Garbhagriha..."
    )
  }

  LaunchedEffect(temple.id) {
    soundEngine.playMantraChantPulse(950)
    val totalSteps = 26
    for (step in 1..totalSteps) {
      delay(95L)
      progress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
      val newDiyas = ((progress * 5f).toInt() + 1).coerceIn(1, 5)
      if (newDiyas > activeDiyaCount) {
        activeDiyaCount = newDiyas
        soundEngine.playDiyaIgnite()
      }
      ritualStageIndex = ((progress * ritualStages.size).toInt())
        .coerceIn(0, ritualStages.lastIndex)
    }
    soundEngine.playTempleBell(528.0, durationMs = 700)
    delay(220L)
    onTransitionComplete()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(
            SanctumGranite,
            ObsidianVoid,
            Color(0xFF040100)
          ),
          radius = 1100f
        )
      )
      .testTag("sanctum_loading_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Sanskrit Invocation & Co-Op Badge
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          color = SacredCardSurface.copy(alpha = 0.9f),
          shape = RoundedCornerShape(50),
          modifier = Modifier.border(1.dp, MoltenBrass.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
          Text(
            text = if (roomCode != null) "CO-OP RITUAL LINK • ROOM $roomCode"
            else "SANCTUM DESCENT • ${temple.dynastyAndEra.uppercase()}",
            style = MaterialTheme.typography.labelLarge,
            color = if (roomCode != null) SacredTeal else MoltenBrass,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = temple.name.uppercase(),
          style = MaterialTheme.typography.headlineMedium,
          color = MoltenBrass,
          textAlign = TextAlign.Center
        )

        Text(
          text = "${temple.location} • ${temple.difficultyLabel}",
          style = MaterialTheme.typography.bodyMedium,
          color = SaffronFlame,
          textAlign = TextAlign.Center
        )
      }

      // Center Animated Sacred Yantra Mandala & 5 Orbiting Brass Diyas
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(290.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val maxR = size.minDimension * 0.44f

          // Ambient golden-crimson aura behind the Yantra
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                SaffronFlame.copy(alpha = 0.28f * flamePulse),
                BloodAltarRed.copy(alpha = 0.12f),
                Color.Transparent
              ),
              center = Offset(cx, cy),
              radius = maxR * 1.25f
            ),
            radius = maxR * 1.25f,
            center = Offset(cx, cy)
          )

          // Outer Rotating Navagraha Lotus Ring
          rotate(degrees = outerRingAngle, pivot = Offset(cx, cy)) {
            drawCircle(
              color = MoltenBrass.copy(alpha = 0.65f),
              radius = maxR,
              center = Offset(cx, cy),
              style = Stroke(width = 3.5f)
            )
            drawCircle(
              color = SaffronFlame.copy(alpha = 0.35f),
              radius = maxR * 0.91f,
              center = Offset(cx, cy),
              style = Stroke(width = 2f)
            )

            // 12 Solar Lotus Spokes
            for (i in 0 until 12) {
              val angleRad = (i * 30.0) * PI / 180.0
              val sx = cx + cos(angleRad).toFloat() * (maxR * 0.82f)
              val sy = cy + sin(angleRad).toFloat() * (maxR * 0.82f)
              val ex = cx + cos(angleRad).toFloat() * maxR
              val ey = cy + sin(angleRad).toFloat() * maxR
              drawLine(
                color = MoltenBrass.copy(alpha = 0.7f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 3f,
                cap = StrokeCap.Round
              )
            }
          }

          // Inner Counter-Rotating Sri Yantra Interlocking Triangles (Shatkona)
          rotate(degrees = innerRingAngle, pivot = Offset(cx, cy)) {
            val triR = maxR * 0.68f
            val upwardTriangle = Path().apply {
              for (i in 0 until 3) {
                val a = (-90.0 + i * 120.0) * PI / 180.0
                val px = cx + cos(a).toFloat() * triR
                val py = cy + sin(a).toFloat() * triR
                if (i == 0) moveTo(px, py) else lineTo(px, py)
              }
              close()
            }
            val downwardTriangle = Path().apply {
              for (i in 0 until 3) {
                val a = (90.0 + i * 120.0) * PI / 180.0
                val px = cx + cos(a).toFloat() * triR
                val py = cy + sin(a).toFloat() * triR
                if (i == 0) moveTo(px, py) else lineTo(px, py)
              }
              close()
            }

            drawPath(
              path = upwardTriangle,
              color = SaffronFlame.copy(alpha = 0.8f),
              style = Stroke(width = 3.5f)
            )
            drawPath(
              path = downwardTriangle,
              color = SacredTeal.copy(alpha = 0.65f),
              style = Stroke(width = 3f)
            )
          }

          // 5 Sacred Diyas around the outer perimeter that ignite sequentially with progress
          for (idx in 0 until 5) {
            val diyaAngle = (-90.0 + idx * 72.0) * PI / 180.0
            val dx = cx + cos(diyaAngle).toFloat() * (maxR * 1.08f)
            val dy = cy + sin(diyaAngle).toFloat() * (maxR * 1.08f)
            val isLit = idx < activeDiyaCount

            drawCircle(
              color = if (isLit) MoltenBrass else Color(0xFF4E342E),
              radius = 12f,
              center = Offset(dx, dy)
            )
            if (isLit) {
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(
                    Color(0xFFFFF59D),
                    SaffronFlame.copy(alpha = 0.7f),
                    Color.Transparent
                  ),
                  center = Offset(dx, dy - 4f),
                  radius = 26f * flamePulse
                ),
                radius = 26f * flamePulse,
                center = Offset(dx, dy - 4f)
              )
            }
          }

          // Center Sacred Brass Diya Bowl & Pulsating Agni Flame
          drawRoundRect(
            color = Color(0xFFB8860B),
            topLeft = Offset(cx - 28f, cy + 12f),
            size = Size(56f, 18f),
            cornerRadius = CornerRadius(10f, 10f)
          )
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFFFFFDE7),
                MoltenBrass,
                SaffronFlame.copy(alpha = 0.4f),
                Color.Transparent
              ),
              center = Offset(cx, cy - 8f),
              radius = 38f * flamePulse
            ),
            radius = 38f * flamePulse,
            center = Offset(cx, cy - 8f)
          )
        }
      }

      // Bottom Ritual Progress Bar & Guardian Lore Warning Card
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = ritualStages[ritualStageIndex],
          style = MaterialTheme.typography.titleMedium,
          color = ParchmentCream,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
          progress = { progress },
          color = SaffronFlame,
          trackColor = SacredCardSurface,
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Ashtadhatu Diyas Ignited: $activeDiyaCount / 5",
            style = MaterialTheme.typography.labelLarge,
            color = MoltenBrass
          )
          Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MantraJade,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          color = SacredCardSurface.copy(alpha = 0.9f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BloodAltarRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "👁 GUARDIAN ENTITY: ${temple.entityName.uppercase()} (${temple.entityTitle})",
              style = MaterialTheme.typography.labelLarge,
              color = BloodAltarRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tip: Keep your brass Diya oil replenished at wall sconces. If you hear 3D footsteps or guttural growls in either ear, strike the 528Hz Ghanta bell or chant the Vedic Kavach.",
              style = MaterialTheme.typography.bodyMedium,
              color = SandstoneMuted
            )
          }
        }
      }
    }
  }
}
