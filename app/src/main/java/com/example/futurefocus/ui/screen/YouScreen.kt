package com.example.futurefocus.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.model.DailyBreakdown
import com.example.futurefocus.model.DailyGoal
import com.example.futurefocus.model.DateRange
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.Goal
import com.example.futurefocus.model.PeriodStats
import com.example.futurefocus.model.SessionStatus
import com.example.futurefocus.ui.component.ProgressBar
import com.example.futurefocus.utils.minutesLabel
import com.example.futurefocus.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================================================
// YouScreen — Clean minimalist redesign (Notion/Linear-inspired)
// Visual layer only. All state, parameters, and business logic unchanged.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    focusViewModel: FocusViewModel,
    onBack: () -> Unit,
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val stats by remember { mutableStateOf(focusViewModel.stats()) }
    val dailyGoal by focusViewModel.dailyGoal.collectAsState()
    val goals by focusViewModel.goals.collectAsState()
    val sessions by focusViewModel.sessions.collectAsState()
    val sessionAchievements by focusViewModel.sessionAchievements.collectAsState()
    val tabs = listOf("Progress", "Aktivitas")
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(com.example.futurefocus.utils.AuthManager.currentUser) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // ---- Header: flat, minimal ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Kamu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
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

        // ---- Tabs: underline style, no pill/gradient ----
        MinimalTabRow(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        when (selectedTab) {
            0 -> ProgressTab(
                modifier = Modifier.weight(1f),
                focusViewModel = focusViewModel,
                stats = stats,
                dailyGoal = dailyGoal,
                goals = goals,
                onAchievementClick = { selectedAchievement = it }
            )
            1 -> ActivitiesTab(
                modifier = Modifier.weight(1f),
                sessions = sessions,
                sessionAchievements = sessionAchievements,
                onAchievementClick = { selectedAchievement = it }
            )
        }
        }

        if (showProfile) {
            ProfileScreen(
                achievements = focusViewModel.allAchievements.filter { it.isUnlocked },
                onBack = { showProfile = false }
            )
        }

        selectedAchievement?.let { ach ->
        AlertDialog(
            onDismissRequest = { selectedAchievement = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ach.type.icon, fontSize = 22.sp)
                }
            },
            title = {
                Text(
                    text = ach.title,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = ach.description,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedAchievement = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
    }
}

