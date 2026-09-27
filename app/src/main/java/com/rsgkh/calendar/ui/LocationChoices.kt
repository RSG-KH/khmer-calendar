// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.ui

import com.rsgkh.calendar.data.PlaceCountry
import com.rsgkh.calendar.data.PlaceDivision
import com.rsgkh.calendar.data.PlaceDocument
import com.rsgkh.calendar.i18n.L
import java.text.Collator
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale

internal data class PlaceOption(
    val id: String, val label: String, val aliases: List<String> = emptyList(), val detail: String? = null,
)

internal fun normalizedPlaceSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC).trim().lowercase(Locale.ROOT)

internal class PlaceSearchIndex(choices: List<PlaceOption>) {
    private val prepared = choices.map { Triple(it, normalizedPlaceSearch(it.label), it.aliases.map(::normalizedPlaceSearch).filter(String::isNotEmpty)) }

    fun matching(query: String): List<PlaceOption> {
        val needle = normalizedPlaceSearch(query)
        val groups = List(4) { mutableListOf<PlaceOption>() }
        for ((choice, label, aliases) in prepared) {
            val rank = when {
                label.startsWith(needle) -> 0
                label.contains(needle) -> 1
                aliases.any { it.startsWith(needle) } -> 2
                aliases.any { it.contains(needle) } -> 3
                else -> continue
            }
            groups[rank].add(choice)
        }
        return groups.flatten()
    }
}

internal fun countryOptions(countries: List<PlaceCountry>, k: Boolean): List<PlaceOption> {
    val locale = Locale.forLanguageTag(if (k) "km" else "en")
    val collator = Collator.getInstance(locale)
    return countries.map { country ->
        val localized = Locale.Builder().setRegion(country.code).build().getDisplayCountry(locale)
        val label = localized.takeUnless { it.isBlank() || it == country.code } ?: country.name
        PlaceOption(country.code, label, listOf(country.name, country.code), country.name.takeIf { it != label })
    }.sortedWith { a, b -> collator.compare(a.label, b.label) }
}

internal fun administrativeOptions(document: PlaceDocument?, adm1: String?, k: Boolean): List<PlaceOption> {
    if (document == null) return emptyList()
    val districts = document.divisions.filter { it.level == 2 && it.parentId == adm1 }.associateBy { it.id }
    val rows = if (adm1 == null) document.divisions.filter { it.level == 1 } else {
        val communes = document.divisions.filter { it.level == 3 && it.parentId in districts }
        // Cambodia offers communes only. Other countries also allow ADM2 points.
        communes + if (document.code == "KH") emptyList() else districts.values
    }
    fun aliases(row: PlaceDivision) = listOf(row.name, row.nameKm, row.code,
        row.nameKm.replace(Regex("^(រាជធានី|ខេត្ត|ស្រុក|ក្រុង|ខណ្ឌ|ឃុំ|សង្កាត់)\\s*"), ""),
        row.name.replace(Regex("^(Province|District|Municipality|Khan|Commune|Sangkat)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(Province|Capital)$", RegexOption.IGNORE_CASE), ""))
    return rows.map { row ->
        val parent = districts[row.parentId]
        val detail = when {
            document.code == "KH" && row.level == 3 && (row.latitude == null || row.longitude == null) -> "coordinates_unavailable"
            document.code != "KH" && runCatching { ZoneId.of(row.timeZone) }.isFailure -> "zone_unavailable"
            row.approximate -> "approximate_point"
            else -> null
        }
        PlaceOption(row.id, listOfNotNull(parent?.label(k), row.label(k)).joinToString(" · "),
            aliases(row) + parent?.let(::aliases).orEmpty(), detail?.let { L.text("location.$it", k) })
    }
}
