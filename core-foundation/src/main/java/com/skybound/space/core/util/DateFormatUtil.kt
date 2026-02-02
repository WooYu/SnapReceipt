package com.skybound.space.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 集中日期时间格式化，使用 java.time（线程安全）。
 * 格式常量统一维护，便于修改和国际化。
 */
object DateFormatUtil {

    /** API 请求/响应日期：yyyy-MM-dd */
    const val PATTERN_API_DATE = "yyyy-MM-dd"

    /** API 日期时间：yyyy-MM-dd HH:mm:ss */
    const val PATTERN_API_DATETIME = "yyyy-MM-dd HH:mm:ss"

    /** 展示用日期：yyyy/MM/dd */
    const val PATTERN_DISPLAY_DATE = "yyyy/MM/dd"

    /** 展示用日期时间：yyyy/MM/dd HH:mm */
    const val PATTERN_DISPLAY_DATETIME = "yyyy/MM/dd HH:mm"

    /** 时间：HH:mm:ss */
    const val PATTERN_TIME = "HH:mm:ss"

    private val defaultZone: ZoneId get() = ZoneId.systemDefault()

    private val apiDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_API_DATE, Locale.getDefault())

    private val apiDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_API_DATETIME, Locale.getDefault())

    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DISPLAY_DATE, Locale.getDefault())

    private val displayDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DISPLAY_DATETIME, Locale.getDefault())

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_TIME, Locale.getDefault())

    /** 当前日期的 API 格式字符串（yyyy-MM-dd） */
    fun todayApiDate(): String =
        LocalDate.now(defaultZone).format(apiDateFormatter)

    /** 毫秒时间戳 -> API 日期 yyyy-MM-dd */
    fun formatApiDate(timeMillis: Long): String =
        Instant.ofEpochMilli(timeMillis).atZone(defaultZone).format(apiDateFormatter)

    /** 毫秒时间戳 -> API 日期时间 yyyy-MM-dd HH:mm:ss */
    fun formatApiDateTime(timeMillis: Long): String =
        Instant.ofEpochMilli(timeMillis).atZone(defaultZone).format(apiDateTimeFormatter)

    /** 毫秒时间戳 -> 展示日期 yyyy/MM/dd */
    fun formatDisplayDate(timeMillis: Long): String =
        Instant.ofEpochMilli(timeMillis).atZone(defaultZone).format(displayDateFormatter)

    /** 毫秒时间戳 -> 展示日期时间 yyyy/MM/dd HH:mm */
    fun formatDisplayDateTime(timeMillis: Long): String =
        Instant.ofEpochMilli(timeMillis).atZone(defaultZone).format(displayDateTimeFormatter)

    /** 毫秒时间戳 -> 时间 HH:mm:ss */
    fun formatTime(timeMillis: Long): String =
        Instant.ofEpochMilli(timeMillis).atZone(defaultZone).format(timeFormatter)

    /** API 日期字符串 yyyy-MM-dd -> 展示格式 yyyy/MM/dd */
    fun apiDateToDisplay(apiDate: String): String {
        if (apiDate.isBlank()) return ""
        return runCatching {
            LocalDate.parse(apiDate, apiDateFormatter).format(displayDateFormatter)
        }.getOrDefault(apiDate)
    }

    /** 解析 API 日期 yyyy-MM-dd 为毫秒（当天 00:00:00） */
    fun parseApiDate(apiDate: String): Long? {
        if (apiDate.isBlank()) return null
        return runCatching {
            LocalDate.parse(apiDate, apiDateFormatter)
                .atStartOfDay(defaultZone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    /** 解析 API 日期时间 yyyy-MM-dd HH:mm:ss 为毫秒 */
    fun parseApiDateTime(date: String, time: String): Long? {
        if (date.isBlank()) return null
        val safeTime = if (time.isBlank()) "00:00:00" else time
        val value = "$date $safeTime"
        return runCatching {
            java.time.LocalDateTime.parse(value, apiDateTimeFormatter)
                .atZone(defaultZone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
}
