package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

@Entity
data class ContributionData(
    @Id
    @Index
    var id: String = "",
    var folio: String = "",
    var name: String = "",
    var message: String = "",
    var type: String = "",
    var imageUri: String = "",
    var imageId: String = "",
    var deadline: String = "",
    var momoNum: String = "",
    var momoName: String = "",
    var contribution: List<Map<String, String>> = ArrayList()
)