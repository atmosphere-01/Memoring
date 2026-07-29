package com.example.memoring.data.util

import android.content.Context
import android.util.Log
import com.example.memoring.data.entity.WordEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object CsvHelper {
    private var assetDictionary: Map<String, String>? = null

    //개발자 CSV 단어 검색/추천을 위한 변환
    fun loadAssetDictionary(context: Context, fileName: String = "words.csv"): Map<String, String> {
        if (assetDictionary != null)
            return assetDictionary!! // 이미 로드된 경우 스킵

        try {
            val inputStream = context.assets.open(fileName)
            val parsedList = parseCsvStream(inputStream)

            assetDictionary = parsedList.associate { (word, meaning, _) ->
                word.lowercase() to meaning
            }

            Log.d("CSV_HELPER", "Asset 사전 로드 성공: ${assetDictionary?.size ?: 0}개 단어")
        } catch (e: Exception) {
            Log.e("CSV_HELPER", "Asset 사전 로드 실패: ${e.message}", e)
            assetDictionary = emptyMap()
        }
        //최종 생성되거나 null 처리된 assetDic return
        return assetDictionary?: emptyMap()
    }

    //단어로 개발자 제공 뜻 찾기 (없으면 null)
    fun getMeaningForWord(word: String): String? {
        val cleanWord = word.trim().lowercase()
        return assetDictionary?.get(cleanWord)
    }

    //사용자 csv 받기
    // inputStream 받아서 parsing
    fun parseCsvStream(inputStream: InputStream): List<Triple<String, String, String?>> {
        val wordList = mutableListOf<Triple<String, String, String?>>()

        try {
            val reader = BufferedReader(StringReader(decodeCsv(inputStream.readBytes())))

            var isHeader = true
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.isEmpty()) continue

                // word,meaning,partOfSpeech 헤더 건너뛰기
                if (isHeader) {
                    isHeader = false
                    continue
                }

                val tokens = currentLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())

                if (tokens.size >= 2) {
                    val word = tokens[0].trim().replace("\"", "")
                    val meaning = tokens[1].trim().replace("\"", "")
                    val partOfSpeech = if (tokens.size > 2) tokens[2].trim().replace("\"", "") else null

                    if (word.isNotEmpty()) {
                        wordList.add(Triple(word, meaning, partOfSpeech))
                    }
                }
            }
            reader.close()
            Log.d("CSV_HELPER", "파싱 성공 단어 수: ${wordList.size}개")
        } catch (e: Exception) {
            Log.e("CSV_HELPER", "CSV 파싱 중 에러 발생: ${e.message}", e)
        }
        return wordList
    }

    /**
     * Most user-created CSV files are UTF-8, while some Korean spreadsheet
     * programs still export EUC-KR. Decode strict UTF-8 first so malformed byte
     * sequences fall back to EUC-KR instead of producing replacement characters.
     */
    internal fun decodeCsv(bytes: ByteArray): String {
        val utf8Bytes = if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }

        return runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(utf8Bytes))
                .toString()
        }.getOrElse {
            String(bytes, charset("EUC-KR")).removePrefix("\uFEFF")
        }
    }
}
