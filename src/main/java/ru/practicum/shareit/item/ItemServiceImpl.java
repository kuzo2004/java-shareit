package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemMapper itemMapper;
    private final BookingMapper bookingMapper;


    @Override
    @Transactional
    public ItemDto createItem(ItemDtoPost itemDtoPost, Long ownerId) {
        // Проверяем, что пользователь существует
        User existingUser = userService.getUserById(ownerId)
                                       .orElseThrow(() ->
                                               new NotFoundException(
                                                       "Пользователь с id=" + ownerId + "не существует"));

        Item item = itemMapper.toItemFromPost(itemDtoPost);
        item.setOwner(existingUser);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId) {

        // Проверяем, что товар существует
        Item existingItem = itemRepository.findById(itemId)
                                          .orElseThrow(() -> new NotFoundException(
                                                  "Товар с id=" + itemId + "не существует"));


        // Проверяем, что владелец существует, без загрузки сущности
        if (!userService.existsById(ownerId)) {
            throw new NotFoundException("Пользователь с id=" + ownerId + " не существует");
        }
        // Проверяем, что владелец совпадает
        if (!Objects.equals(existingItem.getOwner().getId(), ownerId)) {
            throw new RuntimeException("Только владелец может редактировать параметры вещи");
        }

        // Обновляем только разрешенные поля
        itemMapper.updateItemFromDto(itemDto, existingItem);

        Item updatedItem = itemRepository.save(existingItem);
        return itemMapper.toItemDto(updatedItem);
    }

    @Override
    public Optional<Item> getItemById(Long itemId) {

        return itemRepository.findById(itemId);
    }

    @Override
    public ItemInfoDto getItemDtoById(Long itemId) {
        Item existingItem = itemRepository.findById(itemId)
                                          .orElseThrow(() -> new NotFoundException(
                                                  "Товар с id=" + itemId + "не существует"));

        List<CommentDto> comments = getCommentsByItem(itemId);
        ItemInfoDto itemInfoDto = itemMapper.toItemInfoDto(existingItem);
        itemInfoDto.setComments(comments);
        return itemInfoDto;
    }


    @Override
    public List<ItemInfoDto> getAllItemsByOwner(Long ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        if (items.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        List<Booking> ownerBookings = bookingRepository.findBookingsByOwner(ownerId);

        // сгруппировали бронирования по товарам
        Map<Long, List<Booking>> bookingsByItem = ownerBookings
                .stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getItem().getId()));

        return items.stream()
                    .map(item -> {
                        List<Booking> itemBookings = bookingsByItem.getOrDefault(item.getId(), List.of());

                        BookingDtoShort lastBooking = findLastBooking(itemBookings, now);
                        BookingDtoShort nextBooking = findNextBooking(itemBookings, now);
                        List<CommentDto> comments = getCommentsByItem(item.getId());

                        ItemInfoDto itemInfoDto = itemMapper.toItemInfoDto(item, lastBooking, nextBooking);
                        itemInfoDto.setComments(comments);
                        return itemInfoDto;
                    })
                    .collect(Collectors.toList());
    }

    // метод для поиска последнего бронирования
    private BookingDtoShort findLastBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                       .filter(booking -> booking.getEnd().isBefore(now))
                       .max(Comparator.comparing(Booking::getEnd))
                       .map(bookingMapper::toBookingDtoShort)
                       .orElse(null);
    }

    // метод для поиска следующего бронирования
    private BookingDtoShort findNextBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                       .filter(booking -> booking.getStart().isAfter(now))
                       .min(Comparator.comparing(Booking::getStart))
                       .map(bookingMapper::toBookingDtoShort)
                       .orElse(null);
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String searchText = text.toLowerCase();
        List<Item> foundItems = itemRepository.searchAvailableItems(searchText);
        return foundItems.stream()
                         .map(itemMapper::toItemDto)
                         .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, CommentDtoPost commentDtoPost, Long authorId) {
        // Проверяем существование пользователя, дальше нужен объект author, берем всю сущность
        User author = userService.getUserById(authorId)
                                 .orElseThrow(() -> new NotFoundException(
                                         "Пользователь с id=" + authorId + " не существует"));

        // Проверяем существование вещи, дальше нужен объект item, берем всю сущность
        Item item = getItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не существует"));

        // Проверяем, что пользователь брал вещь в аренду
        boolean hasBooked = bookingRepository.findPastByBookerId(authorId, LocalDateTime.now())
                                             .stream()
                                             .anyMatch(booking -> booking.getItem().getId().equals(itemId));

        if (!hasBooked) {
            throw new ValidationException("Пользователь не брал вещь в аренду или аренда еще не завершена");
        }

        Comment comment = itemMapper.toComment(commentDtoPost, item, author);
        Comment savedComment = commentRepository.save(comment);
        return itemMapper.toCommentDto(savedComment);
    }

    @Override
    public List<CommentDto> getCommentsByItem(Long itemId) {
        List<Comment> comments = commentRepository.findByItemId(itemId);
        return comments.stream()
                       .map(itemMapper::toCommentDto)
                       .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsByOwner(Long ownerId) {
        List<Comment> comments = commentRepository.findByItemOwnerId(ownerId);
        return comments.stream()
                       .map(itemMapper::toCommentDto)
                       .collect(Collectors.toList());
    }
}

