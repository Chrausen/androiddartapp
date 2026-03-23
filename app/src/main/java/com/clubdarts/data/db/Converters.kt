package com.clubdarts.data.db

import androidx.room.TypeConverter
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.GameType

class Converters {
    @TypeConverter
    fun fromCheckoutRule(rule: CheckoutRule): String = rule.name

    @TypeConverter
    fun toCheckoutRule(value: String): CheckoutRule = CheckoutRule.valueOf(value)

    @TypeConverter
    fun fromGameType(type: GameType): String = type.name

    @TypeConverter
    fun toGameType(value: String): GameType = try { GameType.valueOf(value) } catch (e: Exception) { GameType.X01 }

    @TypeConverter
    fun fromNullableLong(value: Long?): Long = value ?: -1L

    @TypeConverter
    fun toNullableLong(value: Long): Long? = if (value == -1L) null else value
}
