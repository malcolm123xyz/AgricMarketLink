package mx.moble.solution.backend.model

import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.annotation.Id
import com.googlecode.objectify.annotation.Index

/**
 * Created by malcolm123xyz on 1/25/2016.
 */
@Entity
class Question(
    @Id
    @Index
    var id: String = "",
    var folio: String = "",
    var imageUrl: String = "",
    var area: String = "",
    var from: String = "",
    var time: String = "",
    var question: String = "",
    var visibility: Boolean = true,
    var downVote: String = "0",
    var upVote: String = "0",
    var numReply: String = "0",
    var replyList: String = ""
)