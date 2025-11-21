package ru.practicum.shareit.user;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDto createUser(User user);

    UserDto updateUser(Long userId, UserDto userDto);

    Optional<User> getUserById(Long userId);

    List<UserDto> getAllUsers();

    UserDto getUserDtoById(Long userId);

    void deleteUser(Long userId);

    boolean existsById(Long userId);
}

