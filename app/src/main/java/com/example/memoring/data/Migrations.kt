package com.example.memoring.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Converts words.createdAt from the original TEXT timestamp to epoch milliseconds.
 *
 * The temporary copies are necessary because dropping words triggers the foreign-key
 * cascades on user_words and quiz_logs. Their contents are restored after words has
 * been recreated with the schema expected by Room.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TEMP TABLE words_backup AS SELECT * FROM words")
        db.execSQL("CREATE TEMP TABLE user_words_backup AS SELECT * FROM user_words")
        db.execSQL("CREATE TEMP TABLE quiz_logs_backup AS SELECT * FROM quiz_logs")

        db.execSQL("DROP TABLE words")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS words (
                wordId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                word TEXT NOT NULL,
                meaning TEXT NOT NULL,
                partOfSpeech TEXT,
                exampleSentence TEXT,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(categoryId)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO words (
                wordId, categoryId, word, meaning, partOfSpeech, exampleSentence, createdAt
            )
            SELECT
                wordId,
                categoryId,
                word,
                meaning,
                partOfSpeech,
                exampleSentence,
                CASE
                    WHEN typeof(createdAt) = 'integer' THEN createdAt
                    ELSE COALESCE(
                        CAST(strftime('%s', createdAt) AS INTEGER) * 1000,
                        CAST(createdAt AS INTEGER),
                        0
                    )
                END
            FROM words_backup
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR REPLACE INTO user_words (
                userWordId, userId, wordId, isFavorite, memorizationStatus, correctCnt, wrongCnt
            )
            SELECT
                userWordId, userId, wordId, isFavorite, memorizationStatus, correctCnt, wrongCnt
            FROM user_words_backup
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO quiz_logs (
                quizLogId, quizSessionId, wordId, selectedAnswer, isCorrect
            )
            SELECT quizLogId, quizSessionId, wordId, selectedAnswer, isCorrect
            FROM quiz_logs_backup
            """.trimIndent()
        )

        db.execSQL("DROP TABLE words_backup")
        db.execSQL("DROP TABLE user_words_backup")
        db.execSQL("DROP TABLE quiz_logs_backup")
    }
}
