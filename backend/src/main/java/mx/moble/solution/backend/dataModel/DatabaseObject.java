package mx.moble.solution.backend.dataModel;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by malcolm123xyz on 1/25/2016.
 */
@Entity
public class DatabaseObject {
    @Id
    @Index
    private String folioNumber;
    private String fullName;

    private String nickName;
    private String sex;
    private String homeTown;
    private String contact;
    private String districtOfResidence;
    private String regionOfResidence;
    private String email;
    private String imageUri;
    private String imageId;
    private long birthDayAlarm;

    private String className;
    private String CourseStudied;
    private String house;
    private String positionHeld;

    private String jobDescription;
    private String specificOrg;
    private String employmentStatus;
    private String employmentSector;
    private String nameOfEstablishment;
    private String establishmentRegion;
    private String establishmentDist;

    private int survivingStatus;
    private String dateDeparted;

    private String biography;

    private String tributes;

    public String getTributes() {
        return tributes;
    }

    public void setTributes(String tributes) {
        this.tributes = tributes;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getEstablishmentRegion() {
        return establishmentRegion;
    }

    public void setEstablishmentRegion(String establishmentRegion) {
        this.establishmentRegion = establishmentRegion;
    }

    public String getEstablishmentDist() {
        return establishmentDist;
    }

    public void setEstablishmentDist(String establishmentDist) {
        this.establishmentDist = establishmentDist;
    }

    public String getSpecificOrg() {
        return specificOrg;
    }

    public void setSpecificOrg(String specificOrg) {
        this.specificOrg = specificOrg;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getEmploymentSector() {
        return employmentSector;
    }

    public void setEmploymentSector(String employmentSector) {
        this.employmentSector = employmentSector;
    }

    public String getNameOfEstablishment() {
        return nameOfEstablishment;
    }

    public void setNameOfEstablishment(String nameOfEstablishment) {
        this.nameOfEstablishment = nameOfEstablishment;
    }

    public String getDateDeparted() {
        return dateDeparted;
    }

    public void setDateDeparted(String dateDeparted) {
        this.dateDeparted = dateDeparted;
    }

    public int getSurvivingStatus() {
        return survivingStatus;
    }

    public void setSurvivingStatus(int survivingStatus) {
        this.survivingStatus = survivingStatus;
    }

    public long getBirthDayAlarm() {
        return birthDayAlarm;
    }

    public void setBirthDayAlarm(long birthDayAlarm) {
        this.birthDayAlarm = birthDayAlarm;
    }

    public String getDistrictOfResidence() {
        return districtOfResidence;
    }

    public void setDistrictOfResidence(String districtOfResidence) {
        this.districtOfResidence = districtOfResidence;
    }

    public String getRegionOfResidence() {
        return regionOfResidence;
    }

    public void setRegionOfResidence(String regionOfResidence) {
        this.regionOfResidence = regionOfResidence;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFolioNumber() {
        return folioNumber;
    }

    public void setFolioNumber(String folioNumber) {
        this.folioNumber = folioNumber;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getHomeTown() {
        return homeTown;
    }

    public void setHomeTown(String homeTown) {
        this.homeTown = homeTown;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCourseStudied() {
        return CourseStudied;
    }

    public void setCourseStudied(String courseStudied) {
        CourseStudied = courseStudied;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getPositionHeld() {
        return positionHeld;
    }

    public void setPositionHeld(String positionHeld) {
        this.positionHeld = positionHeld;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

}
