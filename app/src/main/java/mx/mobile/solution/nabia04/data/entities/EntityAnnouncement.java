package mx.mobile.solution.nabia04.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "announcement_table")
public class EntityAnnouncement {

    @PrimaryKey
    @NonNull
    private Long id;
    private String heading;
    private String message;
    private int annType;
    private int eventType;
    private String imageUri;
    private long eventDate;
    private int priority;
    private int rowNum;
    private String venue;
    private String isAboutWho;
    private boolean isArelative;
    private boolean read;

    public int getAnnType() {
        return annType;
    }

    public void setAnnType(int annType) {
        this.annType = annType;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getIsAboutWho() {
        return isAboutWho;
    }

    public void setIsAboutWho(String isAboutWho) {
        this.isAboutWho = isAboutWho;
    }

    public boolean isArelative() {
        return isArelative;
    }

    public void setArelative(boolean arelative) {
        isArelative = arelative;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getEventDate() {
        return eventDate;
    }

    public void setEventDate(long eventDate) {
        this.eventDate = eventDate;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}