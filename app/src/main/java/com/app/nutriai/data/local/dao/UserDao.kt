package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.nutriai.data.local.entity.UserEntity

/**
 * Data Access Object for [UserEntity] operations.
 * Uses OnConflictStrategy.REPLACE for upsert behavior during sync.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /** Removes all rows. Called on sign-out to prevent cross-user data leakage. */
    @Query("DELETE FROM users")
    suspend fun clearAll()
}
