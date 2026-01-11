package com.example.dnd_quest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Estado de la UI
data class AdventureUiState(
    val adventureList: List<Adventure> = emptyList(),
    val currentAdventure: Adventure? = null,
    val currentNode: StoryNode? = null,
    val isEditorMode: Boolean = false // <--- Esto faltaba
)

class AdventureViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AdventureStorage(application.applicationContext)
    private val _uiState = MutableStateFlow(AdventureUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAdventureList()
    }

    // --- NAVEGACIÓN Y CARGA ---

    fun refreshAdventureList() {
        viewModelScope.launch {
            val savedAdventures = storage.getAllAdventures()
            _uiState.update { it.copy(adventureList = savedAdventures) }
        }
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

    fun navigateToNode(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val nextNode = adventure.nodes[nodeId]
        if (nextNode != null) {
            _uiState.update { it.copy(currentNode = nextNode) }
        }
    }

    // --- ¡ESTA ES LA FUNCIÓN QUE FALTABA! ---
    fun toggleEditorMode() {
        _uiState.update { it.copy(isEditorMode = !it.isEditorMode) }
    }

    fun createNewAdventure(title: String, description: String) {
        val startNode = DialogueNode(
            id = UUID.randomUUID().toString(),
            characterName = "Narrador",
            dialogueText = "Inicio de la aventura. ¡Activa el modo editor para cambiar esto!",
            options = emptyList()
        )

        val newAdventure = Adventure(
            title = title,
            description = description,
            startNodeId = startNode.id,
            nodes = mapOf(startNode.id to startNode)
        )
        saveAdventureState(newAdventure)
        selectAdventure(newAdventure.id)
    }

    // --- LÓGICA DE EDICIÓN (LO QUE FALTABA PARA EL EDITOR) ---

    // 1. Guardar cambios en el nodo actual (ej: editar texto)
    fun updateNode(updatedNode: StoryNode) {
        val adventure = _uiState.value.currentAdventure ?: return

        // Actualizamos el mapa de nodos
        val newNodes = adventure.nodes.toMutableMap()
        newNodes[updatedNode.id] = updatedNode

        val updatedAdventure = adventure.copy(nodes = newNodes)
        saveAdventureState(updatedAdventure)

        // Actualizamos la vista inmediata
        _uiState.update { it.copy(currentNode = updatedNode) }
    }

    fun changeNodeType(newTypeString: String) {
        val currentNode = _uiState.value.currentNode ?: return
        val adventure = _uiState.value.currentAdventure ?: return

        // Conservamos el ID y tratamos de conservar la siguiente conexión si existe
        val sameId = currentNode.id
        // Para nodos lineales, intentamos rescatar a dónde iban
        val defaultNextId = when(currentNode) {
            is DialogueNode -> currentNode.options.firstOrNull()?.nextNodeId ?: UUID.randomUUID().toString()
            is CombatNode -> currentNode.nextNodeId
            is ExplorationNode -> currentNode.paths.firstOrNull()?.nextNodeId ?: UUID.randomUUID().toString()
            is LootNode -> currentNode.nextNodeId
            is ItemNode -> currentNode.nextNodeId
            is SkillNode -> currentNode.successNodeId // Por defecto tomamos el éxito
        }

        // Creamos la nueva instancia según el tipo solicitado
        val newNode: StoryNode = when (newTypeString) {
            "Combate" -> CombatNode(sameId, "Zona de combate", emptyList(), defaultNextId)
            "Exploración" -> ExplorationNode(sameId, "Descripción del lugar...", emptyList())
            "Habilidad" -> SkillNode(sameId, "Fuerza", 10, "Contexto del chequeo", defaultNextId, UUID.randomUUID().toString())
            "Objeto" -> ItemNode(sameId, "Nombre Objeto", "Descripción", "Texto al encontrarlo", defaultNextId)
            "Loot" -> LootNode(sameId, emptyList(), defaultNextId)
            else -> DialogueNode(sameId, "Narrador", "Nuevo diálogo...", emptyList()) // Default a Diálogo
        }

        updateNode(newNode)
    }

    // 2. Agregar un nuevo camino/opción y crear el nodo destino automáticamente
    fun addPathToCurrentNode(optionText: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val currentNode = _uiState.value.currentNode ?: return

        // Creamos el nodo nuevo (destino)
        val newNodeId = UUID.randomUUID().toString()
        val newNode = DialogueNode(
            id = newNodeId,
            characterName = "Nuevo Personaje",
            dialogueText = "Nuevo nodo creado. ¡Edítame!",
            options = emptyList()
        )

        // Dependiendo del tipo de nodo actual, lo conectamos diferente
        val updatedCurrentNode = when (currentNode) {
            is DialogueNode -> currentNode.copy(
                options = currentNode.options + DialogueOption(optionText, nextNodeId = newNodeId)
            )
            is ExplorationNode -> currentNode.copy(
                paths = currentNode.paths + ExplorationPath(optionText, nextNodeId = newNodeId)
            )
            // En combate y otros, reemplazamos el destino siguiente
            is CombatNode -> currentNode.copy(nextNodeId = newNodeId)
            is LootNode -> currentNode.copy(nextNodeId = newNodeId)
            is ItemNode -> currentNode.copy(nextNodeId = newNodeId)
            // SkillNode es complejo (tiene 2 salidas), por ahora no lo auto-conectamos aquí
            else -> currentNode
        }

        // Guardamos ambos: el padre actualizado y el hijo nuevo
        val newNodes = adventure.nodes.toMutableMap()
        newNodes[updatedCurrentNode.id] = updatedCurrentNode
        newNodes[newNode.id] = newNode // Agregamos el nuevo al mapa

        val updatedAdventure = adventure.copy(nodes = newNodes)
        saveAdventureState(updatedAdventure)

        _uiState.update { it.copy(currentNode = updatedCurrentNode) }
    }

    private fun saveAdventureState(adventure: Adventure) {
        storage.saveAdventure(adventure)
        _uiState.update { it.copy(currentAdventure = adventure) }
    }
}