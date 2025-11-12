package ru.practicum.shareit.request;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;

import java.util.List;


@Slf4j
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto createRequest(@RequestBody ItemRequestDtoPost itemRequestDtoPost,
                                            @RequestHeader("X-Sharer-User-Id") Long requesterId) {
        log.info("POST /requests by user {}", requesterId);
        return itemRequestService.createRequest(itemRequestDtoPost, requesterId);
    }

    @GetMapping
    public List<ItemRequestDto> getMyRequests(@RequestHeader("X-Sharer-User-Id") Long requesterId) {
        log.info("GET /requests by user {}", requesterId);
        return itemRequestService.getMyRequests(requesterId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAllRequests(@RequestHeader("X-Sharer-User-Id") Long requesterId) {
        log.info("GET /requests/all by user {}", requesterId);
        return itemRequestService.getAllRequests(requesterId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getRequestById(@PathVariable Long requestId,
                                         @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /requests/{} by user {}", requestId, userId);
        return itemRequestService.getRequestById(requestId, userId);
    }
}