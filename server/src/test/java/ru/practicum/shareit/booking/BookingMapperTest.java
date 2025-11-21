package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.model.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


class BookingMapperTest {

    private BookingMapper mapper;
    private User booker;
    private Item item;


    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        // создаём "вручную" экземпляры зависимых мапперов
        ItemMapper itemMapper = Mappers.getMapper(ItemMapper.class);
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);

        // создаём экземпляр реализации MapStruct
        BookingMapperImpl impl = new BookingMapperImpl();

        // через reflection устанавливаем приватные поля itemMapper и userMapper
        Field itemMapperField = BookingMapperImpl.class.getDeclaredField("itemMapper");
        itemMapperField.setAccessible(true);
        itemMapperField.set(impl, itemMapper);

        Field userMapperField = BookingMapperImpl.class.getDeclaredField("userMapper");
        userMapperField.setAccessible(true);
        userMapperField.set(impl, userMapper);

        // теперь используем impl как mapper в тестах
        this.mapper = impl;

        // тестовые модели
        User owner = new User(1L, "Alice", "alice@mail.com");
        booker = new User(2L, "Bob", "bob@mail.com");
        item = new Item(10L, "Drill", "Powerful drill",
                true, owner, null);
    }

    @Test
    void toBookingDtoShouldMapAllFields() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        Booking booking = new Booking(100L, start, end, item, booker, BookingStatus.WAITING);

        BookingDto dto = mapper.toBookingDto(booking);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getStart()).isEqualTo(start);
        assertThat(dto.getEnd()).isEqualTo(end);
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.WAITING);

        // Проверяем, что маппинг вложенных сущностей выполнен
        assertThat(dto.getItem()).isNotNull();
        assertThat(dto.getItem().getId()).isEqualTo(10L);
        assertThat(dto.getBooker()).isNotNull();
        assertThat(dto.getBooker().getId()).isEqualTo(2L);
    }

    @Test
    void toBookingFromPostShouldMapFieldsAndSetStatusWaitingAndIgnoreId() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        BookingDtoPost post = new BookingDtoPost(item.getId(), start, end);

        Booking booking = mapper.toBookingFromPost(post, item, booker);

        assertThat(booking).isNotNull();
        assertThat(booking.getId()).isNull(); // id должен быть проигнорирован
        assertThat(booking.getStart()).isEqualTo(start);
        assertThat(booking.getEnd()).isEqualTo(end);
        assertThat(booking.getItem()).isEqualTo(item);
        assertThat(booking.getBooker()).isEqualTo(booker);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void toBookingDtoShortShouldMapCorrectly() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        Booking booking = new Booking(50L, start, end, item, booker, BookingStatus.APPROVED);

        BookingDtoShort dtoShort = mapper.toBookingDtoShort(booking);

        assertThat(dtoShort).isNotNull();
        assertThat(dtoShort.getId()).isEqualTo(50L);
        assertThat(dtoShort.getStart()).isEqualTo(start);
        assertThat(dtoShort.getEnd()).isEqualTo(end);
        assertThat(dtoShort.getBookerId()).isEqualTo(2L);
    }
}