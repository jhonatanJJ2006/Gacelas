package com.gacelas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GacelasTheme {
                GacelasNavHost()
            }
        }
    }
}
