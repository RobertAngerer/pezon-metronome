package com.example.pezonn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pezonn.ui.MetronomeScreen
import com.example.pezonn.ui.theme.PezonnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PezonnTheme {
                MetronomeScreen()
            }
        }
    }
}
