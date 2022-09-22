package mx.mobile.solution.nabia04_beta1.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by malcolm123xyz on 1/25/2016.
 */
@Entity(tableName = "user_data_table")
data class EntityUserData(
    @PrimaryKey
    var folioNumber: String = "",
    var fullName: String = "",
    var nickName: String = "",
    var sex: String = "",
    var homeTown: String = "",
    var contact: String = "",
    var districtOfResidence: String = "",
    var regionOfResidence: String = "",
    var email: String = "",
    var imageUri: String = "",
    var imageId: String = "",
    var birthDayAlarm: Long = 0,
    var className: String = "",
    var courseStudied: String = "",
    var house: String = "",
    var positionHeld: String = "",
    var jobDescription: String = "",
    var specificOrg: String = "",
    var employmentStatus: String = "",
    var employmentSector: String = "",
    var nameOfEstablishment: String = "",
    var establishmentRegion: String = "",
    var establishmentDist: String = "",
    var survivingStatus: Int = 0,
    var dateDeparted: String = "",
    var biography: String = "",
    var tributes: String = "",
)