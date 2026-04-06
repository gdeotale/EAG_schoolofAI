package com.splittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentSlide by remember { mutableStateOf(0) }

    val slides = listOf(
        Pair("Welcome to Split Tracker! \uD83D\uDC4B", "Create dynamic groups and track shared expenses with your friends."),
        Pair("Smart Splitting \uD83E\uDDEE", "Select exactly who paid and instantly let the app calculate everyone's exact net balance."),
        Pair("Settle Up \uD83D\uDCB8", "View beautiful balance charts and eliminate debts with a single tap. Let's get started!")
    )

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onFinish) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (currentSlide < slides.size - 1) {
                                currentSlide++
                            } else {
                                onFinish()
                            }
                        }
                    ) {
                        Text(if (currentSlide == slides.size - 1) "Done" else "Next")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slides[currentSlide].first,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = slides[currentSlide].second,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            // Step indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in slides.indices) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (i == currentSlide) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
