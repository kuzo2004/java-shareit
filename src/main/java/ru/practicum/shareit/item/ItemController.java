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
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;

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
        log.info("Post /items{} owner: {}", itemDtoPost, ownerId);
        return itemService.createItem(itemDtoPost, ownerId);
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@PathVariable Long itemId,
                              @RequestBody ItemDto itemDto,
                              @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Patch /items/{} - {} owner: {}", itemId, itemDto, ownerId);
        return itemService.updateItem(itemId, itemDto, ownerId);
    }

    @GetMapping("/{itemId}")
    public ItemInfoDto getItem(@PathVariable Long itemId) {
        log.info("Get /items/{}", itemId);
        return itemService.getItemDtoById(itemId);
    }

    @GetMapping
    public List<ItemInfoDto> getAllItemsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Get /items/ - getAllItemsByOwner -> {}", ownerId);
        return itemService.getAllItemsByOwner(ownerId);
    }

    @GetMapping("/search")
    public Object searchItems(@RequestParam String text) {
        log.info("Get /items/search - text: {}", text);
        return itemService.searchItems(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@PathVariable Long itemId,
                                 @Valid @RequestBody CommentDtoPost commentDtoPost,
                                 @RequestHeader("X-Sharer-User-Id") Long authorId) {
        log.info("Post /items/{}/comment by user {}", itemId, authorId);
        return itemService.addComment(itemId, commentDtoPost, authorId);
    }

    @GetMapping("/{itemId}/comment")
    public List<CommentDto> getCommentsByItem(@PathVariable Long itemId) {
        log.info("Get /items/{}/comment", itemId);
        return itemService.getCommentsByItem(itemId);
    }

    @GetMapping("/comment")
    public List<CommentDto> getCommentsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Get /items/comment for owner {}", ownerId);
        return itemService.getCommentsByOwner(ownerId);
    }
}
