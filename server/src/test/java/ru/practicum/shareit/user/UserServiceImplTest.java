package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationExceptionDuplicate;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Alice", "alice@mail.com");
        // пока не отличается от User, задел на будущее
        userDto = new UserDto(1L, "Alice", "alice@mail.com");
    }
    // -------------------------------------------
    // createUser()
    // -------------------------------------------

    @Test
    void createUserWhenEmailAlreadyExistsThenThrowValidationExceptionDuplicate() {
        // Подготовка
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        // Вызов + Проверка
        ValidationExceptionDuplicate exception = assertThrows(
                ValidationExceptionDuplicate.class,
                () -> userService.createUser(user)
        );

        assertEquals("Email уже используется другим пользователем", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWhenNameIsBlankThenNameReplacedWithEmail() {
        // Подготовка
        user.setName("");
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(userCaptor.capture())).thenReturn(user);
        when(userMapper.toUserDto(any(User.class))).thenReturn(userDto);

        // Вызов
        UserDto result = userService.createUser(user);

        // Проверки
        User savedUser = userCaptor.getValue(); // получаем переданный объект для проверок
        assertEquals(user.getEmail(), savedUser.getName());

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
        verify(userMapper).toUserDto(user);
    }

    @Test
    void createUserWhenValidThenSaveAndReturnDto() {
        // Подготовка
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(userDto);

        // Вызов
        UserDto result = userService.createUser(user);

        // Проверки
        assertThat(result).isEqualTo(userDto);
        verify(userRepository).save(user);
        verify(userMapper).toUserDto(user);
    }

    // -------------------------------------------
    // updateUser()
    // -------------------------------------------

    @Test
    void updateUserWhenUserNotFoundThenThrowNotFoundException() {
        // Подготовка
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Вызов + Проверка
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.updateUser(99L, userDto)
        );

        assertEquals("Пользователь с id=99 не существует", exception.getMessage());
        verify(userRepository, never()).save(any());

    }

    @Test
    void updateUserWhenEmailAlreadyUsedByAnotherUserThenThrowValidationExceptionDuplicate() {
        // Подготовка
        long userId = 1L;
        User existingUser = new User(userId, "OldName", "old@example.com");
        UserDto updateDto = new UserDto(userId, "NewName", "duplicate@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("duplicate@example.com", userId)).thenReturn(true);

        // Вызов + Проверка типа и текста исключения
        ValidationExceptionDuplicate exception = assertThrows(
                ValidationExceptionDuplicate.class,
                () -> userService.updateUser(userId, updateDto)
        );

        assertEquals("Email уже используется другим пользователем", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserWhenValidThenUserUpdatedAndReturned() {
        // Подготовка
        UserDto updatedDto = new UserDto(1L, "Bob", "bob@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user)); // "Alice"

        // Мокаем поведение void-маппера (он обновляет entity user с помощью userDto)
        doAnswer(invocation -> {
            UserDto dto = invocation.getArgument(0);
            User entity = invocation.getArgument(1);
            entity.setName(dto.getName());
            entity.setEmail(dto.getEmail());
            return null;
        }).when(userMapper).updateUserFromDto(any(UserDto.class), any(User.class));

        // Не подменяем результат save() — просто возвращаем тот же объект
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Упрощённый мок — не проверяем маппер, просто возвращаем DTO
        // логику работы маппера проеряем в методе маппера
        when(userMapper.toUserDto(any(User.class)))
                .thenReturn(updatedDto);

        // Вызываем метод
        UserDto result = userService.updateUser(1L, updatedDto);

        // Проверяем, что в save() попал уже обновлённый user
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Bob");
        assertThat(savedUser.getEmail()).isEqualTo("bob@mail.com");

        // Проверять результат нет смысла, т.к. на последнем этапе подставляется updatedDto
    }


    // -------------------------------------------
    // getUserById()
    // -------------------------------------------

    @Test
    void getUserByIdWhenUserExistsThenReturnOptionalWithUser() {
        // Подготовка
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Вызов
        Optional<User> result = userService.getUserById(1L);

        // Проверки
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
    }

    @Test
    void getUserByIdWhenUserNotExistsThenReturnEmptyOptional() {
        // Подготовка
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Вызов
        Optional<User> result = userService.getUserById(1L);

        // Проверки
        assertThat(result).isEmpty();
    }

    // -------------------------------------------
    // getUserDtoById()
    // -------------------------------------------

    @Test
    void getUserDtoByIdWhenUserExistsThenReturnDto() {
        // Подготовка
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(user)).thenReturn(userDto);

        // Вызов
        UserDto result = userService.getUserDtoById(1L);

        // Проверки
        assertThat(result).isEqualTo(userDto);
    }

    @Test
    void getUserDtoByIdWhenUserNotExistsThenThrowNotFoundException() {
        // Подготовка
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Вызов + Проверка
        assertThrows(NotFoundException.class, () -> userService.getUserDtoById(1L));
    }

    // -------------------------------------------
    // getAllUsers()
    // -------------------------------------------

    @Test
    void getAllUsersWhenUsersExistThenReturnListOfDtos() {
        // Подготовка
        User user2 = new User(2L, "Bob", "bob@mail.com");
        UserDto userDto2 = new UserDto(2L, "Bob", "bob@mail.com");
        when(userRepository.findAll()).thenReturn(List.of(user, user2));
        when(userMapper.toUserDto(user)).thenReturn(userDto);
        when(userMapper.toUserDto(user2)).thenReturn(userDto2);

        // Вызов
        List<UserDto> result = userService.getAllUsers();

        // Проверки
        assertThat(result).containsExactlyInAnyOrder(userDto, userDto2);
        verify(userRepository).findAll();
        verify(userMapper, times(2)).toUserDto(any(User.class));
    }

    @Test
    void getAllUsersWhenEmptyThenReturnEmptyList() {
        // Подготовка
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Вызов
        List<UserDto> result = userService.getAllUsers();

        // Проверки
        assertThat(result).isEmpty();
        // Проверяем, что findAll() действительно вызывался один раз
        verify(userRepository, times(1)).findAll();
        // Проверяем, что mapper вообще не вызывался, потому что список пустой
        verifyNoInteractions(userMapper);
    }

    // -------------------------------------------
    // deleteUser()
    // -------------------------------------------

    @Test
    void deleteUserWhenCalledThenRepositoryDeleteByIdInvoked() {
        // Вызов
        userService.deleteUser(1L);

        // Проверки
        verify(userRepository).deleteById(1L);
    }

    // -------------------------------------------
    // existsById()
    // -------------------------------------------

    @Test
    void existsByIdWhenUserExistsThenReturnTrue() {
        // Подготовка
        when(userRepository.existsById(1L)).thenReturn(true);

        // Вызов
        boolean result = userService.existsById(1L);

        // Проверки
        assertThat(result).isTrue();
    }

    @Test
    void existsByIdWhenUserNotExistsThenReturnFalse() {
        // Подготовка
        when(userRepository.existsById(1L)).thenReturn(false);

        // Вызов
        boolean result = userService.existsById(1L);

        // Проверки
        assertThat(result).isFalse();
        verify(userRepository, times(1)).existsById(1L);
    }
}