// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

val CAMBODIA_ZONE: ZoneId = ZoneId.of("Asia/Phnom_Penh")

data class CustomEvent(
    val id: String = UUID.randomUUID().toString(), val title: String,
    val date: LocalDate, val time: LocalTime, val notes: String = "",
    val remindersEligible: Boolean = true,
    val zoneId: String = CAMBODIA_ZONE.id,
    val offsetSeconds: Int? = null,
) {
    val zone: ZoneId get() = ZoneId.of(zoneId)
    val zonedDateTime: ZonedDateTime get() = ZonedDateTime.ofLocal(date.atTime(time), zone, offsetSeconds?.let(ZoneOffset::ofTotalSeconds))
    val instant: Instant get() = zonedDateTime.toInstant()
    fun asCalendarEvent(displayZone: ZoneId = zone): CalendarEvent {
        val displayed = instant.atZone(displayZone)
        return CalendarEvent("custom:$id", displayed.toLocalDate(), title, title, EventKind.CUSTOM, DateBasis.USER, displayed.toLocalTime().withSecond(0).withNano(0), notes)
    }
}

class CustomEventRepository(context: Context) : SQLiteOpenHelper(context.applicationContext, "custom-events.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, notes TEXT NOT NULL, remind INTEGER NOT NULL, zone_id TEXT NOT NULL DEFAULT 'Asia/Phnom_Penh', offset_seconds INTEGER)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Earlier versions interpreted every saved event in Cambodia time.
            db.execSQL("ALTER TABLE events ADD COLUMN zone_id TEXT NOT NULL DEFAULT 'Asia/Phnom_Penh'")
            db.execSQL("ALTER TABLE events ADD COLUMN offset_seconds INTEGER")
        }
    }
    fun all(): List<CustomEvent> = readableDatabase.query("events", null, null, null, null, null, "date, time, title").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                fun string(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name))
                val offsetIndex = cursor.getColumnIndexOrThrow("offset_seconds")
                add(CustomEvent(string("id"), string("title"), LocalDate.parse(string("date")), LocalTime.parse(string("time")),
                    string("notes"), cursor.getInt(cursor.getColumnIndexOrThrow("remind")) == 1, string("zone_id"),
                    if (cursor.isNull(offsetIndex)) null else cursor.getInt(offsetIndex)))
            }
        }
    }
    fun save(event: CustomEvent, now: Instant = Instant.now()) {
        require(event.title.isNotBlank() && event.title.trim().length <= 120)
        require(event.notes.length <= 2000 && event.date.year in 1800..2200)
        val saved = event.copy(time = event.time.withSecond(0).withNano(0))
        require(saved.zone.rules.getValidOffsets(saved.date.atTime(saved.time)).isNotEmpty())
        val data = ContentValues().apply {
            put("id", event.id); put("title", event.title.trim()); put("date", event.date.toString())
            put("time", saved.time.toString()); put("notes", event.notes.trim())
            put("zone_id", saved.zone.id); put("offset_seconds", saved.zonedDateTime.offset.totalSeconds)
            // Saving a historical date must never create retrospective notifications.
            put("remind", if (saved.instant > now) 1 else 0)
        }
        check(writableDatabase.insertWithOnConflict("events", null, data, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun delete(id: String) { writableDatabase.delete("events", "id = ?", arrayOf(id)) }
}
