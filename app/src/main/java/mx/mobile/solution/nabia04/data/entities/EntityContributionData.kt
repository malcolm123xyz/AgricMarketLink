package mx.mobile.solution.nabia04.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import mx.mobile.solution.nabia04.data.converters.DataConverter

@Entity(tableName = "contribution")
data class EntityContributionData(
    @PrimaryKey
    var id: String = "",
    var folio: String = "",
    var name: String = "",
    var message: String = "",
    var type: String = "",
    var imageUri: String = "",
    var imageId: String = "",
    var deadline: String = "",
    var momoNum: String = "",
    var momoName: String = "",

    @get:TypeConverters(DataConverter::class)
    @set:TypeConverters(DataConverter::class)
    @TypeConverters(DataConverter::class)
    var contribution: List<Map<String, String>> = ArrayList()
)