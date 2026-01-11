package com.example.dnd_quest

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Este estado representa todo lo que la UI necesita saber para dibujarse
data class AdventureUiState(
    val currentAdventure: Adventure? = null,
    val currentNode: StoryNode? = null,
    val isGameOver: Boolean = false
)

class AdventureViewModel : ViewModel() {

    // Usamos StateFlow para que la UI se actualice automáticamente cuando algo cambie aquí
    private val _uiState = MutableStateFlow(AdventureUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Al iniciar, cargamos nuestra aventura de prueba
        loadSampleAdventure()
    }

    // Esta función será llamada desde la UI cuando el usuario toque un botón
    fun navigateToNode(nodeId: String) {
        val adventure = _uiState.value.currentAdventure ?: return
        val nextNode = adventure.nodes[nodeId]

        if (nextNode != null) {
            _uiState.update { it.copy(currentNode = nextNode) }
        } else {
            // Si el nodo no existe, asumimos que terminó la demo o hubo un error
            // Aquí podrías agregar lógica para "Fin del Juego"
        }
    }

    private fun loadSampleAdventure() {
        val adventure = createSampleAdventure()
        _uiState.update {
            it.copy(
                currentAdventure = adventure,
                currentNode = adventure.nodes[adventure.startNodeId]
            )
        }
    }

    // --- DATOS DE PRUEBA: UNA PEQUEÑA AVENTURA ---
    private fun createSampleAdventure(): Adventure {
        // Definimos los nodos
        val node1 = DialogueNode(
            id = "start",
            characterName = "Narrador",
            dialogueText = "Te despiertas en una celda oscura y húmeda. Escuchas pasos acercándose.",
            options = listOf(
                DialogueOption("Hacerse el dormido", nextNodeId = "wait"),
                DialogueOption("Gritar pidiendo ayuda", nextNodeId = "combat_guard")
            )
        )

        val nodeWait = ExplorationNode(
            id = "wait",
            description = "El guardia pasa de largo, ignorándote. Ves que se le cayó una llave cerca de los barrotes.",
            paths = listOf(
                ExplorationPath("Intentar alcanzar la llave", nextNodeId = "skill_dexterity"),
                ExplorationPath("Esperar más tiempo", nextNodeId = "game_over") // Ejemplo de fin
            )
        )

        val nodeCombat = CombatNode(
            id = "combat_guard",
            locationDescription = "Pasillo de la Mazmorra",
            enemies = listOf(
                Enemy("Guardia Goblin", "Humanoide", 1),
                Enemy("Rata Gigante", "Bestia", 2)
            ),
            nextNodeId = "loot_guard"
        )

        val nodeSkill = SkillNode(
            id = "skill_dexterity",
            category = "Destreza (Sigilo)",
            difficultyClass = 12,
            context = "Intentas tomar la llave sin hacer ruido...",
            successNodeId = "escape_success",
            failureNodeId = "combat_guard" // Si fallas, el guardia te ve
        )

        val nodeLoot = LootNode(
            id = "loot_guard",
            lootTable = listOf("Espada Corta Oxidada", "10 Monedas de Oro", "Llave de la Celda"),
            nextNodeId = "escape_success"
        )

        val nodeSuccess = DialogueNode(
            id = "escape_success",
            characterName = "Narrador",
            dialogueText = "¡Lograste salir de la celda! Frente a ti se abren los caminos hacia la libertad.",
            options = listOf(DialogueOption("Volver a empezar", "start"))
        )

        // Mapa de todos los nodos
        val allNodes = listOf(node1, nodeWait, nodeCombat, nodeSkill, nodeLoot, nodeSuccess)
            .associateBy { it.id }

        return Adventure(
            title = "Escape de la Mazmorra",
            description = "Una aventura corta para probar el sistema.",
            nodes = allNodes,
            startNodeId = "start"
        )
    }
}