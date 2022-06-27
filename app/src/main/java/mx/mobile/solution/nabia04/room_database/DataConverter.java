package mx.mobile.solution.nabia04.room_database;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import androidx.room.TypeConverter;


public class DataConverter {
    @TypeConverter
    public String getStrPayments(String[] payments) {
        if (payments == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<String[]>() {}.getType();
        return gson.toJson(payments, type);
    }

    @TypeConverter
    public String[] getPaymentFrmStr(String payments) {

        if (payments == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<String[]>() {
        }.getType();
        return gson.fromJson(payments, type);
    }

    public String getPaymentToString(List<Map<String, String>> countryLang) {
        if (countryLang == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, String>>>() {
        }.getType();
        return gson.toJson(countryLang, type);
    }

    @TypeConverter
    public List<Map<String, String>> getPaymentList(String countryLangString) {
        if (countryLangString == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, String>>>() {
        }.getType();
        return gson.fromJson(countryLangString, type);
    }


}
