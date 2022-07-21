package mx.moble.solution.backend.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
public class EventsPicsData {

    @Id
    @Index
    private Long id;
    private String picturName;
    private String picturePath;
    private String pictureSize;
    @Index
    private int type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPicturName() {
        return picturName;
    }

    public void setPicturName(String picturName) {
        this.picturName = picturName;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public String getPictureSize() {
        return pictureSize;
    }

    public void setPictureSize(String pictureSize) {
        this.pictureSize = pictureSize;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}