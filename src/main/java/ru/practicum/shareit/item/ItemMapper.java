package ru.practicum.shareit.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ItemMapper {

    // Item -> ItemDto
    ItemDto toItemDto(Item item);

    // ItemDtoPost -> Item
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    Item toItemFromPost(ItemDtoPost itemDtoPost);

    // Item -> ItemInfoDto с бронированиями
    @Mapping(target = "id", source = "item.id")
    // явно берём id из сущности
    ItemInfoDto toItemInfoDto(Item item, BookingDtoShort lastBooking, BookingDtoShort nextBooking);

    // Postman требует для
    default ItemInfoDto toItemInfoDto(Item item) {
        return toItemInfoDto(item, null, null);
    }

    // ItemDto -> Item
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    void updateItemFromDto(ItemDto itemDto, @MappingTarget Item item);

    // Comment -> CommentDto (с заполнением authorName)
    @Mapping(target = "authorName", source = "author.name")
    CommentDto toCommentDto(Comment comment);

    // CommentDtoPost -> Comment
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", expression = "java(java.time.LocalDateTime.now())")
    Comment toComment(CommentDtoPost commentDtoPost, Item item, User author);
}
