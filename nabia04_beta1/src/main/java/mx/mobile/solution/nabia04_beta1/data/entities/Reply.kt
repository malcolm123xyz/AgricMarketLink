package mx.mobile.solution.nabia04_beta1.data.entities

/**
 * Created by malcolm123xyz on 1/25/2016.
 */

data class Reply(
    var id: String = "",
    var folio: String = "",
    var questionId: String = "",
    var imageUrl: String = "",
    var from: String = "",
    var time: String = "",
    var reply: String = "",
    var downVote: String = "0",
    var upVote: String = "0",
    var numReply: String = "0"
)