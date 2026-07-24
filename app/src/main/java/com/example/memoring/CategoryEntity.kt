package com.example.memoring.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Int = 0,
    val userId: Int,
    val categoryName: String
)