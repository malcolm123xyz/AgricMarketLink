package mx.moble.solution.backend.others;
import java.util.List;

import mx.moble.solution.backend.dataModel.Announcement;
import mx.moble.solution.backend.dataModel.ContributionData;
import mx.moble.solution.backend.dataModel.DatabaseObject;
import mx.moble.solution.backend.dataModel.EventsPicsData;
import mx.moble.solution.backend.dataModel.HotSeatPersonDataModel;
import mx.moble.solution.backend.dataModel.LoginData;

/**
 * Created by malcolm123xyz on 8/23/2016.
 */
public class ReturnObj {
    public static final int OK = 1;
    public static final int ALREADY_SIGN_UP = 0;
    public static final int SUSPENDED = 2;
    public static final int NOT_FOUND = 3;
    public static final int UNKNOWN_ERROR_CODE = 99;
    public static final int NOT_LOGED_IN = 4;
    public static final int WRONG_PASSWORD = 5;
    public static final String NOTIFY_USER_DELETED = "userDeleted";
    public static final String NOTIFY_SUSPENSE = "notifySuspense";
    public static final String NOTIFY_MESSAGE = "notifyMessage";
    public static final String NOTIFY_CLEARANCE = "notifyClearance";
    public static final String NOTIFY_NEW_PICTURE = "notifyNewPic";
    public static final String NOTIFY_NEW_CONTRIBUTION = "notifyNewContribution";
    public static final String NOTIFY_NEW_ANN = "notifyNewAnn";
    public static final String NOTIFY_DATABASE_UPDATE = "notifyDatabaseUpdate";
    public static final String NOTIFY_NEW_PAYMENT = "notifyNewPayment";
    private List<DatabaseObject> dataBaseData;
    private List<Announcement> announcements;
    private ContributionData contributionData;
    private List<HotSeatPersonDataModel> hotSeatPerfObjList;
    private HotSeatPersonDataModel hotSeatData;
    private List<EventsPicsData> eventsPicsData;
    private LoginData loginData;
    private String returnMsg;
    private int returnCode;
    private int hotSeatNumberOftimes;
    private String token;
    public static final String PRESIDENT = "President";
    public static final String VICE_PRESIDENT = "Vice President";
    public static final String SECRETARY = "Secretary";
    public static final String TREASURER = "Treasurer";
    public static final String PRO = "PRO";


    public ReturnObj(){
        returnCode = -1;
        returnMsg = "Unknown error: Please try again";
    }


    public ReturnObj(int returnCode, String returnMsg){
        this.returnCode = returnCode;
        this.returnMsg = returnMsg;
    }

    public LoginData getLoginData() {
        return loginData;
    }

    public void setLoginData(LoginData loginData) {
        this.loginData = loginData;
    }

    public List<EventsPicsData> getEventsPicsData() {
        return eventsPicsData;
    }

    public void setEventsPicsData(List<EventsPicsData> eventsPicsData) {
        this.eventsPicsData = eventsPicsData;
    }

    public ContributionData getContributionData() {
        return contributionData;
    }

    public void setContributionData(ContributionData contributionData) {
        this.contributionData = contributionData;
    }

    public int getHotSeatNumberOftimes() {
        return hotSeatNumberOftimes;
    }

    public void setHotSeatNumberOftimes(int hotSeatNumberOftimes) {
        this.hotSeatNumberOftimes = hotSeatNumberOftimes;
    }

    public HotSeatPersonDataModel getHotSeatData() {
        return hotSeatData;
    }

    public void setHotSeatData(HotSeatPersonDataModel hotSeatData) {
        this.hotSeatData = hotSeatData;
    }

    public List<HotSeatPersonDataModel> getHotSeatPerfObjList() {
        return hotSeatPerfObjList;
    }

    public void setHotSeatPerfObjList(List<HotSeatPersonDataModel> hotSeatPerfObjList) {
        this.hotSeatPerfObjList = hotSeatPerfObjList;
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(List<Announcement> announcements) {
        this.announcements = announcements;
    }

    public List<DatabaseObject> getDataBaseData() {
        return dataBaseData;
    }

    public void setDataBaseData(List<DatabaseObject> dataBaseData) {
        this.dataBaseData = dataBaseData;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMsg() {
        return returnMsg;
    }

    public void setReturnMsg(String returnMsg) {
        this.returnMsg = returnMsg;
    }

}
