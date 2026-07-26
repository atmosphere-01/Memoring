package com.example.memoring.data.dao

import androidx.room.*
import com.example.memoring.data.entity.CategoryEntity
import com.example.memoring.data.entity.UserEntity

@Dao
interface CategoryDao {
    //카테고리 등록
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    //카테고리 조회
    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesByUserId(userId: Int): List<CategoryEntity>

    //카테고리 삭제
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    //카테고리 수정
    @Update
    suspend fun updateCategory(category: CategoryEntity)
}