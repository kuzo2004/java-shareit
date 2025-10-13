package ru.practicum.shareit.item;

import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

public class ItemMapper {
    public static ItemDto toItemDto(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                null,
                null
        );
    }

    public static Item toItemFromPost(ItemDtoPost itemDto) {
        if (itemDto == null) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setAvailable(itemDto.getAvailable());
        return item;
    }

    public static ItemInfoDto toItemInfoDto(
            Item item, BookingDtoShort lastBooking, BookingDtoShort nextBooking) {
        if (item == null) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }
        return new ItemInfoDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                lastBooking,
                nextBooking,
                null, // requestId добавляется в сервисе
                null // comments добавляются в сервисе
        );
    }

    // тоже ItemInfoDto, но когда явно передаются null в lastBooking и nextBooking (из-за Postman теста)
    public static ItemInfoDto toItemInfoDto(Item item) {
        return toItemInfoDto(item, null, null);
    }

    public static CommentDto toCommentDto(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getAuthor().getName(),
                comment.getCreated()
        );
    }

    public static Comment toComment(CommentDtoPost commentDtoPost, Item item, User author) {
        if (commentDtoPost == null) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        Comment comment = new Comment();
        comment.setText(commentDtoPost.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        return comment;
    }
}

