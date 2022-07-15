package mx.mobile.solution.nabia04.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import mx.mobile.solution.nabia04.data.entities.EntityDues

@Dao
interface DuesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(EntityDues: List<EntityDues?>?)

    @Delete
    suspend fun delete(announcement: EntityDues): Int

    @get:Query("SELECT * FROM yearly_dues_table ORDER BY id DESC")
    val annsLiveData: LiveData<List<EntityDues?>?>?

    @get:Query("SELECT * FROM yearly_dues_table ORDER BY id DESC")
    val getAllDues: List<EntityDues>

    @Update
    suspend fun updateAnnouncement(EntityDues: EntityDues?)

    @Query("DELETE FROM yearly_dues_table")
    suspend fun nukeTable()

    @Query("SELECT count(*) FROM yearly_dues_table")
    suspend fun tableCount(): Int

}