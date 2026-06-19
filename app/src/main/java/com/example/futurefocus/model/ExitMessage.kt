package com.example.futurefocus.model

data class ExitMessage(
    val level: Int,
    val title: String,
    val message: String,
    val quote: String? = null,
    val author: String? = null
)
