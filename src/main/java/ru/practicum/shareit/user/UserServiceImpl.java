package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationExceptionDuplicate;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> users = new HashMap<>();

    @Override
    public UserDto createUser(User user) {
        // Проверка уникальности email
        checkEmailUniqueness(user.getEmail(), null);

        if (user.getName().isBlank()) {
            user.setName(user.getEmail());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + "не существует"));

        String newName = userDto.getName();
        String newEmail = userDto.getEmail();

        // Обновляем имя, если передано
        if (newName != null) {
            existingUser.setName(newName);
        }

        // Обновляем email, если передан и отличается от текущего
        if (newEmail != null && !newEmail.equals(existingUser.getEmail())) {
            // Проверка уникальности email
            checkEmailUniqueness(newEmail, userId);
            existingUser.setEmail(newEmail);

            // Если имя отсутствует или пустое — подставляем email как имя
            if (newName == null || newName.isBlank()) {
                existingUser.setName(newEmail);
            }
        }
        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public Optional<User> getUserById(Long userId) {

        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public UserDto getUserDtoById(Long userId) {
        User existingUser = getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + "не существует"));
        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return users.values().stream()
                    .map(UserMapper::toUserDto)
                    .collect(Collectors.toList());
    }

    private void checkEmailUniqueness(String email, Long currentUserId) {
        boolean emailExists = users.values().stream()
                                   .anyMatch(u -> u.getEmail().equals(email)
                                           && !Objects.equals(u.getId(), currentUserId));

        if (emailExists) {
            log.error("Email {} уже используется другим пользователем", email);
            throw new ValidationExceptionDuplicate("Email уже используется другим пользователем");
        }
    }

    @Override
    public void deleteUser(Long userId) {
        users.remove(userId);
    }

    private Long getNextId() {
        return users.keySet()
                    .stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L) + 1;
    }
}
