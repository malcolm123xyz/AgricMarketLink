package mx.moble.solution.backend.endpoints

import com.google.api.server.spi.config.Api
import com.google.api.server.spi.config.ApiMethod
import com.google.api.server.spi.config.ApiNamespace
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MulticastMessage
import com.googlecode.objectify.NotFoundException
import com.googlecode.objectify.ObjectifyService
import mx.moble.solution.backend.model.*
import mx.moble.solution.backend.responses.Constants
import mx.moble.solution.backend.responses.Response
import mx.moble.solution.backend.responses.Response.Companion.error
import mx.moble.solution.backend.responses.Response.Companion.success
import mx.moble.solution.backend.transport_model.*
import java.io.IOException
import java.util.*
import java.util.logging.Logger
import javax.inject.Named

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 *
 *
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
    name = "mainEndpoint",
    version = "v1",
    namespace = ApiNamespace(
        ownerDomain = "com.malcolm1234xyz.mx.mobile.solutions",
        ownerName = "backend.nabia04.endpoint",
        packagePath = ""
    )
)
class MainEndpoint {

    @get:ApiMethod(
        name = "getNoticeBoardData",
        path = "getNoticeBoardData",
        httpMethod = ApiMethod.HttpMethod.GET
    )
    val noticeBoardData: Response<List<Announcement>>
        get() {
            return try {
                val announcements = ObjectifyService.ofy().load().type(
                    Announcement::class.java
                ).list()
                if (announcements.isNotEmpty()) {
                    success(announcements)
                } else {
                    error("No Announcement found", null)
                }
            } catch (e: Exception) {
                error("An error occurred: ${e.localizedMessage}", null)
            }
        }

    @ApiMethod(
        name = "insertAnnouncement",
        path = "insertAnnouncement",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(IOException::class)
    fun insertAnnouncement(announcementData: Announcement): Response<String> {
        ObjectifyService.ofy().save().entity(announcementData).now()
        val timeStamp = announcementData.id
        val message = announcementData.message
        val heading = announcementData.heading
        val id = timeStamp.toString()
        val annType = announcementData.eventType.toString()
        val eventDate = announcementData.eventDate.toString()
        val priority = announcementData.priority.toString()
        val records = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).list()
        val registrationTokens: MutableList<String> = ArrayList()
        for (s in records) {
            registrationTokens.add(s.token)
        }
        if (registrationTokens.size > 0) {
            val options = FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setDatabaseUrl("https://nabia04.firebaseio.com")
                .setProjectId("nabia04")
                .build()
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            val fbcMessage = MulticastMessage.builder()
                .putData("NOTIFICATION_TYPE", Constants.NOTIFY_NEW_ANN)
                .putData("heading", heading)
                .putData("id", id)
                .putData("annTyp", annType)
                .putData("eventDate", eventDate)
                .putData("message", message)
                .putData("priority", priority)
                .putData("trancated", "no")
                .addAllTokens(registrationTokens)
                .build()
            var response: BatchResponse? = null
            try {
                response = FirebaseMessaging.getInstance().sendMulticast(fbcMessage)
            } catch (e: FirebaseMessagingException) {
                e.printStackTrace()
                println("Error while sending msg: " + e.message)
            }
            if (response!!.failureCount > 0) {
                val responses = response.responses
                for (i in responses.indices) {
                    if (!responses[i].isSuccessful) {
                        // The order of responses corresponds to the order of the registration tokens.
                        val thisToken = registrationTokens[i]
                        println("Error while sending to this token: " + thisToken + "Error: " + responses[i].exception)
                        val tokenData = ObjectifyService.ofy().load().type(
                            RegistrationToken::class.java
                        ).filter("token =", thisToken).list()
                        if (tokenData != null && tokenData.size > 0) {
                            println("Deleting token " + tokenData[0].token)
                            ObjectifyService.ofy().delete().entity(tokenData[0]).now()
                        }
                    }
                }
            }
        }
        return success(null)
    }

    @get:ApiMethod(name = "getMembers", path = "getMembers", httpMethod = ApiMethod.HttpMethod.GET)
    val members: Response<List<DatabaseObject>>
        get() {
            val userDataModels: List<DatabaseObject> = try {
                ObjectifyService.ofy().load().type(DatabaseObject::class.java).list()
            } catch (e: Exception) {
                return error("Server error: ${e.localizedMessage}", null)
            } ?: return error("Server error: Data not found", null)
            return if (userDataModels.isEmpty()) {
                return error("Server error: Data not found", null)
            } else success(userDataModels)
        }

