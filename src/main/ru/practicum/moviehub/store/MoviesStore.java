package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.InputException;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int generatedId = 0;

    public MoviesStore() {
    }

    public int generateId() {
        return ++generatedId;
    }

    public void clearStore() {
        movies.clear();
        generatedId = 0;
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public Movie addMovie(Movie movie) throws InputException {
        if (validate(movie.getYear(), movie.getTitle())) {
            int id = generateId();
            movie.setId(id);
            movies.put(id, movie);
        }
        return movie;
    }

    public void deleteMovie(int id) {
        movies.remove(id);
    }

    public Movie findMovie(int id) {
        return movies.get(id);
    }

    public List<Movie> filterByYear(int year) {
        return getMovies().stream().filter(movie -> movie.getYear() == year).toList();
    }

    private boolean validate(int year, String title) throws InputException {
        if (year < 1888 || year > 2027) {
            throw new InputException("Год не может меньше 1888 и больше 2027");
        }
        if (title.isBlank() || title.length() > 100) {
            throw new InputException("Название фильма не может быть пустым или содержать больше 100 символов");
        }
        return true;
    }
}
