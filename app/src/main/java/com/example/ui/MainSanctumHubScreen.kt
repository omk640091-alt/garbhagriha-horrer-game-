package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.audio.TempleSoundEngine
import com.example.data.CodexEntry
import com.example.data.ExpeditionRoom
import com.example.data.GarbhagrihaRepository
import com.example.data.PlayerPresence
import com.example.data.SacredShrineDatabase
import com.example.data.SacredShrineRepository
import com.example.data.SacredShrineSaveEntity
import com.example.data.SanataniLoreCatalog
import com.example.data.TempleInfo
import com.example.data.UserProgress
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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainSanctumHubScreen(
  currentUserId: String,
  defaultDisplayName: String,
  onSignOut: () -> Unit
) {
  val viewModel: GarbhagrihaViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val shrineDb = SacredShrineDatabase.getInstance(app)
        GarbhagrihaViewModel(
          repository = GarbhagrihaRepository(db),
          shrineRepository = SacredShrineRepository(shrineDb.sacredShrineSaveDao()),
          currentUserId = currentUserId,
          defaultDisplayName = defaultDisplayName
        )
      }
    }
  )

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val soundEngine = remember { TempleSoundEngine() }

  val selectedTemple by viewModel.selectedTemple.collectAsStateWithLifecycle()
  val selectedTempleShrineSave by viewModel.selectedTempleShrineSave.collectAsStateWithLifecycle()
  val userProgressState by viewModel.userProgressState.collectAsStateWithLifecycle()
  val waitingRoomsState by viewModel.waitingRoomsState.collectAsStateWithLifecycle()
  val activeRoomCode by viewModel.activeRoomCode.collectAsStateWithLifecycle()
  val activeRoom by viewModel.activeRoomState.collectAsStateWithLifecycle()
  val activeRoomPlayers by viewModel.activeRoomPlayers.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var isTransitioningToSanctum by rememberSaveable { mutableStateOf(false) }
  var inFirstPersonGameplay by rememberSaveable { mutableStateOf(false) }
  var resumeFromShrineSave by rememberSaveable { mutableStateOf(false) }

  if (isTransitioningToSanctum) {
    SanctumTransitionLoadingScreen(
      temple = selectedTemple,
      roomCode = activeRoomCode,
      soundEngine = soundEngine,
      onTransitionComplete = {
        isTransitioningToSanctum = false
        inFirstPersonGameplay = true
      }
    )
    return
  }

  if (inFirstPersonGameplay) {
    SanctumGameplayScreen(
      temple = selectedTemple,
      roomCode = activeRoomCode,
      activeRoom = activeRoom,
      coOpPlayers = activeRoomPlayers,
      currentUserId = currentUserId,
      soundEngine = soundEngine,
      initialShrineSave = if (resumeFromShrineSave) selectedTempleShrineSave else null,
      existingShrineSave = selectedTempleShrineSave,
      onSaveAtSacredShrine = {
        shrineTitle, px, py, pAngle, prana, sanity, stamina, oil, lit, diyas, seals, relics, survivalSec ->
        viewModel.saveProgressAtSacredShrine(
          temple = selectedTemple,
          shrineTitle = shrineTitle,
          playerX = px,
          playerY = py,
          playerAngle = pAngle,
          pranaPercent = prana,
          sanityPercent = sanity,
          staminaPercent = stamina,
          diyaOilPercent = oil,
          diyaLit = lit,
          litDiyaCoords = diyas,
          solvedSealCoords = seals,
          collectedArtifactIds = relics,
          survivalSeconds = survivalSec
        )
      },
      onPublishTelemetry = { x, y, angle, prana, diyaLit, chanting ->
        viewModel.publishPlayerTelemetry(x, y, angle, prana, diyaLit, chanting)
      },
      onPublishRitualEvent = { status, diyas, seals, ex, ey, alert, msg ->
        viewModel.publishRitualEvent(status, diyas, seals, ex, ey, alert, msg)
      },
      onExpeditionFinished = { cleansed, diyas, puzzles, survivalSec, unlockedId ->
        viewModel.recordCompletedExpedition(cleansed, diyas, puzzles, survivalSec, unlockedId)
      },
      onExitToHub = {
        inFirstPersonGameplay = false
        resumeFromShrineSave = false
      }
    )
    return
  }

  Scaffold(
    containerColor = ObsidianVoid,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      SanctumTopHeader(
        displayName = defaultDisplayName,
        userProgressState = userProgressState,
        onTestBell = { soundEngine.playTempleBell(528.0) },
        onSignOut = {
          signOutUser(
            credentialManager = credentialManager,
            onSignOutComplete = onSignOut,
            scope = scope
          )
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = SanctumGranite,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
      ) {
        NavigationBarItem(
          selected = currentTab == 0,
          onClick = { currentTab = 0 },
          icon = { Icon(Icons.Default.Explore, contentDescription = "Sanctums") },
          label = { Text("Sanctums") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ObsidianVoid,
            selectedTextColor = MoltenBrass,
            indicatorColor = SaffronFlame,
            unselectedIconColor = SandstoneMuted,
            unselectedTextColor = SandstoneMuted
          ),
          modifier = Modifier.testTag("nav_sanctums_tab")
        )
        NavigationBarItem(
          selected = currentTab == 1,
          onClick = { currentTab = 1 },
          icon = { Icon(Icons.Default.Groups, contentDescription = "Online Co-Op") },
          label = { Text("Online Co-Op") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ObsidianVoid,
            selectedTextColor = MoltenBrass,
            indicatorColor = SaffronFlame,
            unselectedIconColor = SandstoneMuted,
            unselectedTextColor = SandstoneMuted
          ),
          modifier = Modifier.testTag("nav_multiplayer_tab")
        )
        NavigationBarItem(
          selected = currentTab == 2,
          onClick = { currentTab = 2 },
          icon = { Icon(Icons.Default.AutoStories, contentDescription = "Puranic Codex") },
          label = { Text("Puranic Lore") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ObsidianVoid,
            selectedTextColor = MoltenBrass,
            indicatorColor = SaffronFlame,
            unselectedIconColor = SandstoneMuted,
            unselectedTextColor = SandstoneMuted
          ),
          modifier = Modifier.testTag("nav_codex_tab")
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      statusBanner?.let { banner ->
        Surface(
          color = SacredCardSurface,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.clearBanner() }
        ) {
          Text(
            text = banner,
            style = MaterialTheme.typography.bodyMedium,
            color = MoltenBrass,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )
        }
      }

      when (currentTab) {
        0 -> SanctumsExplorerTab(
          temples = SanataniLoreCatalog.temples,
          selectedTemple = selectedTemple,
          selectedTempleShrineSave = selectedTempleShrineSave,
          onSelectTemple = { temple ->
            soundEngine.playStoneChakraGrind()
            viewModel.selectTemple(temple)
          },
          onEnterSoloSanctum = {
            soundEngine.playTempleBell(432.0)
            viewModel.leaveMultiplayerRoom()
            resumeFromShrineSave = false
            isTransitioningToSanctum = true
          },
          onResumeFromSacredShrine = {
            soundEngine.playTempleBell(528.0)
            viewModel.leaveMultiplayerRoom()
            resumeFromShrineSave = true
            isTransitioningToSanctum = true
          },
          onOpenMultiplayer = {
            currentTab = 1
          }
        )

        1 -> OnlineMultiplayerTab(
          selectedTemple = selectedTemple,
          waitingRoomsState = waitingRoomsState,
          activeRoomCode = activeRoomCode,
          activeRoom = activeRoom,
          activeRoomPlayers = activeRoomPlayers,
          onHostRoom = {
            soundEngine.playTempleBell(528.0)
            viewModel.hostMultiplayerExpedition { _, _ ->
              isTransitioningToSanctum = true
            }
          },
          onJoinRoom = { code ->
            soundEngine.playTempleBell(639.0)
            viewModel.joinMultiplayerExpedition(code) { _, _ ->
              isTransitioningToSanctum = true
            }
          },
          onResumeActiveRoom = {
            isTransitioningToSanctum = true
          }
        )

        2 -> PuranicCodexTab(
          entries = SanataniLoreCatalog.codexEntries,
          soundEngine = soundEngine
        )
      }
    }
  }
}

