package com.example.dnd_quest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ==========================================
// HELPERS
// ==========================================
fun getNodeColor(node: StoryNode): Color = when(node) {
    is CombatNode -> Color(0xFFB71C1C)
    is ExplorationNode -> Color(0xFF2E7D32)
    is SkillNode -> Color(0xFF1565C0)
    is LootNode, is ItemNode -> Color(0xFFFFD700)
    else -> Color.Gray
}

fun getNodeIcon(node: StoryNode): ImageVector = when(node) {
    is CombatNode -> Icons.Default.Warning
    is ExplorationNode -> Icons.Default.LocationOn
    is SkillNode -> Icons.Default.Star
    is LootNode, is ItemNode -> Icons.Default.ShoppingCart
    else -> Icons.Default.Person
}

// ACTUALIZADO: Usa 'name' para habilidades si existe
fun getNodeTitle(node: StoryNode): String = when(node) {
    is DialogueNode -> node.characterName.ifEmpty { "Diálogo" }
    is CombatNode -> node.locationDescription.ifEmpty { "Combate" }
    is ExplorationNode -> node.description.take(20)
    is ItemNode -> node.itemName
    is SkillNode -> node.name.ifEmpty { node.category.ifEmpty { "Habilidad" } }
    else -> "Nodo"
}

fun getNodeTypeString(node: StoryNode): String = when(node) {
    is DialogueNode -> "Diálogo"
    is CombatNode -> "Combate"
    is ExplorationNode -> "Exploración"
    is SkillNode -> "Habilidad"
    is ItemNode -> "Objeto"
    is LootNode -> "Loot"
}

// ==========================================
// COMPONENTES COMUNES
// ==========================================

@Composable
fun DeleteConfirmDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("¿Estás seguro? Esta acción no se puede deshacer.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CleanTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = false, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("...", color = Color.Gray.copy(0.5f)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary, unfocusedBorderColor = MaterialTheme.colorScheme.tertiary)
    )
}

@Composable
fun NodeSelector(label: String, currentId: String, allNodes: List<StoryNode>, onNodeSelected: (String) -> Unit, onCreateNew: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNode = allNodes.find { it.id == currentId }
    val displayText = selectedNode?.let { "${getNodeTitle(it)} (${getNodeTypeString(it)})" } ?: "Seleccionar Destino..."
    val displayColor = if(selectedNode != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).clickable { expanded = true }.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayText, color = displayColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
            DropdownMenuItem(text = { Text("➕ CREAR NUEVO", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }, onClick = { expanded = false; onCreateNew() })
            HorizontalDivider()
            allNodes.forEach { node ->
                DropdownMenuItem(
                    text = { Column { Text(getNodeTitle(node), fontWeight = FontWeight.Bold); Text(getNodeTypeString(node), style = MaterialTheme.typography.bodySmall) } },
                    onClick = { onNodeSelected(node.id); expanded = false },
                    leadingIcon = { Icon(getNodeIcon(node), null, tint = getNodeColor(node)) }
                )
            }
        }
    }
}

// ==========================================
// DASHBOARD
// ==========================================

@Composable
fun EditorDashboard(viewModel: AdventureViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val nodes = uiState.currentAdventure?.nodes?.values?.toList() ?: emptyList()
    var showAddDialog by remember { mutableStateOf(false) }
    BackHandler { viewModel.handleBackPress() }

    if (uiState.editingNode != null) {
        NodeForm(node = uiState.editingNode!!, allNodes = nodes, viewModel = viewModel, onClose = { viewModel.clearEditingNode() })
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ESTRUCTURA DE LA HISTORIA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(nodes.sortedBy { it.id != uiState.currentAdventure?.startNodeId }) { node ->
                        NodeListItem(node = node, isStart = node.id == uiState.currentAdventure?.startNodeId, onClick = { viewModel.selectNodeToEdit(node.id) })
                    }
                }
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.Black
            ) { Icon(Icons.Default.Add, "Nuevo") }
        }
    }

    if (showAddDialog) {
        AddNodeDialog(onDismiss = { showAddDialog = false }, onConfirm = { type, link -> viewModel.createNode(type, link); showAddDialog = false })
    }
}

