package ru.practicum.shareit.request.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface ItemRequestMapper {
    // ItemRequest -> ItemRequestDto
    ItemRequestDto toItemRequestDto(ItemRequest itemRequest);

    // ItemRequestDtoPost -> ItemRequest
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", expression = "java(java.time.LocalDateTime.now())")
    ItemRequest toItemRequest(ItemRequestDtoPost itemRequestDtoPost, User requester);
}
