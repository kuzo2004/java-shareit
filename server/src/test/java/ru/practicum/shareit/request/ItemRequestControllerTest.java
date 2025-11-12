package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoPost;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestService itemRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemRequestDtoPost postDto;
    private ItemRequestDto dto;
    private Long requesterId;

    @BeforeEach
    void setup() {
        requesterId = 1L;
        postDto = new ItemRequestDtoPost("Need a drill");
        dto = new ItemRequestDto(1L, "Need a drill", LocalDateTime.now(), List.of());
    }

    // --------------------------------------------------------
    // POST /requests
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void createRequestShouldReturnCreatedRequest() {
        Mockito.when(itemRequestService.createRequest(any(ItemRequestDtoPost.class), eq(requesterId)))
               .thenReturn(dto);

        mockMvc.perform(post("/requests")
                       .contentType(MediaType.APPLICATION_JSON)
                       .header("X-Sharer-User-Id", requesterId)
                       .content(objectMapper.writeValueAsString(postDto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(dto.getId()))
               .andExpect(jsonPath("$.description").value(dto.getDescription()));

        verify(itemRequestService).createRequest(any(ItemRequestDtoPost.class), eq(requesterId));
    }

    @Test
    @SneakyThrows
    void createRequestShouldReturnBadRequestOnValidationException() {
        Mockito.when(itemRequestService.createRequest(any(), anyLong()))
               .thenThrow(new ValidationException("Invalid request"));

        mockMvc.perform(post("/requests")
                       .contentType(MediaType.APPLICATION_JSON)
                       .header("X-Sharer-User-Id", requesterId)
                       .content(objectMapper.writeValueAsString(postDto)))
               .andExpect(status().isBadRequest());

        verify(itemRequestService).createRequest(any(), eq(requesterId));
    }

    // --------------------------------------------------------
    // GET /requests
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getMyRequestsShouldReturnList() {
        Mockito.when(itemRequestService.getMyRequests(requesterId))
               .thenReturn(List.of(dto));

        mockMvc.perform(get("/requests")
                       .header("X-Sharer-User-Id", requesterId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].description").value(dto.getDescription()));

        verify(itemRequestService).getMyRequests(requesterId);
    }

    // --------------------------------------------------------
    // GET /requests/all
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getAllRequestsShouldReturnList() {
        Mockito.when(itemRequestService.getAllRequests(requesterId))
               .thenReturn(List.of(dto));

        mockMvc.perform(get("/requests/all")
                       .header("X-Sharer-User-Id", requesterId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].id").value(dto.getId()));

        verify(itemRequestService).getAllRequests(requesterId);
    }

    // --------------------------------------------------------
    // GET /requests/{requestId}
    // --------------------------------------------------------
    @Test
    @SneakyThrows
    void getRequestByIdShouldReturnRequest() {
        Mockito.when(itemRequestService.getRequestById(1L, requesterId))
               .thenReturn(dto);

        mockMvc.perform(get("/requests/1")
                       .header("X-Sharer-User-Id", requesterId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(dto.getId()))
               .andExpect(jsonPath("$.description").value(dto.getDescription()));

        verify(itemRequestService).getRequestById(1L, requesterId);
    }

    @Test
    @SneakyThrows
    void getRequestByIdShouldReturnNotFound() {
        Long requestId = 99L;
        Mockito.when(itemRequestService.getRequestById(anyLong(), anyLong()))
               .thenThrow(new NotFoundException("Запрос с id=" + requestId + " не существует"));

        mockMvc.perform(get("/requests/{requestId}", requestId)
                       .header("X-Sharer-User-Id", requesterId))
               .andExpect(status().isNotFound());

        verify(itemRequestService).getRequestById(requestId, requesterId);
    }
}
