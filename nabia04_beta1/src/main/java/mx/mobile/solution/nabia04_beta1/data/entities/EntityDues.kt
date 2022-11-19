@file:Suppress("ArrayInDataClass")

package mx.mobile.solution.nabia04_beta1.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import mx.mobile.solution.nabia04_beta1.data.converters.DataConverter

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "yearly_dues_table")
data class EntityDues(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var index: String = "",
    var folio: String = "",
    var name: String = "",

    @get:TypeConverters(DataConverter::class)
    @set:TypeConverters(DataConverter::class)
    @TypeConverters(DataConverter::class)
    var payments: Array<String> = arrayOf()
)