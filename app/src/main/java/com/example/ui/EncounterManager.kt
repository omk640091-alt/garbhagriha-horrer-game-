package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.audio.CreatureProximityZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Behavioral AI states for mythological entities (Brahmarakshasa, Chhaya Pret, Pishacha, Vetala)
 * stalking the Garbhagriha corridors.
 */
enum class CreatureBehaviorState(
  val badgeLabel: String,
  val sanskritState: String,
  val speedMultiplier: Double
) {
  DORMANT_PATROL(
    badgeLabel = "PATROLLING",
    sanskritState = "सुप्त भ्रमण • Dormant Vigil",
    speedMultiplier = 0.72
  ),
  SHADOW_STALKING(
    badgeLabel = "STALKING",
    sanskritState = "छाया अनुसरण • Shadow Stalking",
    speedMultiplier = 1.0
  ),
  SANITY_AMBUSH(
    badgeLabel = "AMBUSH",
    sanskritState = "माया आक्रमण • Apparition Ambush",
    speedMultiplier = 1.28
  ),
  FRENZIED_CHASE(
    badgeLabel = "HUNTING",
    sanskritState = "उग्र प्रहार • Frenzied Hunt",
    speedMultiplier = 1.48
  ),
  STUNNED_BY_NADA(
    badgeLabel = "BANISHED / STUNNED",
    sanskritState = "नाद स्तम्भन • Mantra & Ghanta Stun",
    speedMultiplier = 0.0
  )
}

/**
 * Result of a single 10Hz AI tick from [EncounterManager].
 */
data class EncounterTickOutcome(
  val entityX: Double,
  val entityY: Double,
  val distanceToPlayer: Double,
  val behaviorState: CreatureBehaviorState,
  val proximityZone: CreatureProximityZone = CreatureProximityZone.OUT_OF_RANGE,
  val enteredDangerZone: Boolean = false,
  val alertLevel: Int,
  val triggeredProceduralSpawn: Boolean = false,
  val triggeredAttack: Boolean = false,
  val repelledByKavach: Boolean = false,
  val eventMessage: String? = null
)

/**
 * Procedural spawn and AI behavior state machine for mythological temple guardians.
 * Dynamically adapts patrol waypoints, stalking speed, shadow-step ambush spawns,
 * and hunt aggression based on player proximity, Diya illumination, and Sanity levels.
 */
