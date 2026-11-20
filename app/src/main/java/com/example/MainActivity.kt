package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.authStateFlow
import com.example.ui.navigation.KhmerHomeApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.KhmerHomeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KhmerHomeTheme {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val uid = currentUser?.uid
    if (uid != null) {
        key(uid) {
            KhmerHomeApp(currentUserId = uid)
        }
    } else {
        AuthScreen(onAuthSuccess = {})
    }
}
