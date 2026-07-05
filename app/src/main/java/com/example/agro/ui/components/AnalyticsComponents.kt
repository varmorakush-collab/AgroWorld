package com.example.agro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AttractiveBarChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxVal = (data.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, value ->
                    val targetHeight = (value / maxVal).toFloat().coerceIn(0.05f, 1f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = targetHeight,
                        animationSpec = tween(durationMillis = 1000, delayMillis = index * 100, easing = FastOutSlowInEasing),
                        label = "barHeight"
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(animatedHeight)
                                .width(24.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(barColor, barColor.copy(alpha = 0.5f))
                                    ),
                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = labels.getOrElse(index) { "" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedPieChart(
    values: List<Float>,
    colors: List<Color>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val total = values.sum().coerceAtLeast(1f)
    val startAngle = -90f
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "pieAnimation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var currentStartAngle = startAngle
                    values.forEachIndexed { index, value ->
                        val sweepAngle = (value / total) * 360f * animatedProgress
                        drawArc(
                            color = colors.getOrElse(index) { Color.Gray },
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 30f, cap = StrokeCap.Round)
                        )
                        currentStartAngle += (value / total) * 360f
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${total.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(colors.getOrElse(index) { Color.Gray }, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${values[index].toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatGrid(stats: List<StatData>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEach { rowStats ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowStats.forEach { stat ->
                    StatItem(stat, Modifier.weight(1f))
                }
                if (rowStats.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

data class StatData(val title: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Composable
fun StatItem(stat: StatData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = stat.color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, stat.color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = stat.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stat.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(stat.value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = stat.color)
        }
    }
}