    @ApiMethod(
        name = "setUserClearance",
        path = "setUserClearance",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun setUserClearance(clearanceTP: ClearanceTP): Response<String> {
        logger.info("Position = $clearanceTP.position")
        if (clearanceTP.position == "President" || clearanceTP.position == "Vice President") {
            val records = ObjectifyService.ofy().load().type(
                LoginData::class.java
            ).list()
            logger.info("Number of users found = " + records.size)
            for (user in records) {
                logger.info("User: " + user.fullName)
                logger.info("Position = " + user.executivePosition)
                if (user.executivePosition == clearanceTP.position) {
                    user.executivePosition = "NONE"
                    ObjectifyService.ofy().save().entity(user).now()
                    logger.info(user.fullName + " position set to none")
                    break
                }
            }
        }
        logger.info("New user to set folio =  $clearanceTP.folio")
        val loginData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).id(clearanceTP.folio).now()
            ?: return error("Specified user is not logged onto the app.", "")
        logger.info("New user to set = " + loginData.fullName)
        loginData.executivePosition = clearanceTP.position
        ObjectifyService.ofy().save().entity(loginData).now()
        val regToken = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).id(clearanceTP.folio).now()
        if (regToken != null) {
            notifyAction(
                Constants.NOTIFY_CLEARANCE,
                getRegistrationTokens(regToken.token),
                clearanceTP.position,
                clearanceTP.folio
            )
        }
        return success("")
    }

    @ApiMethod(
        name = "setDeceaseStatus",
        path = "setDeceaseStatus",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun setDeceaseStatus(data: DeceaseStatusTP): Response<String> {
        logger.info("folio: ${data.folio}, status: ${data.status}, date: ${data.date}")
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(data.folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.survivingStatus = data.status
            dataBaseDataModel.dateDeparted = data.date
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            //notifyAction(Constants.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            return success("")
        }
        return error("The specified user was not found in the database", "")
    }


    @ApiMethod(name = "setBiography", path = "setBiography", httpMethod = ApiMethod.HttpMethod.POST)
    fun setBiography(biographyTP: BiographyTP): Response<String> {
        logger.info("Folio: $biographyTP.folio")
        logger.info("biograpgy: ${biographyTP.bio}")
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(biographyTP.folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.biography = biographyTP.bio
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            return success("")
        }
        error("Server error please try again")
    }

    @ApiMethod(name = "addTribute", path = "addTribute", httpMethod = ApiMethod.HttpMethod.POST)
    fun addTribute(tribute: TributeTP): Response<String> {
        logger.info("Folio: $tribute.folio")
        logger.info("Tribute: $tribute.message")
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(tribute.folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.tributes = tribute.msg
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            logger.info("addTribute: DONE")
            return success("")
        }
        error("Server error. Please try again")
    }

    @ApiMethod(name = "deleteUser", path = "deleteUser", httpMethod = ApiMethod.HttpMethod.POST)
    fun deleteUser(@Named("folio") folio: String?): Response<String> {
        return try {
            val databaseItem = ObjectifyService.ofy().load().type(
                DatabaseObject::class.java
            ).id(folio).safe()
            ObjectifyService.ofy().delete().entity(databaseItem).now()
            notifyAction(Constants.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            success("")
        } catch (e: NotFoundException) {
            error("The user with this folio number was not found in the database", "")
        } catch (e: IOException) {
            error("Server error: ${e.localizedMessage}", "")
        }
    }

    @ApiMethod(name = "addNewMember", path = "addNewMember", httpMethod = ApiMethod.HttpMethod.POST)
    fun addNewMember(memberData: DatabaseObject): Response<String> {
        return try {
            ObjectifyService.ofy().load().type(
                DatabaseObject::class.java
            ).id(memberData.folioNumber).safe()
            error("The folio number you have entered already exist in the database", "")
        } catch (e: NotFoundException) {
            ObjectifyService.ofy().save().entity(memberData).now()
            //notifyAction(Constants.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            success("")
        } catch (e: Exception) {
            error("Server error: ${e.localizedMessage}")
        }
    }

    @ApiMethod(
        name = "insertDataModel",
        path = "insertDataModel",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun upDateMemberDetails(memberData: DatabaseObject): Response<String> {
        try {
            ObjectifyService.ofy().save().entity(memberData).now()
        } catch (e: IOException) {
            error("Server error: ${e.localizedMessage}")
        }
        //notifyAction(Constants.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
        return success("")
    }

    @ApiMethod(
        name = "setContRequest",
        path = "setContRequest",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun setContRequest(contribution: ContributionData): Response<String> {
        try {
            ObjectifyService.ofy().save().entity(contribution).now()
        } catch (e: IOException) {
            error("Server error: ${e.localizedMessage}")
        }
        notifyAction(
            Constants.NOTIFY_NEW_CONTRIBUTION,
            getRegistrationTokens(""),
            contribution.message,
            ""
        )
        return success("")
    }

    @get:ApiMethod(
        name = "getContributions",
        path = "getContributions",
        httpMethod = ApiMethod.HttpMethod.GET
    )
    val contributions: Response<ContributionData>
        get() {
            val contributionData: ContributionData?
            return try {
                contributionData = ObjectifyService.ofy().load().type(
                    ContributionData::class.java
                ).id("Nabia04_contr").now()
                if (contributionData != null) {
                    success(contributionData)
                } else {
                    error<ContributionData>("Unknown error, resource returned null", null)
                }
            } catch (e: Exception) {
                error<ContributionData>(e.localizedMessage, null)
            }
        }

    @ApiMethod(
        name = "upDateContPayment",
        path = "upDateContPayment",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun upDateContPayment(contributionData: ContributionData): Response<String> {
        return try {
            ObjectifyService.ofy().save().entity<Any>(contributionData).now()
            notifyAction(
                Constants.NOTIFY_NEW_PAYMENT,
                getRegistrationTokens(""),
                contributionData.name,
                contributionData.folio
            )
            success("")
        } catch (e: java.lang.Exception) {
            error("Server error: ${e.localizedMessage}", "")
        }
    }

    private fun alreadyInDB(folio: String): Boolean {
        return try {
            ObjectifyService.ofy().load().type(
                DatabaseObject::class.java
            ).id(folio).safe()
            true
        } catch (e: NotFoundException) {
            false
        }
    }

    private fun checkLoginStatus(id: String): Int {
        //Check if suspended, not found or Already login
        try {
            val loginData = ObjectifyService.ofy().load().type(
                LoginData::class.java
            ).id(id).safe()
        } catch (e: NotFoundException) {
            return Constants.NOT_FOUND
        }
        return Constants.ALREADY_SIGN_UP
    }

    @ApiMethod(name = "signUp", path = "signUp", httpMethod = ApiMethod.HttpMethod.POST)

    fun signUp(loginData: LoginData): Response<LoginData> {

        //Check if suspended, not found or Already login
        val loginStatus = checkLoginStatus(loginData.folioNumber)
        if (loginStatus == Constants.NOT_FOUND) {
            loginData.accessToken = UUID.randomUUID().toString()
            ObjectifyService.ofy().save().entity(loginData).now()
            if (!alreadyInDB(loginData.folioNumber)) {
                val updateModel = DatabaseObject()
                updateModel.folioNumber = loginData.folioNumber
                updateModel.fullName = loginData.fullName
                updateModel.email = loginData.emailAddress
                updateModel.contact = loginData.contact
                ObjectifyService.ofy().save().entity(updateModel).now()
            }
            //notifyAction(Constants.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            return success(loginData)
        } else if (loginStatus == Constants.ALREADY_SIGN_UP) {
            val resMsg =
                "Already signed up. If you have forgotten your folio number contact the administrator"
            error(resMsg, "")
        } else if (loginStatus == Constants.SUSPENDED) {
            val resMsg = "Sorry You have been Suspended. Contact the PRO"
            error(resMsg, "")
        }
        error("Unknown Error: Unknown error. Please try again")
    }

    @ApiMethod(name = "upDateToken", path = "upDateToken", httpMethod = ApiMethod.HttpMethod.POST)
    fun upDateToken(regToken: RegistrationToken): Response<String> {
        ObjectifyService.ofy().save().entity(regToken).now()
        return success("")
    }

    @ApiMethod(name = "userLogin", path = "userLogin", httpMethod = ApiMethod.HttpMethod.POST)
    fun userLogin(loginTP: LoginTP): Response<LoginData> {
        val loginData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).id(loginTP.folio).now()
            ?: return error("Not signed up. Sign up first", null)
        return if (loginData.password == loginTP.password) {
            loginData.accessToken = UUID.randomUUID().toString()
            ObjectifyService.ofy().save().entity(loginData).now()
            if (!alreadyInDB(loginData.folioNumber)) {
                val updatModel = DatabaseObject()
                updatModel.folioNumber = loginData.folioNumber
                updatModel.fullName = loginData.fullName
                updatModel.email = loginData.emailAddress
                updatModel.contact = loginData.contact
                updatModel.districtOfResidence = ""
                updatModel.regionOfResidence = ""
                updatModel.nickName = ""
                updatModel.sex = ""
                updatModel.homeTown = ""
                updatModel.birthDayAlarm = 0
                updatModel.className = ""
                updatModel.courseStudied = ""
                updatModel.house = ""
                updatModel.positionHeld = ""
                updatModel.jobDescription = ""
                updatModel.specificOrg = ""
                updatModel.employmentSector = ""
                updatModel.employmentStatus = ""
                updatModel.nameOfEstablishment = ""
                updatModel.establishmentDist = ""
                updatModel.establishmentRegion = ""
                ObjectifyService.ofy().save().entity(updatModel).now()
                try {
                    notifyAction(
                        Constants.NOTIFY_DATABASE_UPDATE,
                        getRegistrationTokens(""),
                        "",
                        ""
                    )
                } catch (ignored: IOException) {
                }
            }
            success(loginData)
        } else {
            error("Invalide password or Folio number", null)
        }
    }

    private fun notifyAction(
        Action: String,
        registrationTokens: List<String>,
        optMsg: String,
        folio: String
    ) {
        if (registrationTokens.isNotEmpty()) {
            val options = FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setDatabaseUrl("https://nabia04.firebaseio.com")
                .setProjectId("nabia04")
                .build()
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            val fbcMessage = MulticastMessage.builder()
                .putData("NOTIFICATION_TYPE", Action)
                .putData("msg", optMsg)
                .putData("folio", folio)
                .addAllTokens(registrationTokens)
                .build()
            val response: BatchResponse?
            var reponseMsg = "All tokens sent successfull"
            try {
                response = FirebaseMessaging.getInstance().sendMulticast(fbcMessage)
                if (response!!.failureCount > 0) {
                    val responses = response.responses
                    for (i in responses.indices) {
                        if (!responses[i].isSuccessful) {
                            // The order of responses corresponds to the order of the registration tokens.
                            val thisToken = registrationTokens[i]
                            println("Error while sending to this token: " + thisToken + "Error: " + responses[i].exception)
                            val tokenData = ObjectifyService.ofy().load().type(
                                RegistrationToken::class.java
                            ).filter("token =", thisToken).list()
                            if (tokenData != null && tokenData.size > 0) {
                                println("Deleting token " + tokenData[0].token)
                                ObjectifyService.ofy().delete().entity(tokenData[0]).now()
                            }
                            reponseMsg = "Could not send to some tokens"
                        }
                    }
                }
            } catch (e: FirebaseMessagingException) {
                e.printStackTrace()
                println("Error while sending msg: " + e.message)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            logger.info(reponseMsg)
        }
    }

    private fun getRegistrationTokens(regtoken: String): List<String> {
        val registrationTokens: MutableList<String> = ArrayList()
        if (regtoken.isNotEmpty()) {
            registrationTokens.add(regtoken)
            return registrationTokens
        }
        val records = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).list()
        for (s in records) {
            if (s.token.isNotEmpty()) {
                registrationTokens.add(s.token)
            }
        }
        return registrationTokens
    }

    @ApiMethod(
        name = "deleteFromServer",
        path = "deleteFromServer",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun deleteFromServer(@Named("long") id: Long): Response<String> {
        return try {
            ObjectifyService.ofy().delete().type(Announcement::class.java).id(id).now()
            return success("null")
        } catch (e: Exception) {
            error("Server error: ${e.localizedMessage}", "")
        }
    }

    companion object {
        private val logger = Logger.getLogger(LoginData::class.java.name)
    }
}