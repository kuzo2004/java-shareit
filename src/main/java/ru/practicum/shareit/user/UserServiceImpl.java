package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationExceptionDuplicate;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(User user) {
        // Проверка уникальности email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ValidationExceptionDuplicate("Email уже используется другим пользователем");
        }

        if (user.getName().isBlank()) {
            user.setName(user.getEmail());
        }

        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = userRepository.findById(userId)
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
            if (userRepository.existsByEmailAndIdNot(newEmail, userId)) {
                throw new ValidationExceptionDuplicate("Email уже используется другим пользователем");
            }
            existingUser.setEmail(newEmail);

            // Если имя отсутствует или пустое — подставляем email как имя
            if (newName == null || newName.isBlank()) {
                existingUser.setName(newEmail);
            }
        }
        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public Optional<User> getUserById(Long userId) {

        return userRepository.findById(userId);
    }

    @Override
    public UserDto getUserDtoById(Long userId) {
        User existingUser = userRepository.findById(userId)
                                          .orElseThrow(() ->
                                                  new NotFoundException(
                                                          "Пользователь с id=" + userId + "не существует"));
        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                             .map(UserMapper::toUserDto)
                             .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {

        userRepository.deleteById(userId);
    }
}
