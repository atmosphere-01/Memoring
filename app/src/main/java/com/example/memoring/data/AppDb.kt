package com.example.memoring.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.memoring.data.util.CsvHelper

/**
 * AppDatabase 싱글톤 제공 + 최초 생성 시 기본 데이터 시드.
 *
 * - 단일 사용자([CURRENT_USER_ID] = 1)로 고정.
 * - 단어는 백엔드가 제공한 assets/words.csv 를 읽어 DB에 적재한다.
 * - 시드는 DB 파일이 처음 만들어질 때 한 번만 실행된다.
 */
object AppDb {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        val appCtx = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                appCtx,
                AppDatabase::class.java,
                "memoring.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(SeedCallback(appCtx))
                .build()
                .also { instance = it }
        }
    }

    private class SeedCallback(private val appCtx: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 사용자 1명
            db.execSQL(
                "INSERT INTO users (userId, username, continuousDay, createdAt) " +
                        "VALUES (1, '시우', 0, '2026-07-29 00:00:00')"
            )
            // 카테고리 (userId=1 소유)
            db.execSQL(
                "INSERT INTO categories (categoryId, userId, categoryName) VALUES " +
                        "(1, 1, 'TOEIC'), (2, 1, '여행 영어'), (3, 1, '내 단어장')"
            )
            seedWordsFromCsv(db)
        }

        /** 백엔드 제공 words.csv → words 테이블 (TOEIC 카테고리로 적재) */
        private fun seedWordsFromCsv(db: SupportSQLiteDatabase) {
            val rows = runCatching {
                CsvHelper.parseCsvStream(appCtx.assets.open("words.csv"))
            }.getOrElse { emptyList() }

            if (rows.isEmpty()) return

            val stmt = db.compileStatement(
                "INSERT INTO words (categoryId, word, meaning, partOfSpeech, exampleSentence, createdAt) " +
                        "VALUES (1, ?, ?, ?, NULL, 0)"
            )
            db.beginTransaction()
            try {
                for ((word, meaning, partOfSpeech) in rows) {
                    stmt.clearBindings()
                    stmt.bindString(1, word)
                    stmt.bindString(2, meaning)
                    if (partOfSpeech.isNullOrBlank()) stmt.bindNull(3) else stmt.bindString(3, partOfSpeech)
                    stmt.executeInsert()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
