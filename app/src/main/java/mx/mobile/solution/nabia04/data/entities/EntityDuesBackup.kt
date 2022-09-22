package mx.mobile.solution.nabia04.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "dues_backup_table")
data class EntityDuesBackup(
    @PrimaryKey
    var id: Long,
    var version: Int = 0,
    var totalAmount: String = "",
    var fileFullPath: String = "",
    var filePath: String = "",
    var fileName: String = "",
)