@Composable
private fun MinimalTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Column(
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .padding(end = 24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 24.dp else 0.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTab(
    modifier: Modifier = Modifier,
    focusViewModel: FocusViewModel,
    stats: FocusStats,
    dailyGoal: DailyGoal,
    goals: List<Goal>,
    onAchievementClick: (Achievement) -> Unit = {}
) {
    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var showHours by remember { mutableStateOf(true) }

    val monthRange = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        DateRange(start, c.timeInMillis)
    }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(selectedYear, selectedMonth, 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    var periodStats by remember { mutableStateOf(PeriodStats()) }

    LaunchedEffect(monthRange.startMillis, monthRange.endMillis) {
        periodStats = focusViewModel.statsForPeriod(monthRange)
    }

    val dailyMap = remember(periodStats) {
        periodStats.dailyBreakdown.associateBy { it.dateKey }
    }

    val monthLabel = remember(selectedYear, selectedMonth) {
        val c = Calendar.getInstance()
        c.set(selectedYear, selectedMonth, 1)
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(c.time)
            .replaceFirstChar { it.uppercase() }
    }

    val maxValue = remember(periodStats, showHours) {
        val values = periodStats.dailyBreakdown.map {
            if (showHours) it.totalMinutes / 60f else it.totalMinutes.toFloat()
        }
        (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---- Month Selector ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                selectedMonth--
                if (selectedMonth < 0) { selectedMonth = 11; selectedYear-- }
            }) {
                Text("◀", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = {
                selectedMonth++
                if (selectedMonth > 11) { selectedMonth = 0; selectedYear++ }
            }) {
                Text("▶", style = MaterialTheme.typography.titleMedium)
            }
        }

        // ---- Toggle Hours / Minutes ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showHours,
                onClick = { showHours = true },
                label = { Text("Jam") },
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !showHours,
                onClick = { showHours = false },
                label = { Text("Menit") },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // ---- Bar Chart ----
        val barColor = MaterialTheme.colorScheme.primary
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        val chartHeight = 220.dp
        val hasData = periodStats.dailyBreakdown.any { it.totalMinutes > 0 }
        val textMeasurer = rememberTextMeasurer()

        val niceMax = remember(maxValue) {
            val raw = maxValue
            when {
                raw <= 0f -> 1f
                raw <= 1f -> 1f
                raw <= 5f -> 5f
                raw <= 10f -> 10f
                raw <= 30f -> 30f
                raw <= 60f -> 60f
                else -> kotlin.math.ceil(raw / 60f) * 60f
            }
        }

        val yLabelStyle = TextStyle(fontSize = 9.sp, color = textColor)
        val xLabelStyle = TextStyle(fontSize = 9.sp, color = textColor)
        val ySteps = 4

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (!hasData) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(chartHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Belum ada data fokus",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Mulai sesi fokus untuk melihat statistik",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val leftMargin = 32.dp.toPx()
                        val bottomMargin = 16.dp.toPx()
                        val chartW = w - leftMargin
                        val chartH = h - bottomMargin
                        val barCount = daysInMonth
                        val barSpacing = 1.dp.toPx()
                        val barWidth = ((chartW - barSpacing * (barCount + 1)) / barCount).coerceAtLeast(1.dp.toPx())

                        // ---- Grid lines ----
                        for (i in 0..ySteps) {
                            val y = chartH * i / ySteps
                            drawLine(
                                color = Color(0x15FFFFFF),
                                start = Offset(leftMargin, y),
                                end = Offset(w, y),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }

                        // ---- Y-axis labels ----
                        for (i in 0..ySteps) {
                            val yVal = niceMax * (ySteps - i) / ySteps
                            val label = if (showHours) String.format("%.0f", yVal) else "${yVal.toInt()}"
                            val yPos = chartH * i / ySteps
                            val result = textMeasurer.measure(
                                text = label,
                                style = yLabelStyle
                            )
                            drawText(
                                textLayoutResult = result,
                                topLeft = Offset(
                                    x = leftMargin - result.size.width - 4.dp.toPx(),
                                    y = yPos - result.size.height / 2f
                                )
                            )
                        }

                        // ---- Bars ----
                        for (day in 1..barCount) {
                            val key = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, day)
                            val minutes = dailyMap[key]?.totalMinutes ?: 0
                            val value = if (showHours) minutes / 60f else minutes.toFloat()
                            val barHeight = if (niceMax > 0f) (value / niceMax) * chartH else 0f

                            val x = leftMargin + barSpacing + (day - 1) * (barWidth + barSpacing)
                            val y = chartH - barHeight.coerceAtLeast(1.dp.toPx())

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight.coerceAtLeast(1.dp.toPx())),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }

                        // ---- X-axis labels (day numbers) ----
                        val xLabelStep = when {
                            daysInMonth <= 7 -> 1
                            daysInMonth <= 14 -> 2
                            daysInMonth <= 21 -> 3
                            else -> 5
                        }
                        val xLabels = (1..daysInMonth step xLabelStep).toMutableList()
                        if (xLabels.last() != daysInMonth) xLabels.add(daysInMonth)

                        for (day in xLabels) {
                            val cx = leftMargin + barSpacing + (day - 1) * (barWidth + barSpacing) + barWidth / 2
                            val label = "$day"
                            val result = textMeasurer.measure(
                                text = label,
                                style = xLabelStyle
                            )
                            drawText(
                                textLayoutResult = result,
                                topLeft = Offset(
                                    x = cx - result.size.width / 2f,
                                    y = chartH + 4.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }
        }

        // ---- Summary Stats ----
        val totalValue = if (showHours) periodStats.totalFocusMinutes / 60f else periodStats.totalFocusMinutes.toFloat()
        val avgValue = if (showHours) periodStats.totalFocusMinutes / 60f / daysInMonth
            else periodStats.dailyAverageMinutes.toFloat()
        val bestDay = periodStats.dailyBreakdown.maxByOrNull { it.totalMinutes }
        val bestValue = if (bestDay != null) {
            if (showHours) bestDay.totalMinutes / 60f else bestDay.totalMinutes.toFloat()
        } else 0f

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatBoxMin(
                value = if (showHours) String.format("%.1f", totalValue) else "$totalValue",
                unit = if (showHours) "jam" else "menit",
                label = "Total",
                modifier = Modifier.weight(1f)
            )
            StatBoxMin(
                value = if (showHours) String.format("%.1f", avgValue) else "${avgValue.toInt()}",
                unit = if (showHours) "jam" else "menit",
                label = "Rata-rata",
                modifier = Modifier.weight(1f)
            )
            StatBoxMin(
                value = if (showHours) String.format("%.1f", bestValue) else "${bestValue.toInt()}",
                unit = if (showHours) "jam" else "menit",
                label = "Terbaik",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatBoxMin(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun actualDurationMinutes(session: FocusSession): Int {
    val end = session.completedAt ?: System.currentTimeMillis()
    return ((end - session.createdAt) / 60000).toInt().coerceAtLeast(0)
}

private fun formatTimeRange(start: Long, end: Long?): String {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startStr = timeFormat.format(Date(start))
    val endStr = if (end != null) timeFormat.format(Date(end)) else "..."
    return "$startStr – $endStr"
}

private fun sessionDateKey(session: FocusSession): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.createdAt))
}

@Composable
private fun ActivitiesTab(
    modifier: Modifier = Modifier,
    sessions: List<FocusSession>,
    sessionAchievements: Map<String, List<Achievement>> = emptyMap(),
    onAchievementClick: (Achievement) -> Unit = {}
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "—", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Belum ada aktivitas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Selesaikan sesi fokus pertamamu\nuntuk membangun histori di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val sorted = remember(sessions) { sessions.sortedByDescending { it.createdAt } }
        val grouped = remember(sorted) { sorted.groupBy { sessionDateKey(it) } }
        val dateFormat = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID")) }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            grouped.forEach { (dateKey, dateSessions) ->
                item {
                    Text(
                        text = dateFormat.format(Date(dateSessions.first().createdAt)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                }
                items(dateSessions, key = { it.id }) { session ->
                    ActivityItemCard(
                        session = session,
                        achievements = sessionAchievements[session.id].orEmpty(),
                        onAchievementClick = onAchievementClick
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ActivityItemCard(
    session: FocusSession,
    achievements: List<Achievement> = emptyList(),
    onAchievementClick: (Achievement) -> Unit = {}
) {
    val isSuccess = session.status == SessionStatus.Success
    val isRunning = session.status == SessionStatus.Running
    val statusColor = when {
        isSuccess -> MaterialTheme.colorScheme.primary
        isRunning -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val actualDuration = remember(session) { actualDurationMinutes(session) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.goalTitle ?: "Sesi Fokus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatTimeRange(session.createdAt, session.completedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (achievements.isNotEmpty()) {
                        achievements.forEach { ach ->
                            Text(
                                text = ach.type.icon,
                                fontSize = 15.sp,
                                modifier = Modifier.clickable { onAchievementClick(ach) }
                            )
                        }
                    }
                    StatusTag(
                        label = when {
                            isSuccess -> "Berhasil"
                            isRunning -> "Berjalan"
                            else -> "Gagal"
                        },
                        color = statusColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Metric row — plain labeled values, no boxed background
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Target", value = minutesLabel(session.durationMinutes))
                MetricItem(label = "Tercapai", value = minutesLabel(actualDuration), valueColor = if (isSuccess) statusColor else null)
                MetricItem(
                    label = "Gangguan",
                    value = "${session.attemptCount}x",
                    valueColor = if (session.attemptCount > 3) MaterialTheme.colorScheme.error else null
                )
            }

            if (isSuccess) {
                val ratio = (actualDuration.toFloat() / session.durationMinutes.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgressBar(
                        progress = ratio,
                        height = 4.dp,
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    Text(
                        text = "${(ratio * 100).toInt()}% dari target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
private fun MetricItem(label: String, value: String, valueColor: Color? = null) {
    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PeriodSelector(
    periods: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        periods.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodSummary(stats: PeriodStats, currentStreak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        PeriodStatBox(
            value = String.format(Locale.ROOT, "%.1f", stats.totalHours),
            unit = "jam",
            label = "Total Fokus",
            modifier = Modifier.weight(1f)
        )
        PeriodStatBox(
            value = stats.dailyAverageMinutes.toString(),
            unit = "menit",
            label = "Rata-rata",
            modifier = Modifier.weight(1f)
        )
        PeriodStatBox(
            value = "$currentStreak",
            unit = "hari",
            label = "Streak",
            modifier = Modifier.weight(1f)
        )
        PeriodStatBox(
            value = "${stats.totalSessions}",
            unit = "sesi",
            label = "Total Sesi",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeriodStatBox(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Minimal monochrome progress ring — flat, single solid color, no gradient
 * sweep or glossy dot. Pure decoration; does not affect goalRate logic.
 */
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
private fun DailyGoalCard(goalRate: Float, todayMinutes: Int, targetMinutes: Int) {
    val clampedRate = goalRate.coerceIn(0f, 1f)
    val isComplete = goalRate >= 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
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
                if (isComplete) {
                    StatusTag(label = "Tercapai", color = MaterialTheme.colorScheme.primary)
                }
            }

            GoalRing(
                progress = clampedRate,
                ringColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(clampedRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "${minutesLabel(todayMinutes)} dari ${minutesLabel(targetMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConsistencyCard(
    dailyBreakdown: List<DailyBreakdown>,
    targetMinutes: Int
) {
    val daysHit = dailyBreakdown.count { it.totalMinutes >= targetMinutes }
    val totalDays = dailyBreakdown.size
    val rate = if (totalDays > 0) daysHit.toFloat() / totalDays else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Konsistensi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$daysHit dari $totalDays hari",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ProgressBar(progress = rate, height = 8.dp, color = MaterialTheme.colorScheme.primary)

            Text(
                text = "${(rate * 100).toInt()}% hari mencapai target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionResultCard(
    successful: Int,
    failed: Int,
    successRate: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "Hasil Sesi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Berhasil",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$successful Sesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Gagal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$failed Sesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Success Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                ProgressBar(
                    progress = successRate,
                    height = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(
    unlockedAchievements: List<Achievement> = emptyList(),
    onAchievementClick: (Achievement) -> Unit = {}
) {
    val unlocked = unlockedAchievements

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Pencapaian",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (unlocked.isEmpty()) {
                Text(
                    text = "Belum ada pencapaian. Terus fokus untuk membuka badge!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unlocked.forEach { ach ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                .clickable { onAchievementClick(ach) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = ach.type.icon, fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ach.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}