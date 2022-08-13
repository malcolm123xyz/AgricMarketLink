package mx.moble.solution.backend.endpoints

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.googlecode.objectify.ObjectifyService
import mx.moble.solution.backend.model.RegistrationToken
import java.io.IOException

class NotificationManager(val token: String) {

    fun sendNotDataOnly(nData: Map<String, String>) {
        val tokens = getTokens()
        println("Tokens: $tokens")
        val message = MulticastMessage.builder()
            .putAllData(nData)
            .addAllTokens(tokens)
            .build()

        send(message)
    }


    fun sendNotOnly(nTitle: String, nBody: String) {

        val tokens = getTokens()

        println("Tokens: $tokens")
        val message = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(nTitle)
                    .setBody(nBody)
                    .build()
            )
            .addAllTokens(tokens)
            .build()

        send(message)
    }

    private fun getTokens(): List<String> {
        val registrationTokens: MutableList<String> = ArrayList()
        if (token.isNotEmpty()) {
            registrationTokens.add(token)
            return registrationTokens
        }
        val records = ObjectifyService.ofy().load().type(RegistrationToken::class.java).list()
        for (s in records) {
            if (s.token.isNotEmpty()) {
                registrationTokens.add(s.token)
            }
        }
        return registrationTokens
    }

    private fun send(message: MulticastMessage) {
        val options = FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setDatabaseUrl("https://nabia04.firebaseio.com")
            .setProjectId("nabia04")
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
        try {
            val response = FirebaseMessaging.getInstance().sendMulticast(message)
            val responses = response.responses
            for (i in responses.indices) {
                if (responses[i].isSuccessful) {
                    //The order of responses corresponds to the order of the registration tokens.
                    println("Sending is successfull")
                }
            }
        } catch (e: FirebaseMessagingException) {
            e.printStackTrace()
            println("Error while sending msg: " + e.message)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}