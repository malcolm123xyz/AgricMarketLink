package mx.mobile.solution.nabia04.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by malcolm123xyz on 1/25/2016.
 */
@Entity(tableName = "question_table")
data class EntityQuestion(
    @PrimaryKey
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