package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.validation.GenreValidator;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class GenreService {

    private static final String NOT_FOUND_MESSAGE = "Жанра с id %s нет";
    private final GenreStorage genreStorage;

    public Genre getGenreById(Integer id) {
        Genre genre = genreStorage.getGenreById(id);

        checkGenreIsNotNull(genre, id);

        return genre;
    }

    public Collection<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    private void checkGenreIsNotNull(Genre genre, Integer id) {
        if (GenreValidator.isGenreNotFound(getAllGenres(), genre)) {
            throw new NotFoundException(String.format(NOT_FOUND_MESSAGE, id));
        }
    }
}
