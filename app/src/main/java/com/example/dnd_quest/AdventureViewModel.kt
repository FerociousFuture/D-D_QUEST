package com.example.dnd_quest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la UI: Ahora incluimos la lista de aventuras disponibles
data class AdventureUiState(
    val adventureList: List<Adventure> = emptyList(), // Lista para el menú principal
    val currentAdventure: Adventure? = null,          // La aventura que estamos jugando/editando
    val currentNode: StoryNode? = null,               // El nodo actual en pantalla
    val isEditorMode: Boolean = false                 // ¿Estamos jugando o editando?
)

class AdventureViewModel(application: Application) : AndroidViewModel(application) {

    // Instanciamos nuestro sistema de guardado
    private val storage = AdventureStorage(application.applicationContext)

    private val _uiState = MutableStateFlow(AdventureUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Al iniciar, cargamos la lista de aventuras guardadas
        refreshAdventureList()
    }

    // --- GESTIÓN DE LA LISTA DE AVENTURAS ---

    fun refreshAdventureList() {
        viewModelScope.launch {
            val savedAdventures = storage.getAllAdventures()
            _uiState.update { it.copy(adventureList = savedAdventures) }
        }
    }

    fun createNewAdventure(title: String, description: String) {
        // Creamos una aventura nueva con un nodo de inicio básico
        val startNode = DialogueNode(
            id = "start",
            characterName = "Narrador",
            dialogueText = "Aquí comienza tu nueva historia...",
            options = emptyList()
        )

        val newAdventure = Adventure(
            title = title,
            description = description,
            startNodeId = startNode.id,
            nodes = mapOf(startNode.id to startNode)
        )

        // Guardamos en archivo y recargamos
        storage.saveAdventure(newAdventure)
        refreshAdventureList()

        // La seleccionamos automáticamente para empezar a editarla
        selectAdventure(newAdventure.id)
    }

    fun selectAdventure(adventureId: String) {
        val adventure = storage.getAdventure(adventureId)
        if (adventure != null) {
            _uiState.update {
                it.copy(
                    currentAdventure = adventure,
                    currentNode = adventure.nodes[adventure.startNodeId]
                )
            }
        }
    }

    fun closeAdventure() {
        _uiState.update { it.copy(currentAdventure = null, currentNode = null) }
        refreshAdventureList()
    }

    // --- NAVEGACIÓN Y JUEGO ---

    fun navigateToNode(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val nextNode = adventure.nodes[nodeId]

        if (nextNode != null) {
            _uiState.update { it.copy(currentNode = nextNode) }
        }
    }

    // --- EDICIÓN (Lo usaremos pronto) ---

    fun saveCurrentState() {
        val current = _uiState.value.currentAdventure
        if (current != null) {
            storage.saveAdventure(current)
            refreshAdventureList() // Actualizar la lista por si cambiamos el título
        }
    }
}