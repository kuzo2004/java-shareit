package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item1;
    private Item item2;
    private Booking pastBooking;
    private Booking currentBooking;
    private Booking futureBooking;

    @BeforeEach
    void setUp() {
        // создаем пользователей
        owner = userRepository.save(new User(null, "Owner", "owner@mail.ru"));
        booker = userRepository.save(new User(null, "Booker", "booker@mail.ru"));

        // создаем вещи
        item1 = itemRepository.save(new Item(null, "Drill", "Hand drill",
                true, owner, null));
        item2 = itemRepository.save(new Item(null, "Saw", "Hand saw",
                true, owner, null));

        LocalDateTime now = LocalDateTime.now();

        // создаем бронирования с разными статусами и датами
        pastBooking = bookingRepository.save(
                new Booking(null, now.minusDays(3), now.minusDays(1),
                        item1, booker, BookingStatus.APPROVED));
        currentBooking = bookingRepository.save(
                new Booking(null, now.minusHours(1), now.plusHours(2),
                        item1, booker, BookingStatus.APPROVED));
        futureBooking = bookingRepository.save(
                new Booking(null, now.plusDays(1), now.plusDays(2),
                        item2, booker, BookingStatus.WAITING));
    }

    // ---------------------------------------------------------
    // findAllByBookerIdOrderByStartDesc
    // ---------------------------------------------------------
    @Test
    void findAllByBookerIdOrderByStartDescShouldReturnAllBookingsForBooker() {
        List<Booking> bookings =
                bookingRepository.findAllByBookerIdOrderByStartDesc(booker.getId());

        // ожидаем все три бронирования, отсортированные по дате начала DESC
        assertThat(bookings).hasSize(3);
        assertThat(bookings.get(0).getId()).isEqualTo(futureBooking.getId());
        assertThat(bookings.get(1).getId()).isEqualTo(currentBooking.getId());
        assertThat(bookings.get(2).getId()).isEqualTo(pastBooking.getId());
    }

    // ---------------------------------------------------------
    // findCurrentByBookerId
    // ---------------------------------------------------------
    @Test
    void findCurrentByBookerIdShouldReturnOnlyCurrentBooking() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings =
                bookingRepository.findCurrentByBookerId(booker.getId(), now);

        // ожидаем только текущее бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(currentBooking.getId());
    }

    // ---------------------------------------------------------
    // findPastByBookerId
    // ---------------------------------------------------------
    @Test
    void findPastByBookerIdShouldReturnOnlyPastBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings =
                bookingRepository.findPastByBookerId(booker.getId(), now);

        // ожидаем только прошлое бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(pastBooking.getId());
    }

    // ---------------------------------------------------------
    // findFutureByBookerId
    // ---------------------------------------------------------
    @Test
    void findFutureByBookerIdShouldReturnOnlyFutureBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings =
                bookingRepository.findFutureByBookerId(booker.getId(), now);

        // ожидаем только будущее бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(futureBooking.getId());
    }

    // ---------------------------------------------------------
    // findByBookerIdAndStatusOrderByStartDesc
    // ---------------------------------------------------------
    @Test
    void findByBookerIdAndStatusOrderByStartDescShouldReturnBookingsWithSpecificStatus() {
        List<Booking> bookings =
                bookingRepository.findByBookerIdAndStatusOrderByStartDesc(
                        booker.getId(), BookingStatus.APPROVED);

        // ожидаем два бронирования со статусом APPROVED
        assertThat(bookings).hasSize(2);
        assertThat(bookings).extracting(Booking::getId)
                            .containsExactlyInAnyOrder(
                                    pastBooking.getId(),
                                    currentBooking.getId());
    }

    // --------------- Тесты для методов по владельцу (owner) ----------------------

    // ---------------------------------------------------------
    // findAllByOwnerIdOrderByStartDesc
    // ---------------------------------------------------------
    @Test
    void findAllByOwnerIdOrderByStartDescShouldReturnAllBookingsForOwner() {
        List<Booking> bookings =
                bookingRepository.findAllByOwnerIdOrderByStartDesc(owner.getId());

        // ожидаем все бронирования на вещи владельца, отсортированные по start DESC
        assertThat(bookings).hasSize(3);
        assertThat(bookings.get(0).getId()).isEqualTo(futureBooking.getId());
        assertThat(bookings.get(1).getId()).isEqualTo(currentBooking.getId());
        assertThat(bookings.get(2).getId()).isEqualTo(pastBooking.getId());
    }

    // ---------------------------------------------------------
    // findCurrentByOwnerId
    // ---------------------------------------------------------
    @Test
    void findCurrentByOwnerIdShouldReturnOnlyCurrentBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings =
                bookingRepository.findCurrentByOwnerId(owner.getId(), now);

        // ожидаем только текущее бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(currentBooking.getId());
    }

    // ---------------------------------------------------------
    // findPastByOwnerId
    // ---------------------------------------------------------
    @Test
    void findPastByOwnerIdShouldReturnOnlyPastBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings =
                bookingRepository.findPastByOwnerId(owner.getId(), now);

        // ожидаем только прошлое бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(pastBooking.getId());
    }

    // ---------------------------------------------------------
    // findFutureByOwnerId
    // ---------------------------------------------------------
    @Test
    void findFutureByOwnerIdShouldReturnOnlyFutureBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository
                .findFutureByOwnerId(owner.getId(), now);

        // ожидаем только будущее бронирование
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getId()).isEqualTo(futureBooking.getId());
    }

    // ---------------------------------------------------------
    // findByItemOwnerIdAndStatusOrderByStartDesc
    // ---------------------------------------------------------
    @Test
    void findByItemOwnerIdAndStatusOrderByStartDescShouldReturnBookingsWithSpecificStatus() {
        List<Booking> bookings =
                bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(
                        owner.getId(), BookingStatus.WAITING);

        // ожидаем одно бронирование со статусом WAITING
        assertThat(bookings).hasSize(1);
        assertThat(bookings).extracting(Booking::getId)
                            .containsExactlyInAnyOrder(futureBooking.getId());
    }

    // ---------------------------------------------------------
    // findBookingsByOwner  получить все APPROVED бронирования владельца
    // ---------------------------------------------------------
    @Test
    void findBookingsByOwnerShouldReturnApprovedBookings() {
        List<Booking> bookings = bookingRepository
                .findBookingsByOwner(owner.getId());

        // возвращает только APPROVED бронирования
        assertThat(bookings).hasSize(2);
        assertThat(bookings).allMatch(
                b -> b.getStatus() == BookingStatus.APPROVED);
    }
}