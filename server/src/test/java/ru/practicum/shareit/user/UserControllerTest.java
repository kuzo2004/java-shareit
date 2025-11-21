package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------- CREATE USER ----------------------
    @Test
    @SneakyThrows
    void createUserShouldReturnCreatedUser() {
        User user = new User(null, "Alice", "alice@mail.com");
        UserDto userDto = new UserDto(1L, "Alice", "alice@mail.com");

        Mockito.when(userService.createUser(any(User.class))).thenReturn(userDto);

        mockMvc.perform(post("/users")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(user)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(userDto.getId()))
               .andExpect(jsonPath("$.name").value(userDto.getName()))
               .andExpect(jsonPath("$.email").value(userDto.getEmail()));

        Mockito.verify(userService).createUser(any(User.class));
    }

    @Test
    @SneakyThrows
    void createUserShouldReturnBadRequestOnValidationException() {
        User invalidUser = new User(null, "", "invalidemail");

        Mockito.when(userService.createUser(any(User.class)))
               .thenThrow(new ValidationException("Некорректные данные"));

        mockMvc.perform(post("/users")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidUser)))
               .andExpect(status().isBadRequest());

        Mockito.verify(userService).createUser(any(User.class));
    }

    // ---------------------- UPDATE USER ----------------------
    @Test
    @SneakyThrows
    void updateUserShouldReturnUpdatedUser() {
        UserDto updateDto = new UserDto(null, "Alice Updated", "alice_updated@mail.com");
        UserDto returnedDto = new UserDto(1L, "Alice Updated", "alice_updated@mail.com");

        Mockito.when(userService.updateUser(eq(1L), any(UserDto.class))).thenReturn(returnedDto);

        mockMvc.perform(patch("/users/1")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(updateDto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(returnedDto.getId()))
               .andExpect(jsonPath("$.name").value(returnedDto.getName()))
               .andExpect(jsonPath("$.email").value(returnedDto.getEmail()));

        Mockito.verify(userService).updateUser(eq(1L), any(UserDto.class));
    }

    @Test
    @SneakyThrows
    void updateUserShouldReturnNotFound() {
        UserDto updateDto = new UserDto(null, "Alice", "alice@mail.com");

        Mockito.when(userService.updateUser(eq(99L), any(UserDto.class)))
               .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(patch("/users/99")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(updateDto)))
               .andExpect(status().isNotFound());

        Mockito.verify(userService).updateUser(eq(99L), any(UserDto.class));
    }

    // ---------------------- GET USER ----------------------
    @Test
    @SneakyThrows
    void getUserShouldReturnUser() {
        UserDto userDto = new UserDto(1L, "Alice", "alice@mail.com");
        Mockito.when(userService.getUserDtoById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(userDto.getId()))
               .andExpect(jsonPath("$.name").value(userDto.getName()))
               .andExpect(jsonPath("$.email").value(userDto.getEmail()));

        Mockito.verify(userService).getUserDtoById(1L);
    }

    @Test
    @SneakyThrows
    void getUserShouldReturnNotFound() {
        Mockito.when(userService.getUserDtoById(99L))
               .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/99"))
               .andExpect(status().isNotFound());

        Mockito.verify(userService).getUserDtoById(99L);
    }

    // ---------------------- GET ALL USERS ----------------------
    @Test
    @SneakyThrows
    void getAllUsersShouldReturnList() {
        UserDto user1 = new UserDto(1L, "Alice", "alice@mail.com");
        UserDto user2 = new UserDto(2L, "Bob", "bob@mail.com");

        Mockito.when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/users"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].name").value("Alice"))
               .andExpect(jsonPath("$[1].name").value("Bob"));

        Mockito.verify(userService).getAllUsers();
    }

    // ---------------------- DELETE USER ----------------------
    @Test
    @SneakyThrows
    void deleteUserShouldReturnNoContent() {
        Mockito.doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/users/1"))
               .andExpect(status().isOk());

        Mockito.verify(userService).deleteUser(1L);
    }
}