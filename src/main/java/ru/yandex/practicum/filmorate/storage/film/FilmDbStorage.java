package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.constants.SearchBy;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.filmDirector.FilmDirectorStorage;
import ru.yandex.practicum.filmorate.storage.filmGenre.FilmGenreStorage;
import ru.yandex.practicum.filmorate.storage.filmMpa.FilmMpaStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private static final String FILMS_SQL =
            "select f.*, m.id as mpa_id, m.name as mpa_name from films f left join film_mpas fm on f.id = fm.film_id " +
                    "left join mpas m on fm.mpa_id = m.id";
    private static final String SEARCH_FILM_BASE_QUERY = "select distinct films.id as id from films " +
            "left join film_directors on films.id = film_directors.film_id " +
            "left join directors on film_directors.director_id = directors.director_id ";
    private final JdbcTemplate jdbcTemplate;
    private final FilmMpaStorage filmMpaStorage;
    private final MpaStorage mpaStorage;
    private final FilmGenreStorage filmGenreStorage;
    private final FilmDirectorStorage filmDirectorStorage;

    @Override
    public Film createFilm(Film film) {
        final String sql = "insert into films (name, release_date, description, duration, rate) " +
                "values (?, ?, ?, ?, ?)";

        KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[]{"id"});
            preparedStatement.setString(1, film.getName());
            preparedStatement.setObject(2, film.getReleaseDate());
            preparedStatement.setString(3, film.getDescription());
            preparedStatement.setInt(4, film.getDuration());
            preparedStatement.setInt(5, film.getRate());

            return preparedStatement;
        }, generatedKeyHolder);

        int filmId = Objects.requireNonNull(generatedKeyHolder.getKey()).intValue();

        film.setId(filmId);

        return addExtraFields(film);
    }

    @Override
    public Film getFilmById(Integer filmId) {
        List<Film> films = jdbcTemplate.query(FILMS_SQL.concat(" where f.id = ?"), new FilmMapper(), filmId);

        if (!films.isEmpty()) {
            Collection<Genre> filmGenres = filmGenreStorage.getAllFilmGenresById(filmId);
            Collection<Director> directors = filmDirectorStorage.getFilmDirectors(filmId);

            return films.get(0).toBuilder().genres(filmGenres).directors(directors).build();
        }

        return null;
    }

    @Override
    public Collection<Film> getAllFilms() {
        Collection<Film> films = jdbcTemplate.query(FILMS_SQL, new FilmMapper());

        return setFilmGenresAndDirectors(films);
    }

    @Override
    public Film updateFilm(Film film) {
        final String sql = "update films set name = ?, release_date = ?, description = ?, duration = ?, " +
                "rate = ? where id = ?";

        filmMpaStorage.deleteFilmMpaById(film.getId());
        filmGenreStorage.deleteAllFilmGenresById(film.getId());
        filmDirectorStorage.deleteFilmDirectors(film.getId());

        jdbcTemplate.update(sql,
                film.getName(),
                film.getReleaseDate(),
                film.getDescription(),
                film.getDuration(),
                film.getRate(),
                film.getId()
        );

        return addExtraFields(film);
    }

    @Override
    public Collection<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        final Collection<String> params = new ArrayList<>();
        String sql =
                "select f.*, m.id as mpa_id, m.name as mpa_name from films f left join likes l on f.id = l.film_id " +
                        "left join film_mpas fm on f.id = fm.film_id left join mpas m on fm.mpa_id = m.id " +
                        "left join film_genres fg on f.id = fg.film_id %s group by f.name, f.id order by count(l.film_id) desc limit ?";

        if (Objects.nonNull(genreId)) {
            params.add(String.format("genre_id = %s", genreId));
        }

        if (Objects.nonNull(year)) {
            params.add(String.format("YEAR(release_date) = %s", year));
        }

        final String genreAndYearParams = !params.isEmpty() ? "where ".concat(String.join(" and ", params)) : "";
        Collection<Film> films = jdbcTemplate.query(String.format(sql, genreAndYearParams), new FilmMapper(), count);

        return setFilmGenresAndDirectors(films);
    }

    @Override
    public Collection<Film> getCommonFilms(Integer userId, Integer friendId) {
        String sql = "select * from (select f.*, m.id as mpa_id, m.name as mpa_name from films f " +
                "left join likes l on f.id = l.film_id left join film_mpas fm on f.id = fm.film_id " +
                "left join mpas m on fm.mpa_id = m.id left join film_genres fg on f.id = fg.film_id " +
                "group by f.name, f.id order by count(l.film_id)) f, likes l1, likes l2 " +
                "where f.id = l1.film_id and f.id = l2.film_id and l1.user_id = ? and l2.user_id = ?";
        Collection<Film> films = jdbcTemplate.query(sql, new FilmMapper(), userId, friendId);

        return setFilmGenresAndDirectors(films);
    }

    @Override
    public boolean deleteFilmById(Integer id) {
        final String sql = "delete from films where id = ?";
        int status = jdbcTemplate.update(sql, id);
        return status != 0;
    }

    @Override
    public Collection<Film> getDirectorFilms(Integer directorId, SortBy sortBy) {
        String yearOrderSql = "select f.*, " +
                "       m.id as mpa_id, " +
                "       m.name as mpa_name " +
                "from film_directors fd " +
                "         join films f on f.id = fd.film_id " +
                "         join film_mpas fm on f.id = fm.film_id " +
                "         join mpas m on fm.mpa_id = m.id " +
                "where director_id = ? " +
                "order by year(f.release_date)";

        String likesOrderSql = "select f.*,  " +
                "       m.id as mpa_id,  " +
                "       m.name as mpa_name,  " +
                "       (select count(*) from likes where fd.film_id = likes.film_id) as likes " +
                "from film_directors fd " +
                "join films f on f.id = fd.film_id " +
                "join film_mpas fm on f.id = fm.film_id " +
                "join mpas m on fm.mpa_id = m.id " +
                "where director_id = ? " +
                "order by likes desc;";

        Collection<Film> films = jdbcTemplate.query(
                sortBy == SortBy.LIKES ? likesOrderSql : yearOrderSql,
                new FilmMapper(),
                directorId
        );

        if (films.isEmpty()) {
            return Collections.emptyList();
        }

        return setFilmGenresAndDirectors(films);
    }

    @Override
    public Collection<Film> getUserRecommendations(Integer userId) {
        String sql = "select l.user_id " +
                "from likes as l " +
                "where l.film_id in " +
                "(select film_id " +
                "from likes l1 " +
                "where user_id = ?) and l.user_id <> ?" +
                "group by l.user_id " +
                "order by count(l.film_id) " +
                "limit 1";

        final List<Integer> userIds = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("user_id"), userId, userId);

        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        int similarUserId = userIds.get(0);

        String filmsFromUser = "select f.*, m.id as mpa_id, m.name as mpa_name " +
                "from films as f left join film_mpas as fm on f.id = fm.film_id " +
                "left join mpas as m on fm.mpa_id = m.id " +
                "left join likes as l on f.id = l.film_id " +
                "where l.user_id = ?";

        List<Film> userFilms = jdbcTemplate.query(filmsFromUser, new FilmMapper(), userId);
        List<Film> similarUserFilms = jdbcTemplate.query(filmsFromUser, new FilmMapper(), similarUserId);

        similarUserFilms.removeAll(userFilms);

        return setFilmGenresAndDirectors(similarUserFilms);
    }

    private Collection<Film> setFilmGenresAndDirectors(Collection<Film> films) {
        Map<Integer, Collection<Genre>> filmGenresMap = filmGenreStorage.getAllFilmGenres(films);
        Map<Integer, Collection<Director>> filmDirectorsMap = filmDirectorStorage.getFilmDirectors(films);

        films.forEach(film -> {
            Integer filmId = film.getId();

            film.setGenres(filmGenresMap.getOrDefault(filmId, new ArrayList<>()));
            film.setDirectors(filmDirectorsMap.getOrDefault(filmId, new ArrayList<>()));
        });

        return films;
    }

    private Film addExtraFields(Film film) {

        int filmId = film.getId();
        int mpaId = film.getMpa().getId();

        filmMpaStorage.addFilmMpa(filmId, mpaId);
        film.getGenres().forEach(genre -> filmGenreStorage.addFilmGenre(filmId, genre.getId()));

        Mpa filmMpa = mpaStorage.getMpaById(mpaId);
        Collection<Genre> filmGenres = filmGenreStorage.getAllFilmGenresById(filmId);

        filmDirectorStorage.setFilmDirectors(film.getDirectors(), filmId);
        Collection<Director> directors = filmDirectorStorage.getFilmDirectors(filmId);

        return film.toBuilder().mpa(filmMpa).genres(filmGenres).directors(directors).build();
    }

    @Override
    public Set<Film> search(String query, Set<SearchBy> searchFields) {

        String searchQueryParameters = searchFields.stream()
                .map(field -> "Lower(" + field.getSqlTableAndFieldName() + ") LIKE '%" + query.toLowerCase() + "%'")
                .collect(Collectors.joining(" OR "));
        String searchQuery = SEARCH_FILM_BASE_QUERY + " WHERE " + searchQueryParameters;
        log.trace("Текст запроса поиска фильмов: {}", SEARCH_FILM_BASE_QUERY + searchQuery);

        Set<Integer> filmIds = new HashSet<>(jdbcTemplate.query(searchQuery, (rs, rowNum) -> rs.getInt("id")));
        log.trace("Получены следующие ID фильмов, подходящие под условия поиска: {}", filmIds);
        Set<Film> films = getFilmsByIds(filmIds);
        log.trace("Получен список фильмов по ID: {}", films);
        return new HashSet<>(setFilmGenresAndDirectors(films));
    }

    private Set<Film> getFilmsByIds(Set<Integer> filmIds) {
        String joinedFilmIds = filmIds.stream().map(x -> Integer.toString(x)).collect(Collectors.joining(","));
        String searchQuery = String.format("%s WHERE f.id IN (%s)", FILMS_SQL, joinedFilmIds);
        return new HashSet<>(jdbcTemplate.query(searchQuery, new FilmMapper()));
    }

}
