package com.puneetfsn.dutytour.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TourDao {

    @Query("SELECT * FROM tours ORDER BY startDate DESC, id DESC")
    fun observeAll(): Flow<List<Tour>>

    @Query("SELECT * FROM tours WHERE id = :id")
    fun observeById(id: Long): Flow<Tour?>

    @Query("SELECT * FROM tours WHERE id = :id")
    suspend fun getById(id: Long): Tour?

    /** Range export: inclusive on both ends, oldest first. */
    @Query("SELECT * FROM tours WHERE startDate >= :fromEpochDay AND startDate <= :toEpochDay ORDER BY startDate ASC, id ASC")
    fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<Tour>>

    @Query("SELECT * FROM tours ORDER BY startDate ASC, id ASC")
    suspend fun getAll(): List<Tour>

    @Query("SELECT * FROM tours WHERE startDate >= :fromEpochDay AND startDate <= :toEpochDay ORDER BY startDate ASC, id ASC")
    suspend fun getBetween(fromEpochDay: Long, toEpochDay: Long): List<Tour>

    @Insert
    suspend fun insert(tour: Tour): Long

    @Update
    suspend fun update(tour: Tour)

    @Delete
    suspend fun delete(tour: Tour)
}
