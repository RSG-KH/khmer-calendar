# Reviewed Recurring Event Fallback

The recurring event engine provides predictable, calculable calendar events for years outside the 2000–2030 dated snapshot across the full **1800–2200** range.

The system incorporates **100 reviewed recurrence rules**, covering Cambodian royal holidays, national observances, UNESCO-inscribed intangible cultural heritage, international remembrance days, Khmer lunar festivals, and floating weekday observances. The authoritative 3,246 dated records for 2000–2030 remain completely intact and take precedence.

---

## Precedence and Scope

- **2000–2030 (Snapshot Priority)**: The application loads the complete, verified historical year dataset from `calendar-events.tsv`. No calculated observances are added, substituted, or deduplicated for covered years. Government public holiday status remains tied to the dated, official records.
- **1800–1999 and 2031–2200 (Calculated Fallback)**: The application calculates applicable recurring events using `RecurringEvents.forYear(year)`, alongside computed Buddhist Holy Days (*Thngai Sil*) from `KhmerCalendar` and user-created custom events.
- **Identity & Attributes**: Generated events have `DateBasis.CALCULATED`, stable `calculated:<rule-id>` identities, `EventKind.OBSERVANCE`, no official-source URL, and no assumed arrival time. In dialogs and lists, they are clearly marked as calculated observances.
- **Conservative Coverage**: A calculated date represents an astronomical or civil calendar pattern, not legal proof of an official government holiday or civil leave in historical or distant future years.

---

## Included Rules Summary (100 Rules)

| Group | Rules | Description & Coverage |
| :--- | :---: | :--- |
| **Royal & National Public Holidays** | 6 | King Sihamoni's Birthday (May 14), Queen Mother's Birthday (Jun 18), King Father's Commemoration (Oct 15), King's Coronation Day (Oct 29), Peace Day in Cambodia (Dec 29), and Win-win Policy Day (2000–2023). |
| **Cambodian Heritage & UNESCO Inscriptions** | 19 | Fixed solar dates commemorating national milestones and UNESCO heritage with dynamic anniversary calculations (ASEAN, National Police, ICJ Preah Vihear, Angkor, Preah Vihear, Sambor Prei Kuk, Koh Ker, Royal Ballet, Shadow Theatre, Lkhon Khol, Kun Lbokator, Chapei Dang Veng, Tug of War, Krama, Tuol Sleng Archives, etc.). |
| **Cambodian National Observances** | 6 | Maternal/Child Health Day, National Mine Awareness Day, Water Policy Day, Cambodian Older Persons Day, Environmental Sanitation Day, National Authors' Day. |
| **International & UN Observances** | 23 | World Water Day, World Tuberculosis Day, World Health Day, International Buddhist Day, World Safety and Health at Work, Press Freedom Day, Red Cross Day, Families Day, No Tobacco Day, Child Labour Day, Anti-Drug Day, World Ranger Day, Indigenous Peoples Day, Youth Day, Literacy Day, Ozone Layer Day, Peace Day, Tourism Day, Halloween, World AIDS Day, Disabled Persons Day, Civil Aviation Day, Anti-Corruption Day. |
| **Khmer Lunar Festivals** | 23 | Meak Bochea, Visak Bochea, Royal Ploughing Ceremony, Pre-Lent Candle Making Day, The Ordained Dragon Monk, Beginning and Ending of Buddhist Lent, Ben 1 to 13, Pchum Ben (3 days), Kathen Kal, Water Festival (3 days). |
| **Khmer New Year Stages** | 3 | First day (Moha Sankranta), middle Vanabat days, and final day (Veareak Laeung Sak), dynamically determined by the astronomical solar transit engine. |
| **Floating Weekday Observances** | 2 | Mother's Day (2nd Sunday of May) and Father's Day (3rd Sunday of June), computed via the `solar_nth_weekday` rule algorithm. |

---

## Special Calculations

