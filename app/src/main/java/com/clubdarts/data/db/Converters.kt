package com.clubdarts.data.db

import androidx.room.TypeConverter
import com.clubdarts.data.model.CheckoutRule

class Converters {
    @TypeConverter
    fun fromCheckoutRule(rule: CheckoutRule): String = rule.name

    @TypeConverter
    fun toCheckoutRule(value: String): CheckoutRule = CheckoutRule.valueOf(value)

    @TypeConverter
    fun fromNullableLong(value: Long?): Long = value ?: -1L

    @TypeConverter
    fun toNullableLong(value: Long): Long? = if (value == -1L) null else value
}
