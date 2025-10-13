package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStateParam;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Override
    @Transactional
    public BookingDto createBooking(BookingDtoPost bookingDtoPost, Long bookerId) {
        // Проверяем существование пользователя
        User booker = userService.getUserById(bookerId)
                                 .orElseThrow(() ->
                                         new NotFoundException(
                                                 "Пользователь с id=" + bookerId + " не существует"));

        // Проверяем существование вещи
        Item item = itemService.getItemById(bookingDtoPost.getItemId())
                               .orElseThrow(() ->
                                       new NotFoundException(
                                               "Вещь с id=" + bookingDtoPost.getItemId() + " не существует"));

        // Проверяем, что владелец не бронирует свою вещь (ValidationException -> 400/AccessDeniedException -> 403)
        if (item.getOwner().getId().equals(bookerId)) {
            throw new ValidationException("Владелец не может бронировать свою вещь");
        }

        // Проверяем доступность вещи
        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        // Проверяем даты бронирования
        if (bookingDtoPost.getStart().isAfter(bookingDtoPost.getEnd()) ||
                bookingDtoPost.getStart().isEqual(bookingDtoPost.getEnd())) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }

        log.info("СОЗДАНИЕ booking: start={}, end={}, itemId={}, bookerId={}",
                bookingDtoPost.getStart(), bookingDtoPost.getEnd(),
                bookingDtoPost.getItemId(), bookerId);

        Booking booking = BookingMapper.toBookingFromPost(bookingDtoPost, item, booker);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("ПОСЛЕ Booking created successfully: id={}", savedBooking.getId());
        return BookingMapper.toBookingDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingDto updateBookingStatus(Long bookingId, Boolean approved, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                                           .orElseThrow(() ->
                                                   new NotFoundException(
                                                           "Бронирование с id=" + bookingId + " не существует"));

        // Проверяем, что пользователь - владелец вещи
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Только владелец вещи может подтверждать бронирование");
        }

        // Проверяем, что статус еще не изменен
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Статус бронирования уже изменен");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking updatedBooking = bookingRepository.save(booking);
        return BookingMapper.toBookingDto(updatedBooking);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                                           .orElseThrow(() ->
                                                   new NotFoundException(
                                                           "Бронирование с id=" + bookingId + " не существует"));

        // Проверяем, что пользователь имеет доступ к бронированию
        if (!booking.getBooker().getId().equals(userId)
                && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Пользователь не имеет доступа к данному бронированию");
        }

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getBookingsByBooker(Long bookerId, String state) {
        userService.getUserById(bookerId)
                   .orElseThrow(() -> new NotFoundException("Пользователь с id=" + bookerId + " не существует"));

        BookingStateParam stateParam = BookingStateParam.valueOf(state);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        bookings = switch (stateParam) {
            case ALL -> bookingRepository.findAllByBookerIdOrderByStartDesc(bookerId);
            case CURRENT -> bookingRepository.findCurrentByBookerId(bookerId, now);
            case PAST -> bookingRepository.findPastByBookerId(bookerId, now);
            case FUTURE -> bookingRepository.findFutureByBookerId(bookerId, now);
            case WAITING -> bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.WAITING);
            case REJECTED ->
                    bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.REJECTED);
            default -> throw new IllegalStateException("Unknown state: " + state);
        };

        return bookings.stream()
                       .map(BookingMapper::toBookingDto)
                       .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getBookingsByOwner(Long ownerId, String state) {
        // Проверяем существование пользователя
        userService.getUserById(ownerId)
                   .orElseThrow(() -> new NotFoundException("Владелец с id=" + ownerId + " не существует"));

        BookingStateParam stateParam = BookingStateParam.valueOf(state);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        bookings = switch (stateParam) {
            case ALL -> bookingRepository.findAllByOwnerIdOrderByStartDesc(ownerId);
            case CURRENT -> bookingRepository.findCurrentByOwnerId(ownerId, now);
            case PAST -> bookingRepository.findPastByOwnerId(ownerId, now);
            case FUTURE -> bookingRepository.findFutureByOwnerId(ownerId, now);
            case WAITING ->
                    bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
            case REJECTED ->
                    bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
            default -> throw new IllegalStateException("Unknown state: " + state);
        };

        return bookings.stream()
                       .map(BookingMapper::toBookingDto)
                       .collect(Collectors.toList());
    }
}






