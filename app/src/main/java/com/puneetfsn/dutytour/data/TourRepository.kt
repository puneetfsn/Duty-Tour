package com.puneetfsn.dutytour.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TourRepository(private val dao: TourDao) {

    fun observeAll(): Flow<List<Tour>> = dao.observeAll()

    fun observeById(id: Long): Flow<Tour?> = dao.observeById(id)

    fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<Tour>> =
        dao.observeBetween(fromEpochDay, toEpochDay)

    suspend fun get(id: Long): Tour? = dao.getById(id)

    suspend fun all(): List<Tour> = dao.getAll()

    suspend fun between(fromEpochDay: Long, toEpochDay: Long): List<Tour> =
        dao.getBetween(fromEpochDay, toEpochDay)

    suspend fun save(tour: Tour): Long {
        val stamped = tour.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) {
            dao.insert(stamped)
        } else {
            dao.update(stamped)
            stamped.id
        }
    }

    suspend fun delete(tour: Tour) = dao.delete(tour)

    fun observeTotalAll(): Flow<Int> = dao.observeAll().map { list -> list.sumOf { it.totalPaise } }
}
