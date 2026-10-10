package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GarbhagrihaColorScheme = darkColorScheme(
  primary = SaffronFlame,
  onPrimary = Color(0xFF1A0900),
  primaryContainer = Color(0xFF4B2406),
  onPrimaryContainer = MoltenBrass,
  secondary = MoltenBrass,
  onSecondary = Color(0xFF1F1100),
  secondaryContainer = Color(0xFF3A220D),
  onSecondaryContainer = ParchmentCream,
  tertiary = SindoorCrimson,
  onTertiary = Color.White,
  background = ObsidianVoid,
  onBackground = ParchmentCream,
  surface = SanctumGranite,
  onSurface = ParchmentCream,
  surfaceVariant = TempleStoneSurface,
  onSurfaceVariant = SandstoneMuted,
  error = BloodAltarRed,
  onError = Color.Black
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = GarbhagrihaColorScheme,
    typography = Typography,
    content = content
  )
}
