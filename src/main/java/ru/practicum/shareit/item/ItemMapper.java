package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoForOwner;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.model.Item;

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
                null
        );
    }

    public static Item toItemFromDto(ItemDto itemDto) {
        if (itemDto == null) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }
        Item item = new Item();
        item.setId(itemDto.getId());
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setAvailable(itemDto.getAvailable());
        return item;
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

    public static ItemDtoForOwner toItemDtoForOwner(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }
        return new ItemDtoForOwner(
                item.getName(),
                item.getDescription()
        );
    }
}

