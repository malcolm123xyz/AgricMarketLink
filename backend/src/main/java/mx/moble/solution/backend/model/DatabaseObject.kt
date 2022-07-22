package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

/**
 * Created by malcolm123xyz on 1/25/2016.
 */
@Entity
class DatabaseObject(
    @Id
    @Index
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