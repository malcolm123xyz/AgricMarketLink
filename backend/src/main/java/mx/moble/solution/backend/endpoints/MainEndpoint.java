package mx.moble.solution.backend.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.googlecode.objectify.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.xml.crypto.Data;

import mx.moble.solution.backend.dataModel.Announcement;
import mx.moble.solution.backend.dataModel.DatabaseObject;
import mx.moble.solution.backend.dataModel.LoginData;
import mx.moble.solution.backend.dataModel.RegistrationToken;
import mx.moble.solution.backend.others.ReturnObj;
import mx.moble.solution.backend.responses.AnnouncementResponse;
import mx.moble.solution.backend.responses.DatabaseResponse;
import mx.moble.solution.backend.responses.Response;
import mx.moble.solution.backend.responses.ResponseCodes;
import mx.moble.solution.backend.responses.SignUpLoginResponse;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "mainEndpoint",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "com.malcolm1234xyz.mx.mobile.solutions",
                ownerName = "backend.nabia04.endpoint",
                packagePath = ""
        )
)
public class MainEndpoint {

    private static final Logger logger = Logger.getLogger(LoginData.class.getName());


    @ApiMethod(name = "getNoticeBoardData",
            path = "getNoticeBoardData",
            httpMethod = ApiMethod.HttpMethod.GET)
    public AnnouncementResponse getNoticeBoardData() {

        List<Announcement> announcements;

        try {
            announcements = ofy().load().type(Announcement.class).list();
            if (announcements.size() > 0) {
                return AnnouncementResponse.OK(announcements);
            } else {
                return AnnouncementResponse.notFound();
            }
        } catch (Exception e) {
            return AnnouncementResponse.unknownError(e);
        }
    }

