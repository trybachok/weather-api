import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Точка входа: конфиг, запуск сценария.
 */
public class Main {

    public static void main(String[] args) {

        WeatherApiClient apiClient = new WeatherApiClient(AppConstants.API_KEY);
        WeatherParser parser = new WeatherParser();
        WeatherService service = new WeatherService(apiClient, parser);

        try {
            service.printWeatherInfo(AppConstants.LAT, AppConstants.LON, AppConstants.LIMIT);
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка при обращении к API погоды: " + e.getMessage());
        }
    }
}

/**
 * Отвечает только за HTTP-запросы к Яндекс.Погоде.
 */
class WeatherApiClient {

    private final HttpClient httpClient;
    private final String apiKey;

    public WeatherApiClient(String apiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = apiKey;
    }

    public String getForecastJson(String lat, String lon, int limit) throws IOException, InterruptedException {
        String url = buildUrl(lat, lon, limit);
        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String buildUrl(String lat, String lon, int limit) {
        return String.format("%s?lat=%s&lon=%s&limit=%d", AppConstants.API_URL, lat, lon, limit);
    }

    private HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Yandex-API-Key", apiKey)
                .GET()
                .build();
    }
}

/**
 * Отвечает только за разбор JSON и извлечение данных.
 */
class WeatherParser {

    public int extractCurrentTemperature(String jsonBody) {
        JSONObject json = new JSONObject(jsonBody);
        JSONObject fact = json.getJSONObject("fact");
        return fact.getInt("temp");
    }

    public double calculateAverageDayTemperature(String jsonBody) {
        JSONObject json = new JSONObject(jsonBody);
        JSONArray forecasts = json.getJSONArray("forecasts");

        double sum = 0;
        int count = 0;

        for (int i = 0; i < forecasts.length(); i++) {
            JSONObject forecast = forecasts.getJSONObject(i);
            JSONObject parts = forecast.getJSONObject("parts");
            JSONObject day = parts.getJSONObject("day");
            int tempAvg = day.getInt("temp_avg");
            sum += tempAvg;
            count++;
        }

        if (count == 0) {
            throw new IllegalStateException("В ответе отсутствуют данные для прогноза");
        }

        return sum / count;
    }
}

/**
 * Сервисный слой: связывает API-клиент и парсер, реализует бизнес-сценарий.
 */
class WeatherService {

    private final WeatherApiClient apiClient;
    private final WeatherParser parser;

    public WeatherService(WeatherApiClient apiClient, WeatherParser parser) {
        this.apiClient = apiClient;
        this.parser = parser;
    }

    public void printWeatherInfo(String lat, String lon, int limit) throws IOException, InterruptedException {
        String json = apiClient.getForecastJson(lat, lon, limit);

        // 1. Выводим полный JSON-ответ
        printFullResponse(json);

        // 2. Текущая температура (fact.temp)
        int currentTemp = parser.extractCurrentTemperature(json);
        System.out.println("\nТекущая температура: " + currentTemp + "°C");

        // 3. Средняя температура по прогнозу (forecasts[*].parts.day.temp_avg)
        double averageTemp = parser.calculateAverageDayTemperature(json);
        System.out.println("Средняя дневная температура за " + limit + " суток: " + averageTemp + "°C");
    }

    private void printFullResponse(String json) {
        System.out.println("Полный ответ от сервиса (JSON):");
        System.out.println(json);
    }
}
