package com.example.memoring

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.memoring.databinding.ActivityWordDetailBinding

class WordDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordDetailBinding
    private var wordId = -1
    private var isFavorite = false
    private var currentStatus = "UNLEARNED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wordId = intent.getIntExtra("wordId", -1)
        val word = intent.getStringExtra("word") ?: ""
        isFavorite = intent.getBooleanExtra("isFavorite", false)
        currentStatus = intent.getStringExtra("memorizationStatus") ?: "UNLEARNED"

        binding.tvWord.text = word
        binding.etMeaning.setText(intent.getStringExtra("meaning") ?: "")
        binding.etPartOfSpeech.setText(intent.getStringExtra("partOfSpeech") ?: "")
        binding.etExample.setText(intent.getStringExtra("example") ?: "")

        updateFavoriteUi()
        updateStatusUi()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteUi()
        }

        binding.statusUnlearned.setOnClickListener {
            currentStatus = "UNLEARNED"
            updateStatusUi()
        }
        binding.statusReview.setOnClickListener {
            currentStatus = "CONFUSED"
            updateStatusUi()
        }
        binding.statusKnown.setOnClickListener {
            currentStatus = "KNOWN"
            updateStatusUi()
        }

        binding.btnSave.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("wordId", wordId)
                putExtra("meaning", binding.etMeaning.text.toString().trim())
                putExtra("partOfSpeech", binding.etPartOfSpeech.text.toString().trim().ifBlank { null })
                putExtra("example", binding.etExample.text.toString().trim().ifBlank { null })
                putExtra("isFavorite", isFavorite)
                putExtra("memorizationStatus", currentStatus)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        binding.btnDelete.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("wordId", wordId)
                putExtra("deleted", true)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun updateFavoriteUi() {
        binding.tvFavorite.text = if (isFavorite) "★" else "☆"
    }

    private fun updateStatusUi() {
        binding.statusUnlearned.setBackgroundResource(
            if (currentStatus == "UNLEARNED") R.drawable.bg_rounded_pill_selected else R.drawable.bg_rounded_pill
        )
        binding.statusReview.setBackgroundResource(
            if (currentStatus == "CONFUSED" || currentStatus == "UNKNOWN") R.drawable.bg_rounded_pill_selected else R.drawable.bg_rounded_pill
        )
        binding.statusKnown.setBackgroundResource(
            if (currentStatus == "KNOWN") R.drawable.bg_rounded_pill_selected else R.drawable.bg_rounded_pill
        )
    }
}