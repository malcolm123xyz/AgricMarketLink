package mx.mobile.solution.nabia04_beta1.workManager

import mx.mobile.solution.nabia04_beta1.data.entities.FcmToken
import java.io.IOException

interface MyCallback {
    fun onFailure(e: IOException?)
    fun onSuccess()
    fun onSaveToken(fcmToken: FcmToken)
}