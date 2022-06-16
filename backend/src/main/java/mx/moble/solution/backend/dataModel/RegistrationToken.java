package mx.moble.solution.backend.dataModel;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class RegistrationToken {
    @Id
    private String folioNumber;
    private String fullName;
    @Index
    private String token;
    private long tokenTimeStamp;

    public String getFolioNumber() {
        return folioNumber;
    }

    public void setFolioNumber(String folioNumber) {
        this.folioNumber = folioNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getTokenTimeStamp() {
        return tokenTimeStamp;
    }

    public void setTokenTimeStamp(long tokenTimeStamp) {
        this.tokenTimeStamp = tokenTimeStamp;
    }
}
