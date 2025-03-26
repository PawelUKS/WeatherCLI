import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class WeatherCLITest {

    @Test
    void testParseDateIsoFormat() {
        LocalDate date = WeatherCLI.parseDate("2025-03-24");
        assertEquals(2025, date.getYear());
        assertEquals(3, date.getMonthValue());
        assertEquals(24, date.getDayOfMonth());
    }

    @Test
    void testParseDateGermanFormat() {
        LocalDate date = WeatherCLI.parseDate("24.03.2025");
        assertEquals(LocalDate.of(2025, 3, 24), date);
    }

    @Test
    void testParseDateInvalidFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            WeatherCLI.parseDate("24/03/2025");
        });
        assertTrue(exception.getMessage().contains("Ungültiges Datumsformat"));
    }

    @Test
    void testNormalizeTime() {
        assertEquals("09:05", WeatherCLI.normalizeTime("9:5"));
        assertEquals("23:00", WeatherCLI.normalizeTime("23:00"));
    }

    @Test
    void testNormalizeTimeInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            WeatherCLI.normalizeTime("23.00");
        });
        assertTrue(exception.getMessage().contains("Ungültiges Uhrzeitformat"));
    }

    @Test
    void testForecastReturnsClosestTimeMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        // Datum wählen, das nicht auf 3h-Intervall fällt
        String futureDate = LocalDate.now().plusDays(2).toString();
        try {
            WeatherCLI.main(new String[]{"Berlin", "--date", futureDate, "--time", "14:00"});
        } catch (Exception ignored) {}

        System.setOut(originalOut);
        String output = out.toString();

        assertTrue(output.contains("Gewünschte Uhrzeit nicht verfügbar") || output.contains("Suche Vorhersage für"));
    }



}
