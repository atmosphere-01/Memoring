package com.example.memoring.data.repository

import android.util.Log
import com.example.memoring.data.api.ApiClient
import com.example.memoring.data.api.safeTranslateText
import com.example.memoring.data.dao.WordDao
import com.example.memoring.data.entity.WordEntity
import com.example.memoring.data.util.CsvHelper

class WordRepository(
    private val wordDao: WordDao,
    private val csvHelper: CsvHelper) {
    suspend fun processAndSaveWord(
        englishWord: String,
        meaningInput: String?, // CSV나 사용자 입력 뜻 (없으면 null 또는 빈값)
        categoryId: Int,
        userEmail: String? = "soyean8751@gmail.com"
    ): Boolean {
        return try {
            val cleanWord = englishWord.trim()

            //이미 등록된 단어 (중복 검사)
            if(wordDao.isWordExists(categoryId,cleanWord)){
                Log.w("REPO_TEST","이미 카테고리에 존재하는 단어: $cleanWord")
                return false
            }

            var finalMeaning = meaningInput?.trim()
            var partOfSpeech: String? = null
            var example: String? = null

            //Free Dictionary API 호출 (품사,예문 추출)
            try {
                val dictResponseList = ApiClient.freeDictionaryApi.getWordInfo(cleanWord)
                if (!dictResponseList.isNullOrEmpty()) {
                    val firstEntry = dictResponseList[0]
                    //가장 먼저 나온 품사
                    partOfSpeech = firstEntry.meanings?.firstOrNull()?.partOfSpeech
                    //가장 먼저 나온 예문
                    example = firstEntry.meanings
                        ?.flatMap { it.definitions ?: emptyList() }
                        ?.firstOrNull { !it.example.isNullOrEmpty() }
                        ?.example
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //Meaning(뜻)
            //입력된 뜻이 없을 때
            if (finalMeaning.isNullOrEmpty()) {
                // 개발자 제공 CSV(Asset)에서 뜻을 먼저 찾음
                val assetMeaning = csvHelper.getMeaningForWord(cleanWord)

                if (!assetMeaning.isNullOrEmpty()) {
                    finalMeaning = assetMeaning
                } else {
                    //Asset에도 없을 경우 1차: MyMemory API 호출
                    finalMeaning = try {
                        ApiClient.myMemoryApi.safeTranslateText(
                            text = cleanWord,
                            langPair = "en|ko",
                            email = userEmail
                        ).responseData?.translatedText
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    //2차 백업: MyMemory 실패/빈값이면 Google Translate (API 키 필요)
                    if (finalMeaning.isNullOrBlank()) {
                        finalMeaning = try {
                            ApiClient.googleTranslateApi.safeTranslateText(cleanWord, "ko")
                                .data?.translations?.firstOrNull()?.translatedText
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                    if (finalMeaning.isNullOrBlank()) finalMeaning = ""
                }
            }

            // WordEntity 생성 및 DB 저장
            val wordEntity = WordEntity(
                categoryId = categoryId,
                word = cleanWord,
                meaning = finalMeaning ?: "",
                partOfSpeech = partOfSpeech,
                exampleSentence = example
            )

            val insertedId = wordDao.insertWord(wordEntity)
            insertedId > 0

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // CSV로 들어간 단어들에 Free Dictionary API를 호출해 예문만 채워주는 함수
    suspend fun fetchAndFillExamplesForCategory(categoryId: Int) {
        val targets = wordDao.getWordsWithoutExample(categoryId)

        for (wordEntity in targets) {
            try {
                // Free Dictionary API 호출
                val dictResponse = ApiClient.freeDictionaryApi.getWordInfo(wordEntity.word)
                if (!dictResponse.isNullOrEmpty()) {
                    val firstEntry = dictResponse[0]

                    // 모든 meanings / definitions 탐색해서 첫 번째 예문 추출
                    val fetchedExample = firstEntry.meanings
                        ?.flatMap { it.definitions ?: emptyList() }
                        ?.firstOrNull { !it.example.isNullOrEmpty() }
                        ?.example

                    // 예문을 찾았다면 DB 업데이트
                    if (!fetchedExample.isNullOrEmpty()) {
                        val updatedWord = wordEntity.copy(exampleSentence = fetchedExample)
                        wordDao.updateWord(updatedWord)
                    }
                }
            } catch (e: Exception) {
                // API 실패 시(네트워크 오류, 사전 데이터 없음 등) 차례대로 다음 단어로 진행
                e.printStackTrace()
            }
        }
    }
}