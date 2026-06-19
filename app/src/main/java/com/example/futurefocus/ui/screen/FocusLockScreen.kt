package com.example.futurefocus.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futurefocus.MainActivity
import com.example.futurefocus.model.ExitMessage
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.service.FocusForegroundService
import com.example.futurefocus.service.FocusSessionTracker
import com.example.futurefocus.utils.countdownLabel
import kotlinx.coroutines.delay

@Composable
fun FocusLockScreen(
    session: FocusSession?,
    onExitAttempt: () -> ExitMessage,
    onGiveUp: () -> Unit,
    onCompleted: () -> Unit,
    onBackgroundExit: () -> Unit = {}
) {
    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Session tidak ditemukan")
        }
        return
    }

    val totalSeconds = session.durationMinutes * 60
    var remainingSeconds by remember(session.id) { mutableIntStateOf(totalSeconds) }
    var exitMessage by remember { mutableStateOf<ExitMessage?>(null) }
    val context = LocalContext.current

    val progress = 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    DisposableEffect(session.id) {
        FocusSessionTracker.startSession(session.id, session.durationMinutes)
        (context as? MainActivity)?.setExcludedFromRecentsForFocus(true)
        FocusSessionTracker.onForceExit = {
            onBackgroundExit()
        }
        FocusSessionTracker.onNavigationAttempt = {
            if (exitMessage == null) {
                exitMessage = onExitAttempt()
            }
        }

        val intent = Intent(context, FocusForegroundService::class.java).putExtra(
            FocusForegroundService.EXTRA_DURATION_MINUTES,
            session.durationMinutes
        )
        context.startForegroundService(intent)

        onDispose {
            (context as? MainActivity)?.setExcludedFromRecentsForFocus(false)
            context.stopService(intent)
        }
    }

    LaunchedEffect(session.id) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        FocusSessionTracker.endSession()
        onCompleted()
    }

    BackHandler {
        if (FocusSessionTracker.consumeRedirectBackIfPending()) {
            return@BackHandler
        }

        if (exitMessage == null) {
            exitMessage = onExitAttempt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SESSION ACTIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = session.goalTitle ?: "Focus Session",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Progress Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = countdownLabel(remainingSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "tersisa",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Motivation & Control
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "Tetap fokus. Satu sesi selesai lebih bernilai daripada ribuan distraksi.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                TextButton(
                    onClick = {
                        exitMessage = onExitAttempt()
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Berhenti?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    exitMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = message.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (message.quote != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "\"${message.quote}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                                Text(
                                    text = "— ${message.author ?: "ZenQuotes"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { exitMessage = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lanjut Fokus")
                }
            },
            dismissButton = if (message.level >= 4) {
                {
                    TextButton(
                        onClick = {
                            FocusSessionTracker.endSession()
                            onGiveUp()
                        }
                    ) {
                        Text(
                            text = "Menyerah",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}
