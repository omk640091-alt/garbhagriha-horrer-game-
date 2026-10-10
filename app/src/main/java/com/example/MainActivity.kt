package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AuthScreen
import com.example.ui.MainSanctumHubScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var localSadhakaActive by androidx.compose.runtime.saveable.rememberSaveable {
    androidx.compose.runtime.mutableStateOf(false)
  }
  val user = currentUser
  if (user == null && !localSadhakaActive) {
    AuthScreen(
      onAuthSuccess = {
        localSadhakaActive = true
      }
    )
  } else {
    MainSanctumHubScreen(
      currentUserId = user?.uid ?: "sadhaka_local_explorer",
      defaultDisplayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Sadhaka",
      onSignOut = {
        localSadhakaActive = false
      }
    )
  }
}