class EncounterManager(
  initialX: Double = 7.5,
  initialY: Double = 5.5,
  seed: Long = 108L
) {
  private val random = Random(seed)

  var entityX by mutableDoubleStateOf(initialX)
    private set

  var entityY by mutableDoubleStateOf(initialY)
    private set

  var behaviorState by mutableStateOf(CreatureBehaviorState.DORMANT_PATROL)
    private set

  var entityAlertLevel by mutableIntStateOf(15)
    private set

  var stunTicksRemaining by mutableIntStateOf(0)
    private set

  var proceduralSpawnCount by mutableIntStateOf(0)
    private set

  var activeProximityZone by mutableStateOf(CreatureProximityZone.OUT_OF_RANGE)
    private set

  private var spawnCooldownTicks = 0
  private var patrolWaypoint: Pair<Double, Double>? = null

  /**
   * Executes one 100ms AI and procedural spawn evaluation tick.
   */
  fun onTick(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    playerAngle: Double,
    sanityPercent: Int,
    diyaLit: Boolean,
    chantingMantra: Boolean,
    entityName: String
  ): EncounterTickOutcome {
    if (spawnCooldownTicks > 0) {
      spawnCooldownTicks--
    }

    val dx = playerX - entityX
    val dy = playerY - entityY
    val currentDist = hypot(dx, dy)

    // 1. Handle Stun State (from 528Hz Ghanta Bell, Vedic Kavach, or Sacred Artifacts)
    if (stunTicksRemaining > 0) {
      stunTicksRemaining--
      behaviorState = CreatureBehaviorState.STUNNED_BY_NADA
      activeProximityZone = CreatureProximityZone.OUT_OF_RANGE
      entityAlertLevel = (entityAlertLevel - 2).coerceAtLeast(10)
      return EncounterTickOutcome(
        entityX = entityX,
        entityY = entityY,
        distanceToPlayer = currentDist,
        behaviorState = behaviorState,
        proximityZone = activeProximityZone,
        alertLevel = entityAlertLevel
      )
    }

    // 2. Procedural Shadow-Step Spawn Trigger when player Sanity is fractured (< 50% or Diya unlit)
    // and the creature is far away (> 6.8 tiles)
    var spawnedThisTick = false
    var eventMsg: String? = null

    val lowSanityTrigger = sanityPercent < 50 || (!diyaLit && sanityPercent < 70)
    if (lowSanityTrigger && currentDist > 6.8 && spawnCooldownTicks == 0) {
      val ambushSpot = findProceduralAmbushTile(
        mapGrid = mapGrid,
        playerX = playerX,
        playerY = playerY,
        playerAngle = playerAngle,
        minDist = 3.2,
        maxDist = 5.4
      )
      if (ambushSpot != null) {
        entityX = ambushSpot.first
        entityY = ambushSpot.second
        proceduralSpawnCount++
        spawnCooldownTicks = 110 // 11 seconds cooldown before another procedural shadow-step
        behaviorState = CreatureBehaviorState.SANITY_AMBUSH
        spawnedThisTick = true
        eventMsg = "$entityName manifested from the temple shadows as your Sanity wavered!"
      }
    }

    val updatedDx = playerX - entityX
    val updatedDy = playerY - entityY
    val dist = hypot(updatedDx, updatedDy)

    // 3. Evaluate Behavior State Machine from Proximity + Sanity + Illumination
    if (!spawnedThisTick) {
      behaviorState = when {
        dist < 3.6 || sanityPercent < 25 || (!diyaLit && dist < 5.5) ->
          CreatureBehaviorState.FRENZIED_CHASE
        sanityPercent < 45 && dist < 6.2 ->
          CreatureBehaviorState.SANITY_AMBUSH
        dist < 7.2 || sanityPercent < 68 || !diyaLit ->
          CreatureBehaviorState.SHADOW_STALKING
        else ->
          CreatureBehaviorState.DORMANT_PATROL
      }
    }

    // 4. Calculate movement vector (towards waypoint if patrolling, or towards player if stalking/chasing)
    val baseSpeed = if (diyaLit) 0.038 else 0.056
    val sanityAggressionBonus = ((100 - sanityPercent).coerceIn(0, 100) / 100.0) * 0.016
    val effectiveSpeed = (baseSpeed + sanityAggressionBonus) * behaviorState.speedMultiplier

    val targetCoord: Pair<Double, Double> = if (behaviorState == CreatureBehaviorState.DORMANT_PATROL) {
      val wp = patrolWaypoint
      if (wp == null || hypot(wp.first - entityX, wp.second - entityY) < 0.6) {
        findRandomWalkableTile(mapGrid).also { patrolWaypoint = it }
      } else {
        wp
      }
    } else {
      playerX to playerY
    }

    val moveDx = targetCoord.first - entityX
    val moveDy = targetCoord.second - entityY
    val moveDist = hypot(moveDx, moveDy)

    if (moveDist > 0.2 && effectiveSpeed > 0.0) {
      val nextEX = entityX + (moveDx / moveDist) * effectiveSpeed
      val nextEY = entityY + (moveDy / moveDist) * effectiveSpeed
      if (isWalkable(mapGrid, nextEX, entityY)) {
        entityX = nextEX
      }
      if (isWalkable(mapGrid, entityX, nextEY)) {
        entityY = nextEY
      }
    }

    val finalDx = playerX - entityX
    val finalDy = playerY - entityY
    val finalDist = hypot(finalDx, finalDy)

    // Compute alert level (0..100) combining proximity and sanity vulnerability
    val proximityAlert = ((10.0 - finalDist.coerceIn(0.0, 10.0)) * 8.5).toInt()
    val sanityAlert = ((100 - sanityPercent) * 0.25).toInt()
    entityAlertLevel = (proximityAlert + sanityAlert).coerceIn(10, 100)

    val previousZone = activeProximityZone
    val nextZone = when {
      finalDist <= CreatureProximityZone.IMMINENT_TERROR_ROAR.maxDistanceTiles ->
        CreatureProximityZone.IMMINENT_TERROR_ROAR
      finalDist <= CreatureProximityZone.STALKING_GROWL.maxDistanceTiles ->
        CreatureProximityZone.STALKING_GROWL
      finalDist <= CreatureProximityZone.DISTANT_OMEN.maxDistanceTiles ->
        CreatureProximityZone.DISTANT_OMEN
      else ->
        CreatureProximityZone.OUT_OF_RANGE
    }
    val enteredCloserZone = nextZone.maxDistanceTiles < previousZone.maxDistanceTiles
    activeProximityZone = nextZone

    // 5. Check Close-Quarters Strike or Mantra Kavach Repulsion
    if (finalDist < 0.74) {
      return if (chantingMantra) {
        stunEntity(ticks = 35)
        relocateAfterStrikeOrRepel(mapGrid, playerX, playerY, minDist = 4.2)
        EncounterTickOutcome(
          entityX = entityX,
          entityY = entityY,
          distanceToPlayer = hypot(playerX - entityX, playerY - entityY),
          behaviorState = CreatureBehaviorState.STUNNED_BY_NADA,
          proximityZone = CreatureProximityZone.OUT_OF_RANGE,
          alertLevel = entityAlertLevel,
          repelledByKavach = true,
          eventMessage = "Vedic Kavach repelled $entityName back into the sanctum shadows!"
        )
      } else {
        // Knock creature back slightly after striking player
        val pushX = (entityX - (finalDx / finalDist.coerceAtLeast(0.1)) * 1.65)
        val pushY = (entityY - (finalDy / finalDist.coerceAtLeast(0.1)) * 1.65)
        if (isWalkable(mapGrid, pushX, pushY)) {
          entityX = pushX
          entityY = pushY
        } else {
          relocateAfterStrikeOrRepel(mapGrid, playerX, playerY, minDist = 2.6)
        }
        EncounterTickOutcome(
          entityX = entityX,
          entityY = entityY,
          distanceToPlayer = hypot(playerX - entityX, playerY - entityY),
          behaviorState = behaviorState,
          proximityZone = CreatureProximityZone.IMMINENT_TERROR_ROAR,
          enteredDangerZone = true,
          alertLevel = 100,
          triggeredAttack = true,
          eventMessage = "$entityName struck from the shadows! Ring Ghanta or Chant Mantra!"
        )
      }
    }

    return EncounterTickOutcome(
      entityX = entityX,
      entityY = entityY,
      distanceToPlayer = finalDist,
      behaviorState = behaviorState,
      proximityZone = nextZone,
      enteredDangerZone = enteredCloserZone || spawnedThisTick,
      alertLevel = entityAlertLevel,
      triggeredProceduralSpawn = spawnedThisTick,
      eventMessage = eventMsg
    )
  }

  /**
   * Stuns the mythological creature for [ticks] (at 10Hz, e.g., 30 ticks = 3 seconds).
   */
  fun stunEntity(ticks: Int) {
    if (ticks <= 0) return
    stunTicksRemaining = maxOf(stunTicksRemaining, ticks)
    behaviorState = CreatureBehaviorState.STUNNED_BY_NADA
  }

  /**
   * Forces a procedural spawn at a valid corridor tile near the player (used for scripted ritual surges
   * or sanity collapse events).
   */
  fun forceProceduralAmbush(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    playerAngle: Double
  ): Boolean {
    val spot = findProceduralAmbushTile(
      mapGrid = mapGrid,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      minDist = 2.8,
      maxDist = 5.2
    ) ?: return false
    entityX = spot.first
    entityY = spot.second
    proceduralSpawnCount++
    spawnCooldownTicks = 90
    behaviorState = CreatureBehaviorState.SANITY_AMBUSH
    return true
  }

  fun syncFromCoOpRoom(remoteX: Double, remoteY: Double, remoteAlert: Int) {
    if (remoteX > 0.5 && remoteY > 0.5) {
      entityX = remoteX
      entityY = remoteY
      entityAlertLevel = remoteAlert.coerceIn(0, 100)
    }
  }

  private fun findProceduralAmbushTile(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    playerAngle: Double,
    minDist: Double,
    maxDist: Double
  ): Pair<Double, Double>? {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: return null
    val candidates = mutableListOf<Pair<Double, Double>>()

    for (r in 1 until rows - 1) {
      for (c in 1 until cols - 1) {
        if (mapGrid[r][c] != '1') {
          val cx = c + 0.5
          val cy = r + 0.5
          val d = hypot(cx - playerX, cy - playerY)
          if (d in minDist..maxDist) {
            // Prefer flanking or behind the player's current gaze for eerie horror staging
            val angleToSpot = atan2(cy - playerY, cx - playerX)
            val diff = kotlin.math.abs(atan2(sin(angleToSpot - playerAngle), cos(angleToSpot - playerAngle)))
            if (diff > PI / 3.0) {
              candidates.add(cx to cy)
            }
          }
        }
      }
    }

    if (candidates.isEmpty()) {
      for (r in 1 until rows - 1) {
        for (c in 1 until cols - 1) {
          if (mapGrid[r][c] != '1') {
            val cx = c + 0.5
            val cy = r + 0.5
            val d = hypot(cx - playerX, cy - playerY)
            if (d in minDist..maxDist) {
              candidates.add(cx to cy)
            }
          }
        }
      }
    }

    return if (candidates.isNotEmpty()) {
      candidates[random.nextInt(candidates.size)]
    } else {
      null
    }
  }

  private fun relocateAfterStrikeOrRepel(
    mapGrid: List<String>,
    playerX: Double,
    playerY: Double,
    minDist: Double
  ) {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: return
    for (attempt in 0 until 25) {
      val r = random.nextInt(1, (rows - 1).coerceAtLeast(2))
      val c = random.nextInt(1, (cols - 1).coerceAtLeast(2))
      if (mapGrid[r][c] != '1') {
        val cx = c + 0.5
        val cy = r + 0.5
        if (hypot(cx - playerX, cy - playerY) >= minDist) {
          entityX = cx
          entityY = cy
          return
        }
      }
    }
  }

  private fun findRandomWalkableTile(mapGrid: List<String>): Pair<Double, Double> {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: 12
    for (attempt in 0 until 20) {
      val r = random.nextInt(1, (rows - 1).coerceAtLeast(2))
      val c = random.nextInt(1, (cols - 1).coerceAtLeast(2))
      if (mapGrid[r][c] != '1') {
        return (c + 0.5) to (r + 0.5)
      }
    }
    return 7.5 to 5.5
  }

  private fun isWalkable(mapGrid: List<String>, x: Double, y: Double): Boolean {
    val rows = mapGrid.size
    val cols = mapGrid.firstOrNull()?.length ?: return false
    val r = y.toInt()
    val c = x.toInt()
    return r in 0 until rows && c in 0 until cols && mapGrid[r][c] != '1'
  }
}
