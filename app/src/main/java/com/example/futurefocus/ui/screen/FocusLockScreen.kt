package com.example.futurefocus.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// ============================================================================
// FocusLockScreen — Clean minimalist redesign (Notion/Linear-inspired)
// Visual layer only. All state, parameters, and business logic unchanged.
// ============================================================================

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
            Text("Session tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---- Header ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Text(
                        text = "SESI AKTIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = session.goalTitle ?: "Sesi Fokus Bebas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ---- Minimal Progress Circle ----
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = countdownLabel(remainingSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tersisa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Motivation & Control ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Tetap fokus. Satu sesi selesai lebih bernilai daripada ribuan distraksi.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    onClick = { exitMessage = onExitAttempt() },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Menyerah?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // ---- Minimalist Dialog ----
    exitMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "!", fontSize = 24.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = message.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (message.quote != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "\"${message.quote}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Lanjut Fokus", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = if (message.level >= 4) {
                {
                    TextButton(
                        onClick = {
                            FocusSessionTracker.endSession()
                            onGiveUp()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Menyerah",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}