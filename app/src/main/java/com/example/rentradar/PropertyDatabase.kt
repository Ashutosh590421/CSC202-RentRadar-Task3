
package com.example.rentradar

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Property::class],
    version = 1,
    exportSchema = false
)
abstract class PropertyDatabase : RoomDatabase() {

    abstract fun propertyDao(): PropertyDao

    companion object {

        @Volatile
        private var INSTANCE: PropertyDatabase? = null

        fun getDatabase(context: Context): PropertyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PropertyDatabase::class.java,
                    "rent_radar_database"
                ).build().also {
                    INSTANCE = it
                }

                instance
            }
        }
    }
}
