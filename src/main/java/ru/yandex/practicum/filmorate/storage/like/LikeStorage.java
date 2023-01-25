package ru.yandex.practicum.filmorate.storage.like;

import java.util.Map;
import java.util.Set;

public interface LikeStorage {

    void addLikeToFilm(Integer filmId, Integer userId);

    void deleteLikeFromFilm(Integer filmId, Integer userId);

    Map<Integer, Set<Integer>> getLikes();
}
