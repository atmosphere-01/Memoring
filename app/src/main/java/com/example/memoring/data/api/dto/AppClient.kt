package com.example.memoring.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val FREE_DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/"
    private const val GOOGLE_TRANSLATE_BASE_URL = "https://translation.googleapis.com/"
    private const val MYMEMORY_BASE_URL = "https://api.mymemory.translated.net/"
    // Free Dictionary API 클라이언트
    val freeDictionaryApi: FreeDictionaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(FREE_DICTIONARY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FreeDictionaryApiService::class.java)
    }

    // 💡 MyMemory API 클라이언트 추가
    val myMemoryApi: MyMemoryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MYMEMORY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MyMemoryApiService::class.java)
    }
    // Google Translate API 클라이언트
    val googleTranslateApi: GoogleTranslateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleTranslateApiService::class.java)
    }
}