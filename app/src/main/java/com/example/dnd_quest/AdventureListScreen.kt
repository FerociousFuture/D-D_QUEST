package com.example.dnd_quest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureListScreen(
    viewModel: AdventureViewModel,
    onAdventureSelected: (String) -> Unit // Deprecado, pero mantenido por compatibilidad
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var adventureToDelete by remember { mutableStateOf<String?>(null) } // ID de la aventura a borrar

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Aventuras") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Aventura")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.adventureList.isEmpty()) {
                Text(text = "No hay aventuras aún.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.adventureList) { adventure ->
                        AdventureCard(
                            adventure = adventure,
                            onPlay = { viewModel.selectAdventure(adventure.id, startInEditor = false) },
                            onEdit = { viewModel.selectAdventure(adventure.id, startInEditor = true) },
                            onDelete = { adventureToDelete = adventure.id } // Pedimos confirmar
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateAdventureDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, desc ->
                    viewModel.createNewAdventure(title, desc)
                    showCreateDialog = false
                }
            )
        }

        // DIÁLOGO DE CONFIRMACIÓN BORRAR
        if (adventureToDelete != null) {
            AlertDialog(
                onDismissRequest = { adventureToDelete = null },
                title = { Text("¿Borrar Aventura?") },
                text = { Text("Se perderá todo el progreso y nodos de esta historia.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAdventure(adventureToDelete!!)
                            adventureToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Borrar") }
                },
                dismissButton = {
                    TextButton(onClick = { adventureToDelete = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun AdventureCard(adventure: Adventure, onPlay: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(adventure.title, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(adventure.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("JUGAR") }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) { Text("EDITAR") }
            }
        }
    }
}

@Composable
fun CreateAdventureDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Aventura") },
        text = { Column { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()) } },
        confirmButton = { Button(onClick = { onConfirm(title, description) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}