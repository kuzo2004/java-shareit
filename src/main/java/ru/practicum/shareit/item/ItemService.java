package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoForOwner;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemService {
    ItemDto createItem(ItemDtoPost itemDto, Long ownerId);

    ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId);

    Optional<Item> getItemById(Long itemId);

    List<ItemDtoForOwner> getAllItemsByOwner(Long ownerId);

    List<ItemDto> searchItems(String text);

    ItemDto getItemDtoById(Long itemId);
}

