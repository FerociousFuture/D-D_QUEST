package com.example.dnd_quest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AdventureUiState(
    val adventureList: List<Adventure> = emptyList(),
    val currentAdventure: Adventure? = null,
    val currentNode: StoryNode? = null,
    val editingNode: StoryNode? = null,
    val isEditorMode: Boolean = false
)

class AdventureViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AdventureStorage(application.applicationContext)
    private val _uiState = MutableStateFlow(AdventureUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAdventureList()
    }

    fun handleBackPress() {
        val state = _uiState.value
        when {
            state.isEditorMode && state.editingNode != null -> clearEditingNode()
            state.currentAdventure != null -> closeAdventure()
        }
    }

    fun refreshAdventureList() {
        viewModelScope.launch {
            val savedAdventures = storage.getAllAdventures()
            _uiState.update { it.copy(adventureList = savedAdventures) }
        }
    }

    fun selectAdventure(adventureId: String, startInEditor: Boolean = false) {
        val adventure = storage.getAdventure(adventureId)
        if (adventure != null) {
            _uiState.update {
                it.copy(
                    currentAdventure = adventure,
                    currentNode = adventure.nodes[adventure.startNodeId],
                    editingNode = null,
                    isEditorMode = startInEditor
                )
            }
        }
    }

    fun createNewAdventure(title: String, description: String) {
        val startNodeId = UUID.randomUUID().toString()
        val startNode = DialogueNode(
            id = startNodeId,
            characterName = "Narrador",
            dialogueText = "Inicio de la aventura...",
            options = emptyList()
        )

        val newAdventure = Adventure(
            title = title,
            description = description,
            startNodeId = startNodeId,
            nodes = mapOf(startNodeId to startNode)
        )

        saveAdventureState(newAdventure)
        refreshAdventureList()
        selectAdventure(newAdventure.id, startInEditor = true)
    }

    // --- BORRAR AVENTURA ---
    fun deleteAdventure(adventureId: String) {
        storage.deleteAdventure(adventureId)
        refreshAdventureList()
    }

    fun closeAdventure() {
        _uiState.update { it.copy(currentAdventure = null, currentNode = null, editingNode = null) }
        refreshAdventureList()
    }

    fun navigateToNode(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val nextNode = adventure.nodes[nodeId]
        if (nextNode != null) {
            _uiState.update { it.copy(currentNode = nextNode) }
        }
    }

    fun toggleEditorMode() {
        _uiState.update { state ->
            val newMode = !state.isEditorMode
            state.copy(
                isEditorMode = newMode,
                editingNode = if (newMode) state.currentNode else null
            )
        }
    }

    fun selectNodeToEdit(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val node = adventure.nodes[nodeId]
        _uiState.update { it.copy(editingNode = node) }
    }

    fun clearEditingNode() {
        _uiState.update { it.copy(editingNode = null) }
    }

    fun createNodeAndGetId(type: String): String {
        val adventure = _uiState.value.currentAdventure ?: return ""
        val newId = UUID.randomUUID().toString()

        val newNode: StoryNode = when (type) {
            "Combate" -> CombatNode(newId, "", emptyList(), "", "")
            "Exploración" -> ExplorationNode(newId, "", emptyList())
            // SkillNode inicializado con name vacío
            "Habilidad" -> SkillNode(newId, "", "", 10, "", "", "")
            "Objeto" -> ItemNode(newId, "", "", "", "")
            "Loot" -> LootNode(newId, emptyList(), "")
            else -> DialogueNode(newId, "", "", emptyList())
        }

        val newNodes = adventure.nodes.toMutableMap()
        newNodes[newId] = newNode

        val updatedAdventure = adventure.copy(nodes = newNodes)
        saveAdventureState(updatedAdventure)

        return newId
    }

    fun createNode(type: String, linkToParent: Boolean) {
        val parentNode = _uiState.value.editingNode ?: _uiState.value.currentNode
        val newId = createNodeAndGetId(type)

        if (newId.isNotEmpty()) {
            if (linkToParent && parentNode != null) {
                val updatedParent = linkNodeToParent(parentNode, newId, "Nuevo Camino")
                updateNode(updatedParent)
            }
            selectNodeToEdit(newId)
        }
    }

    fun deleteNode(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        if (nodeId == adventure.startNodeId) return

        val newNodes = adventure.nodes.toMutableMap()
        newNodes.remove(nodeId)

        val updatedAdventure = adventure.copy(nodes = newNodes)
        saveAdventureState(updatedAdventure)
        _uiState.update { it.copy(editingNode = null) }
    }

    fun updateNode(updatedNode: StoryNode) {
        val adventure = _uiState.value.currentAdventure ?: return
        val newNodes = adventure.nodes.toMutableMap()
        newNodes[updatedNode.id] = updatedNode

        val updatedAdventure = adventure.copy(nodes = newNodes)
        saveAdventureState(updatedAdventure)

        _uiState.update {
            it.copy(
                editingNode = if (it.editingNode?.id == updatedNode.id) updatedNode else it.editingNode,
                currentNode = if (it.currentNode?.id == updatedNode.id) updatedNode else it.currentNode
            )
        }
    }

    fun changeNodeType(newTypeString: String) {
        val currentNode = _uiState.value.editingNode ?: return
        val sameId = currentNode.id

        val defaultNextId = when(currentNode) {
            is DialogueNode -> currentNode.options.firstOrNull()?.nextNodeId ?: ""
            is CombatNode -> currentNode.nextNodeId
            is ExplorationNode -> currentNode.paths.firstOrNull()?.nextNodeId ?: ""
            is LootNode -> currentNode.nextNodeId
            is ItemNode -> currentNode.nextNodeId
            is SkillNode -> currentNode.successNodeId
        }

        val newNode: StoryNode = when (newTypeString) {
            "Combate" -> CombatNode(sameId, "Zona de combate", emptyList(), defaultNextId, "")
            "Exploración" -> ExplorationNode(sameId, "Descripción...", emptyList())
            // SkillNode inicializado con name
            "Habilidad" -> SkillNode(sameId, "Nueva Prueba", "Fuerza", 10, "Contexto", defaultNextId, UUID.randomUUID().toString())
            "Objeto" -> ItemNode(sameId, "Objeto", "Desc", "Lore", defaultNextId)
            "Loot" -> LootNode(sameId, emptyList(), defaultNextId)
            else -> DialogueNode(sameId, "Narrador", "...", emptyList())
        }

        updateNode(newNode)
    }

    private fun linkNodeToParent(parent: StoryNode, childId: String, text: String): StoryNode {
        return when (parent) {
            is DialogueNode -> parent.copy(options = parent.options + DialogueOption(text, childId))
            is ExplorationNode -> parent.copy(paths = parent.paths + ExplorationPath(text, childId))
            is CombatNode -> parent.copy(nextNodeId = childId)
            is LootNode -> parent.copy(nextNodeId = childId)
            is ItemNode -> parent.copy(nextNodeId = childId)
            is SkillNode -> parent
        }
    }

    fun getParentNodes(targetNodeId: String): List<StoryNode> {
        val adventure = _uiState.value.currentAdventure ?: return emptyList()
        return adventure.nodes.values.filter { parent ->
            when (parent) {
                is DialogueNode -> parent.options.any { it.nextNodeId == targetNodeId }
                is CombatNode -> parent.nextNodeId == targetNodeId || parent.defeatNodeId == targetNodeId
                is ExplorationNode -> parent.paths.any { it.nextNodeId == targetNodeId }
                is LootNode -> parent.nextNodeId == targetNodeId
                is ItemNode -> parent.nextNodeId == targetNodeId
                is SkillNode -> parent.successNodeId == targetNodeId || parent.failureNodeId == targetNodeId
            }
        }
    }

    private fun saveAdventureState(adventure: Adventure) {
        storage.saveAdventure(adventure)
        _uiState.update { it.copy(currentAdventure = adventure) }
    }
}