@Composable
private fun SanctumTopHeader(
  displayName: String,
  userProgressState: UiState<UserProgress>,
  onTestBell: () -> Unit,
  onSignOut: () -> Unit
) {
  Surface(
    color = SanctumGranite,
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "GARBHAGRIHA",
            style = MaterialTheme.typography.headlineMedium,
            color = MoltenBrass
          )
          Text(
            text = "Sadhaka $displayName",
            style = MaterialTheme.typography.bodyMedium,
            color = SandstoneMuted
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onTestBell,
            modifier = Modifier.testTag("header_bell_button")
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "Ring Temple Ghanta",
              tint = SaffronFlame
            )
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = ParchmentCream
            )
          }
        }
      }

      if (userProgressState is UiState.Success) {
        val p = userProgressState.data
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(SacredCardSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "🏛 Cleansed: ${p.sanctumsCleansed}",
            style = MaterialTheme.typography.labelLarge,
            color = MantraJade
          )
          Text(
            text = "🪔 Diyas Lit: ${p.diyasLit}",
            style = MaterialTheme.typography.labelLarge,
            color = MoltenBrass
          )
          Text(
            text = "☸ Seals: ${p.puzzlesSolved}",
            style = MaterialTheme.typography.labelLarge,
            color = SacredTeal
          )
        }
      }
    }
  }
}

