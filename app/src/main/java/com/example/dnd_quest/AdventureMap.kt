package com.example.dnd_quest

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.hypot

// --- Modelo Visual ---
data class VisualNode(
    val id: String,
    val type: String,
    val title: String,
    val x: Float,
    val y: Float,
    val childrenIds: List<String>
)

// --- Composable del Diálogo ---
@Composable
fun AdventureMapDialog(
    adventure: Adventure,
    currentNodeId: String,
    onNodeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
            Box(modifier = Modifier.fillMaxSize()) {

                AdventureGraphView(adventure, currentNodeId, onNodeSelected)

                // Botón Cerrar
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset((-16).dp, 16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }

                // Título
                Text(
                    "MAPA (Toca para editar)",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

// --- Vista del Grafo ---
@Composable
fun AdventureGraphView(adventure: Adventure, currentNodeId: String, onNodeClick: (String) -> Unit) {
    val visualNodes = remember(adventure) { calculateLayout(adventure) }

    var scale by remember { mutableFloatStateOf(0.8f) }
    var offset by remember { mutableStateOf(Offset(100f, 100f)) }

    // Tamaños grandes
    val nodeRadius = 50f
    val selectionRadius = 60f
    val hitBoxRadius = 70f
    val connectionWidth = 5f

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 1. Detectar Tap
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val mapX = (tapOffset.x - offset.x) / scale
                    val mapY = (tapOffset.y - offset.y) / scale

                    val clickedNode = visualNodes.values.find { node ->
                        hypot(node.x - mapX, node.y - mapY) < hitBoxRadius
                    }

                    if (clickedNode != null) {
                        onNodeClick(clickedNode.id)
                    }
                }
            }
            // 2. Detectar Zoom/Pan
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    scale = scale.coerceIn(0.3f, 3.0f)
                    offset += pan
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
        ) {
            // Dibujar Líneas
            visualNodes.values.forEach { parent ->
                parent.childrenIds.forEach { childId ->
                    visualNodes[childId]?.let { child ->
                        drawLine(
                            color = Color.Gray,
                            start = Offset(parent.x, parent.y),
                            end = Offset(child.x, child.y),
                            strokeWidth = connectionWidth
                        )
                    }
                }
            }

            // Dibujar Nodos
            visualNodes.values.forEach { node ->
                val isCurrent = node.id == currentNodeId
                // Usamos nuestra función privada para evitar conflictos de tipos
                val color = getMapNodeColor(node.type)

                drawCircle(
                    color = if (isCurrent) Color.White else color,
                    radius = nodeRadius,
                    center = Offset(node.x, node.y)
                )

                if (isCurrent) {
                    drawCircle(
                        color = Color.Red,
                        radius = selectionRadius,
                        center = Offset(node.x, node.y),
                        style = Stroke(width = 6f)
                    )
                }

                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        node.title.take(15),
                        node.x,
                        node.y + nodeRadius + 40f,
                        textPaint
                    )
                }
            }
        }
    }
}

// --- Lógica del Layout ---
fun calculateLayout(adventure: Adventure): Map<String, VisualNode> {
    val nodes = adventure.nodes
    val startId = adventure.startNodeId
    val result = mutableMapOf<String, VisualNode>()

    val levels = mutableMapOf<Int, MutableList<String>>()
    val visited = mutableSetOf<String>()

    // Especificamos tipos explícitos para evitar error de inferencia
    val queue: ArrayDeque<Pair<String, Int>> = ArrayDeque()

    if (nodes.containsKey(startId)) {
        queue.add(startId to 0)
        visited.add(startId)
    }

    while (queue.isNotEmpty()) {
        val item = queue.removeFirst()
        val id = item.first
        val level = item.second

        levels.computeIfAbsent(level) { mutableListOf() }.add(id)

        nodes[id]?.let { node ->
            // Usamos la función privada local
            getMapChildrenIds(node).forEach { childId ->
                if (childId !in visited && nodes.containsKey(childId)) {
                    visited.add(childId)
                    queue.add(childId to level + 1)
                }
            }
        }
    }

    val orphans = nodes.keys - visited
    if (orphans.isNotEmpty()) {
        levels.computeIfAbsent(levels.keys.maxOrNull()?.plus(1) ?: 1) { mutableListOf() }.addAll(orphans)
    }

    val nodeSpacingX = 220f
    val nodeSpacingY = 250f

    levels.forEach { (level, ids) ->
        val rowWidth = ids.size * nodeSpacingX
        var startX = -(rowWidth / 2) + 500f

        ids.forEach { id ->
            val node = nodes[id]!!
            result[id] = VisualNode(
                id = id,
                type = getMapTypeString(node),
                title = getMapNodeTitle(node),
                x = startX,
                y = (level * nodeSpacingY) + 150f,
                childrenIds = getMapChildrenIds(node)
            )
            startX += nodeSpacingX
        }
    }
    return result
}

// --- FUNCIONES PRIVADAS (Para evitar conflictos con el resto del proyecto) ---

private fun getMapChildrenIds(node: StoryNode): List<String> {
    return when(node) {
        is DialogueNode -> node.options.map { it.nextNodeId }
        is ExplorationNode -> node.paths.map { it.nextNodeId }
        is CombatNode -> listOf(node.nextNodeId, node.defeatNodeId).filter { it.isNotEmpty() }
        is LootNode -> listOf(node.nextNodeId).filter { it.isNotEmpty() }
        is ItemNode -> listOf(node.nextNodeId).filter { it.isNotEmpty() }
        is SkillNode -> listOf(node.successNodeId, node.failureNodeId).filter { it.isNotEmpty() }
        // Caso por defecto para cualquier otro nodo futuro
        else -> emptyList()
    }
}

private fun getMapNodeTitle(node: StoryNode): String {
    return when(node) {
        is DialogueNode -> "Diálogo"
        is CombatNode -> "Combate"
        is ExplorationNode -> "Exploración"
        is SkillNode -> "Habilidad"
        is ItemNode -> "Objeto"
        is LootNode -> "Loot"
        else -> "Nodo"
    }
}

private fun getMapTypeString(node: StoryNode): String = when(node) {
    is DialogueNode -> "Diálogo"
    is CombatNode -> "Combate"
    is ExplorationNode -> "Exploración"
    is SkillNode -> "Habilidad"
    is ItemNode -> "Objeto"
    is LootNode -> "Loot"
    else -> "Desconocido"
}

private fun getMapNodeColor(type: String): Color = when(type) {
    "Combate" -> Color(0xFFB71C1C)
    "Exploración" -> Color(0xFF2E7D32)
    "Habilidad" -> Color(0xFF1565C0)
    "Objeto", "Loot" -> Color(0xFFFFD700)
    else -> Color.Gray
}