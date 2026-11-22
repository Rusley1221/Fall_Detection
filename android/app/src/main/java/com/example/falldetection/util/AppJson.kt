package com.example.falldetection.util

import kotlinx.serialization.json.Json

object AppJson {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
}
