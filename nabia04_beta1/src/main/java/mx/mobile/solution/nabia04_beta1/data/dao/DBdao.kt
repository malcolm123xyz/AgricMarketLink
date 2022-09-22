package mx.mobile.solution.nabia04_beta1.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData

@Dao
interface DBdao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entityUserData: List<EntityUserData>)

    @get:Query("SELECT * FROM user_data_table ORDER BY folioNumber DESC")
    val getAllUserData: List<EntityUserData>

    @Query("SELECT * FROM user_data_table WHERE folioNumber = :folio")
    suspend fun userData(folio: String): EntityUserData?

}