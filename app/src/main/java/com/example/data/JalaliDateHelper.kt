package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object JalaliDateHelper {

    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val persianWeekDays = listOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        val monthName: String get() = persianMonths.getOrElse(month - 1) { "" }
        val formattedStandard: String get() = "%04d/%02d/%02d".format(year, month, day)
        val formattedReadable: String get() = "$day $monthName $year"
        val monthKey: String get() = "%04d-%02d".format(year, month)
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0 until 11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        if (jm == 0) {
            jm = 12
        }
        val jd = jDayNo + 1
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (i in 0 until jm2) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        for (i in 0 until 12) {
            if (gDayNo < gDaysInMonth[i]) {
                gm = i + 1
                break
            }
            gDayNo -= gDaysInMonth[i]
        }
        val gd = gDayNo + 1
        return Triple(gy, gm, gd)
    }

    fun getTodayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    fun getTodayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(Date())
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        return sdf.format(Date())
    }

    fun formatIsoToJalali(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            if (parts.size == 3) {
                val gy = parts[0].toInt()
                val gm = parts[1].toInt()
                val gd = parts[2].toInt()
                val j = gregorianToJalali(gy, gm, gd)
                val cal = Calendar.getInstance().apply {
                    set(gy, gm - 1, gd)
                }
                val dayOfWeek = persianWeekDays.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "" }
                "$dayOfWeek — ${j.formattedReadable}"
            } else {
                isoDate
            }
        } catch (_: Exception) {
            isoDate
        }
    }

    fun getDayOfWeekPersian(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            val gy = parts[0].toInt()
            val gm = parts[1].toInt()
            val gd = parts[2].toInt()
            val cal = Calendar.getInstance().apply {
                set(gy, gm - 1, gd)
            }
            persianWeekDays.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "" }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Business Rule 8 & 46:
     * Current shift: 06:00 to 18:00
     * "ساعت کاری امروز قبل از پایان شیفت قابل ثبت نهایی نباشد.
     * بعد از پایان شیفت (18:00) امکان ثبت ساعت کاری نهایی آن روز وجود داشته باشد."
     * Past dates CAN be submitted at any time.
     */
    fun canSubmitWorkHourForDate(targetIsoDate: String): Pair<Boolean, String?> {
        val todayIso = getTodayIsoDate()
        if (targetIsoDate < todayIso) {
            // Past dates can always be submitted
            return Pair(true, null)
        }
        if (targetIsoDate == todayIso) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour < 18) {
                return Pair(false, "ساعت کاری امروز قبل از پایان شیفت (ساعت ۱۸:۰۰) قابل ثبت نهایی نیست. شیفت کاری: ۰۶:۰۰ الی ۱۸:۰۰")
            }
            return Pair(true, null)
        }
        // Future dates cannot be submitted
        return Pair(false, "امکان ثبت ساعت کاری برای روزهای آینده وجود ندارد.")
    }

    /**
     * Returns a list of past N days up to today as ISO dates (YYYY-MM-DD)
     */
    fun getRecentDays(count: Int = 14): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0 until count) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }
}
