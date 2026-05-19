package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static final Gson gson = new Gson();
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getMoviesStore().clearStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpResponse<String> resp = sendGetMoviesRequest();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expected = gson.toJson(List.of());
        assertEquals(expected, body, "Ожидается пустой JSON-массив");
    }

    @Test
    void getMovies_returnsArrayMovies() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMoviesRequest();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        List<Movie> movies = List.of(new Movie(1, "Зеленая миля", 1999),
                new Movie(2, "Джентльмены", 2019));
        String expected = gson.toJson(movies);
        assertEquals(expected, body, "Ожидается JSON-массив с двумя фильмами");
    }

    @Test
    void postMovies_whenDataIsCorrect_addMovie() throws IOException, InterruptedException {

        HttpResponse<String> resp = sendPostRequest("Форрест Гамп", 1994);

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        HttpResponse<String> getResp = sendGetMoviesRequest();

        String body = getResp.body().trim();
        String expected = gson.toJson(List.of(new Movie(1, "Форрест Гамп", 1994)));
        assertEquals(expected, body, "Ожидается JSON-массив c одним добавленным фильмом");
    }

    @Test
    void postMovies_whenTitleIsEmpty_returnsError() throws IOException, InterruptedException {

        HttpResponse<String> resp = sendPostRequest("", 1994);

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void postMovies_whenTitleIsTooLong_returnsError() throws IOException, InterruptedException {

        HttpResponse<String> resp = sendPostRequest("«Зеленая миля» – культовый роман Стивена Кинга. Читателям " +
                "предстоит переместиться в страшный мир тюремного блока смертников. Убийцы, маньяки и психопаты в блоке " +
                "Е ждут своего последнего часа, а работающие там надзиратели – либо садисты, либо очень несчастные " +
                "люди.", 1994);

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void postMovies_whenYearIs2037_returnsError() throws IOException, InterruptedException {

        HttpResponse<String> resp = sendPostRequest("", 2037);

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void postMovies_whenContentTypeIsInvalid_returnsError() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Зеленая миля\",\"year\":\"1999\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void postMovies_whenJsonIsInvalid_returnsError() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Зеленая миля\",\"year\":1999\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void getMoviesById_whenIdCorrect_returnsMovie() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMovieByIdRequest("2");

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expected = gson.toJson(new Movie(2, "Джентльмены", 2019));
        assertEquals(expected, body);
    }

    @Test
    void getMoviesById_whenMovieNotFound_returnsError() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMovieByIdRequest("3");

        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void getMoviesById_whenIdIsNotNumber_returnsError() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMovieByIdRequest("a");

        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void deleteMoviesById_whenIdCorrect_deletesMovie() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendDeleteMovieByIdRequest("1");

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        HttpResponse<String> getResp = sendGetMoviesRequest();

        String body = getResp.body().trim();
        String expected = gson.toJson(List.of(new Movie(2, "Джентльмены", 2019)));
        assertEquals(expected, body);
    }

    @Test
    void deleteMoviesById_whenMovieNotFound_returnsError() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendDeleteMovieByIdRequest("3");

        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void deleteMoviesById_whenIdIsNotNumber_returnsError() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendDeleteMovieByIdRequest("h");

        assertEquals(400, resp.statusCode(), "DELETE /movies/{id} должен вернуть 400");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void getMoviesByYear_whenYearIsCorrect_returnsMovies() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);
        sendPostRequest("Бойцовский клуб", 1999);

        HttpResponse<String> resp = sendGetMoviesByYearRequest("1999");

        assertEquals(200, resp.statusCode(), "GET /movies?year= должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expected = gson.toJson(List.of(new Movie(1, "Зеленая миля", 1999),
                new Movie(3, "Бойцовский клуб", 1999)));
        assertEquals(expected, body, "Ожидается JSON-массив с двумя фильмами 1999г");
    }

    @Test
    void getMoviesByYear_whenNoMoviesFound_returnsEmptyArray() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMoviesByYearRequest("2016");

        assertEquals(200, resp.statusCode(), "GET /movies?year= должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expected = gson.toJson(List.of());
        assertEquals(expected, body, "Ожидается пустой JSON-массив");
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returnsError() throws Exception {

        sendPostRequest("Зеленая миля", 1999);
        sendPostRequest("Джентльмены", 2019);

        HttpResponse<String> resp = sendGetMoviesByYearRequest("a");

        assertEquals(400, resp.statusCode(), "GET /movies?year= должен вернуть 400");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    @Test
    void whenMethodNotAllowed_returnsError() throws Exception {

        HttpResponse<String> resp = sendPutRequest();

        assertEquals(405, resp.statusCode(), "PUT /movies должен вернуть 405");

        String body = resp.body().trim();

        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(jsonObject.has("error"), "Ответ сервера должен содержать поле 'error'");
    }

    private HttpResponse<String> sendPostRequest(String title, int year) throws IOException, InterruptedException {
        String jsonBody = String.format("{\"title\":\"%s\",\"year\":\"%d\"}", title, year);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private HttpResponse<String> sendGetMoviesRequest() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response;
    }

    private HttpResponse<String> sendGetMovieByIdRequest(String id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response;
    }

    private HttpResponse<String> sendDeleteMovieByIdRequest(String id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response;
    }

    private HttpResponse<String> sendGetMoviesByYearRequest(String year) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=" + year))
                .GET()
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response;
    }

    private HttpResponse<String> sendPutRequest() throws IOException, InterruptedException {
        String jsonBody = gson.toJson("");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response;
    }

}