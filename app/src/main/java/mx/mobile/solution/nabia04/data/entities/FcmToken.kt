package mx.mobile.solution.nabia04.data.entities

data class FcmToken(var token: String, var time: Long) {
    var isNewToken: Boolean = true
    var sent: Boolean = false
}