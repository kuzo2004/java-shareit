package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoForOwner;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final Map<Long, Item> items = new HashMap<>();
    private final UserService userService;

    @Override
    public ItemDto createItem(ItemDtoPost itemDtoPost, Long ownerId) {
        // Проверяем, что пользователь существует
        User existingUser = userService.getUserById(ownerId)
                                       .orElseThrow(() ->
                                               new NotFoundException("Пользователь с id=" + ownerId + "не существует"));

        Item item = ItemMapper.toItemFromPost(itemDtoPost);
        item.setId(getNextId());
        item.setOwner(existingUser);
        items.put(item.getId(), item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId) {

        // Проверяем, что товар существует
        Item existingItem = getItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Товар с id=" + itemId + "не существует"));


        // Проверяем, что владелец существует
        userService.getUserById(ownerId)
                   .orElseThrow(() -> new NotFoundException("Пользователь с id=" + ownerId + "не существует"));

        // Проверяем, что владелец совпадает
        if (!Objects.equals(existingItem.getOwner().getId(), ownerId)) {
            throw new RuntimeException("Только владелец может редактировать параметры вещи");
        }

        // Обновляем только разрешенные поля
        if (itemDto.getName() != null) {
            existingItem.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            existingItem.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public Optional<Item> getItemById(Long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public ItemDto getItemDtoById(Long itemId) {
        Item existingItem = getItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Товар с id=" + itemId + "не существует"));
        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public List<ItemDtoForOwner> getAllItemsByOwner(Long ownerId) {
        return items.values().stream()
                    .filter(item -> item.getOwner().getId().equals(ownerId))
                    .map(ItemMapper::toItemDtoForOwner)
                    .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase();
        return items.values().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                    .filter(item -> item.getName().toLowerCase().contains(searchText) ||
                            item.getDescription().toLowerCase().contains(searchText))
                    .map(ItemMapper::toItemDto)
                    .collect(Collectors.toList());
    }

    private Long getNextId() {
        return items.keySet()
                    .stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L) + 1;
    }
}

