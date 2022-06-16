package mx.moble.solution.backend.responses;

import java.util.List;

import mx.moble.solution.backend.dataModel.Announcement;
import mx.moble.solution.backend.dataModel.LoginData;


public class AnnouncementResponse {

    public static String response = "DONE";
    private static int returnCode = ResponseCodes.UNKNOWN_ERROR_CODE;

    private List<Announcement> announcements;

    public AnnouncementResponse(String msg, int code){
        response = msg;
        returnCode = code;
    }

    private AnnouncementResponse(){

    }

    public String getResponse() {
        return response;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(List<Announcement> announcements) {
        this.announcements = announcements;
    }

    public static AnnouncementResponse OK(List<Announcement> announcements){
        AnnouncementResponse response = new AnnouncementResponse("",1);
        response.setAnnouncements(announcements);
        return response;
    }

    public static AnnouncementResponse noAccess() {
        String msg = "No Access: You cannot access this function. Kindly login again or contact the PRO";
        return new AnnouncementResponse(msg, ResponseCodes.NOT_LOGGED_IN);
    }

    public static AnnouncementResponse notFound() {
        return new AnnouncementResponse("No Announcement found",
                ResponseCodes.NOT_FOUND);
    }

    public static AnnouncementResponse unknownError(Exception e) {
        return new AnnouncementResponse("An Error occurred: "+e.getLocalizedMessage(),
                ResponseCodes.NOT_FOUND);
    }
}
