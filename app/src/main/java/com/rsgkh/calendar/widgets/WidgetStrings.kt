// SPDX-License-Identifier: Apache-2.0
package com.rsgkh.calendar.widgets

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.rsgkh.calendar.R
import com.rsgkh.calendar.data.getKhmerPeriod
import com.rsgkh.calendar.domain.ZodiacSign
import com.rsgkh.calendar.i18n.CalendarWords
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The app's own language setting, not the launcher's locale, controls widget content. */
internal class WidgetStrings(private val context: Context, val khmer: Boolean) {
    private val locale = Locale.forLanguageTag(if (khmer) "km" else "en")
    private val localized = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        },
    )
    private val clock = DateTimeFormatter.ofPattern(
        if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale,
    )

    operator fun invoke(@StringRes id: Int, vararg arguments: Any): String {
        val map = if (khmer) KHMER_MAP else ENGLISH_MAP
        val template = map[id] ?: return runCatching { localized.getString(id, *arguments) }.getOrDefault("")
        return if (arguments.isEmpty()) template else String.format(locale, template, *arguments)
    }

    fun number(value: Int): String = CalendarWords.number(value, khmer)

    fun plannerDate(date: java.time.LocalDate): String {
        return "${plannerListDate(date)}/${number(date.year)}"
    }

    fun plannerListDate(date: java.time.LocalDate): String {
        val zero = if (khmer) '០' else '0'
        return "${number(date.monthValue).padStart(2, zero)}/${number(date.dayOfMonth).padStart(2, zero)}"
    }

    fun plannerTime(time: LocalTime): String {
        val zero = if (khmer) '០' else '0'
        return "${number(time.hour)}:${number(time.minute).padStart(2, zero)}"
    }

    fun zodiac(sign: ZodiacSign): String =
        "${if (khmer) sign.signNameKm else sign.signName} ${sign.emoji.trim()}"

    fun time(time: LocalTime): String {
        if (!khmer) return time.format(clock)
        val is24 = DateFormat.is24HourFormat(context)
        return if (is24) {
            val formatted = time.format(DateTimeFormatter.ofPattern("HH:mm", locale))
            buildString {
                formatted.forEach { append(if (it in '0'..'9') ('០'.code + (it - '0')).toChar() else it) }
            }
        } else {
            val hour12 = if (time.hour % 12 == 0) 12 else time.hour % 12
            val digits = "%d:%02d".format(hour12, time.minute)
            val khmerDigits = buildString {
                digits.forEach { append(if (it in '0'..'9') ('០'.code + (it - '0')).toChar() else it) }
            }
            val period = getKhmerPeriod(time.hour, time.minute)
            "$khmerDigits $period"
        }
    }

    companion object {
        private val KHMER_MAP = mapOf(
            R.string.widget_today_heading to "ថ្ងៃនេះ",
            R.string.widget_yesterday to "ម្សិលមិញ",
            R.string.widget_tomorrow to "ថ្ងៃស្អែក",
            R.string.widget_buddhist_year to "ព.ស. %1\$s",
            R.string.widget_traditional_year to "ឆ្នាំ%1\$s · %2\$s",
            R.string.widget_holy_day to "ថ្ងៃសីល",
            R.string.widget_shaving_day to "ថ្ងៃកោរ",
            R.string.widget_local_time to "ម៉ោងក្នុងតំបន់",
            R.string.widget_cambodia_time to "ម៉ោងកម្ពុជា · UTC+7",
            R.string.widget_no_events to "គ្មានព្រឹត្តិការណ៍",
            R.string.widget_event_count to "ព្រឹត្តិការណ៍ %1\$s",
            R.string.widget_event_count_one to "ព្រឹត្តិការណ៍ %1\$s",
            R.string.widget_personal_count to "ព្រឹត្តិការណ៍ផ្ទាល់ខ្លួន %1\$s",
            R.string.widget_personal_count_one to "ព្រឹត្តិការណ៍ផ្ទាល់ខ្លួន %1\$s",
            R.string.widget_more to "+%1\$s ទៀត",
            R.string.widget_more_today to "+%1\$s ទៀតនៅថ្ងៃនេះ",
            R.string.widget_refresh to "ធ្វើឱ្យថ្មី",
            R.string.widget_configure to "ការកំណត់",
            R.string.widget_lunar_unavailable to "មិនមានទិន្នន័យចន្ទគតិ",
            R.string.widget_events_unavailable to "ព្រឹត្តិការណ៍ខ្លះមិនអាចបង្ហាញបាន",
            R.string.widget_unsupported_date to "កាលបរិច្ឆេទមិនគាំទ្រ",
            R.string.widget_load_failed to "មិនអាចផ្ទុកធាតុក្រាហ្វិកបាន",
            R.string.widget_holiday_legend to "ថ្ងៃឈប់សម្រាក",
            R.string.widget_holy_legend to "ថ្ងៃសីល",
            R.string.widget_observance_legend to "ពិធី និងទិវា",
            R.string.widget_personal_legend to "ផ្ទាល់ខ្លួន",
        )

        private val ENGLISH_MAP = mapOf(
            R.string.widget_today_heading to "Today",
            R.string.widget_yesterday to "Yesterday",
            R.string.widget_tomorrow to "Tomorrow",
            R.string.widget_buddhist_year to "BE %1\$s",
            R.string.widget_traditional_year to "%1\$s · %2\$s",
            R.string.widget_holy_day to "B. Holy",
            R.string.widget_shaving_day to "B. Shaving",
            R.string.widget_local_time to "Local time",
            R.string.widget_cambodia_time to "Cambodia time · UTC+7",
            R.string.widget_no_events to "No events",
            R.string.widget_event_count to "%1\$s events",
            R.string.widget_event_count_one to "%1\$s event",
            R.string.widget_personal_count to "%1\$s personal events",
            R.string.widget_personal_count_one to "%1\$s personal event",
            R.string.widget_more to "+%1\$s more",
            R.string.widget_more_today to "+%1\$s more today",
            R.string.widget_refresh to "Refresh",
            R.string.widget_configure to "Settings",
            R.string.widget_lunar_unavailable to "Lunar date unavailable",
            R.string.widget_events_unavailable to "Events unavailable",
            R.string.widget_unsupported_date to "Supported dates: 1800–2200",
            R.string.widget_load_failed to "Could not load widget",
            R.string.widget_holiday_legend to "Holiday",
            R.string.widget_holy_legend to "Buddhist holy day",
            R.string.widget_observance_legend to "Observance",
            R.string.widget_personal_legend to "Personal",
        )
    }
}
