// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.util.Locale

data class Birthplace(
    val source: String, val id: String, val label: String, val countryCode: String,
    val latitude: Double, val longitude: Double, val timeZone: String,
    val datasetVersion: String = "",
) {
    fun toJson(): String = JSONObject().put("source", source).put("id", id).put("label", label)
        .put("countryCode", countryCode).put("latitude", latitude).put("longitude", longitude)
        .put("timeZone", timeZone).put("datasetVersion", datasetVersion).toString()

    companion object {
        val DEFAULT = Birthplace("CambodiaDivisions", "018ae4a2-6397-49c9-8e5d-f5e0adabff2b",
            "Sangkat Voat Phnum", "KH", 11.5745158, 104.9241723, "Asia/Phnom_Penh",
            "2eddb97241b92329c5f5547f6101fb32ee02ccb3cf510ec1371fd91a84dc732f")

        fun fromJson(raw: String?): Birthplace? = runCatching {
            val json = JSONObject(raw ?: return null)
            Birthplace(json.getString("source"), json.getString("id"), json.getString("label"),
                json.getString("countryCode"), json.getDouble("latitude"), json.getDouble("longitude"),
                json.getString("timeZone"), json.optString("datasetVersion")).also {
                require(it.source in setOf("manual", "GeoNamesAdmin", "CambodiaDivisions"))
                require(it.label.isNotBlank() && it.label.length <= 160 && it.id.isNotBlank())
                require(it.latitude.isFinite() && it.latitude in -90.0..90.0)
                require(it.longitude.isFinite() && it.longitude in -180.0..180.0)
                require(it.countryCode.isEmpty() || it.countryCode.matches(Regex("[A-Z]{2}")))
                if (it.source != "manual") require(it.datasetVersion.matches(Regex("[0-9a-f]{64}")))
                if (it.source == "GeoNamesAdmin") require((it.id.toLongOrNull() ?: 0) > 0)
                if (it.source == "CambodiaDivisions") require(it.countryCode == "KH" && it.id.matches(Regex("[0-9a-f-]{36}")))
                ZoneId.of(it.timeZone)
            }
        }.getOrNull()
    }
}

internal data class PlaceCountry(val code: String, val name: String)
internal data class PlaceDivision(
    val id: String, val parentId: String?, val level: Int, val name: String, val nameKm: String,
    val latitude: Double?, val longitude: Double?, val timeZone: String,
    val code: String = "", val approximate: Boolean = false,
) {
    fun label(khmer: Boolean) = if (khmer && nameKm.isNotBlank()) nameKm else name
}
internal data class PlaceDocument(val code: String, val version: String, val divisions: List<PlaceDivision>) {
    fun selection(division: PlaceDivision, khmer: Boolean): Birthplace? {
        if (code == "KH" && division.level != 3) return null
        val latitude = division.latitude ?: return null
        val longitude = division.longitude ?: return null
        if (runCatching { ZoneId.of(division.timeZone) }.isFailure) return null
        return Birthplace(if (code == "KH") "CambodiaDivisions" else "GeoNamesAdmin", division.id,
            division.label(khmer), code, latitude, longitude, division.timeZone, version)
    }
}

/** Immutable bundled catalogs, loaded on IO and retained for at most two countries. */
internal class BirthplaceCatalog(context: Context) {
    private val assets = context.applicationContext.assets
    private val documents = object : LinkedHashMap<String, PlaceDocument>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PlaceDocument>?) = size > 2
    }
    fun countries(): List<PlaceCountry> {
        val json = assets.open("birthplaces/country-index.json").bufferedReader().use { JSONObject(it.readText()) }
        val countries = json.getJSONArray("countries")
        return List(countries.length()) { countries.getJSONObject(it).let { row ->
            PlaceCountry(row.getString("code"), row.getString("name"))
        } }
    }

    @Synchronized fun divisions(code: String): PlaceDocument {
        require(code.matches(Regex("[A-Z]{2}")))
        documents[code]?.let { return it }
        val path = if (code == "KH") "cambodia.json" else "countries/$code.json"
        // The Android asset merger expands .gz inputs and removes the extension.
        val json = assets.open("birthplaces/$path").bufferedReader().use { JSONObject(it.readText()) }
        require(json.getString("countryCode") == code)
        val rows = json.getJSONArray("divisions")
        val document = PlaceDocument(code, json.getString("datasetVersion"), List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            PlaceDivision(row.get("id").toString(), if (row.isNull("parentId")) null else row.get("parentId").toString(),
                row.getInt("level"), row.getString(if (code == "KH") "nameEn" else "name"), row.optString("nameKm"),
                if (row.isNull("latitude")) null else row.getDouble("latitude"),
                if (row.isNull("longitude")) null else row.getDouble("longitude"),
                if (code == "KH") "Asia/Phnom_Penh" else row.optString("timeZone"),
                row.optString("code"), row.optString("coordinateConfidence") == "low")
        })
        documents[code] = document
        return document
    }

    fun timeZones(khmer: Boolean = false): List<Pair<String, String>> {
        val json = assets.open("birthplaces/time-zones.json").bufferedReader().use { JSONObject(it.readText()) }
        val countries = json.getJSONObject("countries")
        val zones = json.getJSONArray("zones")
        return (List(zones.length()) { zones.getJSONObject(it) }.mapNotNull { row ->
            val id = row.getString("id")
            val country = Locale.Builder().setRegion(row.getString("countryCode")).build()
                .getDisplayCountry(Locale.forLanguageTag(if (khmer) "km" else "en"))
            if (runCatching { ZoneId.of(id) }.isFailure) null
            else id to "$country ${countries.getString(row.getString("countryCode"))} ${row.optString("comment")}"
        } + ("UTC" to "Coordinated Universal Time GMT")).sortedBy { it.first }
    }
}

internal class SavedBirthplaces(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("birthplaces", Context.MODE_PRIVATE)
    fun read(): List<Birthplace> = runCatching {
        val array = JSONArray(prefs.getString("places", "[]"))
        List(array.length()) { array.optJSONObject(it)?.toString() }.mapNotNull(Birthplace::fromJson)
    }.getOrDefault(emptyList())
    fun save(place: Birthplace, previousManualId: String? = null) {
        require(Birthplace.fromJson(place.toJson()) != null)
        write(read().filterNot {
            it.source == place.source && (if (place.source == "manual")
                it.id == previousManualId || it.label.equals(place.label, ignoreCase = true) else it.id == place.id)
        } + place)
    }
    fun remove(place: Birthplace) = write(read().filterNot { it.id == place.id && it.source == place.source })
    private fun write(places: List<Birthplace>) {
        val array = JSONArray()
        places.forEach { array.put(JSONObject(it.toJson())) }
        prefs.edit { putString("places", array.toString()) }
    }
}
