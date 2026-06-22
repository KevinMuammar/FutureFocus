package com.example.futurefocus.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futurefocus.service.FocusLockService
import com.example.futurefocus.utils.PermissionHelper
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🎯",
        title = "Selamat Datang di FutureFocus",
        description = "Tingkatkan fokus dan produktivitasmu dengan sesi fokus terkunci. Capai tujuanmu tanpa gangguan!",
    ),
    OnboardingPage(
        emoji = "📊",
        title = "Pantau Progressmu",
        description = "Catat setiap sesi fokus, lihat statistik harian, dan capai target fokusmu setiap hari.",
    ),
    OnboardingPage(
        emoji = "🎯",
        title = "Buat & Capai Goal",
        description = "Buat goal belajar atau bekerja, bagi jadi subtugas, dan lihat progresmu menyelesaikannya.",
    ),
    OnboardingPage(
        emoji = "🔒",
        title = "Aktifkan Aksesibilitas",
        description = "FutureFocus butuh izin aksesibilitas untuk mengunci layar saat sesi fokus berlangsung.",
        ),
)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onEnableAccessibility: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false,
                pageSpacing = 16.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
            ) { pageIndex ->
                val page = pages[pageIndex]
                OnboardingCard(
                    page = page,
                    isLastPage = pageIndex == pages.lastIndex,
                    onEnableAccessibility = onEnableAccessibility,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val color by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (pagerState.currentPage == pages.lastIndex) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = "Mulai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = "Lanjut",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(
                        text = "Lewati",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingCard(
    modifier: Modifier = Modifier,
    page: OnboardingPage,
    isLastPage: Boolean,
    onEnableAccessibility: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = page.emoji,
                fontSize = 72.sp,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            if (isLastPage) {
                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp),
                        )
                        .clickable(onClick = onEnableAccessibility)
                        .padding(20.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aktifkan Layanan Aksesibilitas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Buka pengaturan & aktifkan FutureFocus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
