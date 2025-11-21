package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoForRequest;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    private final ItemMapper mapper = Mappers.getMapper(ItemMapper.class);

    @Test
    void toItemDtoShouldMapAllFields() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Drill", "Powerful drill",
                true, owner, null);

        ItemDto dto = mapper.toItemDto(item);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Drill");
        assertThat(dto.getDescription()).isEqualTo("Powerful drill");
        assertThat(dto.getAvailable()).isTrue();
    }

    @Test
    void toItemFromPostShouldMapFieldsAndIgnoreIdOwnerRequest() {
        ItemDtoPost dtoPost = new ItemDtoPost("Drill", "Powerful drill",
                true, null);
        Item item = mapper.toItemFromPost(dtoPost);

        assertThat(item).isNotNull();
        assertThat(item.getId()).isNull();
        assertThat(item.getName()).isEqualTo("Drill");
        assertThat(item.getDescription()).isEqualTo("Powerful drill");
        assertThat(item.getAvailable()).isTrue();
        assertThat(item.getOwner()).isNull();
        assertThat(item.getRequest()).isNull();
    }

    @Test
    void toItemInfoDtoShouldMapAllFields() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Drill", "Powerful drill",
                true, owner, null);

        LocalDateTime start1 = LocalDateTime.now().minusDays(2);
        LocalDateTime end1 = LocalDateTime.now().minusDays(1);
        BookingDtoShort lastBooking = new BookingDtoShort();
        lastBooking.setId(100L);
        lastBooking.setStart(start1);
        lastBooking.setEnd(end1);
        lastBooking.setBookerId(2L);

        LocalDateTime start2 = LocalDateTime.now().plusDays(1);
        LocalDateTime end2 = LocalDateTime.now().plusDays(2);
        BookingDtoShort nextBooking = new BookingDtoShort();
        nextBooking.setId(101L);
        nextBooking.setStart(start2);
        nextBooking.setEnd(end2);
        nextBooking.setBookerId(3L);


        ItemInfoDto infoDto = mapper.toItemInfoDto(item, lastBooking, nextBooking);

        assertThat(infoDto).isNotNull();
        assertThat(infoDto.getId()).isEqualTo(10L);
        assertThat(infoDto.getName()).isEqualTo("Drill");
        assertThat(infoDto.getDescription()).isEqualTo("Powerful drill");
        assertThat(infoDto.getAvailable()).isTrue();
        assertThat(infoDto.getLastBooking()).isEqualTo(lastBooking);
        assertThat(infoDto.getNextBooking()).isEqualTo(nextBooking);
    }

    @Test
    void defaultToItemInfoDtoShouldCallMainMethod() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Drill", "Powerful drill",
                true, owner, null);

        // Перегруженный default-метод принимает только Item, а остальные аргументы подставляет null
        ItemInfoDto infoDto = mapper.toItemInfoDto(item);

        assertThat(infoDto).isNotNull();
        assertThat(infoDto.getId()).isEqualTo(10L);
        assertThat(infoDto.getDescription()).isEqualTo("Powerful drill");
        assertThat(infoDto.getAvailable()).isTrue();
        assertThat(infoDto.getLastBooking()).isNull();
        assertThat(infoDto.getNextBooking()).isNull();
    }

    @Test
    void updateItemFromDtoShouldIgnoreIdAndOwner() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "OldName", "OldDesc",
                true, owner, null);

        ItemDto dto = new ItemDto(99L, "NewName", "NewDesc",
                false, null, null);

        mapper.updateItemFromDto(dto, item);

        assertThat(item.getId()).isEqualTo(10L); // id не изменился
        assertThat(item.getOwner()).isEqualTo(owner); // owner не изменился
        assertThat(item.getName()).isEqualTo("NewName");
        assertThat(item.getDescription()).isEqualTo("NewDesc");
        assertThat(item.getAvailable()).isFalse();
    }

    @Test
    void toCommentDtoShouldMapFieldsAndAuthorName() {
        User author = new User(1L, "Alice", "alice@mail.com");
        User owner = new User(2L, "Bob", "bob@mail.com");
        Item item = new Item(10L, "Drill", "Desc", true, owner, null);
        LocalDateTime created = LocalDateTime.now().minusHours(1);
        Comment comment = new Comment(10L, "Good", item, author, created);

        CommentDto dto = mapper.toCommentDto(comment);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getText()).isEqualTo("Good");
        assertThat(dto.getAuthorName()).isEqualTo("Alice");
        assertThat(dto.getCreated()).isEqualTo(created);
    }

    @Test
    void toCommentShouldMapFieldsAndSetCreated() {
        User author = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Drill", "Desc", true, author, null);
        CommentDtoPost dtoPost = new CommentDtoPost("Nice");

        LocalDateTime before = LocalDateTime.now();
        Comment comment = mapper.toComment(dtoPost, item, author);
        LocalDateTime after = LocalDateTime.now();

        assertThat(comment).isNotNull();
        assertThat(comment.getId()).isNull();
        assertThat(comment.getText()).isEqualTo("Nice");
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getItem()).isEqualTo(item);

        // created находится «в разумных пределах»
        assertThat(comment.getCreated()).isNotNull();
        assertThat(comment.getCreated()).isAfterOrEqualTo(before);
        assertThat(comment.getCreated()).isBeforeOrEqualTo(after);
    }

    @Test
    void toItemDtoForRequestShouldMapFields() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Drill", "Desc", true, owner, null);

        ItemDtoForRequest dto = mapper.toItemDtoForRequest(item);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Drill");
        assertThat(dto.getOwnerId()).isEqualTo(1L);
    }

    // ---------------------------------------------------------
    // NULL аргументы
    // ---------------------------------------------------------

    @Test
    void toItemDtoShouldReturnNullWhenInputIsNull() {
        assertThat(mapper.toItemDto(null)).isNull();
    }

    @Test
    void toItemFromPostShouldReturnNullWhenInputIsNull() {
        assertThat(mapper.toItemFromPost(null)).isNull();
    }

    @Test
    void toItemInfoDtoShouldReturnNullWhenAllInputsNull() {
        assertThat(mapper.toItemInfoDto(
                null, null, null)).isNull();
    }

    @Test
    void updateItemFromDtoShouldDoNothingWhenDtoIsNull() {
        User owner = new User(1L, "Alice", "alice@mail.com");
        Item item = new Item(10L, "Name", "Desc", true, owner, null);

        mapper.updateItemFromDto(null, item);

        // Проверяем, что объект остался без изменений
        assertThat(item.getId()).isEqualTo(10L);
        assertThat(item.getName()).isEqualTo("Name");
        assertThat(item.getDescription()).isEqualTo("Desc");
        assertThat(item.getAvailable()).isTrue();
        assertThat(item.getOwner()).isEqualTo(owner);
    }

    @Test
    void toCommentDtoShouldReturnNullWhenInputIsNull() {
        assertThat(mapper.toCommentDto(null)).isNull();
    }

    @Test
    void toCommentShouldReturnNullWhenAllInputsNull() {
        assertThat(mapper.toComment(
                null, null, null)).isNull();
    }

    @Test
    void toItemDtoForRequestShouldReturnNullWhenInputIsNull() {
        assertThat(mapper.toItemDtoForRequest(null)).isNull();
    }
}