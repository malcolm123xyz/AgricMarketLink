package mx.mobile.solution.nabia04.data.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement;

@Dao
public interface AnnDao {
    //EntityAnnouncement methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAnnouncement(EntityAnnouncement entityAnnouncement);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAnnouncement(List<EntityAnnouncement> entityAnnouncements);

    @Query("DELETE FROM announcement_table")
    void deleteAll();

    @Query("DELETE FROM announcement_table WHERE id = :annId")
    void delete(long annId);

    @Query("SELECT * FROM announcement_table ORDER BY id DESC")
    LiveData<List<EntityAnnouncement>> getAnnsLiveData();

    @Query("SELECT * FROM announcement_table ORDER BY id DESC")
    List<EntityAnnouncement> getAnnList();

    @Query("SELECT * FROM announcement_table WHERE type = '0'")
    List<EntityAnnouncement> getGeneralAnnouncements();

    @Query("SELECT * FROM announcement_table WHERE type > '0'")
    List<EntityAnnouncement> getEventsAnnouncements();

    @Query("SELECT * FROM announcement_table WHERE id = :id")
    EntityAnnouncement getAnnouncement(long id);

    @Update
    void updateAnnouncement(EntityAnnouncement entityAnnouncement);

    @Query("DELETE FROM announcement_table")
    public void nukeTable();

    @Query("SELECT count(*) FROM announcement_table")
    public int tableCount();
}
