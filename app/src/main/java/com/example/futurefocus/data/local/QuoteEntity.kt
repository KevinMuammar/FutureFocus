package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quote: String,
    val author: String,
    val category: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
