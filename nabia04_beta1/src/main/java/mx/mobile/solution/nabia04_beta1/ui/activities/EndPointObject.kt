package mx.mobile.solution.nabia04_beta1.ui.activities

import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import mx.mobile.solution.nabia04_beta1.utilities.Const
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint

val endpoint: MainEndpoint
    get() {
        val builder = MainEndpoint.Builder(
            NetHttpTransport(), AndroidJsonFactory(), null
        ).setRootUrl(Const.ROOT_URL)
        return builder.build()
    }
