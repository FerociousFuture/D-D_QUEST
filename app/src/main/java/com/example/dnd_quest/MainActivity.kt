package com.example.dnd_quest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel // Importante
import com.example.dnd_quest.ui.theme.DND_QUESTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DND_QUESTTheme {
                // Instanciamos el ViewModel UNA sola vez aquí arriba
                val viewModel: AdventureViewModel = viewModel()

                // Observamos el estado global
                val uiState by viewModel.uiState.collectAsState()

                // LÓGICA DE NAVEGACIÓN SIMPLE
                if (uiState.currentAdventure == null) {
                    // Si no hay aventura seleccionada, mostramos la lista
                    AdventureListScreen(
                        viewModel = viewModel,
                        onAdventureSelected = { adventureId ->
                            viewModel.selectAdventure(adventureId)
                        }
                    )
                } else {
                    // Si hay aventura, mostramos el juego
                    AdventureScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.closeAdventure()
                        }
                    )
                }
            }
        }
    }
}