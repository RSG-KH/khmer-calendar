// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.rsgkh.calendar.data.*
import com.rsgkh.calendar.domain.parseCoordinate
import com.rsgkh.calendar.domain.parseCoordinatePair
import com.rsgkh.calendar.ui.copyToClipboard
import com.rsgkh.calendar.ui.administrativeOptions
import com.rsgkh.calendar.ui.countryOptions
import com.rsgkh.calendar.ui.PlaceSearchIndex
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ReleaseTwelveTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun coordinatesAcceptMapsFormatsAndRejectAmbiguousValues() {
        assertEquals(11.5745158, parseCoordinate("11.5745158° N", true)!!, 1e-9)
        assertEquals(-73.985656, parseCoordinate("73°59′8.3616″W", false)!!, 1e-9)
        val pair = parseCoordinatePair("11°34'28.25688\"N, 104°55'27.02028\"E")!!
        assertEquals(11.5745158, pair.first, 1e-9)
        assertEquals(104.9241723, pair.second, 1e-9)
        assertEquals(11.5 to 104.9, parseCoordinatePair("104.9E 11.5N"))
        for (bad in listOf("91", "-12 N", "12 E", "11 60", "11 2 60", "NaN", "1e2", "11.2 3")) {
            assertNull(bad, parseCoordinate(bad, true))
        }
    }

    @Test fun bundledCatalogRestoresDefaultAndHasSelectableAdministrativePoints() {
        val catalog = BirthplaceCatalog(context)
        assertTrue(catalog.countries().any { it.code == "KH" })
        val cambodia = catalog.divisions("KH")
        val commune = cambodia.divisions.single { it.id == Birthplace.DEFAULT.id }
        assertEquals(Birthplace.DEFAULT, cambodia.selection(commune, false))
        val district = cambodia.divisions.single { it.id == commune.parentId }
        assertEquals(2, district.level)
        assertNull(cambodia.selection(district, false))
        assertEquals(commune.nameKm, cambodia.selection(commune, true)!!.label)
        assertNotEquals(commune.name, commune.nameKm)
        val belgium = catalog.divisions("BE")
        assertTrue(belgium.divisions.any { belgium.selection(it, false) != null })
        val first = catalog.divisions("KH")
        catalog.divisions("FR")
        assertSame(first, catalog.divisions("KH"))
        catalog.divisions("US")
        catalog.divisions("JP")
        assertNotSame(first, catalog.divisions("KH"))
        assertTrue(catalog.timeZones().any { it.first == "Europe/Brussels" })
    }

    @Test fun savedSettingsRoundTripWithoutChangingNotificationOrEmojiChoices() {
        val preferences = AppPreferences(context)
        val expected = AppSettings(enableAstrologyAndZodiac = false, astrologyMinutes = 435,
            risingPlace = Birthplace("manual", "Brussels", "Brussels", "", 50.85, 4.35, "Europe/Brussels"),
            useEmojiForWesternZodiac = true, useEmojiForGanzhiAnimals = true, notificationsEnabled = true)
        preferences.write(expected)
        assertEquals(expected, preferences.read())
        val raw = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        raw.edit().putString("fontScale", "unknown").putString("showGanzhi", "broken")
            .putString("risingPlace", "{broken").putInt("astrologyMinutes", -1).commit()
        val restored = preferences.read()
        assertEquals(Birthplace.DEFAULT, restored.risingPlace)
        assertEquals(720, restored.astrologyMinutes)
        assertTrue(restored.showGanzhi)
        assertTrue(restored.notificationsEnabled)
    }

    @Test fun savedPlacesDeduplicateRemoveAndValidate() {
        val saved = SavedBirthplaces(context)
        saved.save(Birthplace.DEFAULT)
        saved.save(Birthplace.DEFAULT)
        assertEquals(1, saved.read().size)
        assertEquals(Birthplace.DEFAULT, Birthplace.fromJson(Birthplace.DEFAULT.toJson()))
        assertNull(Birthplace.fromJson(Birthplace.DEFAULT.copy(latitude = 100.0).toJson()))
        saved.remove(Birthplace.DEFAULT)
        assertTrue(saved.read().isEmpty())
    }

    @Test fun eventOrderingKeepsPersonalTimesFirstAndSavedTiesStable() {
        val date = LocalDate.of(2026, 9, 27)
        fun event(id: String, kind: EventKind, time: LocalTime? = null) = CalendarEvent(id, date, id, id, kind, DateBasis.USER, time)
        val input = listOf(event("observance", EventKind.OBSERVANCE), event("untimed", EventKind.CUSTOM),
            event("holiday", EventKind.HOLIDAY), event("holy", EventKind.HOLY_DAY),
            event("late", EventKind.CUSTOM, LocalTime.of(17, 0)), event("z", EventKind.CUSTOM, LocalTime.of(9, 0)),
            event("a", EventKind.CUSTOM, LocalTime.of(9, 0)), event("midnight", EventKind.CUSTOM, LocalTime.MIDNIGHT))
        assertEquals(listOf("midnight", "z", "a", "late", "untimed", "holiday", "observance", "holy"), input.sortedWith(calendarEventOrder).map { it.id })
    }

    @Test fun yearCacheRetainsRecentEntriesAndEvictsOldOnes() {
        EventRepository.clearCache()
        val old = EventRepository.forYear(2026)
        for (year in 2000..2010) EventRepository.forYear(year)
        assertSame(old, EventRepository.forYear(2026))
        for (year in 2030..2041) EventRepository.forYear(year)
        assertNotSame(old, EventRepository.forYear(2026))
    }

    @Test fun unavailableClipboardDoesNotReportSuccess() {
        val unavailable = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? = if (name == Context.CLIPBOARD_SERVICE) null else super.getSystemService(name)
        }
        val denied = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? = if (name == Context.CLIPBOARD_SERVICE) throw SecurityException("Clipboard unavailable") else super.getSystemService(name)
        }
        assertFalse(copyToClipboard(unavailable, "Date", "September 27, 2026"))
        assertFalse(copyToClipboard(denied, "Date", "September 27, 2026"))
        assertTrue(copyToClipboard(context, "Date", "September 27, 2026"))
    }

    @Test fun locationChoicesMatchPwaLevelsAndSearchAliases() {
        val catalog = BirthplaceCatalog(context)
        val countries = countryOptions(catalog.countries(), true)
        val belgiumCountry = PlaceSearchIndex(countries).matching("Belgium").single()
        assertEquals("BE", belgiumCountry.id)
        assertEquals("Belgium", belgiumCountry.detail)
        val cambodia = catalog.divisions("KH")
        val commune = cambodia.divisions.single { it.id == Birthplace.DEFAULT.id }
        val district = cambodia.divisions.single { it.id == commune.parentId }
        val choices = administrativeOptions(cambodia, district.parentId, true)
        assertTrue(choices.isNotEmpty())
        assertTrue(choices.all { option -> cambodia.divisions.single { it.id == option.id }.level == 3 })
        assertTrue(PlaceSearchIndex(choices).matching("Voat Phnum").any { it.id == commune.id })
        assertTrue(PlaceSearchIndex(choices).matching(district.nameKm).any { it.id == commune.id })
        val belgium = catalog.divisions("BE")
        val provinces = administrativeOptions(belgium, null, false)
        assertEquals("Bruxelles-Capitale", PlaceSearchIndex(provinces).matching("BRU").first().label)
        val missingPoint = commune.copy(latitude = null)
        val missingChoices = administrativeOptions(cambodia.copy(divisions = cambodia.divisions.map { if (it.id == commune.id) missingPoint else it }), district.parentId, true)
        assertEquals("គ្មានកូអរដោនេ; សូមប្រើទីកន្លែងផ្ទាល់ខ្លួន", missingChoices.single { it.id == commune.id }.detail)
    }

    @Test fun sourceUrlsTranslationMatchesCatalog() {
        assertEquals("ប្រភព URL", com.rsgkh.calendar.i18n.L.text("about.source_urls_title", true))
        assertEquals("Source URLs", com.rsgkh.calendar.i18n.L.text("about.source_urls_title", false))
        assertEquals("មិនអាចចម្លងបានទេ។ សូមជ្រើសរើសប្រភព URL ដើម្បីចម្លងដោយខ្លួនឯង។", com.rsgkh.calendar.i18n.L.text("ui.could_not_copy_urls", true))
        assertEquals("ព្រឹត្តិការណ៍ទាំងអស់ក្នុងខែវិច្ឆិកា (៥)", com.rsgkh.calendar.i18n.L.text("ui.all_events_in_month.ab923a", true, "month" to "វិច្ឆិកា", "count" to "៥"))
        assertEquals("All events in November (5)", com.rsgkh.calendar.i18n.L.text("ui.all_events_in_month.ab923a", false, "month" to "November", "count" to 5))
        assertEquals("Calculations by Khmer Calendar Engine v0.6.0.", com.rsgkh.calendar.i18n.L.text("events.engine_calculations", false, "version" to "0.6.0"))
        assertEquals("ការគណនាធ្វើឡើងដោយ Khmer Calendar Engine កំណែ 0.6.0 ។", com.rsgkh.calendar.i18n.L.text("events.engine_calculations", true, "version" to "0.6.0"))
        assertEquals("Calculations by Khmer Calendar Engine v0.6.0.", com.rsgkh.calendar.i18n.L.text("astrology.engine_calculations", false, "version" to "0.6.0"))
        assertEquals("ការគណនាធ្វើឡើងដោយ Khmer Calendar Engine កំណែ 0.6.0 ។", com.rsgkh.calendar.i18n.L.text("astrology.engine_calculations", true, "version" to "0.6.0"))
    }

    @Test fun truncateSettingLabelLimitsLengthToTwelveCharsAndAppendsThreeDots() {
        assertEquals("Sangkat Voat...", com.rsgkh.calendar.ui.truncateSettingLabel("Sangkat Voat Phnum", khmer = false))
        assertEquals("Phnom Penh", com.rsgkh.calendar.ui.truncateSettingLabel("Phnom Penh", khmer = false))
        assertEquals("123456789012", com.rsgkh.calendar.ui.truncateSettingLabel("123456789012", khmer = false))
        assertEquals("123456789012...", com.rsgkh.calendar.ui.truncateSettingLabel("1234567890123", khmer = false))

        val watPhnomKm = "សង្កាត់ វត្ដភ្នំ" // 16 characters
        assertEquals(16, watPhnomKm.length)
        assertEquals(watPhnomKm, com.rsgkh.calendar.ui.truncateSettingLabel(watPhnomKm, khmer = true))
        assertEquals("1234567890123456", com.rsgkh.calendar.ui.truncateSettingLabel("1234567890123456", khmer = true))
        assertEquals("1234567890123456...", com.rsgkh.calendar.ui.truncateSettingLabel("12345678901234567", khmer = true))
    }
}
