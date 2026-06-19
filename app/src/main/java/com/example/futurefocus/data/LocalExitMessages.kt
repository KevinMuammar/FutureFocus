package com.example.futurefocus.data

import com.example.futurefocus.model.ExitMessage

object LocalExitMessages {
    private val messages = listOf(
        ExitMessage(1, "Reflection Stage", "Yakin ingin keluar sekarang? Kamu baru saja mulai, dan masih ada waktu untuk melanjutkan."),
        ExitMessage(2, "Motivation Stage", "Kamu sudah sejauh ini. Tetap lanjutkan dan beri dirimu kesempatan untuk selesai."),
        ExitMessage(3, "Consequence Stage", "Usahamu akan sia-sia jika berhenti sekarang. Selesaikan komitmen yang kamu mulai."),
        ExitMessage(4, "Emotional Reflection", "Hari ini mungkin terasa berat, tapi kamu masih bisa memilih untuk menyelesaikannya.")
    )

    fun forAttempt(attempt: Int): ExitMessage {
        val index = (attempt - 1).coerceIn(0, messages.lastIndex)
        return messages[index]
    }
}
