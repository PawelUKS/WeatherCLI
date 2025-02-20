import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;



public class WeatherCLI {
    private static final String API_KEY = "0bd000f340eeaf963372a49178ad3704";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";



    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Bitte gib eine Stadt an! Beispiel: java WetterCLI Kassel");
            return;
        }

        String town = args[0];
        try {
            JsonNode weatherData = getWeatherData(town);
            showWeather(weatherData);
        } catch (IOException e) {
            System.out.println("Fehler beim Abrufen der Wetterdaten: " + e.getMessage());
        }
    }
    private static JsonNode getWeatherData(String town) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + town + "&units=metric&lang=de&appid=" + API_KEY;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Fehler: " + response);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.body().string());
        }
    }

    public static void showWeather(JsonNode rootNode) {
        String town = rootNode.get("name").asText();
        double temp = rootNode.get("main").get("temp").asDouble();
        String weather = rootNode.get("weather").get(0).get("description").asText();

        System.out.println("🌤 Wetter in " + town + ":");
        System.out.println("------------------------");
        System.out.println("🌡 Temperatur: " + temp + "°C");
        System.out.println("☁ Wetter: " + weather);
    }

}
