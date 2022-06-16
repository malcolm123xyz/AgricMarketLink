package mx.mobile.solution.nabia04.room_database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import mx.mobile.solution.nabia04.room_database.entities.EntityUserData;
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues;

@Dao
public interface DuesDetailDao {

    @Insert()
    void insert(List<EntityYearlyDues> entityUserData);

    @Query("SELECT * FROM yearly_dues_table ORDER BY `index` ASC")
    List<EntityYearlyDues> getAllData();

    @Query("SELECT * FROM yearly_dues_table WHERE year = :year")
    List<EntityYearlyDues> getThisYearDues(String year);

    @Query("DELETE FROM yearly_dues_table")
    void deleteTable();

    @Query("SELECT count(*) FROM yearly_dues_table")
    public int tableCount();
}