@Composable
private fun SanctumsExplorerTab(
  temples: List<TempleInfo>,
  selectedTemple: TempleInfo,
  selectedTempleShrineSave: SacredShrineSaveEntity?,
  onSelectTemple: (TempleInfo) -> Unit,
  onEnterSoloSanctum: () -> Unit,
  onResumeFromSacredShrine: () -> Unit,
  onOpenMultiplayer: () -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      // Ultra-HD Hero Sanctum Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SanctumGranite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, SaffronFlame, RoundedCornerShape(24.dp))
      ) {
        Box(modifier = Modifier.height(210.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_temple_sanctum_1791611662727),
            contentDescription = selectedTemple.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Transparent,
                    ObsidianVoid.copy(alpha = 0.92f)
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(16.dp)
          ) {
            Text(
              text = selectedTemple.dynastyAndEra.uppercase(),
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
            Text(
              text = selectedTemple.name,
              style = MaterialTheme.typography.headlineMedium,
              color = MoltenBrass
            )
            Text(
              text = "Haunted by ${selectedTemple.entityName} (${selectedTemple.entityTitle})",
              style = MaterialTheme.typography.bodyMedium,
              color = BloodAltarRed
            )
          }
        }

        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = selectedTemple.curseLore,
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentCream
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onEnterSoloSanctum,
              colors = ButtonDefaults.buttonColors(
                containerColor = SaffronFlame,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("launch_first_person_button")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("ENTER 3D SANCTUM", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = onOpenMultiplayer,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .height(52.dp)
                .testTag("co_op_shortcut_button")
            ) {
              Icon(Icons.Default.Groups, contentDescription = null, tint = MoltenBrass)
              Spacer(modifier = Modifier.width(6.dp))
              Text("CO-OP", color = MoltenBrass, fontWeight = FontWeight.Bold)
            }
          }

          if (selectedTempleShrineSave != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onResumeFromSacredShrine,
              colors = ButtonDefaults.buttonColors(
                containerColor = MantraJade,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("resume_sacred_shrine_button")
            ) {
              Text(
                text = "RESUME SACRED SHRINE (${selectedTempleShrineSave.shrineTitle} • SANITY ${selectedTempleShrineSave.sanityPercent}%)",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    item {
      Text(
        text = "SELECT HISTORICAL TEMPLE EXPEDITION",
        style = MaterialTheme.typography.titleLarge,
        color = MoltenBrass
      )
    }

    items(temples, key = { it.id }) { temple ->
      val isSelected = temple.id == selectedTemple.id
      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) SacredCardSurface else SanctumGranite
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MoltenBrass else SandstoneMuted.copy(alpha = 0.25f),
            shape = RoundedCornerShape(18.dp)
          )
          .clickable { onSelectTemple(temple) }
          .testTag("temple_card_${temple.id}")
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = temple.name,
                style = MaterialTheme.typography.titleMedium,
                color = MoltenBrass
              )
              if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected",
                  tint = MantraJade,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Text(
              text = "${temple.location} • ${temple.difficultyLabel}",
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Guardian: ${temple.entityName} • ${temple.requiredDiyas} Diyas • ${temple.requiredSeals} Vedic Seals",
              style = MaterialTheme.typography.bodyMedium,
              color = SandstoneMuted
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OnlineMultiplayerTab(
  selectedTemple: TempleInfo,
  waitingRoomsState: UiState<List<ExpeditionRoom>>,
  activeRoomCode: String?,
  activeRoom: ExpeditionRoom?,
  activeRoomPlayers: List<PlayerPresence>,
  onHostRoom: () -> Unit,
  onJoinRoom: (String) -> Unit,
  onResumeActiveRoom: () -> Unit
) {
  var roomCodeInput by rememberSaveable { mutableStateOf("") }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = SanctumGranite),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, MoltenBrass, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "ONLINE CO-OP SANCTUM RITUAL",
            style = MaterialTheme.typography.titleLarge,
            color = MoltenBrass
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Experience the terror with friends in real time. Shared Diyas, synchronized Vedic Seals, and live teammate coordinates inside ${selectedTemple.name}.",
            style = MaterialTheme.typography.bodyMedium,
            color = SandstoneMuted
          )
          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = onHostRoom,
            colors = ButtonDefaults.buttonColors(
              containerColor = SaffronFlame,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("host_coop_room_button")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "HOST NEW EXPEDITION (${selectedTemple.name.uppercase()})",
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = roomCodeInput,
              onValueChange = { roomCodeInput = it.uppercase().take(6) },
              label = { Text("6-Char Sanctum Code") },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("join_room_code_input")
            )
            Button(
              onClick = { onJoinRoom(roomCodeInput) },
              colors = ButtonDefaults.buttonColors(
                containerColor = MoltenBrass,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .height(54.dp)
                .testTag("join_room_code_button")
            ) {
              Text("JOIN", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    if (activeRoomCode != null) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = SacredCardSurface),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SacredTeal, RoundedCornerShape(18.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "ACTIVE EXPEDITION ROOM: $activeRoomCode",
              style = MaterialTheme.typography.titleMedium,
              color = SacredTeal
            )
            Text(
              text = "Temple: ${activeRoom?.templeName ?: selectedTemple.name} •Connected Sadhakas: ${activeRoomPlayers.size.coerceAtLeast(1)}",
              style = MaterialTheme.typography.bodyMedium,
              color = ParchmentCream
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onResumeActiveRoom,
              colors = ButtonDefaults.buttonColors(
                containerColor = MantraJade,
                contentColor = Color.Black
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_active_room_button")
            ) {
              Text("RETURN TO LIVE CO-OP SANCTUM", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Text(
        text = "OPEN SANCTUM LOBBIES",
        style = MaterialTheme.typography.titleLarge,
        color = MoltenBrass
      )
    }

    when (waitingRoomsState) {
      is UiState.Loading -> {
        item {
          Text(
            text = "Scanning ancient temple frequencies...",
            style = MaterialTheme.typography.bodyMedium,
            color = SandstoneMuted
          )
        }
      }

      is UiState.Error -> {
        item {
          Text(
            text = waitingRoomsState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = BloodAltarRed
          )
        }
      }

      is UiState.Success -> {
        val rooms = waitingRoomsState.data
        if (rooms.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = SanctumGranite),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.LocalFireDepartment,
                  contentDescription = null,
                  tint = SaffronFlame,
                  modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No open sanctum lobbies waiting right now. Host an expedition above and invite friends with your 6-character code!",
                  style = MaterialTheme.typography.bodyMedium,
                  color = SandstoneMuted
                )
              }
            }
          }
        } else {
          items(rooms, key = { it.roomCode }) { room ->
            Card(
              colors = CardDefaults.cardColors(containerColor = SanctumGranite),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MoltenBrass.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${room.templeName} • [${room.roomCode}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = MoltenBrass
                  )
                  Text(
                    text = "Host: ${room.hostName} • Entity: ${room.entityName} • ${room.memberUids.size}/4 Sadhakas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandstoneMuted
                  )
                }
                Button(
                  onClick = { onJoinRoom(room.roomCode) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronFlame,
                    contentColor = Color.Black
                  ),
                  modifier = Modifier.testTag("join_listed_room_${room.roomCode}")
                ) {
                  Text("JOIN", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PuranicCodexTab(
  entries: List<CodexEntry>,
  soundEngine: TempleSoundEngine
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SanctumGranite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, BloodAltarRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
      ) {
        Column {
          Image(
            painter = painterResource(id = R.drawable.img_entity_brahmarakshasa_1791611674320),
            contentDescription = "Brahmarakshasa Ancient Temple Guardian",
            modifier = Modifier
              .fillMaxWidth()
              .height(190.dp),
            contentScale = ContentScale.Crop
          )
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "PURANIC BESTIARY & VEDIC ARCHIVES",
              style = MaterialTheme.typography.titleLarge,
              color = MoltenBrass
            )
            Text(
              text = "Study the weaknesses of cursed temple spirits and master the acoustic Swara & Surya Yantra counter-rituals.",
              style = MaterialTheme.typography.bodyMedium,
              color = SandstoneMuted
            )
          }
        }
      }
    }

    items(entries, key = { it.id }) { entry ->
      Card(
        colors = CardDefaults.cardColors(containerColor = SacredCardSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MoltenBrass.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
          .clickable { soundEngine.playMantraChantPulse(700) }
          .testTag("codex_card_${entry.id}")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${entry.title} (${entry.sanskritName})",
              style = MaterialTheme.typography.titleMedium,
              color = MoltenBrass
            )
            Text(
              text = entry.category.uppercase(),
              style = MaterialTheme.typography.labelLarge,
              color = SaffronFlame
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = entry.threatLevel,
            style = MaterialTheme.typography.labelLarge,
            color = BloodAltarRed
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = entry.description,
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentCream
          )
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = SanctumGranite,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "⚡ Counter-Ritual: ${entry.counterRitual}",
              style = MaterialTheme.typography.bodyMedium,
              color = MantraJade,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }
  }
}
