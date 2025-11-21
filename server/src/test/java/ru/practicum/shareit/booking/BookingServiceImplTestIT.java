package ru.practicum.shareit.booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class BookingServiceImplTestIT {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User(null, "Owner", "owner@mail.ru");
        owner = userRepository.save(owner);

        booker = new User(null, "Booker", "booker@mail.ru");
        booker = userRepository.save(booker);

        item = new Item(null, "Drill", "Cordless drill",
                true, owner, null);
        item = itemRepository.save(item);
    }

    @AfterEach
    void clear() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------------- CREATE BOOKING ----------------------
    @Test
    void createBookingShouldSaveBooking() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        BookingDto created = bookingService.createBooking(bookingDtoPost, booker.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getItem().getId()).isEqualTo(item.getId());
        assertThat(created.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void createBookingShouldThrowValidationExceptionIfBookerIsOwner() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(bookingDtoPost, owner.getId())
        );
    }

    @Test
    void createBookingShouldThrowValidationExceptionIfEndBeforeStart() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1)
        );

        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingDtoPost, booker.getId())
        );
    }

    // ---------------------- UPDATE BOOKING STATUS ----------------------
    @Test
    void updateBookingStatusShouldApproveBooking() {
        // создаём бронирование
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // владелец подтверждает бронирование
        BookingDto updated = bookingService
                .updateBookingStatus(booking.getId(), true, owner.getId());

        assertThat(updated.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void updateBookingStatusShouldRejectBooking() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // владелец отклоняет бронирование
        BookingDto updated = bookingService
                .updateBookingStatus(booking.getId(), false, owner.getId());

        assertThat(updated.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void updateBookingStatusShouldThrowAccessDeniedIfNotOwner() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // другой пользователь пытается подтвердить бронирование
        User otherUser = userRepository.save(new User(null, "Other", "other@mail.ru"));

        assertThrows(AccessDeniedException.class,
                () -> bookingService.updateBookingStatus(booking.getId(), true, otherUser.getId()));
    }

    @Test
    void updateBookingStatusShouldThrowValidationExceptionIfAlreadyChanged() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // владелец подтверждает бронирование
        bookingService.updateBookingStatus(booking.getId(), true, owner.getId());

        // повторная попытка изменения статуса
        assertThrows(ValidationException.class,
                () -> bookingService.updateBookingStatus(booking.getId(), false, owner.getId()));
    }

    // ---------------------- GET BOOKING BY ID ----------------------
    @Test
    void getBookingByIdShouldReturnBookingForBooker() {
        // создаём бронирование
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // booker получает бронирование
        BookingDto found = bookingService.getBookingById(booking.getId(), booker.getId());

        assertThat(found.getId()).isEqualTo(booking.getId());
        assertThat(found.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void getBookingByIdShouldReturnBookingForOwner() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // владелец получает бронирование
        BookingDto found = bookingService.getBookingById(booking.getId(), owner.getId());

        assertThat(found.getId()).isEqualTo(booking.getId());
        assertThat(found.getItem().getId()).isEqualTo(item.getId());
    }

    @Test
    void getBookingByIdShouldThrowAccessDeniedIfNotBookerOrOwner() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // другой пользователь пытается получить бронирование
        User otherUser = userRepository.save(new User(null, "Other", "other@mail.ru"));

        assertThrows(AccessDeniedException.class,
                () -> bookingService.getBookingById(booking.getId(), otherUser.getId()));
    }

    @Test
    void getBookingByIdShouldThrowNotFoundIfBookingDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> bookingService.getBookingById(999L, booker.getId()));
    }

    @Test
    void getBookingByIdShouldThrowNotFoundIfUserDoesNotExist() {
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        assertThrows(NotFoundException.class,
                () -> bookingService.getBookingById(booking.getId(), 999L));
    }

    // ---------------------- GET BOOKINGS BY BOOKER ----------------------
    @Test
    void getBookingsByBookerShouldReturnBookingsList() {
        // создаём бронирование
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // получаем все бронирования booker
        List<BookingDto> bookings = bookingService.getBookingsByBooker(booker.getId(), "ALL");

        assertThat(bookings).isNotEmpty();
        assertThat(bookings.get(0).getId()).isEqualTo(booking.getId());
        assertThat(bookings.get(0).getBooker().getId()).isEqualTo(booker.getId());
    }

    // ---------------------- GET BOOKINGS BY OWNER ----------------------
    @Test
    void getBookingsByOwnerShouldReturnBookingsList() {
        // создаём бронирование
        BookingDtoPost bookingDtoPost = new BookingDtoPost(
                item.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        BookingDto booking = bookingService.createBooking(bookingDtoPost, booker.getId());

        // владелец получает свои бронирования
        List<BookingDto> bookings = bookingService.getBookingsByOwner(owner.getId(), "ALL");

        assertThat(bookings).isNotEmpty();
        assertThat(bookings.get(0).getId()).isEqualTo(booking.getId());
        assertThat(bookings.get(0).getItem().getId()).isEqualTo(item.getId());
    }
}
