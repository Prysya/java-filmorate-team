package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final FilmService filmService;
    private final EventService eventService;

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") Integer id) {
        return userService.getUserById(id);
    }

    @GetMapping("/{id}/friends")
    public Collection<User> getUserFriends(@PathVariable("id") Integer id) {
        return userService.getUserFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<User> getCommonFriends(@PathVariable("id") Integer id, @PathVariable("otherId") Integer otherId) {
        return userService.getCommonFriends(id, otherId);
    }

    @GetMapping("/{id}/recommendations")
    public Collection<Film> getRecommendations(@PathVariable("id") Integer id) {
        return filmService.getRecommendations(id);
    }

    @GetMapping("/{id}/feed")
    public List<Event> getFeed(@PathVariable int id) {
        return eventService.getFeed(id);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        return userService.updateUser(user);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable("id") Integer id, @PathVariable("friendId") Integer friendId) {
        userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void deleteFriend(@PathVariable("id") Integer id, @PathVariable("friendId") Integer friendId) {
        userService.deleteFriend(id, friendId);
    }

    @DeleteMapping("/{userId}")
    public void deleteUserById(@PathVariable("userId") Integer id) {
        userService.deleteUserById(id);
    }
}
