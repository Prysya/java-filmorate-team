package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.constants.SortBy;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Set;

public interface FilmStorage {

    Film createFilm(Film film);

    Film getFilmById(Integer id);

    Collection<Film> getAllFilms();

    Collection<Film> getAllFilms(Set<Integer> filmIds);

    Film updateFilm(Film film);

    Collection<Film> getPopularFilms(Integer count);

    Collection<Film> getDirectorFilms(Integer directorId, SortBy sortBy);

}
