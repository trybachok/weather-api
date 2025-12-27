public final class AppConstants {

    public static final String API_URL = "https://api.weather.yandex.ru/v2/forecast";
    public static final String API_KEY = "22ade84f-58ad-451e-b836-e8e23bb30c4a";

    public static final String LAT = "55.75";
    public static final String LON = "37.62";
    public static final int LIMIT = 5;

    private AppConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
