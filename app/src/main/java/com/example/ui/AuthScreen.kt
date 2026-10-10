package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.MoltenBrass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.SacredCardSurface
import com.example.ui.theme.SaffronFlame
import com.example.ui.theme.SandstoneMuted
import com.example.ui.theme.SanctumGranite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianVoid)
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_temple_sanctum_1791611662727),
      contentDescription = "Ancient Hindu Temple Garbhagriha Sanctum",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              ObsidianVoid.copy(alpha = 0.55f),
              ObsidianVoid.copy(alpha = 0.85f),
              ObsidianVoid.copy(alpha = 0.98f)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        color = SacredCardSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(
          width = 1.dp,
          color = MoltenBrass.copy(alpha = 0.45f),
          shape = RoundedCornerShape(50)
        )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Sacred Flame",
            tint = SaffronFlame,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "ॐ गम गणपतये नमः • SANATANI HORROR",
            style = MaterialTheme.typography.labelLarge,
            color = MoltenBrass
          )
        }
      }

      Card(
        colors = CardDefaults.cardColors(
          containerColor = SanctumGranite.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(
              colors = listOf(SaffronFlame, MoltenBrass.copy(alpha = 0.35f), SaffronFlame)
            ),
            shape = RoundedCornerShape(24.dp)
          )
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_app_icon_foreground_1791611652958),
            contentDescription = "Sacred Brass Diya & Trishul Emblem",
            modifier = Modifier
              .size(84.dp)
              .clip(RoundedCornerShape(20.dp))
              .border(1.dp, MoltenBrass, RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "GARBHAGRIHA",
            style = MaterialTheme.typography.displayLarge,
            color = MoltenBrass,
            textAlign = TextAlign.Center
          )

          Text(
            text = "THE CURSED SANCTUM",
            style = MaterialTheme.typography.titleMedium,
            color = SaffronFlame,
            letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Descend into ancient Chola, Konark, and Kailasa temple crypts. Solve Vedic Surya Yantra & Ghanta puzzles in Ultra-HD 3D first-person while evading the Brahmarakshasa with friends online.",
            style = MaterialTheme.typography.bodyMedium,
            color = SandstoneMuted,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(18.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            FeaturePill(icon = Icons.Default.LocalFireDepartment, label = "3D Raycast FP")
            FeaturePill(icon = Icons.Default.VolumeUp, label = "Spatial Audio")
            FeaturePill(icon = Icons.Default.Shield, label = "Online Co-Op")
          }

          Spacer(modifier = Modifier.height(24.dp))

          if (errorMessage != null) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("auth_error_text")
            )
          }

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = {
                  isLoading = false
                }
              )
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
              containerColor = SaffronFlame,
              contentColor = Color(0xFF1A0800)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF1A0800),
                strokeWidth = 2.5.dp
              )
            } else {
              Text(
                text = "Sign in with Google to Enter Sanctum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Text(
        text = "Wear headphones for 3D acoustic Ghanta & Brahmarakshasa spatial audio",
        style = MaterialTheme.typography.bodyMedium,
        color = ParchmentCream.copy(alpha = 0.65f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun FeaturePill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .background(SacredCardSurface, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = MoltenBrass,
      modifier = Modifier.size(15.dp)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = ParchmentCream
    )
  }
}
