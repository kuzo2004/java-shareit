package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testSerialize() throws Exception {
        LocalDateTime start = LocalDateTime.of(2025, 1, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 11, 10, 0);

        ItemDto item = new ItemDto(1L, "Drill", "Cordless drill",
                true, null, null);
        UserDto booker = new UserDto(2L, "Alice", "alice@mail.com");

        BookingDto booking = new BookingDto(1L, start, end, item, booker, BookingStatus.APPROVED);

        var result = json.write(booking);

        // Проверяем сериализацию в JSON
        assertThat(result).hasJsonPathValue("$.id");
        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");
        assertThat(result)
                .extractingJsonPathStringValue("$.booker.email").isEqualTo("alice@mail.com");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("APPROVED");

        // Проверяем формат даты — Jackson по умолчанию сериализует LocalDateTime как ISO-8601
        assertThat(result).extractingJsonPathStringValue("$.start").startsWith("2025-01-10T10:00");
    }

    @Test
    void testDeserialize() throws Exception {
        String jsonContent = "{"
                + "\"id\": 1,"
                + "\"start\": \"2025-01-10T10:00:00\","
                + "\"end\": \"2025-01-11T10:00:00\","
                + "\"item\": {\"id\": 1, \"name\": \"Drill\"},"
                + "\"booker\": {\"id\": 2, \"name\": \"Alice\"},"
                + "\"status\": \"APPROVED\""
                + "}";

        BookingDto dto = json.parseObject(jsonContent);

        // Проверяем что JSON корректно десериализовался в объект
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getItem().getName()).isEqualTo("Drill");
        assertThat(dto.getBooker().getName()).isEqualTo("Alice");
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2025, 1, 10, 10, 0));
    }
}
