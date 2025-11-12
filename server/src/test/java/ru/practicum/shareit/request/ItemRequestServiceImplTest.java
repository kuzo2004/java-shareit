package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDtoForRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.model.ItemRequestMapper;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemRequestMapper itemRequestMapper;

    @Mock
    private ItemService itemService;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    private User requester;
    private User owner;
    private ItemRequestDtoPost requestDtoPost;
    private ItemRequestDto requestDto;
    private ItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        requester = new User(1L, "Alice", "alice@mail.com");
        owner = new User(2L, "Bob", "bob@mail.com");
        requestDtoPost = new ItemRequestDtoPost("Need a drill");
        itemRequest = new ItemRequest(null, "Need a drill", requester, LocalDateTime.now());
        requestDto = new ItemRequestDto(10L, "Need a drill", LocalDateTime.now(), null);
    }

    // --------------------------
    //  createRequest(ItemRequestDtoPost itemRequestDtoPost, Long requesterId)
    // --------------------------
    @Test
    void createRequestWhenUserDoesNotExistThenThrowNotFoundException() {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemRequestService.createRequest(requestDtoPost, 99L)
        );

        assertThat(exception.getMessage()).isEqualTo("Пользователь с id=99 не существует");
        verifyNoInteractions(itemRequestRepository);
    }

    @Test
    void createRequestWhenUserExistsThenReturnDto() {
        when(userService.getUserById(requester.getId())).thenReturn(Optional.of(requester));
        when(itemRequestMapper.toItemRequest(requestDtoPost, requester)).thenReturn(itemRequest);

        when(itemRequestRepository.save(any(ItemRequest.class)))
                .thenAnswer(invocation -> {
                    ItemRequest saved = invocation.getArgument(0);
                    saved.setId(10L); // имитируем присвоение ID при сохранении
                    return saved;
                });
        when(itemRequestMapper.toItemRequestDto(any(ItemRequest.class))).thenReturn(requestDto);

        ItemRequestDto result = itemRequestService.createRequest(requestDtoPost, requester.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("Need a drill");

        verify(userService).getUserById(requester.getId());
        verify(itemRequestMapper).toItemRequest(requestDtoPost, requester);
        verify(itemRequestRepository).save(itemRequest);
        verify(itemRequestMapper).toItemRequestDto(itemRequest);
    }

    // ---------------------------------
    //  getMyRequests(Long requesterId)
    // ---------------------------------
    @Test
    void getMyRequestsWhenUserDoesNotExistThenThrowNotFoundException() {
        when(userService.existsById(requester.getId())).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemRequestService.getMyRequests(requester.getId())
        );

        assertEquals("Пользователь с id=" + requester.getId()
                + " не существует", exception.getMessage());
        verifyNoInteractions(itemRequestRepository, itemRepository, itemMapper, itemRequestMapper);
    }

    @Test
    void getMyRequestsWhenNoRequestsThenReturnEmptyList() {
        when(userService.existsById(requester.getId())).thenReturn(true);
        when(itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(requester.getId()))
                .thenReturn(List.of());

        List<ItemRequestDto> result = itemRequestService.getMyRequests(requester.getId());

        assertNotNull(result); // метод всегда возвращает коллекцию, даже если она пустая
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyRequestsWhenRequestsExistThenReturnListWithItems() {
        ItemRequest request1 = new ItemRequest(10L, "Need a drill",
                requester, LocalDateTime.now().minusDays(2));
        ItemRequest request2 = new ItemRequest(11L, "Need a saw",
                requester, LocalDateTime.now().minusDays(1));

        ItemRequestDto dto1 = new ItemRequestDto(10L, "Need a drill",
                request1.getCreated(), List.of());
        ItemRequestDto dto2 = new ItemRequestDto(11L, "Need a saw",
                request2.getCreated(), List.of());

        when(userService.existsById(requester.getId())).thenReturn(true);
        when(itemRequestRepository.findAllByRequesterIdOrderByCreatedDesc(requester.getId()))
                .thenReturn(List.of(request1, request2));
        when(itemRequestMapper.toItemRequestDto(request1)).thenReturn(dto1);
        when(itemRequestMapper.toItemRequestDto(request2)).thenReturn(dto2);
        when(itemRepository.findAllByRequestIdOrderById(List.of(10L, 11L))).thenReturn(List.of());

        List<ItemRequestDto> result = itemRequestService.getMyRequests(requester.getId());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(dto1, dto2)));

        verify(itemRequestRepository).findAllByRequesterIdOrderByCreatedDesc(requester.getId());
        verify(itemRequestMapper).toItemRequestDto(request1);
        verify(itemRequestMapper).toItemRequestDto(request2);
        verify(itemRepository).findAllByRequestIdOrderById(List.of(10L, 11L));
    }

    // ---------------------------------
    //  getAllRequests(Long requesterId)
    // ---------------------------------
    @Test
    void getAllRequestsWhenUserDoesNotExistThenThrowNotFoundException() {
        long requesterId = 99L;

        when(userService.existsById(requesterId)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemRequestService.getAllRequests(requesterId)
        );

        assertEquals("Пользователь с id=" + requesterId + " не существует", exception.getMessage());
        verifyNoInteractions(itemRequestRepository);
    }

    @Test
    void getAllRequestsWhenRequestsExistThenReturnListWithItems() {
        ItemRequest request1 = new ItemRequest(10L, "Need a drill",
                requester, LocalDateTime.now().minusDays(2));
        ItemRequest request2 = new ItemRequest(11L, "Need a saw",
                requester, LocalDateTime.now().minusDays(1));

        ItemRequestDto dto1 = new ItemRequestDto(10L, "Need a drill",
                request1.getCreated(), List.of());
        ItemRequestDto dto2 = new ItemRequestDto(11L, "Need a saw",
                request2.getCreated(), List.of());

        when(userService.existsById(requester.getId())).thenReturn(true);
        when(itemRequestRepository.findAllByRequesterIdNotOrderByCreatedDesc(requester.getId()))
                .thenReturn(List.of(request1, request2));
        when(itemRequestMapper.toItemRequestDto(request1)).thenReturn(dto1);
        when(itemRequestMapper.toItemRequestDto(request2)).thenReturn(dto2);
        // Заглушка для itemRepository: пока список вещей пуст
        when(itemRepository.findAllByRequestIdOrderById(List.of(10L, 11L))).thenReturn(List.of());

        List<ItemRequestDto> result = itemRequestService.getAllRequests(requester.getId());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(dto1, dto2)));

        verify(itemRequestRepository).findAllByRequesterIdNotOrderByCreatedDesc(requester.getId());
        verify(itemRequestMapper).toItemRequestDto(request1);
        verify(itemRequestMapper).toItemRequestDto(request2);
        verify(itemRepository).findAllByRequestIdOrderById(List.of(10L, 11L));
    }

    // ---------------------------------
    //  getRequestById(Long requestId, Long userId)
    // ---------------------------------
    @Test
    void getRequestByIdWhenUserDoesNotExistThenThrowNotFoundException() {
        long userId = 99L;
        long requestId = 10L;

        when(userService.existsById(userId)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemRequestService.getRequestById(requestId, userId)
        );

        assertEquals("Пользователь с id=" + userId +
                " не существует", exception.getMessage());
        verifyNoInteractions(itemRequestRepository, itemService);
    }

    @Test
    void getRequestByIdWhenRequestDoesNotExistThenThrowNotFoundException() {
        long requestId = 10L;
        long userId = requester.getId();

        when(userService.existsById(userId)).thenReturn(true);
        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemRequestService.getRequestById(requestId, userId)
        );

        assertEquals("Запрос с id=" + requestId +
                " не существует", exception.getMessage());
        verifyNoInteractions(itemService);
    }

    @Test
    void getRequestByIdWhenRequestExistsThenReturnDtoWithItems() {
        long requestId = 10L;
        long userId = requester.getId();

        ItemRequest request = new ItemRequest(requestId, "Need a drill",
                requester, LocalDateTime.now().minusDays(1));
        ItemRequestDto dto = new ItemRequestDto(requestId, "Need a drill",
                request.getCreated(), List.of());

        Item item1 = new Item(1L, "Drill", "Powerful drill", true,
                requester, request);
        Item item2 = new Item(2L, "Hammer", "Heavy hammer", true,
                requester, request);

        ItemDtoForRequest itemDto1 = new ItemDtoForRequest(1L, "Drill", owner.getId());
        ItemDtoForRequest itemDto2 = new ItemDtoForRequest(2L, "Hammer", owner.getId());

        when(userService.existsById(userId)).thenReturn(true);
        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(itemRequestMapper.toItemRequestDto(request)).thenReturn(dto);
        when(itemService.findItemsByRequestId(requestId)).thenReturn(List.of(item1, item2));
        when(itemMapper.toItemDtoForRequest(item1)).thenReturn(itemDto1);
        when(itemMapper.toItemDtoForRequest(item2)).thenReturn(itemDto2);

        ItemRequestDto result = itemRequestService.getRequestById(requestId, userId);

        assertNotNull(result);
        assertEquals(requestId, result.getId());
        assertEquals(dto.getDescription(), result.getDescription());
        assertEquals(2, result.getItems().size());
        assertTrue(result.getItems().containsAll(List.of(itemDto1, itemDto2)));

        verify(itemRequestRepository).findById(requestId);
        verify(itemService).findItemsByRequestId(requestId);
        verify(itemMapper).toItemDtoForRequest(item1);
        verify(itemMapper).toItemDtoForRequest(item2);
        verify(itemRequestMapper).toItemRequestDto(request);
    }
}