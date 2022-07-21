package mx.moble.solution.backend.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
public class Announcement {

    @Id
    @Index
    private Long id;
    private String heading;
    private String message;
    private String imageUri;
    private int annType;
    private int eventType;
    private long eventDate;
    private int priority;
    private int rowNum;
    private String venue;
    private String isAboutWho;
    private boolean isArelative;

    public int getAnnType() {
        return annType;
    }

    public void setAnnType(int annType) {
        this.annType = annType;
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

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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