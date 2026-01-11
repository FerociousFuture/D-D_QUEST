package com.example.dnd_quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NodeEditorPanel(viewModel: AdventureViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val node = uiState.currentNode ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("🛠️ MODO EDITOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            // --- SELECTOR DE TIPO DE NODO ---
            NodeTypeSelector(currentType = node::class.simpleName ?: "") { newType ->
                viewModel.changeNodeType(newType)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Selector de Editor según el tipo de nodo
            when (node) {
                is DialogueNode -> DialogueEditor(node, viewModel)
                is CombatNode -> CombatEditor(node, viewModel)
                is ExplorationNode -> ExplorationEditor(node, viewModel)
                is LootNode -> LootEditor(node, viewModel)
                is SkillNode -> SkillEditor(node, viewModel)
                is ItemNode -> ItemEditor(node, viewModel)
            }
        }
    }
}

@Composable
fun NodeTypeSelector(currentType: String, onTypeChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Diálogo", "Combate", "Exploración", "Habilidad", "Objeto", "Loot")

    // Mapeo simple para mostrar nombre bonito vs nombre de clase interna si fuera necesario
    // Aquí asumimos que el usuario selecciona el nombre "humano"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tipo Actual: $currentType")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeChanged(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- EDITOR DE DIÁLOGO ---
@Composable
fun DialogueEditor(node: DialogueNode, viewModel: AdventureViewModel) {
    var charName by remember(node.id) { mutableStateOf(node.characterName) }
    var text by remember(node.id) { mutableStateOf(node.dialogueText) }
    var newOptionText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = charName,
        onValueChange = { charName = it; viewModel.updateNode(node.copy(characterName = it)) },
        label = { Text("Nombre del Personaje") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; viewModel.updateNode(node.copy(dialogueText = it)) },
        label = { Text("Texto del Diálogo") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text("Opciones (Respuestas):", fontWeight = FontWeight.Bold)
    node.options.forEach { option ->
        Text("- ${option.text}", style = MaterialTheme.typography.bodySmall)
    }

    // Agregar opción
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newOptionText,
            onValueChange = { newOptionText = it },
            label = { Text("Nueva Opción") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {
            if (newOptionText.isNotBlank()) {
                viewModel.addPathToCurrentNode(newOptionText)
                newOptionText = ""
            }
        }) { Icon(Icons.Default.Add, contentDescription = "Add") }
    }
}

// --- EDITOR DE COMBATE ---
@Composable
fun CombatEditor(node: CombatNode, viewModel: AdventureViewModel) {
    var location by remember(node.id) { mutableStateOf(node.locationDescription) }

    OutlinedTextField(
        value = location,
        onValueChange = { location = it; viewModel.updateNode(node.copy(locationDescription = it)) },
        label = { Text("Ubicación") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Enemigos:", fontWeight = FontWeight.Bold)
    node.enemies.forEachIndexed { index, enemy ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("• ${enemy.name} (${enemy.type}) x${enemy.count}")
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val newEnemies = node.enemies.toMutableList().apply { removeAt(index) }
                viewModel.updateNode(node.copy(enemies = newEnemies))
            }) { Icon(Icons.Default.Delete, contentDescription = "Del") }
        }
    }

    var newEnemyName by remember { mutableStateOf("") }
    Row {
        OutlinedTextField(value = newEnemyName, onValueChange = { newEnemyName = it }, label = { Text("Nombre Enemigo") }, modifier = Modifier.weight(1f))
        Button(onClick = {
            if (newEnemyName.isNotBlank()) {
                viewModel.updateNode(node.copy(enemies = node.enemies + Enemy(newEnemyName, "Normal", 1)))
                newEnemyName = ""
            }
        }) { Text("Add") }
    }
}

// --- EDITOR DE EXPLORACIÓN ---
@Composable
fun ExplorationEditor(node: ExplorationNode, viewModel: AdventureViewModel) {
    var desc by remember(node.id) { mutableStateOf(node.description) }
    var newPathText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = desc,
        onValueChange = { desc = it; viewModel.updateNode(node.copy(description = it)) },
        label = { Text("Descripción") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Caminos:", fontWeight = FontWeight.Bold)
    node.paths.forEach { path -> Text("➤ ${path.description}") }

    Row {
        OutlinedTextField(value = newPathText, onValueChange = { newPathText = it }, label = { Text("Nuevo Camino") }, modifier = Modifier.weight(1f))
        IconButton(onClick = {
            if (newPathText.isNotBlank()) {
                viewModel.addPathToCurrentNode(newPathText)
                newPathText = ""
            }
        }) { Icon(Icons.Default.Add, contentDescription = "Add") }
    }
}

// --- EDITOR DE HABILIDAD (NUEVO) ---
@Composable
fun SkillEditor(node: SkillNode, viewModel: AdventureViewModel) {
    var category by remember(node.id) { mutableStateOf(node.category) }
    var context by remember(node.id) { mutableStateOf(node.context) }
    var dcText by remember(node.id) { mutableStateOf(node.difficultyClass.toString()) }

    OutlinedTextField(
        value = category,
        onValueChange = { category = it; viewModel.updateNode(node.copy(category = it)) },
        label = { Text("Habilidad (ej: Fuerza)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = dcText,
        onValueChange = {
            dcText = it
            val newDc = it.toIntOrNull() ?: 10
            viewModel.updateNode(node.copy(difficultyClass = newDc))
        },
        label = { Text("Dificultad (DC)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = context,
        onValueChange = { context = it; viewModel.updateNode(node.copy(context = it)) },
        label = { Text("Contexto (¿Qué pasa?)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Rutas automáticas:", style = MaterialTheme.typography.bodySmall)
    Text("• Éxito -> Nodo ID: ...${node.successNodeId.takeLast(4)}")
    Text("• Fallo -> Nodo ID: ...${node.failureNodeId.takeLast(4)}")
    Text("(Usa el modo 'Jugar' para visitar y editar esos nodos)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
}

// --- EDITOR DE OBJETO (NUEVO) ---
@Composable
fun ItemEditor(node: ItemNode, viewModel: AdventureViewModel) {
    var name by remember(node.id) { mutableStateOf(node.itemName) }
    var desc by remember(node.id) { mutableStateOf(node.itemDescription) }
    var expo by remember(node.id) { mutableStateOf(node.expositionText) }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it; viewModel.updateNode(node.copy(itemName = it)) },
        label = { Text("Nombre del Objeto") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = desc,
        onValueChange = { desc = it; viewModel.updateNode(node.copy(itemDescription = it)) },
        label = { Text("Descripción Corta") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = expo,
        onValueChange = { expo = it; viewModel.updateNode(node.copy(expositionText = it)) },
        label = { Text("Exposición (Lore)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}

// --- EDITOR DE LOOT ---
@Composable
fun LootEditor(node: LootNode, viewModel: AdventureViewModel) {
    var newItem by remember { mutableStateOf("") }

    Text("Tabla de Botín:", fontWeight = FontWeight.Bold)
    node.lootTable.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💎 $item")
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val newList = node.lootTable.toMutableList().apply { removeAt(index) }
                viewModel.updateNode(node.copy(lootTable = newList))
            }) { Icon(Icons.Default.Delete, contentDescription = "Del") }
        }
    }

    Row {
        OutlinedTextField(value = newItem, onValueChange = { newItem = it }, label = { Text("Nuevo Item") }, modifier = Modifier.weight(1f))
        Button(onClick = {
            if (newItem.isNotBlank()) {
                viewModel.updateNode(node.copy(lootTable = node.lootTable + newItem))
                newItem = ""
            }
        }) { Text("Add") }
    }
}