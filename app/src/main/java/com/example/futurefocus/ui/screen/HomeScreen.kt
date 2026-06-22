package com.example.futurefocus.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.model.DailyGoal
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.Goal
import com.example.futurefocus.ui.component.GoalListItem
import com.example.futurefocus.ui.component.PrimaryActionButton
import com.example.futurefocus.ui.component.ProgressBar
import com.example.futurefocus.ui.component.TextAction
import com.example.futurefocus.utils.AuthManager
import com.example.futurefocus.utils.minutesLabel
import com.google.firebase.auth.FirebaseAuth

// ============================================================================
// HomeScreen — Clean minimalist redesign (Notion/Linear-inspired)
// Visual layer only. Logic remains untouched.
// ============================================================================

@Composable
fun HomeScreen(
    stats: FocusStats,
    dailyGoal: DailyGoal,
    goals: List<Goal>,
    achievements: List<Achievement> = emptyList(),
    showSessionFailed: Boolean = false,
    completedGoalId: String? = null,
    onDismissSessionFailed: () -> Unit = {},
    onDismissCompletedGoal: () -> Unit = {},
    onUpdateDailyGoal: (Int) -> Unit,
    onStartFocus: () -> Unit,
    onOpenCreateGoal: () -> Unit,
    onProfileClick: () -> Unit = {},
    onGoalClick: (String) -> Unit
) {
    var showProfile by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(AuthManager.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    val goalProgress = stats.todayFocusMinutes.toFloat() / dailyGoal.targetMinutes.coerceAtLeast(1).toFloat()
    val activeGoals = goals.filter { !it.isCompleted }
    val progressPercent = (goalProgress * 100).toInt().coerceAtMost(100)
    val isGoalComplete = goalProgress >= 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Padding for bottom CTA
        ) {
            // ---- HEADER SECTION ----
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "FutureFocus",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Siap menjaga fokus hari ini?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(onClick = { showProfile = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.email?.firstOrNull()?.uppercase() ?: "U"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ---- HERO CARD (DAILY GOAL) ----
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Harian",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusTag(
                                label = "Streak: ${stats.focusStreak} Hari",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        GoalRing(
                            progress = goalProgress,
                            ringColor = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isGoalComplete) {
                                    Text(
                                        text = "Tercapai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${minutesLabel(stats.todayFocusMinutes)} dari ${minutesLabel(dailyGoal.targetMinutes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sesuaikan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showGoalDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        ProgressBar(
                            progress = goalProgress,
                            height = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // ---- ACTIVE GOALS HEADER ----
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Goal Aktif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeGoals.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${activeGoals.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TextAction(
                        text = "Lihat Semua",
                        onClick = { /* TODO */ }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- ACTIVE GOALS LIST OR EMPTY STATE ----
            if (activeGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenCreateGoal)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Belum ada goal aktif",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Ketuk untuk membuat goal baru",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeGoals, key = { it.id }) { goal ->
                    GoalListItem(
                        goal = goal,
                        onClick = { onGoalClick(goal.id) },
                        progressColor = MaterialTheme.colorScheme.primary, // Solid consistent color
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }

        // ---- FLOATING BOTTOM ACTION (CTA) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PrimaryActionButton(
                text = "Mulai Fokus",
                onClick = onStartFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        if (showProfile) {
            ProfileScreen(
                achievements = achievements,
                onBack = { showProfile = false }
            )
        }
    }

    // ---- DIALOGS ----
    if (showSessionFailed) {
        MinimalDialog(
            icon = "!",
            title = "Sesi Gagal",
            text = "Sesi fokus sebelumnya gagal diselesaikan karena aplikasi ditinggalkan.",
            confirmText = "Mengerti",
            onConfirm = onDismissSessionFailed,
            isDestructive = true
        )
    }

    if (completedGoalId != null) {
        val goal = goals.firstOrNull { it.id == completedGoalId }
        MinimalDialog(
            icon = "✓",
            title = "Goal Selesai",
            text = "Kamu telah menyelesaikan goal:\n${goal?.title ?: ""}",
            confirmText = "Lanjut",
            onConfirm = onDismissCompletedGoal
        )
    }

    if (showGoalDialog) {
        var goalInput by remember(showGoalDialog) { mutableStateOf(dailyGoal.targetMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏱️", fontSize = 20.sp)
                }
            },
            title = {
                Text(
                    text = "Set Target Harian",
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter(Char::isDigit).take(4) },
                    label = { Text("Target fokus", style = MaterialTheme.typography.bodyMedium) },
                    suffix = { Text("menit", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateDailyGoal(goalInput.toIntOrNull() ?: dailyGoal.targetMinutes)
                        showGoalDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoalDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ---- MINIMALIST COMPONENTS ----

@Composable
private fun StatusTag(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GoalRing(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    size: Dp = 132.dp,
    strokeWidth: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (clamped > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

@Composable
private fun MinimalDialog(
    icon: String,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}