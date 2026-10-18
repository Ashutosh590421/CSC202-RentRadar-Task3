package com.example.rentradar

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    var title: String = "",
    var address: String = "",
    var inspectionDate: Long = System.currentTimeMillis(),

    var propertyType: String = "Apartment",
    var weeklyRent: Int = 0,
    var bedrooms: Int = 0,
    var applicationStatus: String = "Inspected",

    var notes: String = "",

    var latitude: Double? = null,
    var longitude: Double? = null,

    var photoPath: String? = null
)
