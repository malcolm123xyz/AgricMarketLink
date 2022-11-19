package mx.mobile.solution.nabia04_beta1.data.entities

data class FcmToken(var token: String, var time: Long) {
    var sent: Boolean = false
}