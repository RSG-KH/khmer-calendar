// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import com.rsgkh.calendar.data.TodayTimeZone
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

/** Representative city coordinates for the same IANA zones supported by the PWA. */
private val representativeCoordinates = mapOf(
    "Asia/Phnom_Penh" to (11.5564 to 104.9282),
    "Asia/Bangkok" to (13.7563 to 100.5018),
    "Asia/Vientiane" to (17.9757 to 102.6331),
    "Asia/Ho_Chi_Minh" to (10.8231 to 106.6297),
    "Asia/Singapore" to (1.3521 to 103.8198),
    "Asia/Kuala_Lumpur" to (3.1390 to 101.6869),
    "Asia/Jakarta" to (-6.2088 to 106.8456),
    "Asia/Tokyo" to (35.6762 to 139.6503),
    "Asia/Seoul" to (37.5665 to 126.9780),
    "Asia/Shanghai" to (31.2304 to 121.4737),
    "Asia/Hong_Kong" to (22.3193 to 114.1694),
    "Asia/Taipei" to (25.0330 to 121.5654),
    "Asia/Yangon" to (16.8661 to 96.1951),
    "Asia/Kolkata" to (22.5726 to 88.3639),
    "Asia/Dubai" to (25.2048 to 55.2708),
    "Europe/London" to (51.5074 to -0.1278),
    "Europe/Brussels" to (50.8503 to 4.3517),
    "Europe/Paris" to (48.8566 to 2.3522),
    "Europe/Berlin" to (52.5200 to 13.4050),
    "America/New_York" to (40.7128 to -74.0060),
    "America/Chicago" to (41.8781 to -87.6298),
    "America/Denver" to (39.7392 to -104.9903),
    "America/Los_Angeles" to (34.0522 to -118.2437),
    "America/Toronto" to (43.6532 to -79.3832),
    "America/Vancouver" to (49.2827 to -123.1207),
    "Australia/Sydney" to (-33.8688 to 151.2093),
    "Australia/Melbourne" to (-37.8136 to 144.9631),
    "Pacific/Auckland" to (-36.8485 to 174.7633),
    "Pacific/Honolulu" to (21.3069 to -157.8583),
)

/** Resolve the selected civil time and location before calling the shared engine. */
internal fun westernBig3Signs(
    date: LocalDate,
    time: LocalTime?,
    timeZone: TodayTimeZone,
    localZone: ZoneId = ZoneId.systemDefault(),
): WesternBig3Signs? {
    if (date.year !in 1800..2200) return null

    val zone = timeZone.zone(localZone)
    val fixedCambodiaOffset = ZoneOffset.ofHours(7)
    val selectedTime = time ?: LocalTime.NOON
    val selectedOffset = if (timeZone == TodayTimeZone.CAMBODIA) fixedCambodiaOffset
        else zone.rules.getValidOffsets(date.atTime(selectedTime)).firstOrNull()
    val validSelectedTime = time != null && selectedOffset != null
    // A skipped daylight-saving time has no instant. Keep Sun and Moon at noon,
    // but leave Rising unavailable instead of assigning it a made-up instant.
    val calculationTime = if (time != null && !validSelectedTime) LocalTime.NOON else selectedTime
    val offset = if (timeZone == TodayTimeZone.CAMBODIA) fixedCambodiaOffset
        else zone.rules.getValidOffsets(date.atTime(calculationTime)).firstOrNull() ?: return null
    val offsetHours = offset.totalSeconds / 3600.0
    val coordinates = representativeCoordinates[zone.id] ?: (11.5564 to (offsetHours * 15.0).coerceIn(-180.0, 180.0))

    return try {
        val horoscope = WesternZodiacCalculator.calculateHoroscope(
            year = date.year, month = date.monthValue, day = date.dayOfMonth,
            hour = calculationTime.hour, minute = calculationTime.minute, second = 0.0,
            utcOffsetHours = offsetHours,
            latitudeDeg = coordinates.first, longitudeDeg = coordinates.second,
        )
        WesternBig3Signs(horoscope.sun.sign, horoscope.moon.sign,
            if (validSelectedTime) horoscope.ascendant?.sign else null)
    } catch (_: Exception) {
        null
    }
}
