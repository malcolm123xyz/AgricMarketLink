package mx.mobile.solution.nabia04.main.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "error_table")
public class EntityError {

    @PrimaryKey
    @NonNull
    private long id;
    private String errorMsg;
    private String errorCause;
    private String logLink;
    private String phoneName;

    public String getPhoneName() {
        return phoneName;
    }

    public void setPhoneName(String phoneName) {
        this.phoneName = phoneName;
    }

    @NonNull
    public long getId() {
        return id;
    }

    public void setId(@NonNull long id) {
        this.id = id;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    public String getLogLink() {
        return logLink;
    }

    public void setLogLink(String logLink) {
        this.logLink = logLink;
    }
}