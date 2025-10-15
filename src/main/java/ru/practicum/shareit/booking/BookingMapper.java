package ru.practicum.shareit.booking;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoPost;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring",
        uses = {ItemMapper.class, UserMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    BookingDto toBookingDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    // expression длинная, но по другому не собирается проект
    @Mapping(target = "status", expression = "java(ru.practicum.shareit.booking.model.BookingStatus.WAITING)")
    Booking toBookingFromPost(BookingDtoPost bookingDtoPost, Item item, User booker);

    BookingDtoShort toBookingDtoShort(Booking booking);
}
