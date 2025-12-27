import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class WeatherServiceTest {

    @Test
    public void testGetWeatherHtml() throws IOException, InterruptedException {
        // Создаем фейковый клиент, который возвращает фиксированный JSON
        WeatherApiClient fakeClient = new WeatherApiClient("fake-key") {
            @Override
            public String getForecastJson(String lat, String lon, int limit) {
                return "{" +
                        "\"fact\": {\"temp\": 10}," +
                        "\"forecasts\": [" +
                        "  {\"parts\": {\"day\": {\"temp_avg\": 20}}}" +
                        "]" +
                        "}";
            }
        };

        WeatherParser parser = new WeatherParser();
        WeatherService service = new WeatherService(fakeClient, parser);

        String html = service.getWeatherHtml("55.75", "37.62", 1);

        assertNotNull(html);
        assertTrue(html.contains("Текущая температура:</b> 10°C"));
        assertTrue(html.contains("Средняя дневная температура за 1 суток:</b> 20,00°C") || 
                   html.contains("Средняя дневная температура за 1 суток:</b> 20.00°C"));
    }
}
