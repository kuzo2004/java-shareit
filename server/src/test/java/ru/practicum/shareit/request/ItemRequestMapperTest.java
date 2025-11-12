package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.ItemRequestMapper;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRequestMapperTest {

    private ItemRequestMapper mapper;

    @BeforeEach
    void setUp() {
        // получаем реализацию MapStruct
        this.mapper = Mappers.getMapper(ItemRequestMapper.class);
    }

    @Test
    void toItemRequestDtoShouldMapAllFields() {
        User requester = new User(1L, "Alice", "alice@mail.com");
        LocalDateTime now = LocalDateTime.now();
        ItemRequest request = new ItemRequest(10L, "Need a drill", requester, now);

        ItemRequestDto dto = mapper.toItemRequestDto(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getDescription()).isEqualTo("Need a drill");
        assertThat(dto.getCreated()).isEqualTo(now);
        assertThat(dto.getItems()).isNull(); // по умолчанию items не маппится
    }

    @Test
    void toItemRequestShouldMapFieldsAndSetCreatedAndIgnoreId() {
        User requester = new User(1L, "Alice", "alice@mail.com");
        ItemRequestDtoPost dtoPost = new ItemRequestDtoPost("Need a drill");

        LocalDateTime before = LocalDateTime.now();
        ItemRequest request = mapper.toItemRequest(dtoPost, requester);
        LocalDateTime after = LocalDateTime.now();

        assertThat(request).isNotNull();
        assertThat(request.getId()).isNull(); // id игнорируется
        assertThat(request.getDescription()).isEqualTo("Need a drill");
        assertThat(request.getRequester()).isEqualTo(requester);

        // проверка, что created установлен в "разумный" диапазон
        assertThat(request.getCreated()).isNotNull();
        assertThat(request.getCreated()).isAfterOrEqualTo(before);
        assertThat(request.getCreated()).isBeforeOrEqualTo(after);
    }
}