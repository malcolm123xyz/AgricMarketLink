package mx.mobile.solution.nabia04.ui.activities

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import mx.mobile.solution.nabia04.utilities.Cons
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint

val endpoint: MainEndpoint
    get() {
        val builder = MainEndpoint.Builder(
            AndroidHttp.newCompatibleTransport(),
            AndroidJsonFactory(), null
        ).setRootUrl(Cons.ROOT_URL)
        return builder.build()
    }
