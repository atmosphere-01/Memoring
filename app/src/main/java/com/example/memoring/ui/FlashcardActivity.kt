package com.example.memoring.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.memoring.R

class FlashcardActivity : AppCompatActivity() {

    private val cards = DummyData.flashcards
    private var index = 0
    private var showingFront = true
    private var flipping = false

    private lateinit var container: FrameLayout
    private lateinit var cardFront: View
    private lateinit var cardBack: View
    private lateinit var progress: TextView
    private lateinit var frontTag: TextView
    private lateinit var frontWord: TextView
    private lateinit var backTag: TextView
    private lateinit var backWordSmall: TextView
    private lateinit var backMeaning: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)

        container = findViewById(R.id.cardContainer)
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        progress = findViewById(R.id.flashProgress)
        frontTag = findViewById(R.id.frontTag)
        frontWord = findViewById(R.id.frontWord)
        backTag = findViewById(R.id.backTag)
        backWordSmall = findViewById(R.id.backWordSmall)
        backMeaning = findViewById(R.id.backMeaning)

        // 3D 회전 시 원근감이 자연스럽도록 카메라 거리 확대
        val distance = 8000 * resources.displayMetrics.density
        cardFront.cameraDistance = distance
        cardBack.cameraDistance = distance

        bindCard()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        container.setOnClickListener { flip() }

        findViewById<LinearLayout>(R.id.moodKnow).setOnClickListener { next("외웠어요") }
        findViewById<LinearLayout>(R.id.moodUnsure).setOnClickListener { next("헷갈려요") }
        findViewById<LinearLayout>(R.id.moodNo).setOnClickListener { next("모르겠어요") }
    }

    private fun bindCard() {
        val card = cards[index]
        frontTag.text = card.category
        frontWord.text = card.word
        backTag.text = card.category
        backWordSmall.text = card.word
        backMeaning.text = card.meaning
        progress.text = "${index + 1} / ${cards.size}"

        // 새 카드는 항상 앞면(단어)부터 표시
        showingFront = true
        cardFront.visibility = View.VISIBLE
        cardBack.visibility = View.GONE
        container.rotationY = 0f
    }

    /** 앞면↔뒷면 Y축 회전 뒤집기 애니메이션 */
    private fun flip() {
        if (flipping) return
        flipping = true

        val out = ObjectAnimator.ofFloat(container, "rotationY", 0f, 90f).apply {
            duration = 150
            interpolator = AccelerateInterpolator()
        }
        val back = ObjectAnimator.ofFloat(container, "rotationY", -90f, 0f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
        }

        out.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                showingFront = !showingFront
                cardFront.visibility = if (showingFront) View.VISIBLE else View.GONE
                cardBack.visibility = if (showingFront) View.GONE else View.VISIBLE
                back.start()
            }
        })
        back.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flipping = false
            }
        })
        out.start()
    }

    /** 다음 카드로 이동 (마지막 카드까지 끝나면 화면 종료) */
    private fun next(mood: String) {
        val card = cards[index]
        // 카드 1장 평가 = 오늘 학습량 1건
        LearningStats.recordStudied(this)
        when (mood) {
            // "외웠어요" 는 암기 완료로 기록
            "외웠어요" -> LearningStats.addMemorizedWord(this, card.word, card.meaning)
            // "헷갈려요/모르겠어요" 는 복습 필요 단어로 기록 (뜻·상태와 함께)
            "헷갈려요", "모르겠어요" -> LearningStats.addReviewWord(this, card.word, card.meaning, mood)
        }
        if (index >= cards.size - 1) {
            // 마지막 카드까지 봤으면 처음으로 돌아가지 않고 종료
            finish()
            return
        }
        index++
        bindCard()
    }
}