### 1. Second Asadh (*Adhikamasa*)
Khmer leap-month years insert an extra month of Asadh (First Asadh and Second Asadh). Traditions tied to Buddhist Lent (Candle Making on 8 waxing, Dragon Monk on 14 waxing, and Beginning of Lent on 1 waning) correctly bind to **Second Asadh** (month index 13) in leap years, matching Cambodian Buddhist tradition and the recorded dataset.

### 2. Multi-Day Festival Sequences
- **Pchum Ben**: Spans 3 days: the day before, the day of, and the day after 15 waning Phutrobot.
- **Water Festival**: Spans 3 days beginning on 14 waxing Kattik.
- **Khmer New Year**: Spans 3 or 4 days depending on the solar transit calculation (*Moha Sankranta* to *Veareak Laeung Sak*).

### 3. Floating Weekday Algorithm (`solar_nth_weekday`)
Mother's Day and Father's Day use a dedicated rule type:
```kotlin
val first = LocalDate.of(year, rule.month, 1)
val daysToAdd = (rule.day - first.dayOfWeek.value + 7) % 7 + (rule.offset - 1) * 7
val anchor = first.plusDays(daysToAdd.toLong())
```
This guarantees exact Sunday calculations across all 401 years (1800–2200) without arbitrary tables.

### 4. Dynamic Anniversary Counts
For heritage and remembrance events whose Khmer title includes `{anniversary}`, `RecurringEvents.forYear` evaluates `year - anniversaryBase` and formats the number in Khmer numerals (`ខួបលើកទី...`). In future years (e.g. 2031+), anniversaries continue incrementing automatically.

---

## Status of Chinese Cultural Festivals

The 2000–2030 dataset includes 8 Chinese traditional festivals observed in Cambodia:
- Chinese New Year & Chinese New Year's Eve
- Kitchen God Festival (*Thngai Sen Dok Cheung Thoob*)
- Tomb-Sweeping Day (*Qingming / Cheng Meng*)
- Sticky Rice Dumpling Festival (*Duanwu / Sen Nom Chang*)
- Hungry Ghost Festival (*Sen Kbal Teuk*)
- Mid-Autumn Festival (*Sen Lok Khae*)
- Winter Solstice Festival (*Dongzhi / Sen Nom Ee*)

Because the Chinese calendar uses astronomical lunisolar calculations referenced to 120°E with different leap-month placement rules from the Khmer Chhankitek calendar, these events remain preserved in the dated 2000–2030 snapshot and are not projected into 1800–1999 or 2031–2200 until a dedicated Chinese lunisolar engine is introduced.

---

## Verification & Documented Differences

Automated regression tests in `RecurringEventsTest.kt` evaluate the 100 recurrence rules against **1,411 captured occurrence dates** across 2000–2030. All rules match the captured historical occurrences exactly, with two documented historical discrepancies:

1. **2012 Khmer New Year Disagreement**:
   - Captured website dates: 13–15 April 2012.
   - Astronomical Sankranta computation: 14–16 April 2012.
   - The app preserves the historical 13–15 April records for 2012 within the snapshot.
2. **2005–2019 King Sihamoni's Birthday Holiday Block**:
   - Captured website dates (2005–2019): 3-day official public holiday (May 13, 14, 15).
   - Recurrence rule computation: May 14 (annual birthday).
   - From 2020 onward, the Cambodian government (Sub-decree No. 112) officially reduced this holiday to the single day of May 14, matching the recurrence calculation.

---

## Reproducibility

1. **Manifest**: `tools/recurring-event-rules.json` contains the declarative definitions of all 100 rules.
2. **Compiler**: Running `python tools/build-recurring-events.py` compiles:
   - `app/src/main/resources/recurrence-rules.tsv` (runtime resource).
   - `app/src/test/resources/recurrence-reference.tsv` (test fixture).
3. **Tests**: Run `./gradlew.bat testDebugUnitTest` to execute all engine, recurrence, and UI scenario tests.
