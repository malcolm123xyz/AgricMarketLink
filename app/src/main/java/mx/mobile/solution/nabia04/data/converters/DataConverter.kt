package mx.mobile.solution.nabia04.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataConverter {
    @TypeConverter
    fun getStrPayments(payments: Array<String?>?): String? {
        if (payments == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<Array<String?>?>() {}.type
        return gson.toJson(payments, type)
    }

    @TypeConverter
    fun getPaymentFrmStr(payments: String?): Array<String>? {
        if (payments == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<Array<String?>?>() {}.type
        return gson.fromJson(payments, type)
    }

    fun getPaymentToString(countryLang: List<Map<String?, String?>?>?): String? {
        if (countryLang == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<Map<String?, String?>?>?>() {}.type
        return gson.toJson(countryLang, type)
    }

    @TypeConverter
    fun getPaymentList(countryLangString: String?): List<Map<String, String>>? {
        if (countryLangString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<Map<String?, String?>?>?>() {}.type
        return gson.fromJson(countryLangString, type)
    }


//    @TypeConverter
//    fun getStrQuestReply(payments: List<Reply>?): String? {
//        if (payments == null) {
//            return null
//        }
//        val gson = Gson()
//        val type = object : TypeToken<List<Reply>>() {}.type
//        return gson.toJson(payments, type)
//    }
//
//    @TypeConverter
//    fun getQuestReplyObj(reply: String?): List<Reply>? {
//        if (reply == null) {
//            return null
//        }
//        val gson = Gson()
//        val type = object : TypeToken<List<Reply>>() {}.type
//        return gson.fromJson(reply, type)
//    }
}