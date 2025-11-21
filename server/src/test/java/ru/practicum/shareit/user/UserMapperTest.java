package ru.practicum.shareit.user;


import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UserMapperTest {
    // ищет сгенерированный класс UserMapperImpl и возвращает экземпляр этого класса
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void updateUserFromDtoShouldIgnoreNullFields() {
        UserDto dto = new UserDto(null, null, "new@mail.com");
        User user = new User(1L, "Alice", "old@mail.com");

        mapper.updateUserFromDto(dto, user);

        assertThat(user.getId()).isEqualTo(1L); // id не меняется
        assertThat(user.getName()).isEqualTo("Alice"); // name не перезаписан null'ом
        assertThat(user.getEmail()).isEqualTo("new@mail.com"); // email обновлён
    }

    @Test
    void updateUserFromDtoShouldOverwriteNonNullFields() {
        // DTO с новыми значениями
        UserDto dto = new UserDto(99L, "Bob", "bob@mail.com");
        User user = new User(1L, "Alice", "alice@mail.com");

        mapper.updateUserFromDto(dto, user);

        // Проверки
        assertThat(user.getId()).isEqualTo(1L);      // id не должен меняться, тк маппер игнорирует его
        assertThat(user.getName()).isEqualTo("Bob"); // name обновился
        assertThat(user.getEmail()).isEqualTo("bob@mail.com"); // email обновился
    }

    @Test
    void toUserDtoShouldMapAllFields() {
        User user = new User(1L, "Alice", "alice@mail.com");

        UserDto dto = mapper.toUserDto(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getEmail()).isEqualTo("alice@mail.com");
    }

    @Test
    void toUserShouldMapAllFields() {
        UserDto dto = new UserDto(1L, "Alice", "alice@mail.com");

        User user = mapper.toUser(dto);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@mail.com");
    }
}
