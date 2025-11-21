package ru.practicum.shareit.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase // Использует тестовую H2 базу
@Transactional
class UserServiceImplTestIT {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(null, "Test User", "test@mail.ru");
    }

    @AfterEach
    void deleteAll() {
        userRepository.deleteAll();
    }

    @Test
    void createUserShouldSaveUser() {
        UserDto created = userService.createUser(user);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test User");
        assertThat(created.getEmail()).isEqualTo("test@mail.ru");

        // Проверяем в БД
        assertThat(userRepository.findById(created.getId())).isPresent();
    }

    @Test
    void updateUserShouldUpdateFields() {
        User saved = userRepository.save(user);
        UserDto update = new UserDto(saved.getId(), "Updated", "new@mail.ru");

        UserDto updated = userService.updateUser(saved.getId(), update);

        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getEmail()).isEqualTo("new@mail.ru");
    }

    @Test
    void getUserByIdShouldReturnOptional() {
        User saved = userRepository.save(user);

        Optional<User> found = userService.getUserById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@mail.ru");
    }

    @Test
    void getUserDtoByIdShouldReturnDto() {
        User saved = userRepository.save(user);

        UserDto dto = userService.getUserDtoById(saved.getId());

        assertThat(dto.getEmail()).isEqualTo("test@mail.ru");
        assertThat(dto.getName()).isEqualTo("Test User");
    }

    @Test
    void getAllUsersShouldReturnList() {
        userRepository.save(new User(null, "U1", "u1@mail.ru"));
        userRepository.save(new User(null, "U2", "u2@mail.ru"));

        List<UserDto> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void deleteUserShouldRemoveUser() {
        User saved = userRepository.save(user);

        userService.deleteUser(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void existsByIdShouldReturnTrueIfExists() {
        User saved = userRepository.save(user);

        boolean exists = userService.existsById(saved.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByIdShouldReturnFalseIfNotExists() {
        boolean exists = userService.existsById(999L);

        assertThat(exists).isFalse();
    }
}