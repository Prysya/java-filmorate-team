package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;
    private static final String MPAS_SQL = "select * from mpas";

    @Override
    public Mpa getMpaById(Integer mpaId) {
        try {
            return jdbcTemplate.queryForObject(MPAS_SQL.concat(" where id = ?"), new MpaMapper(), mpaId);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public Collection<Mpa> getAllMpa() {
        return jdbcTemplate.query(MPAS_SQL, new MpaMapper());
    }
}
