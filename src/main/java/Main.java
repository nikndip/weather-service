import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Main {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("YANDEX_WEATHER_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Задай переменную окружения YANDEX_WEATHER_KEY"
            );
        }

        int limit = 3;
        String url = "https://api.weather.yandex.ru/v2/forecast"
            + "?lat=55.75&lon=37.62&limit=" + limit;

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-Yandex-Weather-Key", apiKey)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        System.out.println("GET " + url);
        System.out.println("HTTP-статус: " + response.statusCode());
        System.out.println("Полный ответ сервиса:");
        System.out.println(response.body());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                "Ошибка API: HTTP " + response.statusCode()
            );
        }

        Files.writeString(
            Path.of("response.json"),
            response.body(),
            StandardCharsets.UTF_8
        );
        ObjectMapper mapper = new ObjectMapper();
JsonNode root = mapper.readTree(response.body());

JsonNode currentTemperature = root.path("fact").path("temp");

if (!currentTemperature.isNumber()) {
    throw new IllegalStateException(
        "Ответ API не содержит числовое значение fact.temp"
    );
}

System.out.println(
    "Текущая температура (fact.temp): "
        + currentTemperature.numberValue() + " °C"
);
JsonNode forecasts = root.path("forecasts");

if (!forecasts.isArray() || forecasts.size() < limit) {
    throw new IllegalStateException(
        "В ответе API недостаточно дней прогноза: требуется " + limit
    );
}

double sum = 0;

System.out.println("Дневные температуры для расчёта:");

for (int i = 0; i < limit; i++) {
    JsonNode forecast = forecasts.get(i);
    JsonNode dayTemperature = forecast
        .path("parts")
        .path("day")
        .path("temp_avg");

    if (!dayTemperature.isNumber()) {
        throw new IllegalStateException(
            "Нет числового parts.day.temp_avg для дня с индексом " + i
        );
    }

    sum += dayTemperature.asDouble();

    System.out.println(
        forecast.path("date").asText()
            + ": " + dayTemperature.numberValue() + " °C"
    );
}

double average = sum / limit;

System.out.printf(
    "Средняя температура за %d суток: %.2f °C%n",
    limit,
    average
);
    }

}