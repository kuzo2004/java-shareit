package ru.practicum.shareit.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class BaseClient {
    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    // ----- GET ------
    protected ResponseEntity<Object> get(String path) {
        // Без userId и параметров
        return get(path, null, null);
    }

    protected ResponseEntity<Object> get(String path, long userId) {
        // С userId, но без параметров
        return get(path, userId, null);
    }

/*    protected ResponseEntity<Object> get(String path, long userId, @Nullable String state) {
        Map<String, Object> parameters = state != null ? Map.of("state", state) : null;
        return get(path, userId, parameters);
    }*/

    // Основной метод, который делает реальный HTTP-запрос
    protected ResponseEntity<Object> get(String path,
                                         Long userId,
                                         @Nullable Map<String, Object> parameters) {

        return makeAndSendRequest(HttpMethod.GET, path, userId, parameters, null);
    }

    // ----- POST ------

    protected <T> ResponseEntity<Object> post(String path, T body) {
        return post(path, null, null, body);
    }

    protected <T> ResponseEntity<Object> post(String path, long userId, T body) {
        return post(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> post(String path, Long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, userId, parameters, body);
    }

    // ----- PUT ------
    protected <T> ResponseEntity<Object> put(String path, long userId, T body) {
        return put(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> put(String path, long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PUT, path, userId, parameters, body);
    }

    // ----- PATCH ------
    protected <T> ResponseEntity<Object> patch(String path, T body) {
        return patch(path, null, null, body);
    }

    protected <T> ResponseEntity<Object> patch(String path, long userId) {
        return patch(path, userId, null, null);
    }

    protected <T> ResponseEntity<Object> patch(String path, long userId, T body) {
        return patch(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> patch(String path, Long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PATCH, path, userId, parameters, body);
    }

    // ----- DELETE ------
    protected ResponseEntity<Object> delete(String path) {
        return delete(path, null, null);
    }

    protected ResponseEntity<Object> delete(String path, long userId) {
        return delete(path, userId, null);
    }

    protected ResponseEntity<Object> delete(String path, Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.DELETE, path, userId, parameters, null);
    }


    // ----- BASE METHOD  формируется HTTP-запрос (HttpEntity),-----
    private <T> ResponseEntity<Object> makeAndSendRequest(
            HttpMethod method,
            String path,
            Long userId,
            @Nullable Map<String, Object> parameters,
            @Nullable T body) {

        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders(userId));

        ResponseEntity<Object> shareitServerResponse;

        // используется Spring RestTemplate.exchange(), который выполняет HTTP-запрос.
        try {
            if (parameters != null) {
                shareitServerResponse = rest.exchange(path, method, requestEntity, Object.class, parameters);
            } else {
                shareitServerResponse = rest.exchange(path, method, requestEntity, Object.class);
            }
        // Вместо падения, создаём ResponseEntity с тем же статусом и телом ошибки, чтобы gateway вернул это клиенту
        } catch (HttpStatusCodeException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsByteArray());
        }

        // хелпер - приводит ответ в аккуратный вид
        return prepareGatewayResponse(shareitServerResponse);
    }

    // Заголовки по умолчанию
    private HttpHeaders defaultHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);  // тело будет JSON
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // ждём JSON в ответе
        if (userId != null) {
            headers.set("X-Sharer-User-Id", String.valueOf(userId));
        }
        return headers;
    }

    /**
     * метод просто «нормализует» ответ:
     * Если ответ успешный (2xx) — возвращаем как есть.
     * Если статус ошибочный, но тело есть — тоже возвращаем тело.
     * Если тела нет — возвращаем только статус.
     */
    private static ResponseEntity<Object> prepareGatewayResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }
}
