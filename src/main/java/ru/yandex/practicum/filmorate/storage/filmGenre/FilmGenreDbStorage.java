package ru.yandex.practicum.filmorate.storage.filmGenre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilmGenreDbStorage implements FilmGenreStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addFilmGenre(Integer filmId, Integer genreId) {
        final String sql = "insert into film_genres (film_id, genre_id) values (?, ?)";

        try {
            jdbcTemplate.update(sql, filmId, genreId);
        }
        catch (DuplicateKeyException e) {
            log.warn("Обнаружен дубликат ключей. filmId: {}, genreId: {}", filmId, genreId);
        }
    }

    @Override
    public Collection<Genre> getAllFilmGenresById(Integer filmId) {
        final String sql = "select g.id as id, name from film_genres fg left join genres g on " +
                "fg.genre_id = g.id where film_id = ?";

        return jdbcTemplate.query(sql, new GenreMapper(), filmId);
    }

    @Override
    public void deleteAllFilmGenresById(Integer filmId) {
        final String sql = "delete from film_genres where film_id = ?";

        jdbcTemplate.update(sql, filmId);
    }

    @Override
    public Map<Integer, Collection<Genre>> getAllFilmGenres(Collection<Film> films) {
        final String sql = "select fg.film_id as film_id, g.id as genre_id, g.name as name from film_genres fg " +
                "left join genres g on fg.genre_id = g.id where fg.film_id in (%s)";

        Map<Integer, Collection<Genre>> filmGenresMap = new HashMap<>();
        Collection<String> ids = films.stream().map(film -> String.valueOf(film.getId())).collect(Collectors.toList());

        jdbcTemplate.query(String.format(sql, String.join(",", ids)), rs -> {
            Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("name"));

            Integer filmId = rs.getInt("film_id");

            filmGenresMap.putIfAbsent(filmId, new ArrayList<>());
            filmGenresMap.get(filmId).add(genre);
        });

        return filmGenresMap;
    }
}