    @ApiMethod(
            name = "insertAnnouncement",
            path = "insertAnnouncement",
            httpMethod = ApiMethod.HttpMethod.POST)
    public AnnouncementResponse insertAnnouncement(
            Announcement announcementData, @Named("accessToken") String accessToken)
            throws IOException {

        if(!hasAccess(accessToken)){
            return AnnouncementResponse.noAccess();
        }

        ofy().save().entity(announcementData).now();

        Long timeStamp = announcementData.getId();
        String message = announcementData.getMessage();
        String heading = announcementData.getHeading();
        String id = Long.toString(timeStamp);
        String annType = String.valueOf(announcementData.getType());
        String eventDate = String.valueOf(announcementData.getEventDate());
        String priority = String.valueOf(announcementData.getPriority());

        List<RegistrationToken> records = ofy().load().type(RegistrationToken.class).list();


        List<String> registrationTokens = new ArrayList<>();

        for (RegistrationToken s : records) {
            registrationTokens.add(s.getToken());
        }

        if(registrationTokens.size() > 0){
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://nabia04.firebaseio.com")
                    .setProjectId("nabia04")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            MulticastMessage fbcMessage = MulticastMessage.builder()
                    .putData("NOTIFICATION_TYPE", ReturnObj.NOTIFY_NEW_ANN)
                    .putData("heading", heading)
                    .putData("id", id)
                    .putData("annTyp", annType)
                    .putData("eventDate", eventDate)
                    .putData("message", message)
                    .putData("priority", priority)
                    .putData("trancated", "no")
                    .addAllTokens(registrationTokens)
                    .build();

            BatchResponse response = null;
            try {
                response = FirebaseMessaging.getInstance().sendMulticast(fbcMessage);
            } catch (FirebaseMessagingException e) {
                e.printStackTrace();
                System.out.println("Error while sending msg: " + e.getMessage());
            }
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        // The order of responses corresponds to the order of the registration tokens.
                        String thisToken = registrationTokens.get(i);
                        System.out.println("Error while sending to this token: " + thisToken + "Error: " + responses.get(i).getException());
                        List<RegistrationToken> tokenData = ofy().load().type(RegistrationToken.class).filter("token =", thisToken).list();
                        if (tokenData != null && tokenData.size() > 0) {
                            System.out.println("Deleting token " + tokenData.get(0).getToken());
                            ofy().delete().entity(tokenData.get(0)).now();
                        }
                    }
                }
            }

        }

        return AnnouncementResponse.OK(null);
    }

    @ApiMethod(
            name = "getMembers",
            path = "getMembers",
            httpMethod = ApiMethod.HttpMethod.GET)
    public DatabaseResponse getMembers(@Named("accessToken") String accessToken) {

        if(!hasAccess(accessToken)){
            return DatabaseResponse.noAccess();
        }

        List<DatabaseObject> userDataModels;

        try {
            userDataModels = ofy().load().type(DatabaseObject.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return DatabaseResponse.unknownError(e);
        }
        if (userDataModels == null) {
            return DatabaseResponse.notFound();
        }

        if (userDataModels.size() < 1) {
            return DatabaseResponse.notFound();
        }
        return DatabaseResponse.OK(userDataModels);
    }

    @ApiMethod(
            name = "setUserClearance",
            path = "setUserClearance",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Response setUserClearance(@Named("folio") String folio, @Named("position") String position) throws IOException {
        logger.info("Position = "+position);

        if(position.equals("President") || position.equals("Vice President")){
            List<LoginData> records = ofy().load().type(LoginData.class).list();
            logger.info("Number of users found = "+records.size());
            for(LoginData user: records){
                logger.info("User: "+user.getFullName());
                logger.info("Position = "+user.getExecutivePosition());
                if(user.getExecutivePosition().equals(position)){
                    user.setExecutivePosition("NONE");
                    ofy().save().entity(user).now();
                    logger.info(user.getFullName()+" position set to none");
                    break;
                }
            }
        }
        logger.info("New user to set folio =  "+folio);
        LoginData loginData = ofy().load().type(LoginData.class).id(folio).now();
        if(loginData == null){
            return new Response("Specified user is not logged onto the app.", 0);
        }
        logger.info("New user to set = "+loginData.getFullName());
        loginData.setExecutivePosition(position);
        ofy().save().entity(loginData).now();
        RegistrationToken regToken = ofy().load().type(RegistrationToken.class).id(folio).now();
        if(regToken != null){
            notifyAction(ReturnObj.NOTIFY_CLEARANCE, getRegistrationTokens(regToken.getToken()), position, folio);
        }
        return Response.OK();
    }

    @ApiMethod(
            name = "sendMessageToMember",
            path = "sendMessageToMember",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj sendMessageToMember(@Named("folio") String folio, @Named("msg") String msg) throws IOException {
        ReturnObj retObj = new ReturnObj();
        retObj.setReturnCode(0);
        retObj.setReturnMsg("Member not found or unexpected error");
        RegistrationToken regToken = ofy().load().type(RegistrationToken.class).id(folio).now();
        if (regToken != null) {
            notifyAction(ReturnObj.NOTIFY_MESSAGE, getRegistrationTokens(regToken.getToken()), msg, "");
        }
        retObj.setReturnCode(1);
        return retObj;
    }

    @ApiMethod(
            name = "suspend",
            path = "suspend",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj suspend(@Named("folio") String folio, @Named("status") Integer status) throws IOException {
        ReturnObj retObj = new ReturnObj();
        retObj.setReturnCode(0);
        LoginData loginData = ofy().load().type(LoginData.class).id(folio).now();
        if (loginData != null) {
            loginData.setSuspended(status);
            ofy().save().entity(loginData).now();
            notifyAction(ReturnObj.NOTIFY_SUSPENSE, getRegistrationTokens(""), String.valueOf(status), folio);
        }
        retObj.setReturnCode(1);
        return retObj;
    }

    @ApiMethod(
            name = "setDeceaseStatus",
            path = "setDeceaseStatus",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj setDeceaseStatus(@Named("folio") String folio, @Named("date") String date,
                                      @Named("status") int status) throws IOException {
        logger.info("folio = " + folio + ", Date = " + date);
        ReturnObj retObj = new ReturnObj();
        retObj.setReturnCode(0);
        DatabaseObject dataBaseDataModel = ofy().load().type(DatabaseObject.class).id(folio).now();
        if (dataBaseDataModel != null) {
            dataBaseDataModel.setSurvivingStatus(status);
            dataBaseDataModel.setDateDeparted(date);
            ofy().save().entity(dataBaseDataModel).now();
            retObj.setReturnCode(1);
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
        }
        return retObj;
    }

    @ApiMethod(
            name = "setBiography",
            path = "setBiography",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Response setBiography(@Named("folio") String folio, @Named("biography") String biography) {
        logger.info("Folio: "+folio);
        logger.info("biograpgy: "+biography);
        DatabaseObject dataBaseDataModel = ofy().load().type(DatabaseObject.class).id(folio).now();
        if (dataBaseDataModel != null) {
            dataBaseDataModel.setBiography(biography);
            ofy().save().entity(dataBaseDataModel).now();
            return Response.OK();
        }
        return new Response();
    }

    @ApiMethod(
            name = "addTribute",
            path = "addTribute",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Response addTribute(@Named("folio") String folio, @Named("message") String message) {
        logger.info("Folio: "+folio);
        logger.info("Tribute: "+message);
        DatabaseObject dataBaseDataModel = ofy().load().type(DatabaseObject.class).id(folio).now();
        if (dataBaseDataModel != null) {
            dataBaseDataModel.setTributes(message);
            ofy().save().entity(dataBaseDataModel).now();
            logger.info("addTribute: DONE");
            return Response.OK();
        }
        return new Response();
    }

    @ApiMethod(
            name = "deleteUser",
            path = "deleteUser",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj deleteUser(@Named("folio") String folio) {
        ReturnObj retObj = new ReturnObj();
        retObj.setReturnCode(0);

        try {
            DatabaseObject databaseItem = ofy().load().type(DatabaseObject.class).id(folio).safe();
            ofy().delete().entity(databaseItem).now();
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
        } catch (NotFoundException | IOException e) {
            if (e instanceof NotFoundException) {
                retObj.setReturnCode(ReturnObj.NOT_FOUND);
                retObj.setReturnMsg("The user with this folio number was not found in the database");
                return retObj;
            }
            retObj.setReturnCode(ReturnObj.UNKNOWN_ERROR_CODE);
            retObj.setReturnMsg("Un error occurred please try again");
            return retObj;
        }

        retObj.setReturnCode(1);
        return retObj;
    }

    @ApiMethod(name = "addNewMember",
            path = "addNewMember",
            httpMethod = ApiMethod.HttpMethod.POST)
    public DatabaseResponse addNewMember(DatabaseObject memberData) {
        try {
            DatabaseObject user = ofy().load().type(DatabaseObject.class).id(memberData.getFolioNumber()).safe();
            return DatabaseResponse.AlreadyExist();
        } catch (com.googlecode.objectify.NotFoundException e) {
            ofy().save().entity(memberData).now();
            try {
                notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
            } catch (IOException ignored) {

            }
            return DatabaseResponse.OK();
        } catch (Exception e) {
            return DatabaseResponse.unknownError(e);
        }
    }

    @ApiMethod(name = "insertDataModel",
            path = "insertDataModel",
            httpMethod = ApiMethod.HttpMethod.POST)
    public DatabaseResponse upDateMemberDetails(DatabaseObject memberData) {
        ofy().save().entity(memberData).now();
        try {
            notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DatabaseResponse.OK();

    }

    private boolean alreadyInDB(String folio) {
        try {
            ofy().load().type(DatabaseObject.class).id(folio).safe();
            return true;
        } catch (com.googlecode.objectify.NotFoundException e) {
            return false;
        }
    }

    private boolean hasAccess(String accessToken) {
        logger.info("User accessToken = "+accessToken);
        List<LoginData> tokenData = ofy().load().type(LoginData.class).
                filter("accessToken =", accessToken).list();
        return tokenData.size() > 0;
    }

    private int checkLoginStatus(String id) {
        //Check if suspended, not found or Already login
        try {
            LoginData loginData = ofy().load().type(LoginData.class).id(id).safe();
            if (loginData.getSuspended() == 1) {
                return ReturnObj.SUSPENDED;
            }
        } catch (NotFoundException e) {
            return ReturnObj.NOT_FOUND;
        }
        return ReturnObj.ALREADY_SIGN_UP;
    }

    @ApiMethod(name = "signUp",
            path = "signUp",
            httpMethod = ApiMethod.HttpMethod.POST)
    public SignUpLoginResponse signUp(LoginData loginData) throws Exception{

        //Check if suspended, not found or Already login
        int loginStatus = checkLoginStatus(loginData.getFolioNumber());

        if (loginStatus == ReturnObj.NOT_FOUND) {
            loginData.setAccessToken(UUID.randomUUID().toString());
            ofy().save().entity(loginData).now();

            if(!alreadyInDB(loginData.getFolioNumber())){
                DatabaseObject updateModel = new DatabaseObject();
                updateModel.setFolioNumber(loginData.getFolioNumber());
                updateModel.setFullName(loginData.getFullName());
                updateModel.setEmail(loginData.getEmailAddress());
                updateModel.setContact(loginData.getContact());
                ofy().save().entity(updateModel).now();
            }

            try {
                notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
            } catch (IOException ignored) {}
            return new SignUpLoginResponse(loginData);
        } else if (loginStatus == ReturnObj.ALREADY_SIGN_UP) {
            String resMsg = "Already signed up. If you have forgotten your folio number contact the administrator";
            return new SignUpLoginResponse(resMsg, ResponseCodes.ALREADY_SIGN_UP);
        } else if (loginStatus == ReturnObj.SUSPENDED) {
            String resMsg = "Sorry You have been Suspended. Contact the PRO";
            return new SignUpLoginResponse(resMsg, ResponseCodes.SUSPENDED);
        }
        return new SignUpLoginResponse("Unknown Error", ResponseCodes.UNKNOWN_ERROR_CODE);
    }

    @ApiMethod(name = "upDateToken",
            path = "upDateToken",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj upDateToken(RegistrationToken regToken) {
        ReturnObj returnObj = new ReturnObj();
        ofy().save().entity(regToken).now();
        returnObj.setReturnCode(ReturnObj.OK);
        return returnObj;
    }

    @ApiMethod(name = "login",
            path = "login",
            httpMethod = ApiMethod.HttpMethod.GET)
    public SignUpLoginResponse login(@Named("folioNumber") String folioNumber, @Named("pass") String pass) {

        LoginData loginData = ofy().load().type(LoginData.class).id(folioNumber).now();

        if (loginData == null) {
            return new SignUpLoginResponse(
                    "Not signed up. Sign up first",
                    ResponseCodes.NOT_LOGGED_IN);
        }

        if (loginData.getSuspended() == 1) {
            String msg = "Sorry You have been Suspended. Contact the PRO";
            return new SignUpLoginResponse(msg, ResponseCodes.SUSPENDED);
        }

        if ((loginData.getPassword()).equals(pass)) {
            loginData.setAccessToken(UUID.randomUUID().toString());
            ofy().save().entity(loginData).now();

            if(!alreadyInDB(loginData.getFolioNumber())){
                DatabaseObject updatModel = new DatabaseObject();
                updatModel.setFolioNumber(loginData.getFolioNumber());
                updatModel.setFullName(loginData.getFullName());
                updatModel.setEmail(loginData.getEmailAddress());
                updatModel.setContact(loginData.getContact());
                updatModel.setDistrictOfResidence("");
                updatModel.setRegionOfResidence("");

                updatModel.setNickName("");
                updatModel.setSex("");
                updatModel.setHomeTown("");

                updatModel.setBirthDayAlarm(0);

                updatModel.setClassName("");
                updatModel.setCourseStudied("");
                updatModel.setHouse("");
                updatModel.setPositionHeld("");

                updatModel.setJobDescription("");
                updatModel.setSpecificOrg("");
                updatModel.setEmploymentSector("");
                updatModel.setEmploymentStatus("");
                updatModel.setNameOfEstablishment("");
                updatModel.setEstablishmentDist("");
                updatModel.setEstablishmentRegion("");

                ofy().save().entity(updatModel).now();

                try {
                    notifyAction(ReturnObj.NOTIFY_DATABASE_UPDATE, getRegistrationTokens(""), "", "");
                } catch (IOException ignored) {

                }
            }

            return new SignUpLoginResponse(loginData);
        } else {
            return new SignUpLoginResponse("Invalide password or Folio number",
                    ResponseCodes.WRONG_PASSWORD);
        }
    }

    private void notifyAction(String Action, List<String> registrationTokens, String optMsg, String folio) throws IOException {

        if (!registrationTokens.isEmpty()){
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://nabia04.firebaseio.com")
                    .setProjectId("nabia04")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            MulticastMessage fbcMessage = MulticastMessage.builder()
                    .putData("NOTIFICATION_TYPE", Action)
                    .putData("msg", optMsg)
                    .putData("folio", folio)
                    .addAllTokens(registrationTokens)
                    .build();

            BatchResponse response = null;
            try {
                response = FirebaseMessaging.getInstance().sendMulticast(fbcMessage);
            } catch (FirebaseMessagingException e) {
                e.printStackTrace();
                System.out.println("Error while sending msg: " + e.getMessage());
            }
            String reponseMsg = "All tokens sent successfull";
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        // The order of responses corresponds to the order of the registration tokens.
                        String thisToken = registrationTokens.get(i);
                        System.out.println("Error while sending to this token: " + thisToken + "Error: " + responses.get(i).getException());
                        List<RegistrationToken> tokenData = ofy().load().type(RegistrationToken.class).filter("token =", thisToken).list();
                        if (tokenData != null && tokenData.size() > 0) {
                            System.out.println("Deleting token " + tokenData.get(0).getToken());
                            ofy().delete().entity(tokenData.get(0)).now();
                        }
                        reponseMsg = "Could not send to some tokens";
                    }
                }
            }
            logger.info(reponseMsg);
        }
    }

    private List<String> getRegistrationTokens(String regtoken) {
        List<String> registrationTokens = new ArrayList<>();
        if (!regtoken.isEmpty()) {
            registrationTokens.add(regtoken);
            return registrationTokens;
        }
        List<RegistrationToken> records = ofy().load().type(RegistrationToken.class).list();
        for (RegistrationToken s : records) {
            if (!s.getToken().isEmpty()) {
                registrationTokens.add(s.getToken());
            }
        }
        return registrationTokens;
    }

    @ApiMethod(
            name = "deleteFromServer",
            path = "deleteFromServer",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReturnObj deleteFromServer(@Named("long") long id) {
        ReturnObj returnObj = new ReturnObj();
        try {
            ofy().delete().type(Announcement.class).id(id).now();
            returnObj.setReturnCode(1);
            returnObj.setReturnMsg("DONE");
            return returnObj;
        } catch (Exception e) {
            returnObj.setReturnCode(0);
            returnObj.setReturnMsg(e.getLocalizedMessage());
            return returnObj;
        }
    }

}