package com.example.dnd_quest //

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dnd_quest.ui.theme.DND_QUESTTheme //

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DND_QUESTTheme {
                // Aquí es donde llamamos a nuestra pantalla principal
                // No necesitamos pasarle el ViewModel manualmente,
                // AdventureScreen ya se encarga de instanciarlo por defecto.
                AdventureScreen()
            }
        }
    }
}