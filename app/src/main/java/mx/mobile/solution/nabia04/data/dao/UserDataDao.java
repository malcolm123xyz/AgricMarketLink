package mx.mobile.solution.nabia04.data.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import mx.mobile.solution.nabia04.data.entities.EntityUserData;

@Dao
public interface UserDataDao {


    //EntityUserData methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserData(EntityUserData entityUserData);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserData(List<EntityUserData> entityUserData);

    @Query("DELETE FROM user_data_table")
    void deleteAllUserData();

    @Query("DELETE FROM user_data_table WHERE folioNumber = :folio")
    void deleteThisUser(String folio);

    @Query("SELECT * FROM user_data_table ORDER BY folioNumber ASC")
    LiveData<List<EntityUserData>> getAllMembers();

    @Query("SELECT * FROM user_data_table ORDER BY folioNumber ASC")
    List<EntityUserData> getUsersDataList();

    @Query("SELECT * FROM user_data_table WHERE survivingStatus = :status")
    LiveData<List<EntityUserData>> getDepartedMembers(int status);

    @Query("SELECT * FROM user_data_table WHERE survivingStatus <> :status ORDER BY folioNumber ASC")
    LiveData<List<EntityUserData>> getMembersAlive(int status);

    @Query("SELECT * FROM user_data_table WHERE folioNumber = :id")
    EntityUserData getUser(String id);

    @Query("DELETE FROM user_data_table")
    public void nukeTable();
}
