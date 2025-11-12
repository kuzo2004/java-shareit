package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> createItem(@Valid @RequestBody ItemDtoPost itemDtoPost,
                                             @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Post /items{} owner: {}", itemDtoPost, ownerId);
        return itemClient.createItem(itemDtoPost, ownerId);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> updateItem(@PathVariable Long itemId,
                                             @RequestBody ItemDto itemDto,
                                             @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Patch /items/{} - {} owner: {}", itemId, itemDto, ownerId);
        return itemClient.updateItem(itemId, itemDto, ownerId);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getItem(@PathVariable Long itemId) {
        log.info("Get /items/{}", itemId);
        return itemClient.getItemById(itemId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllItemsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Get /items/ - getAllItemsByOwner -> {}", ownerId);
        return itemClient.getItemsByOwner(ownerId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchItems(@RequestParam String text) {
        log.info("Get /items/search - text: {}", text);
        return itemClient.searchItems(text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(@PathVariable Long itemId,
                                             @Valid @RequestBody CommentDtoPost commentDtoPost,
                                             @RequestHeader("X-Sharer-User-Id") Long authorId) {
        log.info("Post /items/{}/comment by user {}", itemId, authorId);
        return itemClient.addComment(itemId, commentDtoPost, authorId);
    }

    @GetMapping("/{itemId}/comment")
    public ResponseEntity<Object> getCommentsByItem(@PathVariable Long itemId) {
        log.info("Get /items/{}/comment", itemId);
        return itemClient.getCommentsByItem(itemId);
    }

    @GetMapping("/comment")
    public ResponseEntity<Object> getCommentsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Get /items/comment for owner {}", ownerId);
        return itemClient.getCommentsByOwner(ownerId);
    }
}
