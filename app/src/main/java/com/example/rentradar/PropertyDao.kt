
package com.example.rentradar

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(property: Property)

    @Update
    suspend fun update(property: Property)

    @Delete
    suspend fun delete(property: Property)

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: String): Property?

    @Query("SELECT * FROM properties ORDER BY inspectionDate DESC")
    fun getPropertiesByNewest(): Flow<List<Property>>

    @Query("SELECT * FROM properties ORDER BY weeklyRent ASC")
    fun getPropertiesByLowestRent(): Flow<List<Property>>
}
