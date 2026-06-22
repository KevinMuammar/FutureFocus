package com.example.futurefocus.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth

// ============================================================================
// ProfileScreen — Clean minimalist redesign (Notion/Linear-inspired)
// Visual layer only. All state, parameters, and business logic unchanged.
// ============================================================================

@Composable
fun ProfileScreen(
    achievements: List<Achievement> = emptyList(),
    onBack: () -> Unit = {}
) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(AuthManager.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---- Header: minimal, flat ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
            Column {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val user = currentUser
            if (user != null) {
                // ---- Avatar: clean border, minimal background ----
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user.email?.firstOrNull()?.uppercase() ?: "U"),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = user.email ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                StatusTag(
                    label = if (user.isAnonymous) "Akun Tamu" else "Terdaftar",
                    color = if (user.isAnonymous) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(32.dp))

                // Account actions
                var showReauth by remember { mutableStateOf(false) }
                var showChangeEmail by remember { mutableStateOf(false) }
                var showChangePassword by remember { mutableStateOf(false) }
                var reauthEmail by remember { mutableStateOf("") }
                var reauthPassword by remember { mutableStateOf("") }
                var newEmail by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var actionMessage by remember { mutableStateOf<String?>(null) }
                var isActionLoading by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                ) {
                    SettingActionRow(
                        icon = "✉️",
                        label = "Ubah Email",
                        onClick = {
                            reauthEmail = user.email ?: ""
                            reauthPassword = ""
                            showReauth = true
                            actionMessage = null
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingActionRow(
                        icon = "🔑",
                        label = "Ubah Kata Sandi",
                        onClick = {
                            reauthEmail = user.email ?: ""
                            reauthPassword = ""
                            showReauth = false
                            showChangePassword = true
                            actionMessage = null
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Re-authentication dialog
                if (showReauth) {
                    MinimalDialog(
                        icon = "🔑",
                        title = "Konfirmasi Kata Sandi",
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Masukkan kata sandi untuk melanjutkan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = reauthPassword,
                                    onValueChange = { reauthPassword = it },
                                    label = { Text("Kata Sandi", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    enabled = !isActionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (actionMessage != null) {
                                    ActionMessageBanner(message = actionMessage!!, isSuccess = false)
                                }
                            }
                        },
                        onDismiss = { showReauth = false },
                        confirmText = "Lanjut",
                        onConfirm = {
                            if (reauthPassword.isBlank()) {
                                actionMessage = "Kata sandi tidak boleh kosong"
                                return@MinimalDialog
                            }
                            isActionLoading = true
                            actionMessage = null
                            AuthManager.reauthenticate(reauthEmail, reauthPassword) { success, error ->
                                isActionLoading = false
                                if (success) {
                                    showReauth = false
                                    showChangeEmail = true
                                } else {
                                    actionMessage = error ?: "Gagal verifikasi"
                                }
                            }
                        },
                        isLoading = isActionLoading
                    )
                }

                // Change email dialog
                if (showChangeEmail) {
                    MinimalDialog(
                        icon = "✉️",
                        title = "Ubah Email",
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = newEmail,
                                    onValueChange = { newEmail = it; actionMessage = null },
                                    label = { Text("Email Baru", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    enabled = !isActionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (actionMessage != null) {
                                    ActionMessageBanner(
                                        message = actionMessage!!,
                                        isSuccess = actionMessage == "Email berhasil diubah"
                                    )
                                }
                            }
                        },
                        onDismiss = { showChangeEmail = false },
                        confirmText = "Simpan",
                        onConfirm = {
                            if (newEmail.isBlank()) {
                                actionMessage = "Email tidak boleh kosong"
                                return@MinimalDialog
                            }
                            isActionLoading = true
                            actionMessage = null
                            AuthManager.updateEmail(newEmail) { success, error ->
                                isActionLoading = false
                                if (success) {
                                    actionMessage = "Email berhasil diubah"
                                } else {
                                    actionMessage = error ?: "Gagal mengubah email"
                                }
                            }
                        },
                        isLoading = isActionLoading
                    )
                }

                // Change password dialog
                if (showChangePassword) {
                    MinimalDialog(
                        icon = "🛡️",
                        title = "Ubah Kata Sandi",
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it; actionMessage = null },
                                    label = { Text("Kata Sandi Baru", style = MaterialTheme.typography.bodyMedium) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    enabled = !isActionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (actionMessage != null) {
                                    ActionMessageBanner(
                                        message = actionMessage!!,
                                        isSuccess = actionMessage == "Kata sandi berhasil diubah"
                                    )
                                }
                            }
                        },
                        onDismiss = { showChangePassword = false },
                        confirmText = "Simpan",
                        onConfirm = {
                            if (newPassword.length < 6) {
                                actionMessage = "Kata sandi minimal 6 karakter"
                                return@MinimalDialog
                            }
                            isActionLoading = true
                            actionMessage = null
                            AuthManager.updatePassword(newPassword) { success, error ->
                                isActionLoading = false
                                if (success) {
                                    actionMessage = "Kata sandi berhasil diubah"
                                } else {
                                    actionMessage = error ?: "Gagal mengubah kata sandi"
                                }
                            }
                        },
                        isLoading = isActionLoading
                    )
                }

                // Achievements section
                if (achievements.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pencapaian",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                StatusTag(label = "${achievements.size} Dibuka", color = MaterialTheme.colorScheme.primary)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            achievements.forEachIndexed { index, ach ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAchievement = ach }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = ach.type.icon, fontSize = 20.sp)
                                    }
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < achievements.size - 1) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }

                Button(
                    onClick = { AuthManager.signOut() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // ---- Guest / Login-Register State ----
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔒", fontSize = 28.sp)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = if (isLoginMode) "Selamat Datang" else "Buat Akun",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Masuk untuk menyimpan progres\ndan pencapaianmu di berbagai perangkat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Kata Sandi", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        if (errorMessage != null) {
                            ActionMessageBanner(message = errorMessage!!, isSuccess = false)
                        }

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Email dan kata sandi tidak boleh kosong"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                if (isLoginMode) {
                                    AuthManager.login(email, password) { success, error ->
                                        isLoading = false
                                        if (!success) errorMessage = error
                                    }
                                } else {
                                    AuthManager.register(email, password) { success, error ->
                                        isLoading = false
                                        if (!success) errorMessage = error
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isLoginMode) "Masuk" else "Daftar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isLoginMode) "Belum punya akun? Daftar" else "Sudah punya akun? Masuk",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    selectedAchievement?.let { ach ->
        MinimalDialog(
            icon = ach.type.icon,
            title = ach.title,
            content = {
                Text(
                    text = ach.description,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onDismiss = { selectedAchievement = null },
            confirmText = "Tutup",
            onConfirm = { selectedAchievement = null }
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
private fun SettingActionRow(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 16.sp)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(text = "›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionMessageBanner(message: String, isSuccess: Boolean) {
    val color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val bgColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = if (isSuccess) "✓" else "!", style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun MinimalDialog(
    icon: String,
    title: String,
    content: @Composable () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
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
        text = content,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Batal",
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}