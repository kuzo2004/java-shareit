package ru.practicum.shareit.item;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoForOwner;
import ru.practicum.shareit.item.dto.ItemDtoPatch;
import ru.practicum.shareit.item.dto.ItemDtoPost;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ItemDto createItem(@Valid @RequestBody ItemDtoPost itemDtoPost,
                              @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.debug("Post /items" + itemDtoPost + " owner: " + ownerId);
        return itemService.createItem(itemDtoPost, ownerId);
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@PathVariable Long itemId,
                              @RequestBody ItemDtoPatch itemDtoPatch,
                              @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.debug("Patch /items/" + itemId + " - " + itemDtoPatch + " owner: " + ownerId);
        return itemService.updateItem(itemId, itemDtoPatch, ownerId);
    }

    @GetMapping("/{itemId}")
    public ItemDto getItem(@PathVariable Long itemId) {
        log.debug("Get /items/" + itemId);
        return itemService.getItemDtoById(itemId);
    }

    @GetMapping
    public List<ItemDtoForOwner> getAllItemsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.debug("Get /items/  - getAllItemsByOwner ->" + ownerId);
        return itemService.getAllItemsByOwner(ownerId);
    }

    @GetMapping("/search")
    public List<ItemDto> searchItems(@RequestParam String text) {
        log.debug("Get /items/  - searchItems  - text ->  " + text);
        return itemService.searchItems(text);
    }
}