// ==========================================
// FORMULARIO MAESTRO
// ==========================================
@Composable
fun NodeForm(node: StoryNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onClose: () -> Unit) {
    val parentNodes = remember(node) { viewModel.getParentNodes(node.id) }
    var showQuickCreateDialog by remember { mutableStateOf(false) }
    var quickCreateCallback by remember { mutableStateOf<(String) -> Unit>({}) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onBackground) }
            Text("EDITANDO: ${getNodeTypeString(node).uppercase()}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error) }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

        // Entradas
        Text("CONEXIONES (ENTRADAS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        if (parentNodes.isEmpty()) Text("⚠️ Nodo huérfano.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        else parentNodes.forEach { parent -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(12.dp), MaterialTheme.colorScheme.tertiary); Text(" De: ${getNodeTitle(parent)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        Spacer(modifier = Modifier.height(16.dp))

        NodeTypeSelector(currentType = getNodeTypeString(node)) { newType -> viewModel.changeNodeType(newType) }
        Spacer(modifier = Modifier.height(16.dp))

        val onOpenQuickCreate: ((String) -> Unit) -> Unit = { cb -> quickCreateCallback = cb; showQuickCreateDialog = true }

        when (node) {
            is DialogueNode -> DialogueForm(node, allNodes, viewModel, onOpenQuickCreate)
            is CombatNode -> CombatForm(node, allNodes, viewModel, onOpenQuickCreate)
            is ExplorationNode -> ExplorationForm(node, allNodes, viewModel, onOpenQuickCreate)
            is LootNode -> LootForm(node, allNodes, viewModel, onOpenQuickCreate)
            is SkillNode -> SkillForm(node, allNodes, viewModel, onOpenQuickCreate)
            is ItemNode -> ItemForm(node, allNodes, viewModel, onOpenQuickCreate)
        }
    }

    if (showQuickCreateDialog) AddNodeDialog(onDismiss = { showQuickCreateDialog = false }, onConfirm = { type, _ -> val newId = viewModel.createNodeAndGetId(type); quickCreateCallback(newId); showQuickCreateDialog = false })
    if (showDeleteConfirm) DeleteConfirmDialog(title = "Borrar Nodo", onConfirm = { viewModel.deleteNode(node.id); showDeleteConfirm = false }, onDismiss = { showDeleteConfirm = false })
}

// ==========================================
// FORMULARIOS INDIVIDUALES
// ==========================================

@Composable
fun SkillForm(node: SkillNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    // 1. Nombre
    CleanTextField(value = node.name, onValueChange = { viewModel.updateNode(node.copy(name = it)) }, label = "Nombre de la Prueba (Identificador)")
    Spacer(modifier = Modifier.height(8.dp))

    // 2. Categoria
    CleanTextField(value = node.category, onValueChange = { viewModel.updateNode(node.copy(category = it)) }, label = "Categoría / Atributo")
    Spacer(modifier = Modifier.height(8.dp))

    // 3. DC
    CleanTextField(value = node.difficultyClass.toString(), onValueChange = { viewModel.updateNode(node.copy(difficultyClass = it.toIntOrNull() ?: 10)) }, label = "Dificultad (DC)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    Spacer(modifier = Modifier.height(8.dp))

    // 4. Contexto
    CleanTextField(value = node.context, onValueChange = { viewModel.updateNode(node.copy(context = it)) }, label = "Contexto (¿Qué sucede?)", modifier = Modifier.height(80.dp))

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Destinos
    NodeSelector(label = "Si ÉXITO ir a:", currentId = node.successNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(successNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(successNodeId = id)); viewModel.selectNodeToEdit(id) } })
    Spacer(modifier = Modifier.height(8.dp))
    NodeSelector(label = "Si FALLA ir a:", currentId = node.failureNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(failureNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(failureNodeId = id)); viewModel.selectNodeToEdit(id) } })
}

