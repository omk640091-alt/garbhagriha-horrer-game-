package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Categories of ancient Sanatani temple puzzles managed by [PuzzleManager]:
 * 1. [PRESSURE_PLATE_SEQUENCE]: Pancha-Bhoota (5 Elements) granite floor pressure plates stepped on in order.
 * 2. [LEVER_ARRANGEMENT]: Dwarpala Stambha stone levers coupled with mechanical gear linkages.
 * 3. [PATTERN_MATCHING]: Navagraha Yantra 3x3 sacred sigil grid pattern matching.
 * 4. [SURYA_CHAKRA_RINGS]: Concentric solar wheel alignment to 0° North.
 */
enum class AncientPuzzleType(
  val title: String,
  val sanskritSubtitle: String,
  val instructionText: String
) {
  PRESSURE_PLATE_SEQUENCE(
    title = "PANCHA-BHOOTA PRESSURE PLATES",
    sanskritSubtitle = "पञ्चभूत शिला क्रम • Elemental Stone Sequence",
    instructionText = "Depress the 4 elemental granite floor plates in the Vedic creation sequence: पृथ्वी (Earth) → जल (Water) → अग्नि (Fire) → वायु (Air)."
  ),
  LEVER_ARRANGEMENT(
    title = "DWARPALA STAMBHA LEVERS",
    sanskritSubtitle = "द्वारपाल स्तम्भ यन्त्र • Coupled Pillar Levers",
    instructionText = "Pull the ancient bronze pillar levers so all 4 temple lock-bolts rise into the UP (उर्ध्व) position. Pulling a lever also shifts its linked neighbor!"
  ),
  PATTERN_MATCHING(
    title = "NAVAGRAHA YANTRA PATTERN",
    sanskritSubtitle = "नवग्रह मण्डल • Sacred Sigil Pattern Match",
    instructionText = "Illuminate the 3x3 copper Yantra tiles to match the sacred Diamond-Bindu Mandala (Top, Left, Center, Right, Bottom)."
  ),
  SURYA_CHAKRA_RINGS(
    title = "SURYA CHAKRA YANTRA",
    sanskritSubtitle = "सूर्य चक्र संरेखण • Solar Wheel Alignment",
    instructionText = "Rotate the Outer, Middle, and Inner concentric stone rings so all markers align at 0° North."
  )
}

/**
 * Represents a single granite pressure plate in a [AncientPuzzleType.PRESSURE_PLATE_SEQUENCE] puzzle.
 */
data class PressurePlateTile(
  val id: Int,
  val sanskritGlyph: String,
  val elementName: String,
  val resonanceHz: Double
)

/**
 * Stateful manager for all ancient temple puzzles in Garbhagriha:
 * - Tracks pressure plate sequences (`PRESSURE_PLATE_SEQUENCE`)
 * - Tracks coupled mechanical lever arrangements (`LEVER_ARRANGEMENT`)
 * - Tracks 3x3 Yantra sigil pattern matching (`PATTERN_MATCHING`)
 * - Tracks concentric Surya Chakra ring rotations (`SURYA_CHAKRA_RINGS`)
 * - Tracks solved puzzle coordinates and total puzzles completed across the expedition
 */
class PuzzleManager {

  var activePuzzleType by mutableStateOf(AncientPuzzleType.PRESSURE_PLATE_SEQUENCE)
    private set

  var isCurrentPuzzleSolved by mutableStateOf(false)
    private set

  var statusMessage by mutableStateOf("")
    private set

  var totalPuzzlesSolved by mutableIntStateOf(0)
    private set

