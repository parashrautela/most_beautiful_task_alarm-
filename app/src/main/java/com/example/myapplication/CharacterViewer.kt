package com.example.myapplication

import android.graphics.PixelFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.Scene
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.node.ModelNode
import dev.romainguy.kotlin.math.Float3
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskBubble(
    title: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 86.dp, height = 35.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F8F8), Color(0xFFE4E4E4))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 0.4.dp,
                color = Color(0xFFD4D4D4),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                fontFamily = AppSatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = time,
                fontFamily = AppSatoshiFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 9.sp,
                color = Color(0xFF6C6C6C),
                maxLines = 1
            )
        }
    }
}

@Composable
fun HomeHeroSection(
    tasks: List<TaskAlarm>,
    modifier: Modifier = Modifier
) {
    var rotationY by remember { mutableStateOf(180f) } // start Y rotation

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(283.dp)
            .background(Color.Transparent)
    ) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val cameraNode = rememberCameraNode(engine)

        // Set camera position relative to origin
        LaunchedEffect(cameraNode) {
            cameraNode.position = Float3(0f, 0.5f, 3.5f)
        }

        // Load the model instance on the main thread coroutine context (same thread that initialized Filament)
        var modelInstance by remember { mutableStateOf<com.google.android.filament.gltfio.FilamentInstance?>(null) }
        LaunchedEffect(modelLoader) {
            modelInstance = modelLoader.createModelInstance("character.glb")
        }

        val modelNode = remember(modelInstance) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.8f
                ).apply {
                    position = Float3(0f, -0.8f, 0f)
                }
            }
        }

        // Apply Y rotation when rotationY state changes
        LaunchedEffect(rotationY, modelNode) {
            modelNode?.rotation = Float3(0f, rotationY, 0f)
        }

        // 3D Scene View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity: Dragging left-right rotates on Y-axis
                        rotationY -= dragAmount.x * 0.5f
                    }
                }
        ) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                cameraNode = cameraNode,
                childNodes = remember(modelNode) { listOfNotNull(modelNode) },
                isOpaque = false,
                onViewCreated = {
                    this.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.holder.setFormat(PixelFormat.TRANSLUCENT)
                    this.uiHelper.isOpaque = false
                    this.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
                    this.scene.skybox = null
                }
            )
        }

        // Overlay Task Bubbles at designated coordinate offsets
        val bubblePositions = listOf(
            OffsetPercent(0.18f, 46.dp),   // Bubble 1
            OffsetPercent(0.44f, 11.dp),   // Bubble 2
            OffsetPercent(0.73f, 59.dp),   // Bubble 3
            OffsetPercent(0.82f, 143.dp),  // Bubble 4
            OffsetPercent(0.05f, 149.dp),  // Bubble 5
            OffsetPercent(0.70f, 4.dp)     // Bubble 6
        )

        val activeTasks = tasks.sortedBy { it.dateTime }.take(6)
        activeTasks.forEachIndexed { index, task ->
            val pos = bubblePositions.getOrNull(index) ?: return@forEachIndexed
            val ldt = LocalDateTime.parse(task.dateTime)
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            val timeStr = ldt.format(timeFormatter)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = pos.y)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = (pos.xPercent * 320).dp) // Scaled within typical phone width
                ) {
                    TaskBubble(
                        title = task.title,
                        time = timeStr
                    )
                }
            }
        }
    }
}

data class OffsetPercent(
    val xPercent: Float,
    val y: androidx.compose.ui.unit.Dp
)
