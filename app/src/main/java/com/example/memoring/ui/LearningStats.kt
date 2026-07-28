package com.example.memoring.ui

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 학습 목표 / 연속 접속일을 관리하는 경량 저장소 (SharedPreferences 기반).
 *
 * - 하루 기준은 "새벽 6시"에 바뀐다. (0~5시59분은 전날로 취급)
 * - 목표: 오늘 맞힌 퀴즈 정답 수 / [GOAL_TARGET]
 * - 연속일: 접속일이 어제면 +1, 하루라도 건너뛰면 1로 리셋
 */
object LearningStats {

    private const val PREFS = "memoring_stats"
    private const val KEY_LAST_ACTIVE = "last_active_day"
    private const val KEY_STREAK = "streak"
    private const val KEY_GOAL_DAY = "goal_day"
    private const val KEY_GOAL_COUNT = "goal_count"
    private const val KEY_MEMORIZED = "memorized_words"
    private const val KEY_REVIEW = "review_words"
    private const val KEY_TOTAL_ANSWERED = "quiz_total_answered"
    private const val KEY_TOTAL_CORRECT = "quiz_total_correct"

    /** 저장 시 단어/뜻을 구분하는 구분자 */
    private const val SEP = ""

    /** 하루의 시작 시각(시). 이 시각 이전은 전날로 계산한다. */
    private const val RESET_HOUR = 6L

    /** 오늘의 목표 정답 개수 */
    const val GOAL_TARGET = 10

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 6시 기준으로 보정한 "논리적 오늘"의 날짜 (yyyy-MM-dd) */
    private fun logicalToday(now: LocalDateTime = LocalDateTime.now()): LocalDate =
        now.minusHours(RESET_HOUR).toLocalDate()

    /** 화면 표기용 오늘 날짜 라벨 (예: "7월 28일 화요일") */
    fun todayLabel(): String =
        logicalToday().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))

    /**
     * 앱 접속 시 1회 호출. 하루에 한 번만 연속일을 갱신한다.
     * - 오늘 이미 접속함 → 변화 없음
     * - 마지막 접속이 어제 → 연속일 +1
     * - 그 외(하루 이상 미접속 / 첫 실행) → 연속일 1로 시작
     */
    fun registerVisit(context: Context) {
        val p = prefs(context)
        val today = logicalToday()
        val last = p.getString(KEY_LAST_ACTIVE, null)
        if (last == today.toString()) return

        val newStreak = if (last == today.minusDays(1).toString()) {
            p.getInt(KEY_STREAK, 0) + 1
        } else {
            1
        }
        p.edit()
            .putString(KEY_LAST_ACTIVE, today.toString())
            .putInt(KEY_STREAK, newStreak)
            .apply()
    }

    /** 현재 연속 접속일 (최소 1) */
    fun streak(context: Context): Int =
        prefs(context).getInt(KEY_STREAK, 1).coerceAtLeast(1)

    /** 퀴즈 정답 1개(또는 count개)를 오늘 목표에 반영 */
    fun addQuizCorrect(context: Context, count: Int = 1) {
        val p = prefs(context)
        val today = logicalToday().toString()
        val base = if (p.getString(KEY_GOAL_DAY, null) == today) p.getInt(KEY_GOAL_COUNT, 0) else 0
        p.edit()
            .putString(KEY_GOAL_DAY, today)
            .putInt(KEY_GOAL_COUNT, base + count)
            .apply()
    }

    /** 오늘 맞힌 정답 수 (날짜가 지났으면 0으로 리셋되어 반환) */
    fun goalCount(context: Context): Int {
        val p = prefs(context)
        val today = logicalToday().toString()
        return if (p.getString(KEY_GOAL_DAY, null) == today) p.getInt(KEY_GOAL_COUNT, 0) else 0
    }

    /** 퀴즈에서 맞힌 단어 (암기 완료 목록에 표시) */
    data class MemorizedWord(val word: String, val meaning: String)

    /** 퀴즈 정답 단어를 암기 완료 목록에 추가 (같은 단어는 중복 저장하지 않음, 리셋 없음) */
    fun addMemorizedWord(context: Context, word: String, meaning: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY_MEMORIZED, emptySet()) ?: emptySet()
        val updated = current.filterNot { it.substringBefore(SEP) == word }.toMutableSet()
        updated.add(word + SEP + meaning)
        p.edit().putStringSet(KEY_MEMORIZED, updated).apply()
    }

    /** 암기 완료 단어 목록 (알파벳순) */
    fun memorizedWords(context: Context): List<MemorizedWord> {
        val set = prefs(context).getStringSet(KEY_MEMORIZED, emptySet()) ?: emptySet()
        return set.map { MemorizedWord(it.substringBefore(SEP), it.substringAfter(SEP, "")) }
            .sortedBy { it.word.lowercase() }
    }

    /** 암기 완료 단어 개수 */
    fun memorizedCount(context: Context): Int =
        prefs(context).getStringSet(KEY_MEMORIZED, emptySet())?.size ?: 0

    /**
     * 복습 필요 단어 추가 (중복 없음).
     * 퀴즈에서 틀리거나, 플래시카드에서 "헷갈려요/모르겠어요" 를 고르면 호출된다.
     */
    fun addReviewWord(context: Context, word: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY_REVIEW, emptySet()) ?: emptySet()
        val updated = HashSet(current).apply { add(word) }
        p.edit().putStringSet(KEY_REVIEW, updated).apply()
    }

    /** 복습 필요 단어 개수 */
    fun reviewNeededCount(context: Context): Int =
        prefs(context).getStringSet(KEY_REVIEW, emptySet())?.size ?: 0

    /** 퀴즈 답변 1건 기록 (정답/오답 누적) — 정답률 계산용 */
    fun recordQuizAnswer(context: Context, correct: Boolean) {
        val p = prefs(context)
        val editor = p.edit()
        editor.putInt(KEY_TOTAL_ANSWERED, p.getInt(KEY_TOTAL_ANSWERED, 0) + 1)
        if (correct) editor.putInt(KEY_TOTAL_CORRECT, p.getInt(KEY_TOTAL_CORRECT, 0) + 1)
        editor.apply()
    }

    fun totalAnswered(context: Context): Int = prefs(context).getInt(KEY_TOTAL_ANSWERED, 0)
    fun totalCorrect(context: Context): Int = prefs(context).getInt(KEY_TOTAL_CORRECT, 0)

    /**
     * 정답률(%) — 소수점 첫째 자리에서 반올림.
     * 답변 기록이 없으면(0으로 나누기) 0.0 을 반환한다.
     */
    fun accuracyPercent(context: Context): Double {
        val answered = totalAnswered(context)
        if (answered == 0) return 0.0
        val correct = totalCorrect(context)
        return Math.round(correct * 1000.0 / answered) / 10.0
    }
}