  // --- 1. Pressure Plate Sequence State (Pancha-Bhoota) ---
  val pressurePlates: List<PressurePlateTile> = listOf(
    PressurePlateTile(0, "पृथ्वी", "Prithvi (Earth)", 432.0),
    PressurePlateTile(1, "जल", "Jala (Water)", 528.0),
    PressurePlateTile(2, "अग्नि", "Agni (Fire)", 639.0),
    PressurePlateTile(3, "वायु", "Vayu (Air)", 741.0)
  )
  // Target order: Earth (0) -> Water (1) -> Fire (2) -> Air (3)
  val targetPlateSequence: List<Int> = listOf(0, 1, 2, 3)
  val pressedPlateSequence = mutableStateListOf<Int>()

  // --- 2. Lever Arrangement State (4 Coupled Bronze Levers, goal = all true/UP) ---
  val leverStates = mutableStateListOf(false, true, false, true)

  // --- 3. Pattern Matching State (3x3 Navagraha Yantra Grid, goal = Diamond + Center) ---
  // Target indices: 1 (Top), 3 (Mid-Left), 4 (Center Bindu), 5 (Mid-Right), 7 (Bottom)
  val targetPatternGrid: List<Boolean> = listOf(
    false, true, false,
    true, true, true,
    false, true, false
  )
  val currentPatternGrid = mutableStateListOf(
    false, true, false,
    false, false, true,
    false, true, false
  )

  // --- 4. Surya Chakra Concentric Rings State (goal = 0, 0, 0) ---
  var outerRingDeg by mutableIntStateOf(180)
    private set
  var middleRingDeg by mutableIntStateOf(90)
    private set
  var innerRingDeg by mutableIntStateOf(270)
    private set

  init {
    loadPuzzleByIndex(0)
  }

  /**
   * Loads and initializes a specific puzzle archetype based on the seal index or explicit type.
   */
  fun loadPuzzleByIndex(sealIndex: Int) {
    val types = AncientPuzzleType.entries
    val selected = types[sealIndex.mod(types.size)]
    selectPuzzleType(selected)
  }

  fun selectPuzzleType(type: AncientPuzzleType) {
    activePuzzleType = type
    isCurrentPuzzleSolved = false
    when (type) {
      AncientPuzzleType.PRESSURE_PLATE_SEQUENCE -> {
        pressedPlateSequence.clear()
        statusMessage = "Step on plates in order: Prithvi → Jala → Agni → Vayu"
      }
      AncientPuzzleType.LEVER_ARRANGEMENT -> {
        leverStates.clear()
        // Initial state where pulling lever 0 and lever 2 solves it in 2 moves
        leverStates.addAll(listOf(false, false, false, false))
        statusMessage = "Arrange all 4 Stambha Levers into the UP (उर्ध्व) lock position."
      }
      AncientPuzzleType.PATTERN_MATCHING -> {
        currentPatternGrid.clear()
        currentPatternGrid.addAll(
          listOf(
            false, true, false,
            false, false, true,
            false, true, false
          )
        )
        statusMessage = "Match the 5-point Diamond Bindu Yantra pattern (Top, Left, Center, Right, Bottom)."
      }
      AncientPuzzleType.SURYA_CHAKRA_RINGS -> {
        outerRingDeg = 180
        middleRingDeg = 90
        innerRingDeg = 270
        statusMessage = "Rotate Outer, Middle, and Inner solar rings to 0°."
      }
    }
  }

  /**
   * Handles stepping on a granite pressure plate in [AncientPuzzleType.PRESSURE_PLATE_SEQUENCE].
   * Returns `true` if the step was valid in the sequence, or `false` if it triggered a reset.
   */
  fun stepOnPressurePlate(plateId: Int): Boolean {
    if (activePuzzleType != AncientPuzzleType.PRESSURE_PLATE_SEQUENCE || isCurrentPuzzleSolved) {
      return isCurrentPuzzleSolved
    }

    val expectedId = targetPlateSequence.getOrNull(pressedPlateSequence.size)
    return if (plateId == expectedId) {
      pressedPlateSequence.add(plateId)
      if (pressedPlateSequence.size == targetPlateSequence.size) {
        isCurrentPuzzleSolved = true
        totalPuzzlesSolved++
        statusMessage = "Pancha-Bhoota Pressure Plates locked in harmony! Seal unlocked!"
      } else {
        val plate = pressurePlates.getOrNull(plateId)
        statusMessage = "${plate?.elementName ?: "Plate"} depressed (${pressedPlateSequence.size}/${targetPlateSequence.size})..."
      }
      true
    } else {
      pressedPlateSequence.clear()
      statusMessage = "Wrong stone plate! The floor mechanism reset—start with पृथ्वी Prithvi (Earth)!"
      false
    }
  }

