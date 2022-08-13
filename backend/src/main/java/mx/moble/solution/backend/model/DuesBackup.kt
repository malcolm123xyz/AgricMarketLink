package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

@Entity
class DuesBackup(
    @Id
    @Index
    var id: Long = 0L,
    var totalAmount: String = "",
    var downloadUrl: String = "",
    var published: Boolean = false

)