@Composable
fun DialogueForm(node: DialogueNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    CleanTextField(value = node.characterName, onValueChange = { viewModel.updateNode(node.copy(characterName = it)) }, label = "Personaje")
    Spacer(modifier = Modifier.height(8.dp))
    CleanTextField(value = node.dialogueText, onValueChange = { viewModel.updateNode(node.copy(dialogueText = it)) }, label = "Diálogo", modifier = Modifier.height(100.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text("OPCIONES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    node.options.forEachIndexed { index, option ->
        Card(modifier = Modifier.padding(top=8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row { Text("Opción ${index + 1}", style = MaterialTheme.typography.labelSmall); Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = { val newOpts = node.options.toMutableList().apply { removeAt(index) }; viewModel.updateNode(node.copy(options = newOpts)) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } }
                CleanTextField(value = option.text, onValueChange = { val newOpts = node.options.toMutableList().apply { set(index, option.copy(text = it)) }; viewModel.updateNode(node.copy(options = newOpts)) }, label = "Texto", singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                NodeSelector(label = "Ir a:", currentId = option.nextNodeId, allNodes = allNodes, onNodeSelected = { newId -> val newOpts = node.options.toMutableList().apply { set(index, option.copy(nextNodeId = newId)) }; viewModel.updateNode(node.copy(options = newOpts)) }, onCreateNew = { onQuickCreate { createdId -> val newOpts = node.options.toMutableList().apply { set(index, option.copy(nextNodeId = createdId)) }; viewModel.updateNode(node.copy(options = newOpts)); viewModel.selectNodeToEdit(createdId) } })
            }
        }
    }
    Button(onClick = { viewModel.updateNode(node.copy(options = node.options + DialogueOption("", ""))) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("+ Opción") }
}

@Composable
fun CombatForm(node: CombatNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    CleanTextField(value = node.locationDescription, onValueChange = { viewModel.updateNode(node.copy(locationDescription = it)) }, label = "Zona")
    Spacer(modifier = Modifier.height(8.dp))
    NodeSelector(label = "Si GANA ir a:", currentId = node.nextNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(nextNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(nextNodeId = id)); viewModel.selectNodeToEdit(id) } })
    Spacer(modifier = Modifier.height(8.dp))
    NodeSelector(label = "Si PIERDE ir a:", currentId = node.defeatNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(defeatNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(defeatNodeId = id)); viewModel.selectNodeToEdit(id) } })
    Spacer(modifier = Modifier.height(16.dp))
    Text("ENEMIGOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    node.enemies.forEachIndexed { index, enemy -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=8.dp)) { CleanTextField(value = enemy.name, onValueChange = { val newEnemies = node.enemies.toMutableList().apply { set(index, enemy.copy(name = it)) }; viewModel.updateNode(node.copy(enemies = newEnemies)) }, label = "Nombre", modifier = Modifier.weight(1f)); IconButton(onClick = { val newEnemies = node.enemies.toMutableList().apply { removeAt(index) }; viewModel.updateNode(node.copy(enemies = newEnemies)) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } } }
    Button(onClick = { viewModel.updateNode(node.copy(enemies = node.enemies + Enemy("", "Normal", 1))) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("+ Enemigo") }
}

@Composable
fun ExplorationForm(node: ExplorationNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    CleanTextField(value = node.description, onValueChange = { viewModel.updateNode(node.copy(description = it)) }, label = "Descripción", modifier = Modifier.height(100.dp))
    Spacer(modifier = Modifier.height(16.dp)); Text("CAMINOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    node.paths.forEachIndexed { index, path -> Card(modifier = Modifier.padding(top=8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(modifier = Modifier.padding(8.dp)) { Row { Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = { val newPaths = node.paths.toMutableList().apply { removeAt(index) }; viewModel.updateNode(node.copy(paths = newPaths)) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } }; CleanTextField(value = path.description, onValueChange = { val newPaths = node.paths.toMutableList().apply { set(index, path.copy(description = it)) }; viewModel.updateNode(node.copy(paths = newPaths)) }, label = "Texto", singleLine = true); Spacer(modifier = Modifier.height(8.dp)); NodeSelector(label = "Ir a:", currentId = path.nextNodeId, allNodes = allNodes, onNodeSelected = { newId -> val newPaths = node.paths.toMutableList().apply { set(index, path.copy(nextNodeId = newId)) }; viewModel.updateNode(node.copy(paths = newPaths)) }, onCreateNew = { onQuickCreate { id -> val newPaths = node.paths.toMutableList().apply { set(index, path.copy(nextNodeId = id)) }; viewModel.updateNode(node.copy(paths = newPaths)); viewModel.selectNodeToEdit(id) } }) } } }
    Button(onClick = { viewModel.updateNode(node.copy(paths = node.paths + ExplorationPath("", ""))) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("+ Camino") }
}

@Composable
fun ItemForm(node: ItemNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    CleanTextField(value = node.itemName, onValueChange = { viewModel.updateNode(node.copy(itemName = it)) }, label = "Objeto"); Spacer(modifier = Modifier.height(8.dp))
    CleanTextField(value = node.itemDescription, onValueChange = { viewModel.updateNode(node.copy(itemDescription = it)) }, label = "Descripción"); Spacer(modifier = Modifier.height(8.dp))
    CleanTextField(value = node.expositionText, onValueChange = { viewModel.updateNode(node.copy(expositionText = it)) }, label = "Lore", modifier = Modifier.height(80.dp)); Spacer(modifier = Modifier.height(16.dp))
    NodeSelector(label = "Ir a:", currentId = node.nextNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(nextNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(nextNodeId = id)); viewModel.selectNodeToEdit(id) } })
}

@Composable
fun LootForm(node: LootNode, allNodes: List<StoryNode>, viewModel: AdventureViewModel, onQuickCreate: ((String) -> Unit) -> Unit) {
    Text("BOTÍN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary); node.lootTable.forEachIndexed { index, item -> Row(modifier = Modifier.padding(top=8.dp)) { CleanTextField(value = item, onValueChange = { val newLoot = node.lootTable.toMutableList().apply { set(index, it) }; viewModel.updateNode(node.copy(lootTable = newLoot)) }, label = "Item", modifier = Modifier.weight(1f)); IconButton(onClick = { val newLoot = node.lootTable.toMutableList().apply { removeAt(index) }; viewModel.updateNode(node.copy(lootTable = newLoot)) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } } }
    Button(onClick = { viewModel.updateNode(node.copy(lootTable = node.lootTable + "")) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("+ Item") }; Spacer(modifier = Modifier.height(16.dp))
    NodeSelector(label = "Ir a:", currentId = node.nextNodeId, allNodes = allNodes, onNodeSelected = { viewModel.updateNode(node.copy(nextNodeId = it)) }, onCreateNew = { onQuickCreate { id -> viewModel.updateNode(node.copy(nextNodeId = id)); viewModel.selectNodeToEdit(id) } })
}

@Composable
fun NodeTypeSelector(currentType: String, onTypeChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Diálogo", "Combate", "Exploración", "Habilidad", "Objeto", "Loot")
    Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Tipo Actual: $currentType"); Spacer(modifier = Modifier.width(8.dp)); Icon(Icons.Default.KeyboardArrowDown, null) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { type -> DropdownMenuItem(text = { Text(type) }, onClick = { onTypeChanged(type); expanded = false }) } } }
}
@Composable
fun NodeListItem(node: StoryNode, isStart: Boolean, onClick: () -> Unit) {
    val typeColor = getNodeColor(node)
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().border(1.dp, if(isStart) MaterialTheme.colorScheme.secondary else Color.Transparent, MaterialTheme.shapes.medium)) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(getNodeIcon(node), null, tint = typeColor); Spacer(modifier = Modifier.width(16.dp)); Column { Text(getNodeTitle(node).ifBlank { "Sin Título" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(getNodeTypeString(node), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }; Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.tertiary) } }
}
@Composable
fun AddNodeDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var selectedType by remember { mutableStateOf("Diálogo") }; var linkToCurrent by remember { mutableStateOf(true) }; val options = listOf("Diálogo", "Combate", "Exploración", "Habilidad", "Objeto", "Loot")
    Dialog(onDismissRequest = onDismiss) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(modifier = Modifier.padding(24.dp)) { Text("NUEVO NODO", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary); HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)); options.forEach { type -> Row(modifier = Modifier.fillMaxWidth().clickable { selectedType = type }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = (selectedType == type), onClick = { selectedType = type }); Text(type) } }; HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = linkToCurrent, onCheckedChange = { linkToCurrent = it }); Text("Enlazar automáticamente") }; Spacer(modifier = Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Cancelar") }; Button(onClick = { onConfirm(selectedType, linkToCurrent) }) { Text("Crear") } } } } }
}