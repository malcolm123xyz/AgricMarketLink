package mx.mobile.solution.nabia04.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.entities.EntityUserData

@Dao
interface DBdao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entityUserData: List<EntityUserData?>?)

    @Delete
    suspend fun delete(announcement: EntityAnnouncement): Int

    @get:Query("SELECT * FROM announcement_table ORDER BY id DESC")
    val annsLiveData: LiveData<List<EntityAnnouncement?>?>?

    @get:Query("SELECT * FROM user_data_table ORDER BY folioNumber DESC")
    val getAllUserData: List<EntityUserData>

    @get:Query("SELECT * FROM announcement_table WHERE type = '0'")
    val generalAnnouncements: List<EntityAnnouncement?>?

    @get:Query("SELECT * FROM announcement_table WHERE type > '0'")
    val eventsAnnouncements: List<EntityAnnouncement?>?

    @Query("SELECT * FROM user_data_table WHERE folioNumber = :folio")
    suspend fun userData(folio: String): EntityUserData?

    @Update
    suspend fun updateAnnouncement(entityAnnouncement: EntityAnnouncement?)

    @Query("DELETE FROM announcement_table")
    suspend fun nukeTable()

    @Query("SELECT count(*) FROM announcement_table")
    suspend fun tableCount(): Int

}