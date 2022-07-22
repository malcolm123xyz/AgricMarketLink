package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

@Entity
class LoginData(
    @Id
    @Index
    var folioNumber: String = "",
    var emailAddress: String = "",
    var fullName: String = "",
    var contact: String = "",
    var password: String = "",

    @Index
    var accessToken: String = "",
    var executivePosition: String = "",
)