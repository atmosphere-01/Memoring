package com.example.memoring.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.memoring.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        WordEntity::class,
        UserWordEntity::class,
        QuizSessionEntity::class,
        QuizLogEntity::class,
        DailyEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    // 팀원들이 필요한 DAO를 등록할 공간
}