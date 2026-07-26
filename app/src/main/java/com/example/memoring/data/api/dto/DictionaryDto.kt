package com.example.memoring.data.api.dto

data class FreeDictionaryResponse(
    val word: String?,
    val phonetic: String?, // 발음 (텍스트)
    val meanings: List<MeaningDto>?
)


data class MeaningDto(
    val partOfSpeech: String?, // 품사 (noun, verb 등)
    val definitions: List<DefinitionDto>?
)

data class DefinitionDto(
    val definition: String?,
    val example: String? // 예문
)