package mx.mobile.solution.nabia04_beta1.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement

@Dao
interface AnnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAnnouncement(entityAnnouncements: List<EntityAnnouncement?>?)

    @Delete
    suspend fun delete(announcement: EntityAnnouncement): Int

    @get:Query("SELECT * FROM announcement_table ORDER BY id DESC")
    val annsLiveData: LiveData<List<EntityAnnouncement?>?>?

    @get:Query("SELECT * FROM announcement_table ORDER BY id DESC")
    val annList: List<EntityAnnouncement>

    @Query("SELECT * FROM announcement_table WHERE id = :id")
    suspend fun getAnnouncement(id: Long): EntityAnnouncement?

    @Update
    suspend fun updateAnnouncement(entityAnnouncement: EntityAnnouncement?)

    @Query("DELETE FROM announcement_table")
    suspend fun nukeTable()

    @Query("SELECT count(*) FROM announcement_table")
    suspend fun tableCount(): Int

}