package com.example.dnd_quest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Adventure(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val nodes: Map<String, StoryNode>,
    val startNodeId: String
)

@Serializable
sealed class StoryNode {
    abstract val id: String
}

@Serializable
@SerialName("dialogue")
data class DialogueNode(
    override val id: String,
    val characterName: String,
    val dialogueText: String,
    val options: List<DialogueOption>
) : StoryNode()

@Serializable
data class DialogueOption(
    val text: String,
    val nextNodeId: String,
    val isIdealPath: Boolean = false,
    val isSkipOption: Boolean = false
)

@Serializable
@SerialName("combat")
data class CombatNode(
    override val id: String,
    val locationDescription: String,
    val enemies: List<Enemy>,
    val nextNodeId: String,
    val defeatNodeId: String = ""
) : StoryNode()

@Serializable
data class Enemy(
    val name: String,
    val type: String,
    val count: Int
)

@Serializable
@SerialName("exploration")
data class ExplorationNode(
    override val id: String,
    val description: String,
    val paths: List<ExplorationPath>
) : StoryNode()

@Serializable
data class ExplorationPath(
    val description: String,
    val nextNodeId: String
)

@Serializable
@SerialName("skill")
data class SkillNode(
    override val id: String,
    val name: String = "",        // Nuevo: Nombre identificador (ej: "Saltar Muro")
    val category: String,         // Categoría (ej: "Atletismo")
    val difficultyClass: Int,
    val context: String,
    val successNodeId: String,
    val failureNodeId: String
) : StoryNode()

@Serializable
@SerialName("item")
data class ItemNode(
    override val id: String,
    val itemName: String,
    val itemDescription: String,
    val expositionText: String,
    val nextNodeId: String
) : StoryNode()

@Serializable
@SerialName("loot")
data class LootNode(
    override val id: String,
    val lootTable: List<String>,
    val nextNodeId: String
) : StoryNode()