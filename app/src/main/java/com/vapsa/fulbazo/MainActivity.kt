package com.vapsa.fulbazo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vapsa.fulbazo.ui.FulbazoApp
import com.vapsa.fulbazo.ui.theme.FulbazoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FulbazoTheme {
                FulbazoApp()
            }
        }
    }
}
