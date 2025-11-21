package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto createRequest(ItemRequestDtoPost itemRequestDtoPost, Long requesterId);

    List<ItemRequestDto> getMyRequests(Long requesterId);

    List<ItemRequestDto> getAllRequests(Long requesterId);

    ItemRequestDto getRequestById(Long requestId, Long userId);
}
