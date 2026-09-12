// Copyright (c) 2026 RSG-KH | Apache-2.0 License
// Development audit only. Run against the app's compiled debug Kotlin classes
// and Kotlin stdlib; no copy of the calendar algorithm is maintained here.
import com.rsgkh.calendar.domain.KhmerCalendar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

class ExportCalendarReference {
    public static void main(String[] args) throws Exception {
        var output = new StringBuilder("date,day,waxing,month,holy,buddhist_year\n");
        for (var date = LocalDate.of(2000, 1, 1); date.isBefore(LocalDate.of(2031, 1, 1)); date = date.plusDays(1)) {
            var lunar = KhmerCalendar.INSTANCE.fromGregorian(date);
            output.append(date).append(',').append(lunar.getDay()).append(',')
                .append(lunar.getWaxing()).append(',').append(lunar.getMonth()).append(',')
                .append(lunar.isHolyDay()).append(',').append(lunar.getBuddhistYear()).append('\n');
        }
        Files.writeString(Path.of(args[0]), output);
    }
}
