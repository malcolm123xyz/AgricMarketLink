package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
data class Announcement(
    @Id
    @Index
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