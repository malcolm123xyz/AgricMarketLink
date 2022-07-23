package mx.mobile.solution.nabia04.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "announcement_table")
data class EntityAnnouncement(
    @PrimaryKey
    var id: Long = 0L,
    var heading: String = "",
    var message: String = "",
    var imageUri: String = "",
    var annType: Int = 0,
    var eventType: Int = 0,
    var eventDate: Long = 0L,
    var priority: Int = 0,
    var rowNum: Int = 0,
    var venue: String = "",
)