package com.example.memoring.data

import android.content.Context
import androidx.room.Room

object AppDb {
    @Volatile private var instance: AppDatabase? = null // 앱 전체에서 딱 1개

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {                  // 이중 검사 락 (스레드 안전)
            instance ?: Room.databaseBuilder(
                context.applicationContext,                // 액티비티 말고 앱 컨텍스트 (메모리 누수 방지)
                AppDatabase::class.java, "memoring.db"     // 실제 SQLite 파일명
            ).build().also { instance = it }
        }
}