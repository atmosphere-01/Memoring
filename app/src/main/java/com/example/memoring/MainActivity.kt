package com.example.memoring

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.memoring.ui.DummyData
import com.example.memoring.ui.LearningStats

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 시스템 바 인셋만큼 상/하 패딩 적용 (엣지투엣지 대응)
        val root = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        // 접속 기록 → 연속일 갱신 (하루 1회만 반영)
        LearningStats.registerVisit(this)

        buildReviewList()
    }

    override fun onResume() {
        super.onResume()
        refreshGoal()
    }

    private fun refreshGoal() {
        val done = LearningStats.goalCount(this)
        val target = LearningStats.GOAL_TARGET
        val shown = done.coerceAtMost(target)

        findViewById<TextView>(R.id.dateText).text = LearningStats.todayLabel()
        findViewById<TextView>(R.id.goalCountText).text = "${target}개 중 ${done}개 완료"
        findViewById<TextView>(R.id.goalCircle).text = "$shown / $target"
        findViewById<ProgressBar>(R.id.goalProgress).progress = shown
        findViewById<TextView>(R.id.streakText).text = "🔥 ${LearningStats.streak(this)}일 연속"
        findViewById<TextView>(R.id.goalSubtitle).text =
            if (done >= target) "오늘 목표 달성! 🎉" else "조금만 더 하면 목표 달성!"

        // 암기 완료(퀴즈 정답 단어) / 복습 필요(오답·헷갈려요·모르겠어요) 개수 갱신
        findViewById<TextView>(R.id.memorizedCount).text =
            LearningStats.memorizedCount(this).toString()
        findViewById<TextView>(R.id.reviewNeededCount).text =
            LearningStats.reviewNeededCount(this).toString()
    }

    private fun buildReviewList() {
        val container = findViewById<LinearLayout>(R.id.reviewList)
        val inflater = LayoutInflater.from(this)
        DummyData.reviewWords.forEach { word ->
            val item = inflater.inflate(R.layout.item_review_word, container, false)
            item.findViewById<TextView>(R.id.reviewEng).text = word.word
            item.findViewById<TextView>(R.id.reviewKor).text = word.meaning
            item.findViewById<TextView>(R.id.reviewStatus).text = word.status
            container.addView(item)
        }
    }
}
