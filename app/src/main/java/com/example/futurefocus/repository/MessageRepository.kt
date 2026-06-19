package com.example.futurefocus.repository

import com.example.futurefocus.data.LocalExitMessages
import com.example.futurefocus.data.local.AppDatabase
import com.example.futurefocus.data.local.QuoteEntity
import com.example.futurefocus.data.remote.ApiService
import com.example.futurefocus.data.remote.RetrofitInstance
import com.example.futurefocus.data.remote.ZenQuoteResponse
import com.example.futurefocus.model.ExitMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList

enum class QuoteCategory { MOTIVATION, DISCIPLINE, HARD_WORK, HOPE }

class MessageRepository(
    private val apiService: ApiService = RetrofitInstance.apiService,
    private val database: AppDatabase
) {
    private val motivationQuotes = CopyOnWriteArrayList<ZenQuoteResponse>()
    private val disciplineQuotes = CopyOnWriteArrayList<ZenQuoteResponse>()
    private val hardWorkQuotes = CopyOnWriteArrayList<ZenQuoteResponse>()
    private val hopeQuotes = CopyOnWriteArrayList<ZenQuoteResponse>()

    @Volatile
    private var isPreloading = false

    private val dao = database.quoteDao()

    private val allCachedQuotes: List<ZenQuoteResponse>
        get() = motivationQuotes + disciplineQuotes + hardWorkQuotes + hopeQuotes

    private val fallbackQuotes = listOf(
        ZenQuoteResponse("Keep going.", "FutureFocus"),
        ZenQuoteResponse("Your future self will thank you.", "FutureFocus"),
        ZenQuoteResponse("Do not stop now.", "FutureFocus"),
        ZenQuoteResponse("Small discipline today becomes confidence tomorrow.", "FutureFocus")
    )

    private val allowedKeywords = listOf(
        "success", "focus", "discipline", "effort", "future", "goal",
        "progress", "work", "strong", "dream", "learn", "improve",
        "achieve", "believe", "commit", "habit", "potential",
        "accomplish", "master", "overcome", "courage", "determination",
        "patience", "persevere", "great", "hope", "faith", "trust",
        "growth", "change", "better", "possibility", "light", "tomorrow"
    )

    private val motivationKeywords = setOf(
        "success", "dream", "believe", "inspire", "determination",
        "never give up", "keep going", "push", "achieve", "overcome",
        "potential", "impossible", "greatness", "great", "win",
        "goal", "accomplish"
    )

    private val disciplineKeywords = setOf(
        "discipline", "habit", "routine", "self-control", "willpower",
        "consistency", "practice", "dedication", "commitment", "commit",
        "sacrifice", "character", "responsible", "duty", "master",
        "temper", "order"
    )

    private val hardWorkKeywords = setOf(
        "hard work", "effort", "strong", "grind", "labor", "struggle",
        "persistence", "perseverance", "persevere", "strength", "work",
        "sacrifice", "endure", "tough", "grit", "courage"
    )

    private val hopeKeywords = setOf(
        "hope", "future", "tomorrow", "light", "faith", "patience",
        "better", "possibility", "change", "growth", "new day",
        "bright", "trust", "promise"
    )

    fun getExitMessage(attempt: Int): ExitMessage {
        val message = LocalExitMessages.forAttempt(attempt)
        if (message.level == 1) return message

        val quote = getQuoteByLevel(message.level)
        if (quote != null) {
            return message.copy(quote = quote.q, author = quote.a)
        }
        return message
    }

    suspend fun preloadQuotes() {
        if (isPreloading) return
        if (allCachedQuotes.isNotEmpty()) return

        val roomQuotes = withContext(Dispatchers.IO) { dao.getAllQuotes() }
        if (roomQuotes.isNotEmpty()) {
            loadFromRoom(roomQuotes)
            return
        }

        isPreloading = true
        try {
            withContext(Dispatchers.IO) {
                val quotes = withTimeoutOrNull(15_000) {
                    try {
                        apiService.getAllQuotes()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (quotes != null) {
                    val entities = mutableListOf<QuoteEntity>()
                    for (quote in quotes) {
                        if (isRelevant(quote)) {
                            val category = categorize(quote)
                            addToMemoryCache(quote, category)
                            entities.add(
                                QuoteEntity(
                                    quote = quote.q,
                                    author = quote.a,
                                    category = category.name
                                )
                            )
                        }
                    }
                    dao.deleteAll()
                    dao.insertAll(entities)
                }
            }
        } catch (_: Exception) {
        } finally {
            isPreloading = false
        }
    }

    private fun loadFromRoom(entities: List<QuoteEntity>) {
        for (entity in entities) {
            val quote = ZenQuoteResponse(entity.quote, entity.author)
            when (entity.category) {
                "MOTIVATION" -> motivationQuotes.add(quote)
                "DISCIPLINE" -> disciplineQuotes.add(quote)
                "HARD_WORK" -> hardWorkQuotes.add(quote)
                "HOPE" -> hopeQuotes.add(quote)
            }
        }
    }

    private fun addToMemoryCache(quote: ZenQuoteResponse, category: QuoteCategory) {
        when (category) {
            QuoteCategory.MOTIVATION -> if (motivationQuotes.none { it.q == quote.q }) motivationQuotes.add(quote)
            QuoteCategory.DISCIPLINE -> if (disciplineQuotes.none { it.q == quote.q }) disciplineQuotes.add(quote)
            QuoteCategory.HARD_WORK -> if (hardWorkQuotes.none { it.q == quote.q }) hardWorkQuotes.add(quote)
            QuoteCategory.HOPE -> if (hopeQuotes.none { it.q == quote.q }) hopeQuotes.add(quote)
        }
    }

    private fun isRelevant(quote: ZenQuoteResponse): Boolean {
        val text = "${quote.q} ${quote.a}".lowercase()
        return allowedKeywords.any { text.contains(it) }
    }

    private fun categorize(quote: ZenQuoteResponse): QuoteCategory {
        val text = "${quote.q} ${quote.a}".lowercase()
        return when {
            motivationKeywords.any { text.contains(it) } -> QuoteCategory.MOTIVATION
            disciplineKeywords.any { text.contains(it) } -> QuoteCategory.DISCIPLINE
            hardWorkKeywords.any { text.contains(it) } -> QuoteCategory.HARD_WORK
            hopeKeywords.any { text.contains(it) } -> QuoteCategory.HOPE
            else -> QuoteCategory.MOTIVATION
        }
    }

    private fun getQuoteByLevel(level: Int): ZenQuoteResponse? {
        return when (level) {
            2 -> motivationQuotes.randomOrNull()
            3 -> disciplineQuotes.randomOrNull()
                ?: hardWorkQuotes.randomOrNull()
                ?: allCachedQuotes.randomOrNull()
            4 -> hopeQuotes.randomOrNull()
                ?: allCachedQuotes.randomOrNull()
            else -> null
        } ?: allCachedQuotes.ifEmpty { fallbackQuotes }.randomOrNull()
    }
}
