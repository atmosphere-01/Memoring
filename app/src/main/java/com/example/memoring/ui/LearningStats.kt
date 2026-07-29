package com.example.memoring.ui

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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
    private const val KEY_REVIEW = "review_words_v2"
    private const val KEY_TOTAL_ANSWERED = "quiz_total_answered"
    private const val KEY_TOTAL_CORRECT = "quiz_total_correct"

    /** 저장 시 단어/뜻을 구분하는 구분자 */
    private const val SEP = ""

    /** 여러 복습 항목을 구분하는 레코드 구분자 (단어/뜻에 없는 줄바꿈) */
    private const val REC = "\n"

    /** 하루의 시작 시각(시). 이 시각 이전은 전날로 계산한다. */
    private const val RESET_HOUR = 6L

    /** 오늘의 목표 정답 개수 */
    const val GOAL_TARGET = 10

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 6시 기준으로 보정한 "논리적 오늘"의 날짜 (yyyy-MM-dd) */
    private fun logicalToday(now: LocalDateTime = LocalDateTime.now()): LocalDate =
        now.minusHours(RESET_HOUR).toLocalDate()

    /** 화면 표기용 오늘 날짜 라벨 (예: "7월 28일 화요일") — 6시 보정된 논리적 날짜 */
    fun todayLabel(): String =
        logicalToday().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))

    /** 화면 표기용 "실제 현재" 날짜 라벨 (6시 보정 없이 오늘 달력 날짜) */
    fun currentDateLabel(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))

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
        removeFromReview(context, word) // 암기 완료되면 복습 목록에서 제거
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

    /** 복습 필요 단어 (홈/복습 페이지에 표시) */
    data class ReviewWord(val word: String, val meaning: String, val status: String)

    /**
     * 복습 필요 단어 추가. 같은 단어는 최신 정보로 갱신하며 맨 앞으로 이동한다.
     * 퀴즈 오답, 플래시카드 "헷갈려요/모르겠어요" 시 호출된다. (가장 최근이 앞)
     */
    fun addReviewWord(context: Context, word: String, meaning: String, status: String) {
        val kept = reviewWords(context).filterNot { it.word == word }
        val updated = listOf(ReviewWord(word, meaning, status)) + kept
        val raw = updated.joinToString(REC) { it.word + SEP + it.meaning + SEP + it.status }
        prefs(context).edit().putString(KEY_REVIEW, raw).apply()
        removeFromMemorized(context, word) // 복습 필요로 바뀌면 암기 완료에서 제거
    }

    /** 복습 목록에서 특정 단어 제거 */
    private fun removeFromReview(context: Context, word: String) {
        val kept = reviewWords(context).filterNot { it.word == word }
        val raw = kept.joinToString(REC) { it.word + SEP + it.meaning + SEP + it.status }
        prefs(context).edit().putString(KEY_REVIEW, raw).apply()
    }

    /** 암기 완료 목록에서 특정 단어 제거 */
    private fun removeFromMemorized(context: Context, word: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY_MEMORIZED, emptySet()) ?: emptySet()
        val updated = current.filterNot { it.substringBefore(SEP) == word }.toSet()
        p.edit().putStringSet(KEY_MEMORIZED, updated).apply()
    }

    /** 복습 필요 단어 목록 (최근순: 가장 최근이 맨 앞) */
    fun reviewWords(context: Context): List<ReviewWord> {
        val raw = prefs(context).getString(KEY_REVIEW, null)
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(REC).mapNotNull { line ->
            val parts = line.split(SEP)
            if (parts.size < 3) return@mapNotNull null
            ReviewWord(parts[0], parts[1], parts[2])
        }
    }

    /** 복습 필요 단어 개수 */
    fun reviewNeededCount(context: Context): Int = reviewWords(context).size

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

    /** 하루 학습량 저장 키 접두사 (뒤에 yyyy-MM-dd) */
    private const val KEY_DAILY_PREFIX = "learned_"

    /** 학습 1건(단어를 보고 퀴즈 답 또는 플래시카드 평가) 기록 — 오늘(논리적)에 누적 */
    fun recordStudied(context: Context, count: Int = 1) {
        val p = prefs(context)
        val key = KEY_DAILY_PREFIX + logicalToday().toString()
        p.edit().putInt(key, p.getInt(key, 0) + count).apply()
    }

    /** 이번 주 월요일(논리적) */
    private fun thisMonday(): LocalDate =
        logicalToday().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 특정 주(월요일 기준) 요일별 학습량. index 0=월 … 6=일 */
    private fun weekLearnedFrom(context: Context, monday: LocalDate): IntArray {
        val p = prefs(context)
        return IntArray(7) { i ->
            p.getInt(KEY_DAILY_PREFIX + monday.plusDays(i.toLong()).toString(), 0)
        }
    }

    /** 이번 주(월~일) 요일별 학습량. index 0=월 … 6=일 */
    fun thisWeekLearned(context: Context): IntArray =
        weekLearnedFrom(context, thisMonday())

    /** 이번 주 총 학습량 */
    fun weeklyTotal(context: Context): Int = thisWeekLearned(context).sum()

    /** 오늘 요일 인덱스 (월=0 … 일=6) */
    fun todayWeekdayIndex(): Int = logicalToday().dayOfWeek.value - 1

    /**
     * 주간 목표 달성률(%). 하루 [GOAL_TARGET]개 학습 = 그날 100%,
     * 7일 모두 채우면 100%. = Σ(min(요일별 학습량, GOAL_TARGET)) / (7 × GOAL_TARGET).
     */
    private fun goalPercentOf(counts: IntArray): Int {
        val achieved = counts.sumOf { minOf(it, GOAL_TARGET) }
        val target = 7 * GOAL_TARGET
        return Math.round(achieved * 100.0 / target).toInt()
    }

    /** 이번 주 목표 달성률(%) */
    fun weeklyGoalPercent(context: Context): Int =
        goalPercentOf(thisWeekLearned(context))

    /** 지난주 목표 달성률(%) */
    fun lastWeekGoalPercent(context: Context): Int =
        goalPercentOf(weekLearnedFrom(context, thisMonday().minusWeeks(1)))

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
