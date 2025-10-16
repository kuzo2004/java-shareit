package ru.practicum.shareit.user;

/**
 * import ru.practicum.shareit.user.dto.UserDto;
 * import ru.practicum.shareit.user.model.User;
 * <p>
 * public class UserMapper {
 * public static UserDto toUserDto(User user) {
 * if (user == null) {
 * throw new IllegalArgumentException("Пользователь не может быть пустым");
 * }
 * return new UserDto(
 * user.getId(),
 * user.getName(),
 * user.getEmail()
 * );
 * }
 * <p>
 * public static User toUser(UserDto userDto) {
 * if (userDto == null) {
 * throw new IllegalArgumentException("Пользователь не может быть пустым");
 * }
 * User user = new User();
 * user.setId(userDto.getId());
 * user.setName(userDto.getName());
 * user.setEmail(userDto.getEmail());
 * return user;
 * }
 * }
 */

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserDto toUserDto(User user);

    User toUser(UserDto userDto);

    @Mapping(target = "id", ignore = true)
    void updateUserFromDto(UserDto userDto, @MappingTarget User user);
}

