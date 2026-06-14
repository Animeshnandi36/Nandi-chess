package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chess.ui.ChessApp
import com.example.chess.utils.SoundEffects
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    SoundEffects.init(applicationContext)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ChessApp()
      }
    }
  }
}
