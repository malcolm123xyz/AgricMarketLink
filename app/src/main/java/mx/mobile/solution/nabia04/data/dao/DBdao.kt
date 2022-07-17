package mx.mobile.solution.nabia04.data.dao

import androidx.room.*
import mx.mobile.solution.nabia04.data.entities.EntityUserData

@Dao
interface DBdao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entityUserData: List<EntityUserData?>?)

    @Query("SELECT fullName FROM user_data_table")
    suspend fun getNamesOnly(): List<Names>

    @get:Query("SELECT * FROM user_data_table ORDER BY folioNumber DESC")
    val getAllUserData: List<EntityUserData>

    @Query("SELECT * FROM user_data_table WHERE folioNumber = :folio")
    suspend fun userData(folio: String): EntityUserData?

    data class Names(
        @ColumnInfo(name = "fullName") val name: String?
    )
}