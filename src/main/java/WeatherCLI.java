import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class WeatherCLI {
    private static final String API_KEY = "0bd000f340eeaf963372a49178ad3704";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";

    public static void main(String[] args) {
        if (args.length == 0) {
            showHelpAndExit("Du hast keine Stadt angegeben.\nBeispiel: java -jar WeatherCLI.jar Kassel");
            return;
        }



        String city = args[0];
        boolean isForecast = args.length > 1;
        boolean isJson = false;
        String inputDate = null;
        String inputTime = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--json":
                    isJson = true;
                    break;
                case "--date":
                    if (i + 1 < args.length) inputDate = args[++i];
                    break;
                case "--time":
                    if (i + 1 < args.length) inputTime = args[++i];
                    break;
                default:
                    showHelpAndExit("Unbekannte Option: " + args[i]);
            }
        }

        try {
            if (isForecast) {
                LocalDate today = LocalDate.now();
                LocalDate targetDate = (inputDate != null) ? parseDate(inputDate) : today;

                LocalTime targetTime;
                if (inputTime != null) {
                    targetTime = LocalTime.parse(normalizeTime(inputTime));
                } else {
                    targetTime = LocalTime.now();
                }

                LocalDateTime targetDateTime = LocalDateTime.of(targetDate, targetTime);

                // Wenn kein Datum angegeben wurde und die Uhrzeit heute bereits vorbei ist → auf morgen verschieben
                if (inputDate == null && targetDateTime.isBefore(LocalDateTime.now())) {
                    targetDate = targetDate.plusDays(1);
                    targetDateTime = LocalDateTime.of(targetDate, targetTime);
                    System.out.println("Hinweis: Uhrzeit liegt heute bereits in der Vergangenheit. Nehme morgen: " + targetDateTime);
                } else if (inputDate != null && targetDateTime.isBefore(LocalDateTime.now())) {
                    System.out.println("Fehler: Datum und Uhrzeit dürfen nicht in der Vergangenheit liegen.");
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                System.out.println("Suche Vorhersage für: " + targetDateTime.format(formatter));

                JsonNode forecastData = getForecastWeather(city, targetDateTime);
                if (forecastData != null) {
                    if (isJson) {
                        System.out.println(forecastData.toPrettyString());
                    } else {
                        String dateTime = forecastData.get("dt_txt").asText();
                        showFormattedWeather(forecastData, dateTime, city);
                    }
                } else {
                    System.out.println("Kein Wettereintrag für diesen Zeitpunkt gefunden.");
                }

            } else {
                JsonNode weatherData = getWeatherData(city);
                if (isJson) {
                    System.out.println(weatherData.toPrettyString());
                } else {
                    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    showFormattedWeather(weatherData, dateTime, city);
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }

    public static void showHelpAndExit(String message) {
        System.out.println("Fehler: " + message);
        System.out.println("\nVerfügbare Optionen:");
        System.out.println("  --date YYYY-MM-DD    : Datum für Vorhersage (z. B. 2025-03-24)");
        System.out.println("  --date DD.MM.YYYY    : Alternativ im deutschen Format");
        System.out.println("  --time HH:mm         : Uhrzeit für Vorhersage (z. B. 23:00)");
        System.out.println("  --json               : Ausgabe im JSON-Format");
        System.exit(1);
    }



    public static JsonNode getWeatherData(String town) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + town + "&units=metric&lang=de&appid=" + API_KEY;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Fehler: " + response);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.body().string());
        }
    }

    public static JsonNode getForecastWeather(String city, LocalDateTime targetDateTime) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + city
                + "&units=metric&lang=de&appid=" + API_KEY;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Fehler: " + response);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode list = root.get("list");

            JsonNode bestMatch = null;
            long minDiff = Long.MAX_VALUE;

            for (JsonNode entry : list) {
                String dateTime = entry.get("dt_txt").asText();
                LocalDateTime forecastTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                long diff = Math.abs(Duration.between(forecastTime, targetDateTime).toMinutes());

                if (diff == 0) {
                    return entry;
                }

                if (diff < minDiff) {
                    minDiff = diff;
                    bestMatch = entry;
                }
            }

            if (bestMatch != null) {
                System.out.println("Gewünschte Uhrzeit nicht verfügbar. Zeige nächstgelegene Zeit: " + bestMatch.get("dt_txt").asText());
            }
            return bestMatch;
        }
    }

    public static LocalDate parseDate(String input) {
        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter germanFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        try {
            return LocalDate.parse(input, isoFormatter);
        } catch (Exception e) {
            try {
                return LocalDate.parse(input, germanFormatter);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Ungültiges Datumsformat! Bitte nutze YYYY-MM-DD oder DD.MM.YYYY.");
            }
        }
    }

    public static String normalizeTime(String input) {
        if (input == null || input.isEmpty()) return "12:00";

        String[] parts = input.split(":");
        if (parts.length == 2) {
            String hour = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
            String minute = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
            return hour + ":" + minute;
        }

        throw new IllegalArgumentException("Ungültiges Uhrzeitformat! Bitte nutze HH:mm (z. B. 00:00 oder 23:45).");
    }

    public static void showFormattedWeather(JsonNode node, String dateTime, String cityName) {
        String town = (cityName != null && !cityName.isEmpty()) ? cityName : "Unbekannt";
        double temp = node.get("main").get("temp").asDouble();
        double feelsLike = node.get("main").get("feels_like").asDouble();
        int humidity = node.get("main").get("humidity").asInt();
        String weatherDesc = node.get("weather").get(0).get("description").asText();
        double windSpeed = node.get("wind").get("speed").asDouble();

        System.out.println("\nWetterdaten für " + town + " am " + dateTime + ":");
        System.out.println("----------------------------------------");
        System.out.println("Temperatur:       " + temp + "°C");
        System.out.println("Gefühlt wie:      " + feelsLike + "°C");
        System.out.println("Luftfeuchtigkeit: " + humidity + "%");
        System.out.println("Wind:             " + windSpeed + " m/s");
        System.out.println("Wetterlage:       " + weatherDesc);
    }
}