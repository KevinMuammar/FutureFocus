package com.example.futurefocus.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.Goal
import com.example.futurefocus.ui.component.FocusCard
import com.example.futurefocus.ui.component.PrimaryAction
import com.example.futurefocus.ui.component.ScreenContainer
import com.example.futurefocus.utils.minutesLabel

@Composable
fun SessionCompleteScreen(
    session: FocusSession?,
    goal: Goal? = null,
    onHome: () -> Unit
) {
    ScreenContainer(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Great job!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Kamu berhasil menyelesaikan sesi fokus.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )

        FocusCard {
            Text(
                text = "Durasi selesai",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Text(
                text = minutesLabel(session?.durationMinutes ?: 0),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Exit attempts: ${session?.attemptCount ?: 0}")
        }

        if (goal != null) {
            FocusCard {
                Text(
                    text = "Progress Goal: ${goal.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LinearProgressIndicator(
                    progress = { goal.progressPercentage },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                Text(
                    text = "${String.format("%.1f", goal.totalHours - goal.remainingHours)} / ${String.format("%.1f", goal.totalHours)} jam",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }

        FocusCard {
            Text(
                text = "Apresiasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("Komitmen kecil yang selesai hari ini membangun fokus yang lebih kuat besok.")
        }

        PrimaryAction(text = "Back to Home", onClick = onHome)
    }
}
