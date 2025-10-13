package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemService {
    ItemDto createItem(ItemDtoPost itemDto, Long ownerId);

    ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId);

    Optional<Item> getItemById(Long itemId);

    ItemInfoDto getItemDtoById(Long itemId);

    List<ItemInfoDto> getAllItemsByOwner(Long ownerId);

    List<ItemDto> searchItems(String text);

    CommentDto addComment(Long itemId, CommentDtoPost commentDtoPost, Long authorId);

    List<CommentDto> getCommentsByItem(Long itemId);

    List<CommentDto> getCommentsByOwner(Long ownerId);
}

