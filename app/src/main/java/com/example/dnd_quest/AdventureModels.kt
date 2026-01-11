package com.example.dnd_quest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// La clase principal que contiene toda la información de una aventura
@Serializable
data class Adventure(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    // El mapa contiene todos los nodos, la clave es el ID del nodo
    val nodes: Map<String, StoryNode>,
    // El ID del nodo donde empieza la historia
    val startNodeId: String
)

// Usamos 'sealed class' para permitir diferentes tipos de nodos pero tratarlos como uno solo
@Serializable
sealed class StoryNode {
    abstract val id: String
}

// 1. NODO DE DIÁLOGO
@Serializable
@SerialName("dialogue")
data class DialogueNode(
    override val id: String,
    val characterName: String, // Quién habla
    val dialogueText: String,
    val options: List<DialogueOption>
) : StoryNode()

@Serializable
data class DialogueOption(
    val text: String,
    val nextNodeId: String,
    val isIdealPath: Boolean = false, // Para el "Camino Ideal"
    val isSkipOption: Boolean = false // Si esto es true, "skipea" una sección
)

// 2. NODO DE COMBATE
@Serializable
@SerialName("combat")
data class CombatNode(
    override val id: String,
    val locationDescription: String,
    val enemies: List<Enemy>,
    val nextNodeId: String // A dónde vamos después de ganar
) : StoryNode()

@Serializable
data class Enemy(
    val name: String,
    val type: String,
    val count: Int
)

// 3. NODO DE EXPLORACIÓN
@Serializable
@SerialName("exploration")
data class ExplorationNode(
    override val id: String,
    val description: String, // Descripción de la zona
    val paths: List<ExplorationPath>
) : StoryNode()

@Serializable
data class ExplorationPath(
    val description: String, // "Tomar el camino oscuro"
    val nextNodeId: String
)

// 4. NODO DE HABILIDAD (Skill Check)
@Serializable
@SerialName("skill")
data class SkillNode(
    override val id: String,
    val category: String, // Ej: "Fuerza", "Percepción"
    val difficultyClass: Int, // La DC (ej: 15)
    val context: String, // Por qué se tira el dado
    val successNodeId: String, // Si pasan el check
    val failureNodeId: String  // Si fallan el check
) : StoryNode()

// 5. NODO DE OBJETO (Item)
@Serializable
@SerialName("item")
data class ItemNode(
    override val id: String,
    val itemName: String,
    val itemDescription: String,
    val expositionText: String, // Texto de lore al obtenerlo
    val nextNodeId: String // Continuación automática
) : StoryNode()

// 6. NODO DE LOOT TABLE
@Serializable
@SerialName("loot")
data class LootNode(
    override val id: String,
    val lootTable: List<String>, // Lista de cosas que pueden salir
    val nextNodeId: String
) : StoryNode()