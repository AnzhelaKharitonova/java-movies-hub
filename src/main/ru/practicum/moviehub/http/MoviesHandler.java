package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.InputException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) {
        try (ex) {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();

            if (!path.startsWith("/movies")) {
                sendErrorResponse(ex, 404, "Некорректный запрос",
                        "Путь должен начинаться с '/movies'");
                return;
            }
            String[] split = path.split("/");
            switch (method) {
                case "GET":
                    handleGet(ex, split, query);
                    break;
                case "POST":
                    handlePost(ex, split, query);
                    break;
                case "DELETE":
                    handleDelete(ex, split);
                    break;
                default:
                    sendErrorResponse(ex, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleGet(HttpExchange ex, String[] split, String query) throws IOException {
        if (split.length == 2 && query == null) {
            sendJson(ex, 200, moviesStore.getMovies());
            return;
        }
        if (split.length == 3) {
            int id = parsePath(split[2]);
            if (id != -1) {
                Movie movie = moviesStore.findMovie(id);
                if (movie != null) {
                    sendJson(ex, 200, movie);
                } else {
                    sendErrorResponse(ex, 404, "Not found", "Фильм не найден");
                }
            } else {
                sendErrorResponse(ex, 400, "Bad request", "Некорректный ID");
            }
        }
        if (query != null && query.startsWith("year=")) {
            String pathYear = query.substring("year=".length());
            int year = parsePath(pathYear);
            if (year != -1) {
                sendJson(ex, 200, moviesStore.filterByYear(year));
            } else {
                sendErrorResponse(ex, 400, "Bad request", "Некорректный параметр запроса — 'year'");
            }
        }
    }

    private void handlePost(HttpExchange ex, String[] split, String query) throws IOException {
        if (split.length == 2 && query == null) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendErrorResponse(ex, 415, "Unsupported Media Type",
                        "Получен запрос с неправильным значением заголовка 'Content-Type'");
                return;
            }
            String jsonBody;
            try (InputStream inputStream = ex.getRequestBody()) {
                jsonBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Movie movie = gson.fromJson(jsonBody, Movie.class);
                Movie movieWithId = moviesStore.addMovie(movie);
                sendJson(ex, 201, movieWithId);
            } catch (JsonSyntaxException e) {
                sendErrorResponse(ex, 400, "Invalid JSON syntax",
                        "Неверный синтаксис JSON или несоответствие типов");
            } catch (InputException e) {
                sendErrorResponse(ex, 422, "Ошибка валидации", e.getMessage());
            }
        }
    }

    private void handleDelete(HttpExchange ex, String[] split) throws IOException {
        if (split.length == 3) {
            int id = parsePath(split[2]);
            if (id != -1) {
                Movie deletedMovie = moviesStore.findMovie(id);
                if (deletedMovie != null) {
                    moviesStore.deleteMovie(id);
                    sendNoContent(ex);
                } else {
                    sendErrorResponse(ex, 404, "Not Found", "Фильм не найден");
                }
            } else {
                sendErrorResponse(ex, 400, "Bad Request", "Некорректный ID");
            }
        }
    }

    private int parsePath(String patch) {
        try {
            return Integer.parseInt(patch);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
