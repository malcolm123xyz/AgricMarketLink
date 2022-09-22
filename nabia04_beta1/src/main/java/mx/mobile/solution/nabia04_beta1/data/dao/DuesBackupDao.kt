package mx.mobile.solution.nabia04_beta1.data.dao

import androidx.room.*
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDuesBackup

@Dao
interface DuesBackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backup: EntityDuesBackup)

    @Delete
    suspend fun delete(announcement: EntityDuesBackup): Int

    @get:Query("SELECT * FROM dues_backup_table ORDER BY id DESC")
    val getBackups: List<EntityDuesBackup>

    @Update
    suspend fun upDateBackup(EntityDuesBackup: EntityDuesBackup?)

    @Query("DELETE FROM dues_backup_table")
    suspend fun deleteBackupTable()

    @Query("SELECT count(*) FROM dues_backup_table")
    suspend fun tableCount(): Int

}