  /**
   * Pulls a stone-and-bronze lever in [AncientPuzzleType.LEVER_ARRANGEMENT].
   * Each lever toggles itself and one coupled neighbor:
   * - Lever 0 toggles (0, 1)
   * - Lever 1 toggles (1, 2)
   * - Lever 2 toggles (2, 3)
   * - Lever 3 toggles (3, 0)
   */
  fun pullLever(leverIndex: Int): Boolean {
    if (leverIndex !in 0 until leverStates.size || isCurrentPuzzleSolved) {
      return isCurrentPuzzleSolved
    }

    val coupledNeighbor = (leverIndex + 1) % leverStates.size
    leverStates[leverIndex] = !leverStates[leverIndex]
    leverStates[coupledNeighbor] = !leverStates[coupledNeighbor]

    val upCount = leverStates.count { it }
    if (upCount == leverStates.size) {
      isCurrentPuzzleSolved = true
      totalPuzzlesSolved++
      statusMessage = "All 4 Dwarpala Stambha Levers locked UP! Ancient gate bolt released!"
      return true
    } else {
      statusMessage = "$upCount / ${leverStates.size} Levers raised to UP position."
      return false
    }
  }

  /**
   * Toggles a tile in the 3x3 Navagraha sigil matrix in [AncientPuzzleType.PATTERN_MATCHING].
   */
  fun togglePatternCell(cellIndex: Int): Boolean {
    if (cellIndex !in 0 until currentPatternGrid.size || isCurrentPuzzleSolved) {
      return isCurrentPuzzleSolved
    }

    currentPatternGrid[cellIndex] = !currentPatternGrid[cellIndex]

    val matchesTarget = currentPatternGrid.indices.all { idx ->
      currentPatternGrid[idx] == targetPatternGrid[idx]
    }
    if (matchesTarget) {
      isCurrentPuzzleSolved = true
      totalPuzzlesSolved++
      statusMessage = "Navagraha Mandala Pattern Matched! Cosmic Yantra resonates!"
      return true
    } else {
      val matchingCells = currentPatternGrid.indices.count { idx ->
        currentPatternGrid[idx] == targetPatternGrid[idx]
      }
      statusMessage = "Yantra Alignment: $matchingCells / 9 cells matching target Mandala."
      return false
    }
  }

  /**
   * Rotates one of the three concentric Surya Chakra rings (0 = Outer, 1 = Middle, 2 = Inner) by 90°.
   */
  fun rotateSuryaRing(ringIndex: Int): Boolean {
    if (isCurrentPuzzleSolved) return true
    when (ringIndex) {
      0 -> outerRingDeg = (outerRingDeg + 90) % 360
      1 -> middleRingDeg = (middleRingDeg + 90) % 360
      2 -> innerRingDeg = (innerRingDeg + 90) % 360
    }
    val aligned = (outerRingDeg % 360 == 0) && (middleRingDeg % 360 == 0) && (innerRingDeg % 360 == 0)
    if (aligned) {
      isCurrentPuzzleSolved = true
      totalPuzzlesSolved++
      statusMessage = "Surya Chakra Rings aligned at 0° Crown! Vedic lock unsealed!"
    } else {
      statusMessage = "Outer ${outerRingDeg}° • Middle ${middleRingDeg}° • Inner ${innerRingDeg}°"
    }
    return aligned
  }
}
