package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson = new Gson();
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, 0);
        String json = gson.toJson(body);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json.getBytes());
        }
    }

    protected void sendErrorResponse(HttpExchange ex, int status, String error, String... details) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, 0);
        ErrorResponse errorResponse = new ErrorResponse(error, List.of(details));
        String json = gson.toJson(errorResponse);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json.getBytes());
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }
}

