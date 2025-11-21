package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(BookingDtoPost bookingDtoPost, Long bookerId);

    BookingDto updateBookingStatus(Long bookingId, Boolean approved, Long ownerId);

    BookingDto getBookingById(Long bookingId, Long userId);

    List<BookingDto> getBookingsByBooker(Long bookerId, String state);

    List<BookingDto> getBookingsByOwner(Long ownerId, String state);
}