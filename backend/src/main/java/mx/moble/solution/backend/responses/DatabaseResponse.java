package mx.moble.solution.backend.responses;

import java.util.List;

import mx.moble.solution.backend.dataModel.Announcement;
import mx.moble.solution.backend.dataModel.DatabaseObject;


public class DatabaseResponse {

    public static String response = "DONE";
    private static int returnCode = ResponseCodes.OK;
    private List<DatabaseObject> databaseObj;

    public DatabaseResponse(String msg, int code){
        response = msg;
        returnCode = code;
    }

    private DatabaseResponse(){

    }

    public String getResponse() {
        return response;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public List<DatabaseObject> getDatabaseObj() {
        return databaseObj;
    }

    public void setDatabaseObj(List<DatabaseObject> databaseObj) {
        this.databaseObj = databaseObj;
    }

    public static DatabaseResponse OK(List<DatabaseObject> announcements){
        DatabaseResponse response = new DatabaseResponse();
        response.setDatabaseObj(announcements);
        return response;
    }

    public static DatabaseResponse OK(){
        return new DatabaseResponse("", ResponseCodes.OK);
    }

    public static DatabaseResponse noAccess() {
        String msg = "No Access: You cannot access this function. Kindly login again or contact the PRO";
        return new DatabaseResponse(msg, ResponseCodes.NOT_LOGGED_IN);
    }

    public static DatabaseResponse AlreadyExist() {
        String msg = "The folio number you have entered already exist in the database";
        return new DatabaseResponse(msg, ResponseCodes.ALREADY_EXIST);
    }

    public static DatabaseResponse notFound() {
        return new DatabaseResponse("Database is empty",
                ResponseCodes.NOT_FOUND);
    }

    public static DatabaseResponse unknownError(Exception e) {
        return new DatabaseResponse("An Error occurred: "+e.getLocalizedMessage(),
                ResponseCodes.NOT_FOUND);
    }
}
