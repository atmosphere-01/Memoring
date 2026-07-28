package com.example.memoring.data.api

import com.example.memoring.data.api.dto.GoogleTranslationResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.memoring.BuildConfig

interface GoogleTranslateApiService {

    companion object {
        const val MAX_CHARACTER_LIMIT = 5000
    }
    @POST("language/translate/v2")
    suspend fun translateText(
        @Query("q") text: String,
        @Query("target") targetLanguage: String = "ko", // 한국어 번역
        @Query("key") apiKey: String = BuildConfig.GOOGLE_TRANSLATE_API_KEY
    ): GoogleTranslationResponse
}

suspend fun GoogleTranslateApiService.safeTranslateText(
    text: String,
    targetLanguage: String = "ko"
): GoogleTranslationResponse {
    if (text.length > GoogleTranslateApiService.MAX_CHARACTER_LIMIT) {
        throw IllegalArgumentException(
            "입력한 텍스트가 너무 깁니다. (${text.length}자 / 최대 ${GoogleTranslateApiService.MAX_CHARACTER_LIMIT}자 제한)"
        )
    }
    return translateText(text = text, targetLanguage = targetLanguage)
}