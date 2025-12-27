import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WeatherParserTest {

    private final WeatherParser parser = new WeatherParser();

    private final String sampleJson = "{" +
            "\"fact\": {\"temp\": 10}," +
            "\"forecasts\": [" +
            "  {\"parts\": {\"day\": {\"temp_avg\": 15}}}," +
            "  {\"parts\": {\"day\": {\"temp_avg\": 25}}}" +
            "]" +
            "}";

    @Test
    public void testExtractCurrentTemperature() {
        int temp = parser.extractCurrentTemperature(sampleJson);
        assertEquals(10, temp);
    }

    @Test
    public void testCalculateAverageDayTemperature() {
        double avg = parser.calculateAverageDayTemperature(sampleJson);
        assertEquals(20.0, avg, 0.001);
    }

    @Test
    public void testCalculateAverageDayTemperatureEmpty() {
        String emptyForecastsJson = "{\"fact\": {\"temp\": 10}, \"forecasts\": []}";
        assertThrows(IllegalStateException.class, () -> {
            parser.calculateAverageDayTemperature(emptyForecastsJson);
        });
    }
}
