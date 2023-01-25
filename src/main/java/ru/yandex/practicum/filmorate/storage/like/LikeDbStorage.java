package ru.yandex.practicum.filmorate.storage.like;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LikeDbStorage implements LikeStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addLikeToFilm(Integer filmId, Integer userId) {
        final String sql = "insert into likes (film_id, user_id) values (?, ?)";

        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void deleteLikeFromFilm(Integer filmId, Integer userId) {
        final String sql = "delete from likes where film_id = ? and user_id = ?";

        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public Map<Integer, Set<Integer>> getLikes() {
        String sql = "select * from likes";

        Map<Integer, Set<Integer>> likes = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            Integer userId = rs.getInt("user_id");
            Integer filmId = rs.getInt("film_id");

            Set userIds = likes.getOrDefault(userId, new HashSet<>());
            userIds.add(filmId);

            likes.put(userId, userIds);
        });

        return likes;
    }
}
