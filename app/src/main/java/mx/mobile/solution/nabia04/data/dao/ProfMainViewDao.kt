package mx.mobile.solution.nabia04.data.dao

import androidx.room.*
import mx.mobile.solution.nabia04.data.entities.EntityQuestion

@Dao
interface ProfMainViewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(EntityQuestion: List<EntityQuestion?>?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entityQuestion: EntityQuestion)

    @get:Query("SELECT * FROM question_table ORDER BY time DESC")
    val getAllQuestions: List<EntityQuestion>

    @Query("SELECT * FROM question_table WHERE id = :id")
    suspend fun getQuestion(id: String): EntityQuestion

    @Delete
    suspend fun delete(question: EntityQuestion): Int

}