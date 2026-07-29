package com.example.memoring.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.memoring.data.dao.CategoryDao
import com.example.memoring.data.dao.DailyDao
import com.example.memoring.data.dao.QuizDao
import com.example.memoring.data.dao.UserDao
import com.example.memoring.data.dao.UserWordDao
import com.example.memoring.data.dao.WordDao
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
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
    abstract fun dailyDao(): DailyDao
    abstract fun quizDao(): QuizDao
}
