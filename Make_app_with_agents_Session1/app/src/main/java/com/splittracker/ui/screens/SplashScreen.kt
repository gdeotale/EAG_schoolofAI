package com.splittracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splittracker.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SplashScreen(onAnimationFinish: () -> Unit) {
    // Animatable scale value for our bounce/zoom effect
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Zoom In for 1 second
        scale.animateTo(
            targetValue = 1.3f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        // Zoom Out for 1 second
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        // Trigger the callback to move to the dashboard
        onAnimationFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF673AB7)), // Deep purple background
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .border(
                    width = 4.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF9933), Color.White, Color(0xFF138808))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("💰", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Split Tracker",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
