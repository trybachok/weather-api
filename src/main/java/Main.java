import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Точка входа: запуск HTTP-сервера.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        // Явно привязываемся к 0.0.0.0, чтобы сервер был доступен снаружи контейнера
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", new WeatherHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(5));
        System.out.println("Сервер успешно запущен и слушает на порту " + port);
        System.out.println("Локальная ссылка: http://localhost:8080");
        server.start();
    }

    static class WeatherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            WeatherApiClient apiClient = new WeatherApiClient(AppConstants.API_KEY);
            WeatherParser parser = new WeatherParser();
            WeatherService service = new WeatherService(apiClient, parser);

            String response;
            int statusCode = 200;

            try {
                response = service.getWeatherHtml(AppConstants.LAT, AppConstants.LON, AppConstants.LIMIT);
            } catch (IOException e) {
                response = "<h1>Ошибка сети или API</h1><p>" + e.getMessage() + "</p>";
                statusCode = 502;
            } catch (InterruptedException e) {
                response = "<h1>Выполнение прервано</h1>";
                statusCode = 500;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                response = "<h1>Произошла непредвиденная ошибка</h1><p>" + e.getMessage() + "</p>";
                statusCode = 500;
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
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

        if (response.statusCode() != 200) {
            throw new IOException("API вернул ошибку. Статус-код: " + response.statusCode() + ". Ответ: " + response.body());
        }

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

    public String getWeatherHtml(String lat, String lon, int limit) throws IOException, InterruptedException {
        String json = apiClient.getForecastJson(lat, lon, limit);

        int currentTemp = parser.extractCurrentTemperature(json);
        double averageTemp = parser.calculateAverageDayTemperature(json);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Погода</title></head><body>");
        html.append("<h1>Информация о погоде</h1>");
        html.append("<p><b>Текущая температура:</b> ").append(currentTemp).append("°C</p>");
        html.append("<p><b>Средняя дневная температура за ").append(limit).append(" суток:</b> ")
                .append(String.format("%.2f", averageTemp)).append("°C</p>");
        html.append("<hr>");
        html.append("<h3>Полный ответ от сервиса (JSON):</h3>");
        html.append("<pre>").append(json).append("</pre>");
        html.append("</body></html>");

        return html.toString();
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
