package com.example.memoring.data.api

import com.example.memoring.data.api.dto.MyMemoryResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MyMemoryApiService {

    companion object {
        // 안전을 위한 요청당 최대 글자 수 제한
        const val MAX_CHARACTER_LIMIT = 3000
    }

    @GET("get")
    suspend fun translateText(
        @Query("q") text: String,
        @Query("langpair") langPair: String = "en|ko",
        @Query("de") email: String? = null // 본인 이메일을 적으면 하루 3만자까지 승급
    ): MyMemoryResponse
}

/**
 * 안전한 번역 호출을 위한 확장 함수 (글자 수 제한 초과 시 API 요청 차단)
 */
suspend fun MyMemoryApiService.safeTranslateText(
    text: String,
    langPair: String = "en|ko",
    email: String? = null
): MyMemoryResponse {
    if (text.length > MyMemoryApiService.MAX_CHARACTER_LIMIT) {
        throw IllegalArgumentException(
            "입력한 텍스트가 너무 깁니다. (${text.length}자 / 최대 ${MyMemoryApiService.MAX_CHARACTER_LIMIT}자 제한)"
        )
    }
    return translateText(text = text, langPair = langPair, email = email)
}