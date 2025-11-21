package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.dto.BookingDtoShort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserService;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Captor
    private ArgumentCaptor<Item> itemCaptor;

    private User owner;
    private Item item;
    private ItemDtoPost itemDtoPost;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "Alice", "alice@mail.com");
        item = new Item(10L, "Drill", "Powerful drill", true, owner, null);
        itemDtoPost = new ItemDtoPost("Drill", "Powerful drill", true, null);
        itemDto = new ItemDto(1L, "Drill", "Powerful drill",
                true, null, null);
    }

    // --------------------------
    // createItem()
    // --------------------------
    @Test
    void createItemWhenUserNotExistsThenThrowNotFoundException() {
        when(userService.getUserById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.createItem(itemDtoPost, 1L)
        );

        assertEquals("Пользователь с id=1 не существует", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItemWhenValidThenSaveAndReturnDto() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(owner));
        when(itemMapper.toItemFromPost(any(ItemDtoPost.class))).thenReturn(item);
        when(itemRepository.save(itemCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemMapper.toItemDto(any(Item.class))).thenReturn(itemDto);

        ItemDto result = itemService.createItem(itemDtoPost, 1L);

        Item savedItem = itemCaptor.getValue();

        assertEquals("Drill", savedItem.getName());
        assertEquals("Powerful drill", savedItem.getDescription());
        assertTrue(savedItem.getAvailable());
        assertEquals(owner, savedItem.getOwner());
        assertThat(result).isEqualTo(itemDto);

        verify(itemMapper).toItemFromPost(itemDtoPost);
        verify(itemRepository).save(item);
        verify(itemMapper).toItemDto(item);
    }

    @Test
    void createItemWhenRequestIdNotFoundThenThrowNotFoundException() {
        itemDtoPost.setRequestId(99L);

        when(userService.getUserById(1L)).thenReturn(Optional.of(owner));
        when(itemMapper.toItemFromPost(any(ItemDtoPost.class))).thenReturn(item);
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.createItem(itemDtoPost, 1L)
        );

        assertEquals("Запрос с id=99 не существует", exception.getMessage());
        verify(userService).getUserById(1L);
        verify(itemMapper).toItemFromPost(any(ItemDtoPost.class));
        verify(itemRequestRepository).findById(99L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItemWhenRequestIdExistsThenSaveWithRequest() {
        User requester = new User(2L, "Bob", "bob@mail.com");
        ItemRequest request = new ItemRequest(99L, "Need drill",
                requester, LocalDateTime.now().minusDays(1));
        itemDtoPost.setRequestId(99L);

        when(userService.getUserById(1L)).thenReturn(Optional.of(owner));
        when(itemMapper.toItemFromPost(any(ItemDtoPost.class))).thenReturn(item);
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.of(request));
        when(itemRepository.save(itemCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemMapper.toItemDto(any(Item.class))).thenReturn(itemDto);

        ItemDto result = itemService.createItem(itemDtoPost, 1L);

        Item savedItem = itemCaptor.getValue();
        assertEquals(request, savedItem.getRequest()); // проверяем, что request присвоен
        assertEquals(owner, savedItem.getOwner());
        assertThat(result).isEqualTo(itemDto);

        verify(itemRequestRepository).findById(99L);
        verify(itemRepository).save(any(Item.class));
    }

    // --------------------------
    // updateItem()
    // --------------------------
    @Test
    void updateItemWhenOwnerNotExistsThenThrowNotFoundException() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userService.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.updateItem(10L, itemDto, 1L)
        );

        assertEquals("Пользователь с id=1 не существует", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemWhenItemNotExistsThenThrowNotFoundException() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.updateItem(10L, itemDto, 1L)
        );

        assertEquals("Товар с id=10 не существует", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemWhenOwnerMismatchThenThrowRuntimeException() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        item.setOwner(anotherUser);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userService.existsById(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> itemService.updateItem(10L, itemDto, 1L)
        );

        assertEquals("Только владелец может редактировать параметры вещи", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemWhenValidThenSaveAndReturnDto() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userService.existsById(1L)).thenReturn(true);
        doNothing().when(itemMapper).updateItemFromDto(any(ItemDto.class), any(Item.class));
        when(itemRepository.save(itemCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemMapper.toItemDto(any(Item.class))).thenReturn(itemDto);

        ItemDto result = itemService.updateItem(10L, itemDto, 1L);

        Item savedItem = itemCaptor.getValue();
        assertEquals(owner, savedItem.getOwner());
        assertEquals(itemDto.getName(), savedItem.getName());
        assertEquals(itemDto.getDescription(), savedItem.getDescription());
        assertEquals(itemDto.getAvailable(), savedItem.getAvailable());

        assertThat(result).isEqualTo(itemDto);

        verify(itemRepository).findById(10L);
        verify(userService).existsById(1L);
        verify(itemMapper).updateItemFromDto(itemDto, item);
        verify(itemRepository).save(item);
        verify(itemMapper).toItemDto(item);
    }

    @Test
    void updateItemWithPartialFieldsThenUpdateOnlyNonNull() {
        ItemDto partialDto = new ItemDto();
        partialDto.setName("New Drill"); // обновляем только имя

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userService.existsById(1L)).thenReturn(true);
        // МОК-Маппер обновляет только имя у item
        doAnswer(invocation -> {
            ItemDto dto = invocation.getArgument(0);
            Item it = invocation.getArgument(1);
            if (dto.getName() != null) it.setName(dto.getName());
            return null;
        }).when(itemMapper).updateItemFromDto(any(ItemDto.class), any(Item.class));
        // в исходном объекте item только поле name изменёно через маппер
        when(itemRepository.save(itemCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemMapper.toItemDto(any(Item.class))).thenReturn(itemDto);

        ItemDto result = itemService.updateItem(10L, partialDto, 1L);

        // проверяем поля сохранённого объекта
        Item savedItem = itemCaptor.getValue();
        assertEquals("New Drill", savedItem.getName());
        assertEquals("Powerful drill", savedItem.getDescription()); // не изменилось
        assertEquals(true, savedItem.getAvailable());
        assertEquals(owner, savedItem.getOwner());
        // проверяем результат метода
        assertThat(result).isEqualTo(itemDto);

        verify(itemRepository).findById(10L);
        verify(userService).existsById(1L);
        verify(itemMapper).updateItemFromDto(partialDto, item);
        verify(itemRepository).save(savedItem);
        verify(itemMapper).toItemDto(savedItem);
    }

    // --------------------------
    // getItemById()
    // --------------------------
    @Test
    void getItemByIdWhenItemExistsThenReturnOptional() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.getItemById(10L);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(item);

        verify(itemRepository).findById(10L);
    }

    @Test
    void getItemByIdWhenItemNotExistsThenReturnEmptyOptional() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.getItemById(10L);

        assertTrue(result.isEmpty(), "Ожидается пустой Optional, так как товар не найден");

        verify(itemRepository).findById(10L);
    }

    // --------------------------
    // getItemDtoById()
    // --------------------------
    @Test
    void getItemDtoByIdWhenItemNotExistsThenThrowNotFoundException() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> itemService.getItemDtoById(10L)
        );

        assertEquals("Товар с id=10 не существует", ex.getMessage());

        verify(commentRepository, never()).findByItemId(any());
        verify(itemMapper, never()).toItemInfoDto(any());
    }

    @Test
    void getItemDtoByIdWhenItemExistsThenReturnItemInfoDto() {
        User user = new User(2L, "Bob", "bob@mail.com");
        LocalDateTime created = LocalDateTime.now();
        Comment comment = new Comment(1L, "Nice", item, user, created);
        CommentDto commentDto = new CommentDto(1L, "Nice", "Bob", created);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(10L)).thenReturn(List.of(comment));

        // каждый comment проходит через itemMapper.toCommentDto(comment) → создаётся CommentDto
        when(itemMapper.toCommentDto(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment captured = invocation.getArgument(0);
                    // Проверяем, что сервис передал корректный Comment объект в маппер
                    assertEquals(comment, captured);
                    // Симуляция настоящего маппинга Comment -> CommentDto
                    return new CommentDto(
                            captured.getId(),
                            captured.getText(),
                            captured.getAuthor().getName(),
                            captured.getCreated());
                });

        // маппер принимает item и возвращает itemInfoDto (без comments)
        when(itemMapper.toItemInfoDto(any(Item.class)))
                .thenAnswer(invocation -> {
                    Item capturedItem = invocation.getArgument(0);
                    // Проверяем, что в сервис реально передаётся нужный Item
                    assertEquals(item, capturedItem);
                    // Симуляция настоящего маппинга Item -> ItemInfoDto
                    return new ItemInfoDto(
                            capturedItem.getId(),
                            capturedItem.getName(),
                            capturedItem.getDescription(),
                            capturedItem.getAvailable(),
                            null, null,
                            null, null
                    );
                });

        ItemInfoDto result = itemService.getItemDtoById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getComments()).containsExactly(commentDto);
        verify(commentRepository).findByItemId(10L);
        verify(itemMapper).toItemInfoDto(item);
    }

    //--------------------------
    // getAllItemsByOwner()
    // --------------------------
    @Test
    void getAllItemsByOwnerWhenItemsExistThenReturnListOfItemInfoDtoWithBookings() {
        // Другой пользователь — автор комментариев и бронирований
        User user = new User(2L, "Bob", "bob@mail.com");

        // Предметы владельца - item и  anotherItem
        Item anotherItem = new Item(20L, "Item2", "Desc2",
                true, owner, null);
        List<Item> items = List.of(item, anotherItem);

        // --- Подготовка бронирований ---
        // Прошлое бронирование (завершено до текущего момента)
        Booking pastBooking = new Booking();
        pastBooking.setId(100L);
        pastBooking.setItem(item);
        pastBooking.setBooker(user);
        pastBooking.setStart(LocalDateTime.now().minusDays(3));
        pastBooking.setEnd(LocalDateTime.now().minusDays(1));
        pastBooking.setStatus(BookingStatus.APPROVED);

        // Будущее бронирование (еще не началось)
        Booking futureBooking = new Booking();
        futureBooking.setId(200L);
        futureBooking.setItem(anotherItem);
        futureBooking.setBooker(user);
        futureBooking.setStart(LocalDateTime.now().plusDays(1));
        futureBooking.setEnd(LocalDateTime.now().plusDays(3));
        futureBooking.setStatus(BookingStatus.APPROVED);

        // --- DTO для бронирований ---
        BookingDtoShort pastBookingDto = new BookingDtoShort();
        pastBookingDto.setId(100L);
        pastBookingDto.setBookerId(user.getId());
        pastBookingDto.setStart(pastBooking.getStart());
        pastBookingDto.setEnd(pastBooking.getEnd());
        pastBookingDto.setBookerId(user.getId());

        BookingDtoShort futureBookingDto = new BookingDtoShort();
        futureBookingDto.setId(200L);
        futureBookingDto.setBookerId(user.getId());
        futureBookingDto.setStart(futureBooking.getStart());
        futureBookingDto.setEnd(futureBooking.getEnd());
        futureBookingDto.setBookerId(user.getId());

        // --- Комментарии и их DTO ---
        Comment comment1 = new Comment(1L, "Nice", item, user, LocalDateTime.now());
        CommentDto commentDto1 = new CommentDto(1L, "Nice", user.getName(), LocalDateTime.now());

        Comment comment2 = new Comment(2L, "Great", anotherItem, user, LocalDateTime.now());
        CommentDto commentDto2 = new CommentDto(2L, "Great", user.getName(), LocalDateTime.now());

        // Мокирование зависимостей
        // Возвращаем предметы владельца
        when(itemRepository.findByOwnerId(1L)).thenReturn(items);

        // Возвращаем все бронирования, связанные с предметами владельца
        when(bookingRepository.findBookingsByOwner(1L)).thenReturn(List.of(pastBooking, futureBooking));

        // Маппинг сущностей Booking → BookingDtoShort
        when(bookingMapper.toBookingDtoShort(pastBooking)).thenReturn(pastBookingDto);
        when(bookingMapper.toBookingDtoShort(futureBooking)).thenReturn(futureBookingDto);

        // Возвращаем комментарии по каждому предмету
        when(commentRepository.findByItemId(item.getId())).thenReturn(List.of(comment1));
        when(commentRepository.findByItemId(anotherItem.getId())).thenReturn(List.of(comment2));
        when(itemMapper.toCommentDto(comment1)).thenReturn(commentDto1);
        when(itemMapper.toCommentDto(comment2)).thenReturn(commentDto2);

        // --- Маппинг предметов в итоговые DTO ---
        when(itemMapper.toItemInfoDto(item, pastBookingDto, null)).thenReturn(
                new ItemInfoDto(item.getId(),
                        item.getName(),
                        item.getDescription(),
                        item.getAvailable(),
                        pastBookingDto,
                        null,
                        null,
                        List.of(commentDto1))
        );
        when(itemMapper.toItemInfoDto(anotherItem, null, futureBookingDto)).thenReturn(
                new ItemInfoDto(anotherItem.getId(),
                        anotherItem.getName(),
                        anotherItem.getDescription(),
                        anotherItem.getAvailable(),
                        null,
                        futureBookingDto,
                        null,
                        List.of(commentDto2))
        );

        // --- Вызов тестируемого метода ---
        List<ItemInfoDto> result = itemService.getAllItemsByOwner(1L);

        // --- Проверки результата ---
        assertThat(result).hasSize(2); // ожидаем два предмета

        ItemInfoDto firstItem = result.get(0);
        assertThat(firstItem.getId()).isEqualTo(item.getId());
        assertThat(firstItem.getName()).isEqualTo(item.getName());
        assertThat(firstItem.getDescription()).isEqualTo(item.getDescription());
        assertThat(firstItem.getAvailable()).isEqualTo(item.getAvailable());
        assertThat(firstItem.getLastBooking()).isEqualTo(pastBookingDto);
        assertThat(firstItem.getNextBooking()).isNull(); // нет будущего бронирования для этого предмета
        assertThat(firstItem.getComments()).containsExactly(commentDto1);

        ItemInfoDto secondItem = result.get(1);
        assertThat(secondItem.getId()).isEqualTo(anotherItem.getId());
        assertThat(secondItem.getName()).isEqualTo(anotherItem.getName());
        assertThat(secondItem.getDescription()).isEqualTo(anotherItem.getDescription());
        assertThat(secondItem.getAvailable()).isEqualTo(anotherItem.getAvailable());
        assertThat(secondItem.getLastBooking()).isNull(); // нет прошедшего бронирования
        assertThat(secondItem.getNextBooking()).isEqualTo(futureBookingDto);
        assertThat(secondItem.getComments()).containsExactly(commentDto2);

        // --- Проверка вызовов моков ---
        verify(itemRepository).findByOwnerId(1L);
        verify(bookingRepository).findBookingsByOwner(1L);
        verify(commentRepository).findByItemId(item.getId());
        verify(commentRepository).findByItemId(anotherItem.getId());
    }

    @Test
    void getAllItemsByOwnerWhenNoItemsThenReturnEmptyList() {
        // Настройка мока для репозитория предметов
        // Возвращаем пустой список, чтобы имитировать отсутствие предметов у владельца
        when(itemRepository.findByOwnerId(owner.getId())).thenReturn(List.of());

        // Вызов тестируемого метода
        List<ItemInfoDto> result = itemService.getAllItemsByOwner(owner.getId());

        // Проверка результата
        assertThat(result).isEmpty(); // ожидаем пустой список

        // Проверка вызовов репозиториев
        verify(itemRepository).findByOwnerId(owner.getId());
        // bookingRepository и commentRepository не должны вызываться
        verifyNoInteractions(bookingRepository, commentRepository);
    }

    // --------------------------
    // searchItems()
    // --------------------------
    @Test
    void searchItemsWhenTextIsBlankThenReturnEmptyList() {
        List<ItemDto> result = itemService.searchItems("  ");
        assertThat(result).isEmpty();
        verifyNoInteractions(itemRepository);
    }

    @Test
    void searchItemsWhenTextIsValidThenReturnList() {
        when(itemRepository.searchAvailableItems("drill")).thenReturn(List.of(item));
        when(itemMapper.toItemDto(item)).thenReturn(itemDto);

        List<ItemDto> result = itemService.searchItems("DRILL");

        assertThat(result).containsExactly(itemDto);
        verify(itemRepository).searchAvailableItems("drill");
    }

    @Test
    void searchItemsWhenNoItemsFoundThenReturnEmptyList() {
        when(itemRepository.searchAvailableItems("hammer")).thenReturn(List.of());

        List<ItemDto> result = itemService.searchItems("hammer");

        assertThat(result).isEmpty();
        verify(itemRepository).searchAvailableItems("hammer");
    }

    @Test
    void searchItemsReturnsMultipleItems() {
        Item anotherItem = new Item();
        ItemDto anotherDto = new ItemDto();

        when(itemRepository.searchAvailableItems("drill")).thenReturn(List.of(item, anotherItem));
        when(itemMapper.toItemDto(item)).thenReturn(itemDto);
        when(itemMapper.toItemDto(anotherItem)).thenReturn(anotherDto);

        List<ItemDto> result = itemService.searchItems("drill");

        assertThat(result).containsExactly(itemDto, anotherDto);
        verify(itemRepository).searchAvailableItems("drill");
    }

    // --------------------------
    // addComment()
    // --------------------------
    @Test
    void addCommentWhenItemNotFoundThenThrowNotFoundException() {
        User user = new User(2L, "Bob", "bob@mail.com");
        CommentDtoPost commentDtoPost = new CommentDtoPost();
        commentDtoPost.setText("Text");

        when(userService.getUserById(2L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.addComment(99L, commentDtoPost, 2L)
        );

        assertEquals("Вещь с id=99 не существует", exception.getMessage());
    }

    @Test
    void addCommentWhenUserNotFoundThenThrowNotFoundException() {
        //User user = new User(2L, "Bob", "bob@mail.com");
        CommentDtoPost commentDtoPost = new CommentDtoPost();
        commentDtoPost.setText("Text");

        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.addComment(item.getId(), commentDtoPost, 99L)
        );

        assertEquals("Пользователь с id=99 не существует", exception.getMessage());
    }

    @Test
    void addCommentWhenUserNotBookedThenThrowValidationException() {
        User user = new User(2L, "Bob", "bob@mail.com");
        CommentDtoPost commentDtoPost = new CommentDtoPost();
        commentDtoPost.setText("Nice item");

        when(userService.getUserById(2L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.findPastByBookerId(eq(user.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of()); //  здесь БД не нашла такой вещи у пользователя

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> itemService.addComment(10L, commentDtoPost, 2L)
        );

        assertEquals("Пользователь не брал вещь в аренду или аренда еще не завершена",
                exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentWhenBookingNotEndedThenThrowValidationException() {
        User user = new User(2L, "Bob", "bob@mail.com");
        CommentDtoPost commentDtoPost = new CommentDtoPost();
        commentDtoPost.setText("Nice item");

        Booking activeBooking = new Booking();
        activeBooking.setItem(item);
        activeBooking.setBooker(user);
        activeBooking.setStart(LocalDateTime.now().minusDays(1)); // уже началась
        activeBooking.setEnd(LocalDateTime.now().plusDays(1));    // но еще не закончилась

        // user есть
        when(userService.getUserById(user.getId())).thenReturn(Optional.of(user));
        // item есть
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        // findPastByBookerId вернет пусто, потому что аренда еще не закончена
        when(bookingRepository.findPastByBookerId(eq(user.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of()); // т.е. активное бронирование не считается "past"

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> itemService.addComment(item.getId(), commentDtoPost, user.getId())
        );

        assertEquals("Пользователь не брал вещь в аренду или аренда еще не завершена",
                exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentWhenValidThenSaveAndReturnDto() {
        User user = new User(2L, "Bob", "bob@mail.com");
        CommentDtoPost commentDtoPost = new CommentDtoPost();
        commentDtoPost.setText("Nice item");

        Comment comment = new Comment(1L, "Nice item", item, user, LocalDateTime.now());
        CommentDto commentDto = new CommentDto(1L, "Nice item", user.getName(), LocalDateTime.now());

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setItem(item);
        booking.setBooker(user);
        booking.setStart(LocalDateTime.now().minusDays(3));
        booking.setEnd(LocalDateTime.now().minusDays(1)); // аренда завершена
        booking.setStatus(BookingStatus.APPROVED);       // завершённая аренда

        when(userService.getUserById(user.getId())).thenReturn(Optional.of(user));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findPastByBookerId(eq(2L), any(LocalDateTime.class)))
                .thenReturn(List.of(booking)); // БД нашла завершенное бронирование
        when(itemMapper.toComment(commentDtoPost, item, user)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(itemMapper.toCommentDto(comment)).thenReturn(commentDto);

        CommentDto result = itemService.addComment(item.getId(), commentDtoPost, user.getId());

        assertThat(result).isEqualTo(commentDto);
        verify(commentRepository).save(comment);
    }

    // --------------------------
    // getCommentsByItem()
    // --------------------------
    @Test
    void getCommentsByItemWhenNoCommentsThenReturnEmptyList() {
        when(commentRepository.findByItemId(item.getId())).thenReturn(List.of());

        List<CommentDto> result = itemService.getCommentsByItem(item.getId());

        assertThat(result).isEmpty();

        verify(commentRepository).findByItemId(item.getId());
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getCommentsByItemWhenCommentsExistThenReturnListOfCommentDto() {
        User author = new User(2L, "Bob", "bob@mail.com");

        Comment comment1 = new Comment(1L, "Nice!", item, author, LocalDateTime.now().minusDays(1));
        Comment comment2 = new Comment(2L, "Works fine", item, author, LocalDateTime.now().minusHours(5));

        CommentDto commentDto1 = new CommentDto(1L, "Nice!", author.getName(), comment1.getCreated());
        CommentDto commentDto2 = new CommentDto(2L, "Works fine", author.getName(), comment2.getCreated());

        when(commentRepository.findByItemId(item.getId())).thenReturn(List.of(comment1, comment2));
        when(itemMapper.toCommentDto(comment1)).thenReturn(commentDto1);
        when(itemMapper.toCommentDto(comment2)).thenReturn(commentDto2);

        List<CommentDto> result = itemService.getCommentsByItem(item.getId());

        assertThat(result)
                .hasSize(2)
                .containsExactly(commentDto1, commentDto2);

        verify(commentRepository).findByItemId(item.getId());
        verify(itemMapper).toCommentDto(comment1);
        verify(itemMapper).toCommentDto(comment2);
    }

    // --------------------------
    // getCommentsByOwner()
    // --------------------------
    @Test
    void getCommentsByOwnerWhenCommentsExistThenReturnListOfDto() {
        Item anotherItem = new Item();
        anotherItem.setId(20L);
        anotherItem.setOwner(owner);

        User author = new User(2L, "Bob", "bob@mail.com");

        Comment comment1 = new Comment(1L, "Nice", item, author, LocalDateTime.now());
        Comment comment2 = new Comment(2L, "Great", anotherItem, author, LocalDateTime.now());

        CommentDto commentDto1 = new CommentDto(1L, "Nice", author.getName(), LocalDateTime.now());
        CommentDto commentDto2 = new CommentDto(2L, "Great", author.getName(), LocalDateTime.now());

        when(commentRepository.findByItemOwnerId(owner.getId())).thenReturn(List.of(comment1, comment2));
        when(itemMapper.toCommentDto(comment1)).thenReturn(commentDto1);
        when(itemMapper.toCommentDto(comment2)).thenReturn(commentDto2);

        List<CommentDto> result = itemService.getCommentsByOwner(owner.getId());

        assertThat(result).containsExactly(commentDto1, commentDto2);
        verify(commentRepository).findByItemOwnerId(owner.getId());
        verify(itemMapper, times(2)).toCommentDto(any(Comment.class));
    }

    @Test
    void getCommentsByOwnerWhenNoCommentsThenReturnEmptyList() {
        when(commentRepository.findByItemOwnerId(99L)).thenReturn(List.of());

        List<CommentDto> result = itemService.getCommentsByOwner(99L);

        assertThat(result).isEmpty();
        verify(commentRepository).findByItemOwnerId(99L);
        verifyNoInteractions(itemMapper);
    }

    // --------------------------
    // findItemsByRequestId()
    // --------------------------
    @Test
    void findItemsByRequestIdWhenItemsExistThenReturnList() {
        Long requestId = 5L;
        Item item2 = new Item(11L, "Saw", "Sharp saw", true, owner, null);
        List<Item> expectedItems = List.of(item, item2);

        when(itemRepository.findByRequestIdOrderById(requestId)).thenReturn(expectedItems);

        List<Item> result = itemService.findItemsByRequestId(requestId);

        assertThat(result)
                .isNotEmpty()
                .hasSize(2)
                .containsExactly(item, item2);

        verify(itemRepository).findByRequestIdOrderById(requestId);
    }

    @Test
    void findItemsByRequestIdWhenNoItemsThenReturnEmptyList() {
        when(itemRepository.findByRequestIdOrderById(99L)).thenReturn(List.of());

        List<Item> result = itemService.findItemsByRequestId(99L);

        assertThat(result).isEmpty();
        verify(itemRepository).findByRequestIdOrderById(99L);
    }
}