package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.CategoryEntity
import com.example.memoring.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY userId LIMIT 1")
    suspend fun getFirst(): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getById(userId: Int): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeById(userId: Int): Flow<UserEntity?>

    @Query("UPDATE users SET continuousDay = :days, lastLearningDate = :date WHERE userId = :userId")
    suspend fun updateLearningStreak(userId: Int, days: Int, date: String): Int
}
