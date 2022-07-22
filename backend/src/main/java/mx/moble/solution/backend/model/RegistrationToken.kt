package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

@Entity
class RegistrationToken(
    @Id
    var folioNumber: String = "",
    var fullName: String = "",

    @Index
    var token: String = "",
    var tokenTimeStamp: Long = 0L
)