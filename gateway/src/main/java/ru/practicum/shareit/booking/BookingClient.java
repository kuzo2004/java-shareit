package ru.practicum.shareit.booking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;

import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.client.BaseClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookingClient extends BaseClient {
    private static final String API_PREFIX = "/bookings";

    @Autowired
    public BookingClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    // Создать бронирование
    // POST /bookings
    public ResponseEntity<Object> bookItem(long userId, BookingDtoPost bookingDtoPost) {
        return post("", userId, bookingDtoPost);
    }

    // Обновить статус бронирования (approve/reject)
    // PATCH /bookings/{bookingId}?approved={true/false}
    public ResponseEntity<Object> updateBookingStatus(Long bookingId, Boolean approved, Long ownerId) {
        Map<String, Object> params = new HashMap<>();
        params.put("approved", approved);
        return patch("/" + bookingId + "?approved={approved}", ownerId, params, null);
    }

    // Получить бронирование по id
    // GET /bookings/{bookingId}
    public ResponseEntity<Object> getBooking(long userId, Long bookingId) {

        return get("/" + bookingId, userId);
    }

    // Получить бронирования текущего пользователя (booker)
    // GET /bookings?state={state}
    public ResponseEntity<Object> getBookingsByBooker(Long bookerId, String state) {
        Map<String, Object> params = new HashMap<>();
        if (state != null) {
            params.put("state", state);
        }
        return get("?state={state}", bookerId, params);
    }

    // Получить бронирования для владельца (owner)
    // GET /bookings/owner?state={state}
    public ResponseEntity<Object> getBookingsByOwner(Long ownerId, String state) {
        Map<String, Object> params = new HashMap<>();
        if (state != null) {
            params.put("state", state);
        }
        return get("/owner?state={state}", ownerId, params);
    }
    }
