package com.example.memoring.data.api.dto

data class GoogleTranslationResponse(
    val data: TranslationData?
)

data class TranslationData(
    val translations: List<TranslationItem>?
)

data class TranslationItem(
    val translatedText: String?
)