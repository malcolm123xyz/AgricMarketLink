package mx.moble.solution.backend.dataModel;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

@Entity
public class HotSeatPersonDataModel {
    @Id
    @Index
    private String folio;
    private String name;
    private long lastDateOn;
    private String totalQuestions;
    private String percentageHonesty;
    private String educativeLevel;
    private String inspirationLevel;
    private String humor;
    private String pornSkills;
    private String percentageInformative;
    private String overallPerformance;
    private int numberOfTimes;
    private String imageUri;
    private String seatStatus;
    private String classs;
    private String house;
    private String occupation;
    private String imageId;

    private List<String> honestyVotes;
    private List<String> educativeVotes;
    private List<String> informativeVotes;
    private List<String> humorVotes;
    private List<String> pornSkillsVotes;
    private List<String> usersList;

    public HotSeatPersonDataModel() {
        educativeVotes = new ArrayList<>();
        informativeVotes = new ArrayList<>();
        humorVotes = new ArrayList<>();
        pornSkillsVotes = new ArrayList<>();
        honestyVotes = new ArrayList<>();
        usersList = new ArrayList<>();
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getClasss() {
        return classs;
    }

    public void setClasss(String classs) {
        this.classs = classs;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(String seatStatus) {
        this.seatStatus = seatStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public boolean hasRated(String folio) {
        return getUsersList().contains(folio);
    }

    public List<String> getUsersList() {
        return usersList;
    }

    public void setUsersList(List<String> usersList) {
        this.usersList = usersList;
    }

    public void addUser(String user) {
        this.usersList.add(user);
    }

    public void addToHonestyVotes(String vote) {
        this.honestyVotes.add(vote);
    }

    public void addToEducativeVotes(String vote) {
        this.educativeVotes.add(vote);
    }

    public void addToInformativeVotes(String vote) {
        this.informativeVotes.add(vote);
    }

    public void addToHumorVotes(String vote) {
        this.humorVotes.add(vote);
    }

    public void addToPornVotes(String vote) {
        this.pornSkillsVotes.add(vote);
    }

    public List<String> getHonestyVotes() {
        return honestyVotes;
    }

    public void setHonestyVotes(List<String> honestyVotes) {
        this.honestyVotes = honestyVotes;
    }

    public List<String> getEducativeVotes() {
        return educativeVotes;
    }

    public void setEducativeVotes(List<String> educativeVotes) {
        this.educativeVotes = educativeVotes;
    }

    public List<String> getInformativeVotes() {
        return informativeVotes;
    }

    public void setInformativeVotes(List<String> informativeVotes) {
        this.informativeVotes = informativeVotes;
    }

    public List<String> getHumorVotes() {
        return humorVotes;
    }

    public void setHumorVotes(List<String> humorVotes) {
        this.humorVotes = humorVotes;
    }

    public List<String> getPornSkillsVotes() {
        return pornSkillsVotes;
    }

    public void setPornSkillsVotes(List<String> pornSkillsVotes) {
        this.pornSkillsVotes = pornSkillsVotes;
    }

    public int getNumberOfTimes() {
        return numberOfTimes;
    }

    public void setNumberOfTimes(int numberOfTimes) {
        this.numberOfTimes = numberOfTimes;
    }

    public String getOverallPerformance() {
        return overallPerformance;
    }

    public void setOverallPerformance(String overallPerformance) {
        this.overallPerformance = overallPerformance;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getPercentageInformative() {
        return percentageInformative;
    }

    public long getLastDateOn() {
        return lastDateOn;
    }

    public void setLastDateOn(long lastDateOn) {
        this.lastDateOn = lastDateOn;
    }

    public String getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(String totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getPercentageHonesty() {
        return percentageHonesty;
    }

    public void setPercentageHonesty(String percentageHonesty) {
        this.percentageHonesty = percentageHonesty;
    }

    public String getEducativeLevel() {
        return educativeLevel;
    }

    public void setEducativeLevel(String educativeLevel) {
        this.educativeLevel = educativeLevel;
    }

    public String getInspirationLevel() {
        return inspirationLevel;
    }

    public void setInspirationLevel(String inspirationLevel) {
        this.inspirationLevel = inspirationLevel;
    }

    public String getHumor() {
        return humor;
    }

    public void setHumor(String humor) {
        this.humor = humor;
    }

    public String getPornSkills() {
        return pornSkills;
    }

    public void setPornSkills(String pornSkills) {
        this.pornSkills = pornSkills;
    }

    public void setPercentageInformative(String percentageInformative) {
        this.percentageInformative = percentageInformative;
    }
}
