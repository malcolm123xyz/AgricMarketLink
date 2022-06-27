package mx.mobile.solution.nabia04.main.data.entities;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import mx.mobile.solution.nabia04.main.data.converters.DataConverter;

@Entity(tableName = "contribution")
public class EntityContributionData {
    @PrimaryKey
    @NonNull
    private String id;
    private String folio;
    private String name;
    private String title;
    private String imageUri;
    private String imageId;
    private long deadline;
    private String total;

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    @TypeConverters(DataConverter.class)
    private List<Map<String, String>> contribution;

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    @TypeConverters(DataConverter.class)
    public List<Map<String, String>> getContribution() {
        return contribution;
    }

    @TypeConverters(DataConverter.class)
    public void setContribution(List<Map<String, String>> contribution) {
        this.contribution = contribution;
    }


    public EntityContributionData() {
        contribution = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
