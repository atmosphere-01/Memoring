package com.example.memoring

import com.example.memoring.data.util.CsvHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class CsvHelperEncodingTest {

    private val csv = "word,meaning,partOfSpeech\napple,사과,명사"

    @Test
    fun decodesUtf8Korean() {
        assertEquals(csv, CsvHelper.decodeCsv(csv.toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun decodesUtf8BomKorean() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            csv.toByteArray(StandardCharsets.UTF_8)

        assertEquals(csv, CsvHelper.decodeCsv(bytes))
    }

    @Test
    fun decodesEucKrKorean() {
        assertEquals(csv, CsvHelper.decodeCsv(csv.toByteArray(charset("EUC-KR"))))
    }
}
