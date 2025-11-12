package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserService userService;
    @Mock
    private ItemService itemService;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private UserDto bookerDto;
    private Item item;
    private ItemDto itemDto;
    private Booking booking;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Alice", "alice@mail.com");
        booker = new User(2L, "Bob", "bob@mail.com");
        bookerDto = new UserDto(2L, "Bob", "bob@mail.com");
        item = new Item(10L, "Drill", "Desc", true, owner, null);
        itemDto = new ItemDto(10L, "Drill", "Desc", true, null, null);
        booking = new Booking(1L, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2), item, booker, BookingStatus.WAITING);
        bookingDto = new BookingDto(1L, booking.getStart(), booking.getEnd(),
                itemDto, bookerDto, booking.getStatus());
    }

    // ----------------------------------------------------------------------
    // CREATE BOOKING
    // ----------------------------------------------------------------------
    @Test
    void createBookingWhenValidThenReturnBookingDto() {
        BookingDtoPost post = new BookingDtoPost(
                item.getId(),
                booking.getStart(),
                booking.getEnd());

        when(userService.getUserById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemService.getItemById(item.getId())).thenReturn(Optional.of(item));
        when(bookingMapper.toBookingFromPost(post, item, booker)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        BookingDto result = bookingService.createBooking(post, booker.getId());

        assertThat(result).isEqualTo(bookingDto);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBookingWhenOwnerTriesToBookThenThrowValidationException() {
        BookingDtoPost post = new BookingDtoPost(item.getId(),
                booking.getStart(), booking.getEnd());

        when(userService.getUserById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemService.getItemById(item.getId())).thenReturn(Optional.of(item));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(post, owner.getId())
        );

        assertEquals("Владелец не может бронировать свою вещь", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingWhenItemUnavailableThenThrowValidationException() {
        item.setAvailable(false);
        BookingDtoPost post = new BookingDtoPost(item.getId(),
                booking.getStart(), booking.getEnd());

        when(userService.getUserById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemService.getItemById(item.getId())).thenReturn(Optional.of(item));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(post, booker.getId())
        );

        assertEquals("Вещь недоступна для бронирования", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingWhenEndBeforeStartThenThrowValidationException() {
        BookingDtoPost post = new BookingDtoPost(item.getId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1));

        when(userService.getUserById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemService.getItemById(item.getId())).thenReturn(Optional.of(item));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(post, booker.getId())
        );

        assertEquals("Дата начала должна быть раньше даты окончания", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingWhenUserNotFoundThenThrowNotFoundException() {
        BookingDtoPost post = new BookingDtoPost(
                item.getId(),
                booking.getStart(),
                booking.getEnd());

        when(userService.getUserById(booker.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(post, booker.getId())
        );

        assertEquals("Пользователь с id=" + booker.getId() + " не существует", exception.getMessage());
        verify(itemService, never()).getItemById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingWhenItemNotFoundThenThrowNotFoundException() {
        BookingDtoPost post = new BookingDtoPost(
                item.getId(),
                booking.getStart(),
                booking.getEnd());

        when(userService.getUserById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemService.getItemById(item.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(post, booker.getId())
        );

        assertEquals("Вещь с id=" + item.getId() + " не существует", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    // ----------------------------------------------------------------------
    // UPDATE BOOKING STATUS
    // ----------------------------------------------------------------------
    @Test
    void updateBookingStatusWhenBookingNotFoundThenThrowNotFoundException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.updateBookingStatus(999L, true, owner.getId())
        );

        assertEquals("Бронирование с id=999 не существует", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBookingStatusWhenNotOwnerThenThrowAccessDeniedException() {
        User stranger = new User(3L, "Eve", "eve@mail.com");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> bookingService.updateBookingStatus(booking.getId(), true, stranger.getId())
        );
        assertEquals("Только владелец вещи может подтверждать бронирование", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBookingStatusWhenAlreadyChangedThenThrowValidationException() {
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.updateBookingStatus(booking.getId(), true, owner.getId())
        );

        assertEquals("Статус бронирования уже изменен", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBookingStatusWhenOwnerApprovesThenReturnUpdatedDto() {
        // бронирование после одобрения (ожидаемый результат)
        Booking updated = new Booking(booking.getId(), booking.getStart(), booking.getEnd(),
                item, booker, BookingStatus.APPROVED);
        BookingDto updatedDto = new BookingDto(booking.getId(), booking.getStart(), booking.getEnd(),
                itemDto, bookerDto, BookingStatus.APPROVED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(updated);
        when(bookingMapper.toBookingDto(updated)).thenReturn(updatedDto);

        BookingDto result = bookingService.updateBookingStatus(booking.getId(), true, owner.getId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertEquals(updatedDto, result);
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateBookingStatusWhenOwnerRejectsThenReturnRejectedDto() {
        // ожидаемое состояние
        Booking updated = new Booking(booking.getId(), booking.getStart(), booking.getEnd(),
                item, booker, BookingStatus.REJECTED);
        BookingDto updatedDto = new BookingDto(booking.getId(), booking.getStart(), booking.getEnd(),
                itemDto, bookerDto, BookingStatus.REJECTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(updated);
        when(bookingMapper.toBookingDto(updated)).thenReturn(updatedDto);

        BookingDto result = bookingService.updateBookingStatus(booking.getId(), false, owner.getId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertEquals(updatedDto, result);
        verify(bookingRepository).save(booking);
    }

    // ----------------------------------------------------------------------
    // GET BOOKING BY ID
    // ----------------------------------------------------------------------
    @Test
    void getBookingByIdWhenBookingNotFoundThenThrowNotFoundException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingById(999L, booker.getId())
        );

        assertEquals("Бронирование с id=999 не существует", exception.getMessage());
        verify(userService, never()).existsById(any());
        verify(bookingMapper, never()).toBookingDto(any());
    }

    @Test
    void getBookingByIdWhenUserNotFoundThenThrowNotFoundException() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userService.existsById(99L)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingById(booking.getId(), 99L)
        );

        assertEquals("Пользователь с id=99 не существует", exception.getMessage());
        verify(bookingMapper, never()).toBookingDto(any());
    }

    @Test
    void getBookingByIdWhenUserHasNoAccessThenThrowAccessDeniedException() {
        User stranger = new User(999L, "Stranger", "stranger@mail.com");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userService.existsById(stranger.getId())).thenReturn(true);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> bookingService.getBookingById(booking.getId(), stranger.getId())
        );

        assertEquals("Пользователь не имеет доступа к данному бронированию", exception.getMessage());
        verify(bookingMapper, never()).toBookingDto(any());
    }

    @Test
    void getBookingByIdWhenBookerRequestsThenReturnBookingDto() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        BookingDto result = bookingService.getBookingById(booking.getId(), booker.getId());

        assertThat(result).isEqualTo(bookingDto);
        verify(bookingRepository).findById(booking.getId());
        verify(userService).existsById(booker.getId());
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingByIdWhenOwnerRequestsThenReturnBookingDto() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        BookingDto result = bookingService.getBookingById(booking.getId(), owner.getId());

        assertThat(result).isEqualTo(bookingDto);
        verify(bookingRepository).findById(booking.getId());
        verify(userService).existsById(owner.getId());
        verify(bookingMapper).toBookingDto(booking);
    }

    // ----------------------------------------------------------------------
    // GET BOOKINGS BY BOOKER
    // ----------------------------------------------------------------------
    @Test
    void getBookingsByBookerWhenUserDoesNotExistThenThrowNotFoundException() {
        long bookerId = 99L;

        when(userService.existsById(bookerId)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingsByBooker(bookerId, "ALL")
        );

        assertEquals("Пользователь с id=" + bookerId + " не существует", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByBookerWhenStateUnknownThenThrowIllegalArgumentException() {
        when(userService.existsById(booker.getId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getBookingsByBooker(booker.getId(), "UNKNOWN_STATE")
        );

        assertTrue(exception.getMessage().contains("No enum constant"));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByBookerWhenStateCanceledThenThrowValidationException() {
        // когда состояние есть в enum, но не добавлено в switch
        String stateParam = "CANCELED";

        when(userService.existsById(booker.getId())).thenReturn(true);

        // состояние CANCELED есть в enum, но нет в switch
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.getBookingsByBooker(booker.getId(), stateParam)
        );

        assertEquals("Неизвестное состояние: " + stateParam, exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByBookerWhenStatePastThenReturnList() {
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository.findPastByBookerId(eq(booker.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "PAST");

        assertThat(result).containsExactly(bookingDto);
    }

    @Test
    void getBookingsByBookerWhenStateFutureThenReturnList() {
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository.findFutureByBookerId(eq(booker.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "FUTURE");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findFutureByBookerId(eq(booker.getId()), any(LocalDateTime.class));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByBookerWhenStateCurrentThenReturnList() {
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository.findCurrentByBookerId(eq(booker.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "CURRENT");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findCurrentByBookerId(eq(booker.getId()), any(LocalDateTime.class));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByBookerWhenStateWaitingThenReturnList() {
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(eq(booker.getId()), eq(BookingStatus.WAITING)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "WAITING");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findByBookerIdAndStatusOrderByStartDesc(eq(booker.getId()), eq(BookingStatus.WAITING));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByBookerWhenStateRejectedThenReturnList() {
        booking.setStatus(BookingStatus.REJECTED);

        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository
                .findByBookerIdAndStatusOrderByStartDesc(eq(booker.getId()), eq(BookingStatus.REJECTED)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "REJECTED");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository)
                .findByBookerIdAndStatusOrderByStartDesc(eq(booker.getId()), eq(BookingStatus.REJECTED));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByBookerWhenStateAllThenReturnList() {
        when(userService.existsById(booker.getId())).thenReturn(true);
        when(bookingRepository.findAllByBookerIdOrderByStartDesc(booker.getId()))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByBooker(booker.getId(), "ALL");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findAllByBookerIdOrderByStartDesc(booker.getId());
        verify(bookingMapper).toBookingDto(booking);
    }

    // ----------------------------------------------------------------------
    // GET BOOKINGS BY OWNER
    // ----------------------------------------------------------------------
    @Test
    void getBookingsByOwnerWhenUserDoesNotExistThenThrowNotFoundException() {
        long ownerId = 99L;

        when(userService.existsById(ownerId)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingsByOwner(ownerId, "ALL")
        );

        assertEquals("Владелец с id=" + ownerId + " не существует", exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByOwnerWhenStateUnknownThenThrowIllegalArgumentException() {
        when(userService.existsById(owner.getId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getBookingsByOwner(owner.getId(), "UNKNOWN_STATE")
        );

        assertTrue(exception.getMessage().contains("No enum constant"));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByOwnerWhenStateCanceledThenThrowValidationException() {
        // состояние есть в enum, но не добавлено в switch
        String stateParam = "CANCELED";

        when(userService.existsById(owner.getId())).thenReturn(true);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.getBookingsByOwner(owner.getId(), stateParam)
        );

        assertEquals("Неизвестное состояние: " + stateParam, exception.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingsByOwnerWhenStatePastThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository.findPastByOwnerId(eq(owner.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "PAST");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findPastByOwnerId(eq(owner.getId()), any(LocalDateTime.class));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByOwnerWhenStateAllThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository.findAllByOwnerIdOrderByStartDesc(owner.getId()))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "ALL");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findAllByOwnerIdOrderByStartDesc(owner.getId());
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByOwnerWhenStateCurrentThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository.findCurrentByOwnerId(eq(owner.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "CURRENT");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findCurrentByOwnerId(eq(owner.getId()), any(LocalDateTime.class));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByOwnerWhenStateFutureThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository.findFutureByOwnerId(eq(owner.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "FUTURE");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository).findFutureByOwnerId(eq(owner.getId()), any(LocalDateTime.class));
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByOwnerWhenStateWaitingThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository
                .findByItemOwnerIdAndStatusOrderByStartDesc(owner.getId(), BookingStatus.WAITING))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "WAITING");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository)
                .findByItemOwnerIdAndStatusOrderByStartDesc(owner.getId(), BookingStatus.WAITING);
        verify(bookingMapper).toBookingDto(booking);
    }

    @Test
    void getBookingsByOwnerWhenStateRejectedThenReturnList() {
        when(userService.existsById(owner.getId())).thenReturn(true);
        when(bookingRepository
                .findByItemOwnerIdAndStatusOrderByStartDesc(owner.getId(), BookingStatus.REJECTED))
                .thenReturn(List.of(booking));
        when(bookingMapper.toBookingDto(booking)).thenReturn(bookingDto);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), "REJECTED");

        assertThat(result).containsExactly(bookingDto);
        verify(bookingRepository)
                .findByItemOwnerIdAndStatusOrderByStartDesc(owner.getId(), BookingStatus.REJECTED);
        verify(bookingMapper).toBookingDto(booking);
    }
}