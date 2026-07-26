package com.example.memoring.data.api

import com.example.memoring.data.api.dto.FreeDictionaryResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApiService {
    // URL의 {word} 자리에 검색할 단어가 동적으로 들어갑니다.
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordInfo(
        @Path("word") word: String
    ): List<FreeDictionaryResponse>
}