package com.example.memoring.ui

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 마이페이지 "단어장 목록" 항목 */
data class Deck(
    val name: String,
    val date: String,
    val count: Int,
    val tone: String // "coral" | "green" | "blue"
)

/**
 * CSV로 업로드한 단어장 목록을 로컬(SharedPreferences)에 저장.
 *
 * 참고: 여기서는 디자인 시안과 동일하게 "단어 수 카운트 + 목록 메타데이터"만 로컬에 보관한다.
 * 실제 단어를 DB(WordEntity)에 적재하는 것은 백엔드(A) 몫이며 [importCsv] 에 연결하면 된다.
 */
object DeckStore {

    private const val PREFS = "memoring_decks"
    private const val KEY_DECKS = "decks"
    private const val REC = "\n"
    private const val SEP = ""

    private val tones = listOf("coral", "green", "blue")

    private val defaults = listOf(
        Deck("TOEIC 단어장", "2026.07.24", 1256, "coral"),
        Deck("여행 영어", "2026.07.18", 842, "green"),
        Deck("JLPT N2", "2026.07.10", 1034, "blue"),
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun decks(context: Context): List<Deck> {
        val raw = prefs(context).getString(KEY_DECKS, null) ?: return defaults
        if (raw.isEmpty()) return emptyList()
        return raw.split(REC).mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size < 4) return@mapNotNull null
            Deck(parts[0], parts[1], parts[2].toIntOrNull() ?: 0, parts[3])
        }
    }

    private fun save(context: Context, decks: List<Deck>) {
        val raw = decks.joinToString(REC) { "${it.name}$SEP${it.date}$SEP${it.count}$SEP${it.tone}" }
        prefs(context).edit().putString(KEY_DECKS, raw).apply()
    }

    /** 새 단어장을 맨 앞에 추가하고 저장한다. */
    fun addDeck(context: Context, name: String, count: Int): Deck {
        val current = decks(context).toMutableList()
        val deck = Deck(
            name = name,
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
            count = count,
            tone = tones[current.size % tones.size]
        )
        current.add(0, deck)
        save(context, current)
        return deck
    }

    /**
     * CSV 텍스트에서 단어 개수를 센다. (첫 줄이 헤더로 보이면 제외)
     * TODO(백엔드): 실제 단어 파싱/DB 적재 로직으로 확장
     */
    fun countCsvWords(text: String): Int {
        val rows = text
            .removePrefix("﻿")
            .split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return 0

        val header = rows.first().lowercase()
        val looksLikeHeader = Regex("(word|english|단어|뜻|meaning|korean)").containsMatchIn(header)
        return (rows.size - if (looksLikeHeader) 1 else 0).coerceAtLeast(0)
    }
}
