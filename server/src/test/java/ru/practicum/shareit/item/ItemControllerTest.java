package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------- CREATE ITEM ----------------------
    @Test
    @SneakyThrows
    void createItemShouldReturnCreatedItem() {
        ItemDtoPost postDto = new ItemDtoPost("Drill", "Hand drill",
                true,null);
        ItemDto returnedDto = new ItemDto(1L, "Drill", "Hand drill",
                true, null, null);

        Mockito.when(itemService.createItem(any(ItemDtoPost.class), eq(1L)))
               .thenReturn(returnedDto);

        mockMvc.perform(post("/items")
                       .header("X-Sharer-User-Id", 1L)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(postDto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(returnedDto.getId()))
               .andExpect(jsonPath("$.name").value(returnedDto.getName()))
               .andExpect(jsonPath("$.description").value(returnedDto.getDescription()))
               .andExpect(jsonPath("$.available").value(returnedDto.getAvailable()));

        Mockito.verify(itemService).createItem(any(ItemDtoPost.class), eq(1L));
    }

    // ---------------------- UPDATE ITEM ----------------------
    @Test
    @SneakyThrows
    void updateItemShouldReturnUpdatedItem() {
        ItemDto updateDto = new ItemDto(1L, "Drill Updated",
                "Updated description", true, null, null);
        Mockito.when(itemService.updateItem(eq(1L), any(ItemDto.class), eq(1L)))
               .thenReturn(updateDto);

        mockMvc.perform(patch("/items/1")
                       .header("X-Sharer-User-Id", 1L)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(updateDto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(updateDto.getId()))
               .andExpect(jsonPath("$.name").value(updateDto.getName()))
               .andExpect(jsonPath("$.description").value(updateDto.getDescription()))
               .andExpect(jsonPath("$.available").value(updateDto.getAvailable()));

        Mockito.verify(itemService).updateItem(eq(1L), any(ItemDto.class), eq(1L));
    }

    // ---------------------- GET ITEM ----------------------
    @Test
    @SneakyThrows
    void getItemShouldReturnItemInfo() {
        ItemInfoDto infoDto = new ItemInfoDto(1L, "Drill", "Cordless drill",
                true, null, null, null, List.of());
        Mockito.when(itemService.getItemDtoById(1L))
               .thenReturn(infoDto);

        mockMvc.perform(get("/items/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(infoDto.getId()))
               .andExpect(jsonPath("$.name").value(infoDto.getName()))
               .andExpect(jsonPath("$.description").value(infoDto.getDescription()))
               .andExpect(jsonPath("$.available").value(infoDto.getAvailable()));

        Mockito.verify(itemService).getItemDtoById(1L);
    }

    // ---------------------- GET ALL ITEMS BY OWNER ----------------------
    @Test
    @SneakyThrows
    void getAllItemsByOwnerShouldReturnList() {
        ItemInfoDto item1 = new ItemInfoDto(1L, "Drill", "Hand drill",
                true,null, null, null, List.of());
        ItemInfoDto item2 = new ItemInfoDto(2L, "Hammer", "Heavy hammer",
                true,null, null, null, List.of());

        Mockito.when(itemService.getAllItemsByOwner(1L))
               .thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/items")
                       .header("X-Sharer-User-Id", 1L))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].name").value("Drill"))
               .andExpect(jsonPath("$[1].name").value("Hammer"));

        Mockito.verify(itemService).getAllItemsByOwner(1L);
    }

    // ---------------------- SEARCH ITEMS ----------------------
    @Test
    @SneakyThrows
    void searchItemsShouldReturnMatchingItems() {
        ItemDto item = new ItemDto(1L, "Drill", "Cordless drill",
                true, null, null);

        Mockito.when(itemService.searchItems("drill"))
               .thenReturn(List.of(item));

        mockMvc.perform(get("/items/search")
                       .param("text", "drill"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].name").value("Drill"));

        Mockito.verify(itemService).searchItems("drill");
    }

    // ---------------------- ADD COMMENT ----------------------
    @Test
    @SneakyThrows
    void addCommentShouldReturnCreatedComment() {
        CommentDtoPost post = new CommentDtoPost("Nice tool");
        CommentDto commentDto = new CommentDto(1L, "Nice tool",
                "Alice", LocalDateTime.now());

        Mockito.when(itemService.addComment(eq(1L), any(CommentDtoPost.class), eq(1L)))
               .thenReturn(commentDto);

        mockMvc.perform(post("/items/1/comment")
                       .header("X-Sharer-User-Id", 1L)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(post)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(commentDto.getId()))
               .andExpect(jsonPath("$.text").value(commentDto.getText()));

        Mockito.verify(itemService).addComment(eq(1L), any(CommentDtoPost.class), eq(1L));
    }

    // ---------------------- GET COMMENTS BY ITEM ----------------------
    @Test
    @SneakyThrows
    void getCommentsByItemShouldReturnList() {
        CommentDto comment = new CommentDto(1L, "Nice tool",
                "Alice", LocalDateTime.now());

        Mockito.when(itemService.getCommentsByItem(1L))
               .thenReturn(List.of(comment));

        mockMvc.perform(get("/items/1/comment"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1)) // возвращает размер массива
               .andExpect(jsonPath("$[0].text").value("Nice tool"));

        Mockito.verify(itemService).getCommentsByItem(1L);
    }

    // ---------------------- GET COMMENTS BY OWNER ----------------------
    @Test
    @SneakyThrows
    void getCommentsByOwnerShouldReturnList() {
        CommentDto comment = new CommentDto(1L, "Nice tool",
                "Alice", LocalDateTime.now());

        Mockito.when(itemService.getCommentsByOwner(1L))
               .thenReturn(List.of(comment));

        mockMvc.perform(get("/items/comment")
                       .header("X-Sharer-User-Id", 1L))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].text").value("Nice tool"));

        Mockito.verify(itemService).getCommentsByOwner(1L);
    }
}