// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import com.rsgkh.calendar.data.TodayTimeZone
import com.rsgkh.calendar.data.Birthplace
import com.rsgkh.calendar.engine.western.WesternZodiacCalculator
import com.rsgkh.calendar.engine.western.WesternZodiacSign
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

internal data class WesternBig3Signs(
    val sun: WesternZodiacSign,
    val moon: WesternZodiacSign,
    val rising: WesternZodiacSign?,
)

/** Resolve the selected civil time and location before calling the shared engine. */
internal fun westernBig3Signs(
    date: LocalDate,
    time: LocalTime?,
    timeZone: TodayTimeZone,
    localZone: ZoneId = ZoneId.systemDefault(),
    place: Birthplace? = null,
): WesternBig3Signs? {
    if (date.year !in 1800..2200) return null

    val zone = place?.let { runCatching { ZoneId.of(it.timeZone) }.getOrNull() ?: return null } ?: timeZone.zone(localZone)
    val fixedCambodiaOffset = ZoneOffset.ofHours(7)
    val selectedTime = time ?: LocalTime.NOON
    val selectedOffset = if (place == null && timeZone == TodayTimeZone.CAMBODIA) fixedCambodiaOffset
        else zone.rules.getValidOffsets(date.atTime(selectedTime)).firstOrNull()
    val validSelectedTime = time != null && selectedOffset != null
    // A skipped daylight-saving time has no instant. Keep Sun and Moon at noon,
    // but leave Rising unavailable instead of assigning it a made-up instant.
    val calculationTime = if (time != null && !validSelectedTime) LocalTime.NOON else selectedTime
    val offset = if (place == null && timeZone == TodayTimeZone.CAMBODIA) fixedCambodiaOffset
        else zone.rules.getValidOffsets(date.atTime(calculationTime)).firstOrNull() ?: return null
    val offsetHours = offset.totalSeconds / 3600.0
    if (place != null && (!place.latitude.isFinite() || place.latitude !in -90.0..90.0 ||
        !place.longitude.isFinite() || place.longitude !in -180.0..180.0)) return null

    return try {
        val horoscope = WesternZodiacCalculator.calculateHoroscope(
            year = date.year, month = date.monthValue, day = date.dayOfMonth,
            hour = calculationTime.hour, minute = calculationTime.minute, second = 0.0,
            utcOffsetHours = offsetHours,
            latitudeDeg = place?.latitude ?: 0.0, longitudeDeg = place?.longitude ?: 0.0,
        )
        WesternBig3Signs(horoscope.sun.sign, horoscope.moon.sign,
            if (validSelectedTime && place != null) horoscope.ascendant?.sign else null)
    } catch (_: Exception) {
        null
    }
}
