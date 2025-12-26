package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.util.jsonUtils
import taboolib.expansion.CustomType
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite

data class IDType(
    val id: String,
    var displayName: String,
    var color: String,
    var icon: String,
    var startChar: String,
    var char: String,
    var endChar: String,
)

data class IDData(
    var id: Long,
    var type: String,
){
    fun serializeToJson(): String {
        return jsonUtils.toJson(this)
    }
}

object IDDataType : CustomType {

    override val type: Class<*> = IDData::class.java

    override val typeSQL: ColumnTypeSQL
        get() = ColumnTypeSQL.LONGTEXT

    override val typeSQLite: ColumnTypeSQLite
        get() = ColumnTypeSQLite.TEXT

    override val length = 6

    override fun serialize(value: Any): Any {
        return (value as IDData).serializeToJson()
    }

    override fun deserialize(value: Any): Any {
        return jsonUtils.fromJson(value as String, IDData::class.java)
    }
}