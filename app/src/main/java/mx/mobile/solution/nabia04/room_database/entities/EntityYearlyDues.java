package mx.mobile.solution.nabia04.room_database.entities;

import org.apache.poi.sl.usermodel.Sheet;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import mx.mobile.solution.nabia04.room_database.DataConverter;
import mx.mobile.solution.nabia04.room_database.repositories.YearlyDuesRepository;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity(tableName = "yearly_dues_table")
public class EntityYearlyDues {

    @PrimaryKey (autoGenerate = true)
    private int id;
    private String index;
    private String folio;
    private String name;
    private String year;

    @TypeConverters(DataConverter.class)
    private String[] payments = new String[13];

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @TypeConverters(DataConverter.class)
    public String[] getPayments() {
        return payments;
    }

    @TypeConverters(DataConverter.class)
    public void setPayments(String[] payments) {
        this.payments = payments;
    }

}