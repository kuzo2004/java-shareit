package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;

@Service
public class ItemRequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    @Autowired
    public ItemRequestClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    // Создание нового запроса
    public ResponseEntity<Object> createRequest(ItemRequestDtoPost itemRequestDtoPost, Long requesterId) {
        return post("", requesterId, itemRequestDtoPost);
    }

    // Получение всех запросов текущего пользователя
    public ResponseEntity<Object> getRequestsByUser(Long requesterId) {
        return get("", requesterId);
    }

    // Получение всех запросов (включая чужие)
    public ResponseEntity<Object> getAllRequests(Long requesterId) {
        return get("/all", requesterId);
    }

    // Получение конкретного запроса по ID
    public ResponseEntity<Object> getRequestById(Long requestId, Long userId) {
        return get("/" + requestId, userId);
    }
}