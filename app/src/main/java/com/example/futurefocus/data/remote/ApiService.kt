package com.example.futurefocus.data.remote

import retrofit2.http.GET

data class ZenQuoteResponse(
    val q: String,
    val a: String
)

interface ApiService {
    @GET("api/quotes")
    suspend fun getAllQuotes(): List<ZenQuoteResponse>
}
