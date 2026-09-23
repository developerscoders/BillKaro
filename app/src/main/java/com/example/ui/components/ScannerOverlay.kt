package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScannerOverlay(
    scanMode: String, // "business_card", "supplier_bill", "barcode"
    hint: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val lineOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineOffsetY"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val boxWidth = when (scanMode) {
                "barcode" -> canvasWidth * 0.85f
                "supplier_bill" -> canvasWidth * 0.85f
                else -> canvasWidth * 0.85f // business_card
            }

            val boxHeight = when (scanMode) {
                "barcode" -> 160f.dp.toPx()
                "supplier_bill" -> 320f.dp.toPx()
                else -> 220f.dp.toPx() // business_card
            }

            val left = (canvasWidth - boxWidth) / 2f
            val top = (canvasHeight - boxHeight) / 2f

            // Semi-transparent overlay outside frame
            drawRect(
                color = Color.Black.copy(alpha = 0.6f)
            )

            // Clear frame inside
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(24f, 24f),
                blendMode = BlendMode.Clear
            )

            // Border stroke
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 6f)
            )

            // Animated scan laser line
            val currentLineY = top + (boxHeight * lineOffsetY)
            drawLine(
                color = Color(0xFF4F46E5),
                start = Offset(left + 20f, currentLineY),
                end = Offset(left + boxWidth - 20f, currentLineY),
                strokeWidth = 8f
            )
        }

        // Hint text under frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 150.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }
    }
}
