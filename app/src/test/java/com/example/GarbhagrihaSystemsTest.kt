package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.CreatureProximityZone
import com.example.audio.SoundManager
import com.example.audio.SpatialCueType
import com.example.audio.SurfaceMaterial
import com.example.audio.TempleSoundEngine
import com.example.data.SanataniLoreCatalog
import com.example.ui.CreatureBehaviorState
import com.example.ui.DialogueManager
import com.example.ui.DivinePassiveBuff
import com.example.ui.EncounterManager
import com.example.ui.EnvironmentalEffectManager
import com.example.ui.EnvironmentalEffectState
import com.example.ui.FogOfWarMiniMapManager
import com.example.ui.GodsFavorManager
import com.example.ui.GodsFavorStanding
import com.example.ui.MahaAartiPhase
import com.example.ui.MoralActionType
import com.example.ui.RitualActionType
import com.example.ui.RitualInteractionManager
import com.example.ui.RitualProgressState
import com.example.ui.SacredVoiceMantra
import com.example.ui.SanctumEnvironmentalHazard
import com.example.ui.SanctumGameplayScreen
import com.example.ui.SanctumLanguageMode
import com.example.ui.ScreenShakeController
import com.example.ui.ScreenShakeTrigger
import com.example.ui.ShakeIntensityState
import com.example.ui.TileFogVisibility
import com.example.ui.TorchFlameState
import com.example.ui.TorchLightManager
import com.example.ui.TorchShadowCasterSystem
import com.example.ui.TorchShadowState
import com.example.ui.VoiceMantraAndBossManager
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GarbhagrihaSystemsTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `1_torchLightManager_consumesFuel_dynamicallyAdjustsRadius_andSupportsRefillAndToggle`() {
    val torch = TorchLightManager(initialFuel = 100f, initiallyLit = true)
    assertTrue(torch.isLit)
    assertEquals(TorchFlameState.BLAZING_AGNI, torch.flameState)
    val initialRadius = torch.currentLightRadiusTiles
    assertTrue("Full fuel radius should be >= 7.0 tiles", initialRadius >= 7.0f)

    // Tick while sprinting near a creature to consume fuel faster
    repeat(30) { idx ->
      torch.onTick(
        animTick = idx * 0.1f,
        isSprinting = true,
        chantingMantra = false,
        entityDistance = 3.0
      )
    }
    assertTrue("Fuel should decrease after ticks", torch.fuelPercent < 100f)

    // Simulate low fuel sputtering state
    torch.restoreState(savedFuel = 18f, savedLit = true)
    assertEquals(TorchFlameState.SPUTTERING_EMBER, torch.flameState)
    assertTrue(
      "Sputtering ember radius (${torch.currentLightRadiusTiles}) should be smaller than blazing radius ($initialRadius)",
      torch.currentLightRadiusTiles < initialRadius
    )

    // Toggle torch off manually to conserve fuel
    val toggleOffMsg = torch.toggleFlame()
    assertFalse(torch.isLit)
    assertEquals(TorchFlameState.EXTINGUISHED_DARKNESS, torch.flameState)
    assertTrue(toggleOffMsg.contains("shuttered", ignoreCase = true))

    // Refill ghee fuel and auto-ignite
    torch.refillFuel(amount = 80f, autoIgnite = true)
    assertTrue(torch.isLit)
    assertEquals(98f, torch.fuelPercent, 0.01f)
    assertEquals(TorchFlameState.BLAZING_AGNI, torch.flameState)
  }

  @Test
  fun `2_soundManager_resolvesSurfaceMaterials_fromEnvironmentTagsAndMapGrid`() {
    val engine = TempleSoundEngine().apply { setMuted(true) }
    val soundManager = SoundManager(engine).apply { setMuted(true) }
    val grid = SanataniLoreCatalog.temples.first().mapGrid

    // Explicit environment tag overrides
    assertEquals(
      SurfaceMaterial.STONE,
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = grid,
        playerX = 1.5,
        playerY = 1.5,
        environmentTagOverride = "ENV_SURFACE_STONE"
      )
    )
    assertEquals(
      SurfaceMaterial.DIRT,
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = grid,
        playerX = 1.5,
        playerY = 1.5,
        environmentTagOverride = "ENV_SURFACE_DIRT_CRYPT"
      )
    )
    assertEquals(
      SurfaceMaterial.METAL,
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = grid,
        playerX = 1.5,
        playerY = 1.5,
        environmentTagOverride = "ENV_SURFACE_METAL_GRATE"
      )
    )

    // Map tile coordinate resolution: (10,3) is a Vedic Seal '3' -> METAL
    assertEquals(
      SurfaceMaterial.METAL,
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = grid,
        playerX = 10.5,
        playerY = 3.5
      )
    )
    // Map tile (4,1) is a Sacred Diya '2' -> DIRT/Ash alcove
    assertEquals(
      SurfaceMaterial.DIRT,
      soundManager.resolveSurfaceMaterialFromEnvironment(
        mapGrid = grid,
        playerX = 4.5,
        playerY = 1.5
      )
    )
  }

  @Test
  fun `3_environmentalEffectManager_triggersFallingDustAndCobwebs_fromSpeedAndWallProximity`() {
    val envManager = EnvironmentalEffectManager()
    val grid = SanataniLoreCatalog.temples.first().mapGrid
    val initialParticles = envManager.dustParticles.size

    // Move rapidly very close to a wall tile (x=1.18 is 0.18 tiles from wall at col 0)
    envManager.onPlayerMoveStep(
      mapGrid = grid,
      playerX = 1.18,
      playerY = 1.5,
      stepMagnitude = 0.52,
      isSprinting = true
    )

    assertTrue("Wall proximity factor should be high near wall", envManager.wallProximityFactor > 0.4f)
    assertTrue("Dust particle count should increase on sprinting near wall", envManager.dustParticles.size > initialParticles)
    assertTrue("Cobweb cling intensity should increase near wall", envManager.cobwebClingIntensity > 0f)
    assertEquals(EnvironmentalEffectState.FALLING_MASONRY_DUST, envManager.effectState)
  }

  @Test
  fun `4_dialogueManager_findsNearbySpiritOrInscription_andProgressesDialogueChoices`() {
    val dialogueManager = DialogueManager()
    val encounters = dialogueManager.getEncountersForTemple("brihadeeswarar")
    assertTrue("Brihadeeswarar should have NPC spirit and ancient inscription encounters", encounters.size >= 2)

    val nearby = dialogueManager.findNearbyEncounter(
      templeId = "brihadeeswarar",
      playerX = 2.4,
      playerY = 1.4
    )
    assertNotNull("Should detect Acharya Kunjara spirit near (2,1)", nearby)
    dialogueManager.startDialogue(nearby!!)
    assertTrue(dialogueManager.isDialogueOpen)

    val firstChoice = dialogueManager.currentNode!!.choices.first()
    val outcome = dialogueManager.selectChoice(firstChoice)
    assertNotNull(outcome)
    assertTrue("Sanity delta should be positive for spirit guidance", outcome!!.sanityDelta > 0)
  }

  @Test
  fun `5_screenShakeController_triggersTraumaOnJumpscare_andProximityTremorOnChase`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shakeController = ScreenShakeController(context)
    assertEquals(ShakeIntensityState.STILL, shakeController.intensityState)

    // Trigger violent jumpscare strike
    shakeController.triggerShake(ScreenShakeTrigger.JUMPSCARE_STRIKE)
    assertTrue("Trauma should spike near 0.95f", shakeController.trauma >= 0.9f)
    assertEquals(ShakeIntensityState.VIOLENT_JUMPSCARE, shakeController.intensityState)

    // Decay over several ticks while creature is in frenzied chase nearby
    repeat(10) { i ->
      shakeController.onTick(
        entityDistance = 2.8,
        behaviorState = CreatureBehaviorState.FRENZIED_CHASE,
        sanityPercent = 40,
        chantingMantra = false,
        animTick = i * 0.1f
      )
    }
    assertTrue("Continuous chase tremor should remain > 0 when creature is at 2.8m", shakeController.continuousTremor > 0.2f)
  }

  @Test
  fun `6_torchShadowCasterSystem_castsRealisticShadows_fromPillarsStatuesAndCreature`() {
    val shadowSystem = TorchShadowCasterSystem()
    val temple = SanataniLoreCatalog.temples.first()
    val encounters = DialogueManager().getEncountersForTemple(temple.id)

    shadowSystem.updateShadowCasters(
      mapGrid = temple.mapGrid,
      playerX = 3.5,
      playerY = 5.5,
      playerAngle = 0.0,
      diyaLit = true,
      torchLightRadiusTiles = 8.2f,
      torchFlickerMultiplier = 1.0f,
      entityX = 5.5,
      entityY = 5.5,
      dialogueEncounters = encounters,
      animTick = 1.0f
    )

    assertTrue("Should cast active shadow volumes in pillared corridor", shadowSystem.activeCasterCount > 0)
    assertTrue("Projected shadow volumes list should be non-empty", shadowSystem.projectedShadowVolumes.isNotEmpty())

    // Verify ray shadow factor darkens rays behind an occluder
    val firstVol = shadowSystem.projectedShadowVolumes.first()
    val numRays = 100
    val rayIdx = (firstVol.screenCenterXNorm * numRays).toInt().coerceIn(0, numRays - 1)
    val factor = shadowSystem.computeRayShadowFactor(
      rayIdx = rayIdx,
      numRays = numRays,
      wallPerpDist = firstVol.distanceToTorch + 1.5f
    )
    assertTrue("Wall behind shadow caster should be darkened (< 1.0f), got $factor", factor < 1.0f)

    // When torch is extinguished, state becomes NO_TORCH_UMBRA
    shadowSystem.updateShadowCasters(
      mapGrid = temple.mapGrid,
      playerX = 3.5,
      playerY = 5.5,
      playerAngle = 0.0,
      diyaLit = false,
      torchLightRadiusTiles = 2.2f,
      torchFlickerMultiplier = 0.8f,
      entityX = 5.5,
      entityY = 5.5,
      dialogueEncounters = encounters,
      animTick = 1.2f
    )
    assertEquals(TorchShadowState.NO_TORCH_UMBRA, shadowSystem.shadowState)
  }

  @Test
  fun `7_ritualInteractionSystem_unlocksSecretTempleChamber_whenOrderedSequenceCompleted`() {
    val temple = SanataniLoreCatalog.temples.first() // brihadeeswarar
    val ritualManager = RitualInteractionManager(temple.id)
    assertEquals(RitualProgressState.DORMANT, ritualManager.progressState)
    assertFalse(ritualManager.isSecretChamberUnlocked)

    // Performing an out-of-order action resets the sequence
    val wrongOutcome = ritualManager.recordAction(
      actionType = RitualActionType.IGNITE_DIRECTIONAL_LAMP,
      coord = 9 to 7
    )
    assertFalse(wrongOutcome.isStepCorrect)
    assertEquals(0, ritualManager.currentStepIndex)

    // Now perform all 5 ordered steps of Brihadeeswarar's Ashta-Dikpala Pradakshina ritual
    for (step in ritualManager.activeRitual.steps) {
      val outcome = ritualManager.recordAction(
        actionType = step.actionType,
        coord = step.targetCoord
      )
      assertTrue("Step ${step.stepIndex} (${step.title}) should succeed", outcome.isStepCorrect)
    }

    assertTrue("Secret temple chamber should now be unlocked", ritualManager.isSecretChamberUnlocked)
    assertEquals(RitualProgressState.CHAMBER_UNLOCKED, ritualManager.progressState)

    // Verify mapGrid transformation converts secret door wall '1' at (5,6) into '5' and interior (5,7) into '6'
    val unlockedGrid = ritualManager.applySecretChamberToMapGrid(temple.mapGrid)
    assertEquals('5', unlockedGrid[6][5])
    assertEquals('6', unlockedGrid[7][5])

    // Claim the inner sanctuary relic
    val claimOutcome = ritualManager.claimSecretSanctuaryBlessing()
    assertTrue(claimOutcome.grantsRelic)
    assertEquals(RitualProgressState.SANCTUARY_CLAIMED, ritualManager.progressState)
  }

  @Test
  fun `8_soundManager_andEncounterManager_triggerDistinctCreatureGrowls_byProximityZone`() {
    val engine = TempleSoundEngine().apply { setMuted(true) }
    val soundManager = SoundManager(engine).apply { setMuted(true) }
    val encounterManager = EncounterManager(initialX = 4.5, initialY = 1.5)
    val temple = SanataniLoreCatalog.temples.first()

    // Player at (1.5, 1.5), entity at (4.5, 1.5) -> ~3.0 tiles away (IMMINENT_TERROR_ROAR <= 3.4m)
    val tickOutcome = encounterManager.onTick(
      mapGrid = temple.mapGrid,
      playerX = 1.5,
      playerY = 1.5,
      playerAngle = 0.0,
      sanityPercent = 75,
      diyaLit = true,
      chantingMantra = false,
      entityName = temple.entityName
    )

    assertEquals(CreatureProximityZone.IMMINENT_TERROR_ROAR, tickOutcome.proximityZone)
    assertTrue("Entering terror roar zone from out-of-range should flag enteredDangerZone", tickOutcome.enteredDangerZone)

    val audioResult = soundManager.triggerEncounterProximityAudio(
      tickCount = 10,
      distanceTiles = tickOutcome.distanceToPlayer,
      listenerX = 1.5,
      listenerY = 1.5,
      listenerAngle = 0.0,
      sourceX = tickOutcome.entityX,
      sourceY = tickOutcome.entityY,
      entityName = "Brahmarakshasa",
      templeId = "brihadeeswarar",
      isAggressiveHunt = true,
      isStunned = false,
      forceImmediateRoar = tickOutcome.enteredDangerZone
    )

    assertEquals(CreatureProximityZone.IMMINENT_TERROR_ROAR, audioResult.proximityZone)
    assertEquals(SpatialCueType.BRAHMARAKSHASA_GROWL, audioResult.triggeredCue)
    assertNotNull("Entering IMMINENT_TERROR_ROAR should produce a directional warning banner", audioResult.warningBanner)

    // Verify distinct creature vocalization cues for Chhaya Pret and Pishacha Vetala
    assertEquals(
      SpatialCueType.CHHAYA_PRET_RASP,
      soundManager.selectCreatureVocalizationCue(
        entityName = "Chhaya Pret",
        templeId = "konark",
        proximityZone = CreatureProximityZone.STALKING_GROWL,
        isAggressiveHunt = true
      )
    )
    assertEquals(
      SpatialCueType.VETALA_SPECTRAL_SHRIEK,
      soundManager.selectCreatureVocalizationCue(
        entityName = "Pishacha Vetala",
        templeId = "kailasa",
        proximityZone = CreatureProximityZone.IMMINENT_TERROR_ROAR,
        isAggressiveHunt = true
      )
    )
  }

  @Test
  fun `9_sanctumGameplayScreen_rendersHudAndInteractiveOverlays_inCompose`() {
    val temple = SanataniLoreCatalog.temples.first()
    val soundEngine = TempleSoundEngine().apply { setMuted(true) }
    val soundManager = SoundManager(soundEngine).apply { setMuted(true) }

    composeTestRule.setContent {
      MyApplicationTheme {
        SanctumGameplayScreen(
          temple = temple,
          roomCode = null,
          activeRoom = null,
          coOpPlayers = emptyList(),
          currentUserId = "test_sadhaka",
          soundEngine = soundEngine,
          soundManager = soundManager,
          onPublishTelemetry = { _, _, _, _, _, _ -> },
          onPublishRitualEvent = { _, _, _, _, _, _, _ -> },
          onExpeditionFinished = { _, _, _, _, _ -> },
          onExitToHub = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify core HUD elements, Fog-of-War Mini-Map, God's Favor chip, and action buttons are displayed
    composeTestRule.onNodeWithTag("first_person_viewport").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fog_of_war_minimap_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_fog_of_war_map_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_gods_favor_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_diya_meter").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_surface_material_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("objective_indicator_obj_secret_ritual").assertIsDisplayed()

    // Tap the HUD God's Favor chip to open the God's Favor Sanctuary Dialog and pray at shrine
    composeTestRule.onNodeWithTag("hud_gods_favor_chip").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gods_favor_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pray_at_shrine_action_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("close_gods_favor_dialog_button").performClick()
    composeTestRule.waitForIdle()

    // Tap the HUD Fog-of-War Cartography chip to open the Expanded Vastu Mandala Map Dialog
    composeTestRule.onNodeWithTag("hud_fog_of_war_map_chip").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("expanded_fog_of_war_map_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_expanded_map_button").performClick()
    composeTestRule.waitForIdle()

    // Step forward once to trigger environmental dust/cobweb and shadow updates
    composeTestRule.onNodeWithTag("move_forward_button").performClick()
    composeTestRule.waitForIdle()

    // Open the Ritual Sequence Krama Altar overlay and verify it renders
    composeTestRule.onNodeWithTag("open_ritual_sequence_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ritual_interaction_overlay_root").assertIsDisplayed()

    // Perform Step 1 directly via the Ritual Altar UI button and verify step completion
    composeTestRule.onNodeWithTag("perform_ritual_step_button_0").performClick()
    composeTestRule.waitForIdle()

    // Close the Ritual overlay and verify the HUD ritual progress chip is now displayed
    composeTestRule.onNodeWithTag("close_ritual_overlay_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hud_ritual_progress_chip").assertIsDisplayed()

    // Open the Spirit & Inscription Dialogue overlay and verify it renders
    composeTestRule.onNodeWithTag("open_dialogue_overlay_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialogue_overlay_root").assertIsDisplayed()
  }

  @Test
  fun `10_fogOfWarMiniMapManager_revealsTilesAndTracksBreadcrumbsAsPlayerExplores`() {
    val meenakshi = SanataniLoreCatalog.temples.first { it.id == "meenakshi" }
    val fogManager = FogOfWarMiniMapManager(templeId = meenakshi.id)

    assertEquals(0, fogManager.explorationPercent)
    assertEquals(TileFogVisibility.UNEXPLORED, fogManager.getTileVisibility(1, 1))
    assertEquals(TileFogVisibility.UNEXPLORED, fogManager.getTileVisibility(10, 10))

    // Player starts at (1.5, 1.5) with torch lit
    fogManager.updateExploration(
      mapGrid = meenakshi.mapGrid,
      playerX = 1.5,
      playerY = 1.5,
      playerAngle = 0.0,
      diyaLit = true,
      torchRadiusTiles = 6.5f
    )

    assertEquals(TileFogVisibility.ACTIVELY_ILLUMINATED, fogManager.getTileVisibility(1, 1))
    assertEquals(TileFogVisibility.UNEXPLORED, fogManager.getTileVisibility(10, 10))
    val initialPercent = fogManager.explorationPercent
    assertTrue("Expected initial exploration > 0%", initialPercent > 0)

    // Player moves deeper into the corridor to (3.5, 1.5); previous far tiles become DISCOVERED_MEMORY
    fogManager.updateExploration(
      mapGrid = meenakshi.mapGrid,
      playerX = 3.5,
      playerY = 1.5,
      playerAngle = 0.0,
      diyaLit = true,
      torchRadiusTiles = 6.5f
    )
    assertTrue("Expected exploration percent to grow or stay charted", fogManager.explorationPercent >= initialPercent)
    assertTrue("Expected breadcrumb trail to record steps", fogManager.getBreadcrumbTrail().size >= 2)
  }

  @Test
  fun `11_voiceMantraAndBossManager_recognizesMantras_togglesHindi_andCompletesMahaAarti`() {
    val manager = VoiceMantraAndBossManager()

    // Toggle Hindi/Sanskrit Devanagari mode
    assertEquals(SanctumLanguageMode.ENGLISH_SANSKRIT, manager.languageMode)
    manager.toggleLanguageMode()
    assertEquals(SanctumLanguageMode.HINDI_DEVANAGARI, manager.languageMode)

    // Voice Mantra evaluation
    val matched = manager.evaluateSpokenMantra("Om Tryambakam Yajamahe")
    assertEquals(SacredVoiceMantra.MAHA_MRITYUNJAYA, matched)
    assertEquals(1, manager.totalVoiceMantrasChanted)

    // 3-Stage Maha-Aarti Boss Exorcism progression
    assertFalse(manager.isBossBanished)
    assertEquals(100, manager.bossShadowIntegrityPercent)

    val (stage2, _) = manager.performBossExorcismStageAction()
    assertEquals(MahaAartiPhase.STAGE_2_SHANKHA_GHANTA_NADA, stage2)
    assertEquals(66, manager.bossShadowIntegrityPercent)

    val (stage3, _) = manager.performBossExorcismStageAction()
    assertEquals(MahaAartiPhase.STAGE_3_PURNA_AHUTI_MANTRA, stage3)
    assertEquals(32, manager.bossShadowIntegrityPercent)

    val (victory, _) = manager.performBossExorcismStageAction()
    assertEquals(MahaAartiPhase.BANISHED_VICTORY, victory)
    assertEquals(0, manager.bossShadowIntegrityPercent)
    assertTrue(manager.isBossBanished)
  }

  @Test
  fun `12_godsFavorManager_grantsPassiveHazardImmunityOnRespectfulActions_andStripsBuffsOnDesecration`() {
    val favorManager = GodsFavorManager(templeId = "konark", initialFavor = 55)
    assertEquals(GodsFavorStanding.NEUTRAL_SEEKER, favorManager.standing)
    assertFalse(favorManager.hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN))

    // Perform respectful shrine prayer -> grants Surya Tejas Aegis (Immunity to Abyssal Darkness Drain)
    val prayOutcome = favorManager.performMoralAction(MoralActionType.PRAY_AT_SHRINE)
    assertEquals(73, favorManager.favorScore)
    assertEquals(GodsFavorStanding.REVERENT_DEVOTEE, favorManager.standing)
    assertEquals(DivinePassiveBuff.SURYA_TEJAS_AEGIS, prayOutcome.activatedBuff)
    assertTrue(favorManager.hasHazardImmunity(SanctumEnvironmentalHazard.ABYSSAL_DARKNESS_DRAIN))
    assertEquals(1, favorManager.respectfulActionsCount)

    // Spare sacred Kalasha -> grants Vayu Prana Purity (Immunity to Choking Masonry Dust) & reaches Exalted Grace
    val spareOutcome = favorManager.performMoralAction(
      MoralActionType.SPARE_SACRED_KALASHA,
      nodeId = "konark_surya_kalasha"
    )
    assertEquals(89, favorManager.favorScore)
    assertEquals(GodsFavorStanding.EXALTED_ANUGRAHA, favorManager.standing)
    assertEquals(DivinePassiveBuff.VAYU_PRANA_SHIELD, spareOutcome.activatedBuff)
    assertTrue(favorManager.hasHazardImmunity(SanctumEnvironmentalHazard.SUFFOCATING_MASONRY_SPORES))

    // Tick 1 second with extinguished torch -> records neutralized hazard
    val pranaRegen = favorManager.onSecondTick(
      diyaLit = false,
      environmentalState = EnvironmentalEffectState.FALLING_MASONRY_DUST,
      entityDistance = 6.0
    )
    assertTrue("Expected positive passive Prana regen in Exalted Grace", pranaRegen > 0)
    assertTrue("Expected hazards neutralized count > 0", favorManager.hazardsNeutralizedCount >= 2)

    // Desecrating an urn strips active buffs and lowers favor
    val desecrateOutcome = favorManager.performMoralAction(MoralActionType.SHATTER_VOTIVE_URN_FOR_LOOT)
    assertEquals(67, desecrateOutcome.updatedFavorScore)
    assertTrue("Expected active buffs to be cleared upon desecration", favorManager.activeBuffSeconds.isEmpty())
    assertEquals(1, favorManager.desecrationsCount)
  }
}
