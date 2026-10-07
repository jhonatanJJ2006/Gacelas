package com.gacelas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gacelas.ui.theme.GacelasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GacelasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GacelasApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GacelasApp(modifier: Modifier = Modifier) {
    // TODO: Navigation setup will go here
    WelcomeScreen(modifier = modifier)
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Bienvenido a Gacelas 🦌\n\nApp Android en desarrollo según arquitectura documentada.",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GacelasAppPreview() {
    GacelasTheme {
        GacelasApp()
    }
}
