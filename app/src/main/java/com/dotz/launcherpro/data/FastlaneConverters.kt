package com.dotz.launcherpro.data

import androidx.room.TypeConverter

class FastlaneConverters {
    @TypeConverter
    fun fromFastlaneType(value: FastlaneType): String {
        return value.name
    }

    @TypeConverter
    fun toFastlaneType(value: String): FastlaneType {
        return FastlaneType.valueOf(value)
    }
}
