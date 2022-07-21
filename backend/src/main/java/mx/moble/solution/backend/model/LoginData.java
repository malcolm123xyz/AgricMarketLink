package mx.moble.solution.backend.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class LoginData {
    @Id
    @Index
    private String folioNumber;
    private String emailAddress;
    private String fullName;
    private String contact;
    private String password;
    @Index
    private String accessToken;
    private int suspended;
    private String executivePosition;

    public String getExecutivePosition() {
        return executivePosition;
    }

    public void setExecutivePosition(String executivePosition) {
        this.executivePosition = executivePosition;
    }

    public int getSuspended() {
        return suspended;
    }

    public void setSuspended(int suspended) {
        this.suspended = suspended;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFolioNumber() {
        return folioNumber;
    }

    public void setFolioNumber(String folioNumber) {
        this.folioNumber = folioNumber;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
