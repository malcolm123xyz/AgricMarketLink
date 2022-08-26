package mx.moble.solution.backend.endpoints

import com.google.api.server.spi.config.Api
import com.google.api.server.spi.config.ApiMethod
import com.google.api.server.spi.config.ApiNamespace
import com.googlecode.objectify.NotFoundException
import com.googlecode.objectify.ObjectifyService
import mx.moble.solution.backend.model.*
import mx.moble.solution.backend.responses.Const
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
        val dataMap = HashMap<String, String>()
        dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_NEW_ANN
        dataMap["message"] = announcementData.message
        dataMap["heading"] = announcementData.heading
        dataMap["id"] = announcementData.id.toString()
        dataMap["annTyp"] = announcementData.annType.toString()
        dataMap["alarm"] = announcementData.eventDate.toString()
        NotificationManager("").sendNotDataOnly(dataMap)
        return success(null)
    }

    @ApiMethod(
        name = "qVote",
        path = "qVote",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun qVote(question: Question): Response<String> {
        return try {


            ObjectifyService.ofy().save().entity(question).now()
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_QUESTION_CHANGE
            dataMap["question"] = question.question
            dataMap["visibility"] = question.visibility.toString()
            dataMap["area"] = question.area
            dataMap["id"] = question.id
            NotificationManager("").sendNotDataOnly(dataMap)
            success(null)
        } catch (e: IOException) {
            error("Server error: ${e.localizedMessage}", "")
        }
    }

    @ApiMethod(
        name = "insertQuestion",
        path = "insertQuestion",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun insertQuestion(question: Question): Response<String> {
        return try {
            ObjectifyService.ofy().save().entity(question).now()
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_QUESTION_CHANGE
            dataMap["question"] = question.question
            dataMap["visibility"] = question.visibility.toString()
            dataMap["area"] = question.area
            dataMap["id"] = question.id
            NotificationManager("").sendNotDataOnly(dataMap)
            success(null)
        } catch (e: IOException) {
            error("Server error: ${e.localizedMessage}", "")
        }
    }

    @get:ApiMethod(
        name = "getQuestions",
        path = "getQuestions",
        httpMethod = ApiMethod.HttpMethod.GET
    )
    val questions: Response<List<Question>>
        get() {
            return try {
                val questions = ObjectifyService.ofy().load().type(
                    Question::class.java
                ).list()
                if (questions.isNotEmpty()) {
                    success(questions)
                } else {
                    error("No Questions found", null)
                }
            } catch (e: Exception) {
                error("An error occurred: ${e.localizedMessage}", null)
            }
        }

    @ApiMethod(
        name = "notifyExcelPublish",
        path = "notifyExcelPublish",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun notifyExcelPublish() {
        val dataMap = HashMap<String, String>()
        dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_EXCEL_UPDATE
        NotificationManager("").sendNotDataOnly(dataMap)
    }

    @ApiMethod(
        name = "deleteDuesBackup",
        path = "deleteDuesBackup",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun deleteDuesBackup(duesBackup: DuesBackup): Response<String> {
        return try {
            ObjectifyService.ofy().delete().entity(duesBackup).now()
            success(null)
        } catch (e: IOException) {
            error(e.localizedMessage, "")
        }
    }

    @ApiMethod(
        name = "deleteQuestion",
        path = "deleteQuestion",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun deleteQuestion(question: Question): Response<String> {
        return try {
            ObjectifyService.ofy().delete().entity(question).now()
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_QUESTION_CHANGE
            NotificationManager("").sendNotDataOnly(dataMap)
            success(null)

            success(null)
        } catch (e: IOException) {
            error(e.localizedMessage, "")
        }
    }

    @get:ApiMethod(
        name = "getDuesBackups",
        path = "getDuesBackups",
        httpMethod = ApiMethod.HttpMethod.GET
    )
    val duesBackup: Response<List<DuesBackup>>
        get() {
            val userDataModels: List<DuesBackup> = try {
                ObjectifyService.ofy().load().type(DuesBackup::class.java).list()
            } catch (e: IOException) {
                return error("Server error: ${e.localizedMessage}", null)
            } ?: return error("Server error: Data not found", null)
            return if (userDataModels.isEmpty()) {
                return error("Server error: Data not found", null)
            } else success(userDataModels)
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
        if (clearanceTP.position == Const.POS_PRESIDENT ||
            clearanceTP.position == Const.POS_VICE_PRESIDENT
            || clearanceTP.position == Const.POS_TREASURER
        ) {
            val records = ObjectifyService.ofy().load().type(LoginData::class.java).list()
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

        val regToken = ObjectifyService.ofy().load().type(RegistrationToken::class.java).list()
        var token = ""
        if (regToken.size > 0) {
            for (m in regToken) {
                if (m.folioNumber == clearanceTP.folio) {
                    token = m.token
                }
            }
        }

        if (token.isNotEmpty()) {
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_CLEARANCE
            dataMap["msg"] = clearanceTP.position
            dataMap["folio"] = clearanceTP.folio
            NotificationManager(token).sendNotDataOnly(dataMap)
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
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_DATABASE_UPDATE
            NotificationManager("").sendNotDataOnly(dataMap)
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
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_DATABASE_UPDATE
            NotificationManager("").sendNotDataOnly(dataMap)
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
        val dataMap = HashMap<String, String>()
        dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_DATABASE_UPDATE
        NotificationManager("").sendNotDataOnly(dataMap)

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
        val dataMap = HashMap<String, String>()
        dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_NEW_CONTRIBUTION
        dataMap["msg"] = contribution.message
        NotificationManager("").sendNotOnly("Silver Contribution Request", contribution.message)

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

            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_NEW_PAYMENT
            val title = "New Contribution Payment"
            val msg = "A user has made a contribution towards the ongoing Silver collection"

            NotificationManager("").sendNotOnly(title, msg)

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
            return Const.NOT_FOUND
        }
        return Const.ALREADY_SIGN_UP
    }

    @ApiMethod(
        name = "changePassword",
        path = "changePassword",
        httpMethod = ApiMethod.HttpMethod.POST
    )

    fun changePassword(ptp: PasswordChangeTP): Response<String> {

        try {
            val loginData =
                ObjectifyService.ofy().load().type(LoginData::class.java).id(ptp.folio).now()
            if (loginData == null) {
                return error("User information found", null)
            } else {
                if (ptp.oldP == loginData.password) {
                    loginData.password = ptp.newP
                    ObjectifyService.ofy().save().entity(loginData).now()
                    return success("")
                }
                return error("Wrong Password. Please enter the current password", "")
            }
        } catch (e: IOException) {
            return error("Server error: ${e.localizedMessage}", "")
        }
    }

    @ApiMethod(name = "signUp", path = "signUp", httpMethod = ApiMethod.HttpMethod.POST)

    fun signUp(loginData: LoginData): Response<LoginData> {

        //Check if suspended, not found or Already login
        val loginStatus = checkLoginStatus(loginData.folioNumber)
        if (loginStatus == Const.NOT_FOUND) {
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
            val dataMap = HashMap<String, String>()
            dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_DATABASE_UPDATE
            NotificationManager("").sendNotDataOnly(dataMap)
            return success(loginData)
        } else if (loginStatus == Const.ALREADY_SIGN_UP) {
            val resMsg =
                "Already signed up. If you have forgotten your folio number contact the administrator"
            error(resMsg, "")
        } else if (loginStatus == Const.SUSPENDED) {
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

                val dataMap = HashMap<String, String>()
                dataMap["NOTIFICATION_TYPE"] = Const.NOTIFY_DATABASE_UPDATE
                NotificationManager("").sendNotDataOnly(dataMap)
            }
            success(loginData)
        } else {
            error("Invalide password or Folio number", null)
        }
    }

    @ApiMethod(
        name = "resetPassword",
        path = "resetPassword",
        httpMethod = ApiMethod.HttpMethod.POST
    )
    fun resetPassword(@Named("folio") folio: String?): Response<LoginData> {
        val loginData = ObjectifyService.ofy().load().type(LoginData::class.java).id(folio).now()
            ?: return error("Not found. User is not signup or not login", null)
        loginData.password = "1234"
        ObjectifyService.ofy().save().entity(loginData).now()
        return success(null)
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