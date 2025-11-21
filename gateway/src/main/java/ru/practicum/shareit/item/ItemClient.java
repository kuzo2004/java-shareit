package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;

import java.util.Map;

@Service
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    @Autowired
    public ItemClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    // Создание нового предмета
    public ResponseEntity<Object> createItem(ItemDtoPost itemDtoPost, Long ownerId) {
        return post("", ownerId, itemDtoPost);
    }

    // Обновление предмета
    public ResponseEntity<Object> updateItem(Long itemId, ItemDto itemDto, Long ownerId) {
        return patch("/" + itemId, ownerId, itemDto);
    }

    // Получение предмета по ID
    public ResponseEntity<Object> getItemById(Long itemId) {
        return get("/" + itemId);
    }

    // Получение всех предметов владельца
    public ResponseEntity<Object> getItemsByOwner(Long ownerId) {
        return get("", ownerId);
    }

    // Поиск предметов
    public ResponseEntity<Object> searchItems(String text) {
        Map<String, Object> params = Map.of("text", text);
        return get("/search?text={text}", null, params);
    }

    // Добавление комментария к предмету
    public ResponseEntity<Object> addComment(Long itemId, CommentDtoPost commentDtoPost, Long authorId) {
        return post("/" + itemId + "/comment", authorId, commentDtoPost);
    }

    // Получение всех комментариев по предмету
    public ResponseEntity<Object> getCommentsByItem(Long itemId) {
        return get("/" + itemId + "/comment");
    }

    // Получение всех комментариев владельца
    public ResponseEntity<Object> getCommentsByOwner(Long ownerId) {
        return get("/comment", ownerId);
    }
}
