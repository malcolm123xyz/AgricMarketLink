package mx.mobile.solution.nabia04.workManager

import mx.mobile.solution.nabia04.data.entities.FcmToken
import java.io.IOException

interface MyCallback {
    fun onFailure(e: IOException?)
    fun onSuccess()
    fun onSaveToken(fcmToken: FcmToken)
}