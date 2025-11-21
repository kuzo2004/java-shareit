package ru.practicum.shareit.request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestMapper itemRequestMapper;
    private final ItemService itemService;
    private final ItemMapper itemMapper;


    @Override
    @Transactional
    public ItemRequestDto createRequest(ItemRequestDtoPost itemRequestDtoPost, Long requesterId) {
        User requester = userService.getUserById(requesterId)
                                    .orElseThrow(() ->
                                            new NotFoundException("Пользователь с id=" + requesterId +
                                                    " не существует"));

        ItemRequest itemRequest = itemRequestMapper.toItemRequest(itemRequestDtoPost, requester);
        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);

        log.info("Создан запрос на вещь: id={}, пользователь id={}", savedRequest.getId(), requesterId);
        return itemRequestMapper.toItemRequestDto(savedRequest);
    }

    @Override
    public List<ItemRequestDto> getMyRequests(Long requesterId) {
        if (!userService.existsById(requesterId)) {
            throw new NotFoundException("Пользователь с id=" + requesterId + " не существует");
        }

        List<ItemRequest> requests = itemRequestRepository
                .findAllByRequesterIdOrderByCreatedDesc(requesterId);
        return buildRequestDtoWithItems(requests);
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long requesterId) {
        if (!userService.existsById(requesterId)) {
            throw new NotFoundException("Пользователь с id=" + requesterId + " не существует");
        }

        List<ItemRequest> requests = itemRequestRepository
                .findAllByRequesterIdNotOrderByCreatedDesc(requesterId);
        return buildRequestDtoWithItems(requests);
    }

    @Override
    public ItemRequestDto getRequestById(Long requestId, Long userId) {
        if (!userService.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не существует");
        }

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                                                       .orElseThrow(() ->
                                                               new NotFoundException("Запрос с id=" + requestId +
                                                                       " не существует"));

        ItemRequestDto dto = itemRequestMapper.toItemRequestDto(itemRequest);
        dto.setItems(itemService.findItemsByRequestId(requestId).stream()
                                .map(itemMapper::toItemDtoForRequest)
                                .collect(Collectors.toList()));

        return dto;
    }

    private List<ItemRequestDto> buildRequestDtoWithItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        // Собираем id всех запросов
        List<Long> requestIds = requests.stream()
                                        .map(ItemRequest::getId)
                                        .toList();

        // Загружаем все вещи одним запросом
        List<Item> allItems = itemRepository.findAllByRequestIdOrderById(requestIds);

        // Группируем вещи по ID запроса, чтобы потом быстро находить список вещей для каждого запроса
        // Ключ — ID запроса, значение — список DTO вещей, относящихся к этому запросу
        Map<Long, List<ItemDtoForRequest>> itemsByRequest = allItems.stream()
                            .collect(Collectors.groupingBy(
                                    i -> i.getRequest().getId(), // группируем по ID запроса

                                    // вложенный коллектор, который:
                                    //сначала применяет функцию itemMapper к каждому элементу группы,
                                    //потом собирает результаты в List
                                    Collectors.mapping(
                                            itemMapper::toItemDtoForRequest,
                                            Collectors.toList())
                            ));
        // Собираем DTO
        return requests.stream()
                       .map(request -> {
                           // Преобразуем сущность запроса в DTO
                           ItemRequestDto dto = itemRequestMapper.toItemRequestDto(request);

                           // Устанавливаем список вещей для этого запроса, если их нет — подставляем пустой список
                           dto.setItems(itemsByRequest.getOrDefault(request.getId(), List.of()));
                           return dto;
                       })
                       .collect(Collectors.toList());
    }
}