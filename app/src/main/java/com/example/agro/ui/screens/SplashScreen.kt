package com.example.agro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LeafGreen,
                        DarkGreen
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value * pulseScale)
                    .alpha(alpha.value),
                shape = CircleShape,
                color = White.copy(alpha = 0.25f),
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, White.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🌱",
                        fontSize = 85.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Agro",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = White,
                modifier = Modifier
                    .alpha(alpha.value)
                    .scale(scale.value)
            )
            
            Text(
                text = "Smart Farming Solutions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = SoftYellow,
                modifier = Modifier.alpha(alpha.value)
            )
        }
        
        // Optional: Add a small loading indicator or text at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(alpha.value)
        ) {
            Text(
                text = "Loading Your Farm...",
                color = White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}
