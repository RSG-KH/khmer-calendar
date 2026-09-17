// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rsgkh.calendar.domain.EventRepeat
import com.rsgkh.calendar.domain.RepeatFrequency
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
    val repeat: EventRepeat? = null,
    val remindersAfter: Instant? = null,
) {
    val zone: ZoneId get() = ZoneId.of(zoneId)
    val zonedDateTime: ZonedDateTime get() = ZonedDateTime.ofLocal(date.atTime(time), zone, offsetSeconds?.let(ZoneOffset::ofTotalSeconds))
    val instant: Instant get() = zonedDateTime.toInstant()
    fun asCalendarEvent(displayZone: ZoneId = zone, sourceDate: LocalDate = date): CalendarEvent {
        return calendarOccurrence(sourceDate, atOccurrence(sourceDate).withZoneSameInstant(displayZone))
    }

    fun atOccurrence(date: LocalDate): ZonedDateTime = if (date == this.date) zonedDateTime
        else date.atTime(time).atZone(zone) // Future gaps shift forward; folds use their first offset.

    fun occurrenceDates(from: LocalDate = date, through: LocalDate = repeat?.until ?: date): Sequence<LocalDate> =
        repeat?.dates(date, from, through) ?: sequenceOf(date).filter { it in from..through }

    fun occurrences(from: LocalDate, through: LocalDate, displayZone: ZoneId): List<CalendarEvent> =
        occurrenceDates(from.minusDays(2), through.plusDays(2)).map { sourceDate ->
            calendarOccurrence(sourceDate, atOccurrence(sourceDate).withZoneSameInstant(displayZone))
        }.filter { it.date in from..through }.toList()

    private fun calendarOccurrence(sourceDate: LocalDate, displayed: ZonedDateTime) = CalendarEvent(
        "custom:$id" + if (repeat != null) "@$sourceDate" else "", displayed.toLocalDate(), title, title,
        EventKind.CUSTOM, DateBasis.USER, displayed.toLocalTime().withSecond(0).withNano(0), notes,
        customSeriesId = if (repeat != null) id else null, repeat = repeat)
}

class CustomEventRepository(context: Context) : SQLiteOpenHelper(context.applicationContext, "custom-events.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, notes TEXT NOT NULL, remind INTEGER NOT NULL, zone_id TEXT NOT NULL DEFAULT 'Asia/Phnom_Penh', offset_seconds INTEGER)")
        addRepeatColumns(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Earlier versions interpreted every saved event in Cambodia time.
            db.execSQL("ALTER TABLE events ADD COLUMN zone_id TEXT NOT NULL DEFAULT 'Asia/Phnom_Penh'")
            db.execSQL("ALTER TABLE events ADD COLUMN offset_seconds INTEGER")
        }
        if (oldVersion < 3) addRepeatColumns(db)
    }
    private fun addRepeatColumns(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN repeat_frequency TEXT")
        db.execSQL("ALTER TABLE events ADD COLUMN repeat_until TEXT")
        db.execSQL("ALTER TABLE events ADD COLUMN repeat_interval INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE events ADD COLUMN include_thirty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE events ADD COLUMN include_february INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE events ADD COLUMN remind_after TEXT")
    }
    fun all(): List<CustomEvent> = readableDatabase.query("events", null, null, null, null, null, "date, time, title").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                fun string(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name))
                val offsetIndex = cursor.getColumnIndexOrThrow("offset_seconds")
                val frequency = cursor.getString(cursor.getColumnIndexOrThrow("repeat_frequency"))
                val repeat = frequency?.let { EventRepeat(RepeatFrequency.valueOf(it), LocalDate.parse(string("repeat_until")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("repeat_interval")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("include_thirty")) == 1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("include_february")) == 1) }
                val remindersAfter = cursor.getString(cursor.getColumnIndexOrThrow("remind_after"))?.let(Instant::parse)
                add(CustomEvent(string("id"), string("title"), LocalDate.parse(string("date")), LocalTime.parse(string("time")),
                    string("notes"), cursor.getInt(cursor.getColumnIndexOrThrow("remind")) == 1, string("zone_id"),
                    if (cursor.isNull(offsetIndex)) null else cursor.getInt(offsetIndex), repeat, remindersAfter))
            }
        }
    }
    fun save(event: CustomEvent, now: Instant = Instant.now()) {
        require(event.title.isNotBlank() && event.title.trim().length <= 120)
        require(event.notes.length <= 2000 && event.date.year in 1800..2200)
        require(event.repeat?.isValid(event.date) != false)
        val saved = event.copy(time = event.time.withSecond(0).withNano(0))
        require(saved.zone.rules.getValidOffsets(saved.date.atTime(saved.time)).isNotEmpty())
        val data = ContentValues().apply {
            put("id", event.id); put("title", event.title.trim()); put("date", event.date.toString())
            put("time", saved.time.toString()); put("notes", event.notes.trim())
            put("zone_id", saved.zone.id); put("offset_seconds", saved.zonedDateTime.offset.totalSeconds)
            // Saving a historical date must never create retrospective notifications.
            put("remind", if (saved.repeat != null || saved.instant > now) 1 else 0)
            put("repeat_frequency", saved.repeat?.frequency?.name)
            put("repeat_until", saved.repeat?.until?.toString())
            put("repeat_interval", saved.repeat?.interval ?: 3L)
            put("include_thirty", if (saved.repeat?.includeThirty == true) 1 else 0)
            put("include_february", if (saved.repeat?.includeFebruary == true) 1 else 0)
            // Past occurrences of a newly saved series must not send same-day repeat reminders.
            put("remind_after", now.toString())
        }
        check(writableDatabase.insertWithOnConflict("events", null, data, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun delete(id: String) { writableDatabase.delete("events", "id = ?", arrayOf(id)) }
}
