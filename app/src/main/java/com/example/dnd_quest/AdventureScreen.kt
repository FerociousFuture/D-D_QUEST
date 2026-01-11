package com.example.dnd_quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentAdventure?.title ?: "Cargando...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // BOTÓN PARA ACTIVAR/DESACTIVAR MODO EDITOR
                    IconButton(onClick = { viewModel.toggleEditorMode() }) {
                        Text(
                            text = if (uiState.isEditorMode) "JUGAR" else "EDITAR",
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isEditorMode) Color.Red else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {

            // 1. SI ESTAMOS EN MODO EDITOR, MOSTRAMOS EL PANEL DE EDICIÓN ARRIBA
            // (Asegúrate de haber creado el archivo AdventureEditor.kt con la función NodeEditorPanel)
            if (uiState.isEditorMode) {
                NodeEditorPanel(viewModel = viewModel)
            }

            // 2. MOSTRAMOS LA VISTA DEL JUEGO (PREVISUALIZACIÓN)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val node = uiState.currentNode
                if (node != null) {
                    when (node) {
                        is DialogueNode -> DialogueView(node) { id -> viewModel.navigateToNode(id) }
                        is CombatNode -> CombatView(node) { id -> viewModel.navigateToNode(id) }
                        is ExplorationNode -> ExplorationView(node) { id -> viewModel.navigateToNode(id) }
                        is SkillNode -> SkillView(
                            node = node,
                            onSuccess = { viewModel.navigateToNode(node.successNodeId) },
                            onFailure = { viewModel.navigateToNode(node.failureNodeId) }
                        )
                        is LootNode -> LootView(node) { viewModel.navigateToNode(node.nextNodeId) }
                        is ItemNode -> ItemView(node) { viewModel.navigateToNode(node.nextNodeId) }
                    }
                } else {
                    Text("Fin de la aventura o error.")
                }
            }
        }
    }
}

// --- VISTAS ESPECÍFICAS PARA CADA NODO (Visualización del Jugador) ---

@Composable
fun DialogueView(node: DialogueNode, onOptionSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = node.characterName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "\"${node.dialogueText}\"",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Opciones:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(node.options) { option ->
                Button(
                    onClick = { onOptionSelected(option.nextNodeId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option.text)
                }
            }
        }
    }
}

@Composable
fun CombatView(node: CombatNode, onVictory: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "⚔️ ENCUENTRO DE COMBATE ⚔️",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Red,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ubicación: ${node.locationDescription}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enemigos:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(node.enemies) { enemy ->
                ListItem(
                    headlineContent = { Text("${enemy.name} (x${enemy.count})") },
                    supportingContent = { Text("Tipo: ${enemy.type}") },
                    leadingContent = { Text("💀", fontSize = 24.sp) }
                )
                HorizontalDivider()
            }
        }

        Button(
            onClick = { onVictory(node.nextNodeId) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Combate Terminado (Continuar)")
        }
    }
}

@Composable
fun ExplorationView(node: ExplorationNode, onPathSelected: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Exploración", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(node.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        node.paths.forEach { path ->
            OutlinedButton(
                onClick = { onPathSelected(path.nextNodeId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(path.description)
            }
        }
    }
}

@Composable
fun SkillView(node: SkillNode, onSuccess: () -> Unit, onFailure: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎲 PRUEBA DE HABILIDAD", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Prueba: ${node.category}", fontWeight = FontWeight.Bold)
            Text(
                "Dificultad (DC): ${node.difficultyClass}",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(node.context, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Éxito")
                }
                Button(
                    onClick = onFailure,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Fallo")
                }
            }
        }
    }
}

@Composable
fun LootView(node: LootNode, onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "💎 BOTÍN ENCONTRADO",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFFF9800)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                node.lootTable.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• ", fontSize = 20.sp)
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Recoger y Continuar")
        }
    }
}

@Composable
fun ItemView(node: ItemNode, onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("OBJETO CLAVE", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            node.itemName,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            node.itemDescription,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(node.expositionText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onContinue) { Text("Guardar en Inventario") }
    }
}