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
import mx.moble.solution.backend.dataModel.*
import mx.moble.solution.backend.others.ReturnObj
import mx.moble.solution.backend.responses.*
import mx.moble.solution.backend.responses.Resource.Companion.error
import mx.moble.solution.backend.responses.Resource.Companion.success
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
    val noticeBoardData: AnnouncementResponse
        get() {
            val announcements: List<Announcement>
            return try {
                announcements = ObjectifyService.ofy().load().type(
                    Announcement::class.java
                ).list()
                if (announcements.isNotEmpty()) {
                    AnnouncementResponse.OK(announcements)
                } else {
                    AnnouncementResponse.notFound()
                }
            } catch (e: Exception) {
                AnnouncementResponse.unknownError(e)
            }
        }

    @ApiMethod(
        name = "insertAnnouncement",
        path = "insertAnnouncement",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun insertAnnouncement(
        announcementData: Announcement, @Named("accessToken") accessToken: String
    ): AnnouncementResponse {
        if (!hasAccess(accessToken)) {
            return AnnouncementResponse.noAccess()
        }
        ObjectifyService.ofy().save().entity(announcementData).now()
        val timeStamp = announcementData.id
        val message = announcementData.message
        val heading = announcementData.heading
        val id = java.lang.Long.toString(timeStamp)
        val annType = announcementData.type.toString()
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
                .putData("NOTIFICATION_TYPE", ReturnObj.NOTIFY_NEW_ANN)
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
        return AnnouncementResponse.OK(null)
    }

    @get:ApiMethod(name = "getMembers", path = "getMembers", httpMethod = ApiMethod.HttpMethod.GET)
    val members: DatabaseResponse
        get() {
            val userDataModels: List<DatabaseObject>?
            userDataModels = try {
                ObjectifyService.ofy().load().type(
                    DatabaseObject::class.java
                ).list()
            } catch (e: Exception) {
                e.printStackTrace()
                return DatabaseResponse.unknownError(e)
            }
            if (userDataModels == null) {
                return DatabaseResponse.notFound()
            }
            return if (userDataModels.size < 1) {
                DatabaseResponse.notFound()
            } else DatabaseResponse.OK(userDataModels)
        }

    @ApiMethod(
        name = "setUserClearance",
        path = "setUserClearance",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun setUserClearance(
        @Named("folio") folio: String,
        @Named("position") position: String
    ): Response {
        logger.info("Position = $position")
        if (position == "President" || position == "Vice President") {
            val records = ObjectifyService.ofy().load().type(
                LoginData::class.java
            ).list()
            logger.info("Number of users found = " + records.size)
            for (user in records) {
                logger.info("User: " + user.fullName)
                logger.info("Position = " + user.executivePosition)
                if (user.executivePosition == position) {
                    user.executivePosition = "NONE"
                    ObjectifyService.ofy().save().entity(user).now()
                    logger.info(user.fullName + " position set to none")
                    break
                }
            }
        }
        logger.info("New user to set folio =  $folio")
        val loginData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).id(folio).now()
            ?: return Response("Specified user is not logged onto the app.", 0)
        logger.info("New user to set = " + loginData.fullName)
        loginData.executivePosition = position
        ObjectifyService.ofy().save().entity(loginData).now()
        val regToken = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).id(folio).now()
        if (regToken != null) {
            notifyAction(
                ReturnObj.NOTIFY_CLEARANCE,
                getRegistrationTokens(regToken.token),
                position,
                folio
            )
        }
        return Response.OK()
    }

    @ApiMethod(
        name = "sendMessageToMember",
        path = "sendMessageToMember",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun sendMessageToMember(@Named("folio") folio: String?, @Named("msg") msg: String): ReturnObj {
        val retObj = ReturnObj()
        retObj.returnCode = 0
        retObj.returnMsg = "Member not found or unexpected error"
        val regToken = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).id(folio).now()
        if (regToken != null) {
            notifyAction(ReturnObj.NOTIFY_MESSAGE, getRegistrationTokens(regToken.token), msg, "")
        }
        retObj.returnCode = 1
        return retObj
    }

    @ApiMethod(name = "suspend", path = "suspend", httpMethod = ApiMethod.HttpMethod.POST)
    @Throws(
        IOException::class
    )
    fun suspend(@Named("folio") folio: String, @Named("status") status: Int): ReturnObj {
        val retObj = ReturnObj()
        retObj.returnCode = 0
        val loginData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).id(folio).now()
        if (loginData != null) {
            loginData.suspended = status
            ObjectifyService.ofy().save().entity(loginData).now()
            notifyAction(
                ReturnObj.NOTIFY_SUSPENSE,
                getRegistrationTokens(""),
                status.toString(),
                folio
            )
        }
        retObj.returnCode = 1
        return retObj
    }

    @ApiMethod(
        name = "setDeceaseStatus",
        path = "setDeceaseStatus",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun setDeceaseStatus(
        @Named("date") date: String, @Named("folio") folio: String,
        @Named("status") status: Int
    ): ReturnObj {
        logger.info("folio = $folio, Date = $date Status = $status")
        val retObj = ReturnObj()
        retObj.returnCode = 0
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.survivingStatus = status
            dataBaseDataModel.dateDeparted = date
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            retObj.returnCode = 1
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
        }
        return retObj
    }

    @ApiMethod(name = "setBiography", path = "setBiography", httpMethod = ApiMethod.HttpMethod.POST)
    fun setBiography(
        @Named("folio") folio: String,
        @Named("biography") biography: String
    ): Response {
        logger.info("Folio: $folio")
        logger.info("biograpgy: $biography")
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.biography = biography
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            return Response.OK()
        }
        return Response()
    }

    @ApiMethod(name = "addTribute", path = "addTribute", httpMethod = ApiMethod.HttpMethod.POST)
    fun addTribute(@Named("folio") folio: String, @Named("message") message: String): Response {
        logger.info("Folio: $folio")
        logger.info("Tribute: $message")
        val dataBaseDataModel = ObjectifyService.ofy().load().type(
            DatabaseObject::class.java
        ).id(folio).now()
        if (dataBaseDataModel != null) {
            dataBaseDataModel.tributes = message
            ObjectifyService.ofy().save().entity(dataBaseDataModel).now()
            logger.info("addTribute: DONE")
            return Response.OK()
        }
        return Response()
    }

    @ApiMethod(name = "deleteUser", path = "deleteUser", httpMethod = ApiMethod.HttpMethod.POST)
    fun deleteUser(@Named("folio") folio: String?): ReturnObj {
        val retObj = ReturnObj()
        retObj.returnCode = 0
        try {
            val databaseItem = ObjectifyService.ofy().load().type(
                DatabaseObject::class.java
            ).id(folio).safe()
            ObjectifyService.ofy().delete().entity(databaseItem).now()
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
        } catch (e: NotFoundException) {
            if (e is NotFoundException) {
                retObj.returnCode = ReturnObj.NOT_FOUND
                retObj.returnMsg = "The user with this folio number was not found in the database"
                return retObj
            }
            retObj.returnCode = ReturnObj.UNKNOWN_ERROR_CODE
            retObj.returnMsg = "Un error occurred please try again"
            return retObj
        } catch (e: IOException) {
            if (e is NotFoundException) {
                retObj.returnCode = ReturnObj.NOT_FOUND
                retObj.returnMsg = "The user with this folio number was not found in the database"
                return retObj
            }
            retObj.returnCode = ReturnObj.UNKNOWN_ERROR_CODE
            retObj.returnMsg = "Un error occurred please try again"
            return retObj
        }
        retObj.returnCode = 1
        return retObj
    }

    @ApiMethod(name = "addNewMember", path = "addNewMember", httpMethod = ApiMethod.HttpMethod.POST)
    fun addNewMember(memberData: DatabaseObject): DatabaseResponse {
        return try {
            ObjectifyService.ofy().load().type(
                DatabaseObject::class.java
            ).id(memberData.folioNumber).safe()
            DatabaseResponse.AlreadyExist()
        } catch (e: NotFoundException) {
            ObjectifyService.ofy().save().entity(memberData).now()
            try {
                notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            } catch (ignored: IOException) {
            }
            DatabaseResponse.OK()
        } catch (e: Exception) {
            DatabaseResponse.unknownError(e)
        }
    }

    @ApiMethod(
        name = "insertDataModel",
        path = "insertDataModel",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun upDateMemberDetails(memberData: DatabaseObject): DatabaseResponse {
        ObjectifyService.ofy().save().entity(memberData).now()
        try {
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return DatabaseResponse.OK()
    }

    @ApiMethod(
        name = "setContRequest",
        path = "setContRequest",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    @Throws(
        IOException::class
    )
    fun setContRequest(contribution: ContributionData): ReturnObj {
        val returnObj = ReturnObj()
        ObjectifyService.ofy().save().entity(contribution).now()
        notifyAction(
            ReturnObj.NOTIFY_NEW_CONTRIBUTION,
            getRegistrationTokens(""),
            contribution.message,
            ""
        )
        returnObj.returnCode = 1
        returnObj.returnMsg = contribution.id
        return returnObj
    }

    @get:ApiMethod(
        name = "getContributions",
        path = "getContributions",
        httpMethod = ApiMethod.HttpMethod.GET
    )
    val contributions: Resource<ContributionData>
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

    private fun hasAccess(accessToken: String): Boolean {
        logger.info("User accessToken = $accessToken")
        val tokenData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).filter("accessToken =", accessToken).list()
        return tokenData.size > 0
    }

    private fun checkLoginStatus(id: String): Int {
        //Check if suspended, not found or Already login
        try {
            val loginData = ObjectifyService.ofy().load().type(
                LoginData::class.java
            ).id(id).safe()
            if (loginData.suspended == 1) {
                return ReturnObj.SUSPENDED
            }
        } catch (e: NotFoundException) {
            return ReturnObj.NOT_FOUND
        }
        return ReturnObj.ALREADY_SIGN_UP
    }

    @ApiMethod(name = "signUp", path = "signUp", httpMethod = ApiMethod.HttpMethod.POST)
    @Throws(
        Exception::class
    )
    fun signUp(loginData: LoginData): SignUpLoginResponse {

        //Check if suspended, not found or Already login
        val loginStatus = checkLoginStatus(loginData.folioNumber)
        if (loginStatus == ReturnObj.NOT_FOUND) {
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
            try {
                notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "")
            } catch (ignored: IOException) {
            }
            return SignUpLoginResponse(loginData)
        } else if (loginStatus == ReturnObj.ALREADY_SIGN_UP) {
            val resMsg =
                "Already signed up. If you have forgotten your folio number contact the administrator"
            return SignUpLoginResponse(resMsg, ResponseCodes.ALREADY_SIGN_UP)
        } else if (loginStatus == ReturnObj.SUSPENDED) {
            val resMsg = "Sorry You have been Suspended. Contact the PRO"
            return SignUpLoginResponse(resMsg, ResponseCodes.SUSPENDED)
        }
        return SignUpLoginResponse("Unknown Error", ResponseCodes.UNKNOWN_ERROR_CODE)
    }

    @ApiMethod(name = "upDateToken", path = "upDateToken", httpMethod = ApiMethod.HttpMethod.POST)
    fun upDateToken(regToken: RegistrationToken): ReturnObj {
        val returnObj = ReturnObj()
        ObjectifyService.ofy().save().entity(regToken).now()
        returnObj.returnCode = ReturnObj.OK
        return returnObj
    }

    @ApiMethod(name = "login", path = "login", httpMethod = ApiMethod.HttpMethod.GET)
    fun login(
        @Named("folioNumber") folioNumber: String?,
        @Named("pass") pass: String
    ): SignUpLoginResponse {
        val loginData = ObjectifyService.ofy().load().type(
            LoginData::class.java
        ).id(folioNumber).now()
            ?: return SignUpLoginResponse(
                "Not signed up. Sign up first",
                ResponseCodes.NOT_LOGGED_IN
            )
        if (loginData.suspended == 1) {
            val msg = "Sorry You have been Suspended. Contact the PRO"
            return SignUpLoginResponse(msg, ResponseCodes.SUSPENDED)
        }
        return if (loginData.password == pass) {
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
                        ReturnObj.NOTIFY_DATABASE_UPDATE,
                        getRegistrationTokens(""),
                        "",
                        ""
                    )
                } catch (ignored: IOException) {
                }
            }
            SignUpLoginResponse(loginData)
        } else {
            SignUpLoginResponse(
                "Invalide password or Folio number",
                ResponseCodes.WRONG_PASSWORD
            )
        }
    }

    @Throws(IOException::class)
    private fun notifyAction(
        Action: String,
        registrationTokens: List<String>,
        optMsg: String,
        folio: String
    ) {
        if (!registrationTokens.isEmpty()) {
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
            var response: BatchResponse? = null
            try {
                response = FirebaseMessaging.getInstance().sendMulticast(fbcMessage)
            } catch (e: FirebaseMessagingException) {
                e.printStackTrace()
                println("Error while sending msg: " + e.message)
            }
            var reponseMsg = "All tokens sent successfull"
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
            logger.info(reponseMsg)
        }
    }

    private fun getRegistrationTokens(regtoken: String): List<String> {
        val registrationTokens: MutableList<String> = ArrayList()
        if (!regtoken.isEmpty()) {
            registrationTokens.add(regtoken)
            return registrationTokens
        }
        val records = ObjectifyService.ofy().load().type(
            RegistrationToken::class.java
        ).list()
        for (s in records) {
            if (!s.token.isEmpty()) {
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
    fun deleteFromServer(@Named("long") id: Long): ReturnObj {
        val returnObj = ReturnObj()
        return try {
            ObjectifyService.ofy().delete().type(Announcement::class.java).id(id).now()
            returnObj.returnCode = 1
            returnObj.returnMsg = "DONE"
            returnObj
        } catch (e: Exception) {
            returnObj.returnCode = 0
            returnObj.returnMsg = e.localizedMessage
            returnObj
        }
    }

    companion object {
        private val logger = Logger.getLogger(LoginData::class.java.name)
    }
}