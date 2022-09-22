package mx.mobile.solution.nabia04.data.repositories

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.data.dao.ProfMainViewDao
import mx.mobile.solution.nabia04.data.entities.EntityQuestion
import mx.mobile.solution.nabia04.utilities.RateLimiter
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Question
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException

@Singleton
class ProfMainViewRepository @Inject constructor(
    var dao: ProfMainViewDao, var endpoint: MainEndpoint,
    var sharedP: SharedPreferences,
    var alarmManager: MyAlarmManager
) {

    suspend fun fetchQuestions(): Response<List<EntityQuestion>> {
        return withContext(Dispatchers.IO) {
            fetch()
        }
    }

    suspend fun refreshDB(): Response<List<EntityQuestion>> {
        return withContext(Dispatchers.IO) {
            refresh()
        }
    }

    suspend fun getQuestion(folio: String): EntityQuestion {
        return dao.getQuestion(folio)
    }

    fun insertQuestion(question: EntityQuestion) {
        dao.insert(question)
    }

    private fun getEntity(list: List<Question>): List<EntityQuestion> {
        val entityQuestionList: MutableList<EntityQuestion> =
            ArrayList()
        for (obj in list) {
            val u =
                EntityQuestion()
            u.id = obj.id
            u.folio = obj.folio
            u.from = obj.from ?: ""
            u.question = obj.question ?: ""
            u.area = obj.area ?: ""
            u.time = obj.time ?: ""
            u.imageUrl = obj.imageUrl ?: ""
            u.upVote = obj.upVote ?: ""
            u.downVote = obj.downVote ?: ""
            u.numReply = obj.numReply ?: ""
            u.replyList = obj.replyList ?: ""
            u.visibility = obj.visibility ?: true
            entityQuestionList.add(u)
        }
        return entityQuestionList
    }

    private fun refresh(): Response<List<EntityQuestion>> {
        val erMsg: String
        try {
            val response = endpoint.questions.execute()
            return if (response?.status == Status.SUCCESS.toString()) {
                val allData = getEntity(response.data).toList()
                dao.insert(allData)
                RateLimiter.reset("Questions")
                Response.success(allData)
            } else {
                Response.error(response.message, null)
            }
        } catch (ex: IOException) {
            erMsg = if (ex is SocketTimeoutException ||
                ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
        }
        return Response.error(erMsg, null)
    }

    private fun fetch(): Response<List<EntityQuestion>> {
        var erMsg = ""

        val list = dao.getAllQuestions

        if (!shouldFetch(list)) {
            return Response.success(list)
        }

        Log.i("TAG", "Fetching questions...")

        try {
            val response = endpoint.questions.execute()
            return if (response?.status == Status.SUCCESS.toString()) {
                val allData = getEntity(response.data).toList()
                dao.insert(allData)
                RateLimiter.reset("Questions")
                Response.success(allData)
            } else {
                Response.error(response.message, null)
            }

        } catch (ex: IOException) {
            erMsg = if (ex is SocketTimeoutException ||
                ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            ex.printStackTrace()
        }
        return Response.error(erMsg, list)
    }

    private fun shouldFetch(data: List<EntityQuestion>): Boolean {
        if (RateLimiter.shouldFetch("Questions", 12, TimeUnit.HOURS)) {
            Log.i("TAG", "Time limit reached, ShouldFetch Questions")
            return true
        }

        if (data.isEmpty()) {
            Log.i("TAG", "Data is empty, ShouldFetch Questions")
            return true
        }

        Log.i("TAG", "Don't fetch new Questions")
        return false
    }


}