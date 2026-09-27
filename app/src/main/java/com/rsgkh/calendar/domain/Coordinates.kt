// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** Decimal degrees, DMS and degrees/decimal minutes; signs and compass axes are validated. */
internal fun parseCoordinate(raw: String, latitude: Boolean): Double? {
    var text = raw.trim().replace('−', '-').uppercase(Locale.ROOT)
    if (text.isEmpty()) return null
    val prefix = text.first().takeIf { it in "NSEW" }
    if (prefix != null) text = text.drop(1).trim()
    val suffix = text.lastOrNull()?.takeIf { it in "NSEW" }
    if (suffix != null) text = text.dropLast(1).trim()
    if (prefix != null && suffix != null) return null
    val direction = prefix ?: suffix
    if (direction != null && (text.startsWith('+') || text.startsWith('-'))) return null
    if (direction != null && direction !in (if (latitude) "NS" else "EW")) return null
    val parts = text.replace(Regex("[°º˚'′’‘\"″“”:]"), " ").trim().split(Regex("\\s+"))
    if (parts.size !in 1..3 || !parts[0].matches(Regex("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"))) return null
    if (parts.size > 1 && (!parts[0].matches(Regex("[+-]?\\d+")) || !parts[1].matches(Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")))) return null
    if (parts.size > 2 && (!parts[1].matches(Regex("\\d+")) || !parts[2].matches(Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")))) return null
    val degrees = parts[0].toDoubleOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    val seconds = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
    if (minutes >= 60 || seconds >= 60) return null
    val magnitude = abs(degrees) + minutes / 60 + seconds / 3600
    if (!magnitude.isFinite() || magnitude > if (latitude) 90 else 180) return null
    val negative = if (direction != null) direction in "SW" else parts[0].startsWith('-')
    val value = if (negative) -magnitude else magnitude
    return if (parts.size == 1) value else round(value * 1e9) / 1e9
}

internal fun parseCoordinatePair(raw: String): Pair<Double, Double>? {
    val text = raw.trim()
    val separated = text.split(Regex("\\s*[,/]\\s*"))
    val parts = if (separated.size == 2) separated else {
        if (separated.size > 2) return null
        val patterns = listOf("(.+?[NS])\\s+(.+?[EW])", "(.+?[EW])\\s+(.+?[NS])",
            "([NS]\\s*.+?)\\s+([EW]\\s*.+)", "([EW]\\s*.+?)\\s+([NS]\\s*.+)",
            "([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s+([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))")
        patterns.firstNotNullOfOrNull { Regex(it, RegexOption.IGNORE_CASE).matchEntire(text) }
            ?.groupValues?.drop(1) ?: return null
    }
    val lat = parseCoordinate(parts[0], true)
    val lon = parseCoordinate(parts[1], false)
    if (lat != null && lon != null) return lat to lon
    if (!Regex("(^[EW]|[EW]$)", RegexOption.IGNORE_CASE).containsMatchIn(parts[0]) ||
        !Regex("(^[NS]|[NS]$)", RegexOption.IGNORE_CASE).containsMatchIn(parts[1])) return null
    return (parseCoordinate(parts[1], true) ?: return null) to (parseCoordinate(parts[0], false) ?: return null)
}
