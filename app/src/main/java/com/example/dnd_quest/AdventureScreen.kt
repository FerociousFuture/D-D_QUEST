package com.example.dnd_quest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureScreen(
    viewModel: AdventureViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    BackHandler { viewModel.handleBackPress() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isEditorMode) Text(uiState.currentAdventure?.title ?: "Juego", style = MaterialTheme.typography.titleMedium)
                    else Text("Modo Edición", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleBackPress() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    TextButton(onClick = { showMap = true }) { Text("MAPA", color = MaterialTheme.colorScheme.onSurface) }
                    TextButton(onClick = { viewModel.toggleEditorMode() }) {
                        Text(if (uiState.isEditorMode) "JUGAR" else "EDITAR", fontWeight = FontWeight.Bold, color = if (uiState.isEditorMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isEditorMode) EditorDashboard(viewModel)
            else GameView(uiState.currentNode, viewModel)
        }

        // MAPA INTERACTIVO
        if (showMap && uiState.currentAdventure != null) {
            AdventureMapDialog(
                adventure = uiState.currentAdventure!!,
                currentNodeId = uiState.currentNode?.id ?: "",
                onNodeSelected = { nodeId ->
                    // Si estamos en modo editor, vamos a editar ese nodo. Si estamos en juego, viajamos (truco de debug) o solo lo visualizamos.
                    // Asumiremos comportamiento de Editor: Ir a editar ese nodo.
                    viewModel.selectNodeToEdit(nodeId)
                    showMap = false // Cerrar mapa
                },
                onDismiss = { showMap = false }
            )
        }
    }
}

// ... (GameView y el resto de vistas de juego DialogueView, CombatView, etc., se mantienen igual que en tu versión anterior)
@Composable
fun GameView(node: StoryNode?, viewModel: AdventureViewModel) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        if (node != null) {
            when (node) {
                is DialogueNode -> DialogueView(node) { id -> viewModel.navigateToNode(id) }
                is CombatNode -> CombatView(node, onVictory = { viewModel.navigateToNode(node.nextNodeId) }, onDefeat = { viewModel.navigateToNode(node.defeatNodeId) })
                is ExplorationNode -> ExplorationView(node) { id -> viewModel.navigateToNode(id) }
                is SkillNode -> SkillView(node = node, onSuccess = { viewModel.navigateToNode(node.successNodeId) }, onFailure = { viewModel.navigateToNode(node.failureNodeId) })
                is LootNode -> LootView(node) { viewModel.navigateToNode(node.nextNodeId) }
                is ItemNode -> ItemView(node) { viewModel.navigateToNode(node.nextNodeId) }
            }
        } else {
            Text("Fin de la aventura.")
        }
    }
}
@Composable
fun CombatView(node: CombatNode, onVictory: () -> Unit, onDefeat: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("⚔️ COMBATE ⚔️", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally)); Spacer(modifier = Modifier.height(16.dp)); Text("Lugar: ${node.locationDescription}", style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.height(16.dp)); LazyColumn(modifier = Modifier.weight(1f)) { items(node.enemies) { enemy -> ListItem(headlineContent = { Text("${enemy.name} (x${enemy.count})") }, supportingContent = { Text("Tipo: ${enemy.type}") }, leadingContent = { Text("💀", fontSize = 24.sp) }); HorizontalDivider() } }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { if (node.defeatNodeId.isNotEmpty()) { Button(onClick = onDefeat, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.weight(1f)) { Text("Huir / Derrota") } }; Button(onClick = onVictory, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f)) { Text("Victoria") } }
    }
}
@Composable
fun DialogueView(node: DialogueNode, onOptionSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) { Text(text = node.characterName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(8.dp)); Card(modifier = Modifier.fillMaxWidth()) { Text(text = "\"${node.dialogueText}\"", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, modifier = Modifier.padding(16.dp)) }; Spacer(modifier = Modifier.height(24.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(node.options) { option -> Button(onClick = { onOptionSelected(option.nextNodeId) }, modifier = Modifier.fillMaxWidth()) { Text(option.text) } } } }
}
@Composable
fun ExplorationView(node: ExplorationNode, onPathSelected: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Exploración", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary); Spacer(modifier = Modifier.height(16.dp)); Text(node.description, style = MaterialTheme.typography.bodyLarge); Spacer(modifier = Modifier.height(32.dp)); node.paths.forEach { path -> OutlinedButton(onClick = { onPathSelected(path.nextNodeId) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(path.description) } } }
}
@Composable
fun SkillView(node: SkillNode, onSuccess: () -> Unit, onFailure: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("🎲 PRUEBA", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary); Text("${node.category} (DC ${node.difficultyClass})", fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); Text(node.context, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Button(onClick = onSuccess, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Éxito") }; Button(onClick = onFailure, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))) { Text("Fallo") } } } }
}
@Composable
fun LootView(node: LootNode, onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("BOTÍN", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary); Spacer(modifier = Modifier.height(16.dp)); Card(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) { node.lootTable.forEach { item -> Text("• $item", style = MaterialTheme.typography.bodyLarge) } } }; Spacer(modifier = Modifier.height(32.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Recoger") } }
}
@Composable
fun ItemView(node: ItemNode, onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("OBJETO", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary); Text(node.itemName, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary); Text(node.itemDescription, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic); Spacer(modifier = Modifier.height(32.dp)); Button(onClick = onContinue) { Text("Guardar") } }
}