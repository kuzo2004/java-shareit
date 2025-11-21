package ru.practicum.shareit.item;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentDtoPost;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoPost;
import ru.practicum.shareit.item.dto.ItemInfoDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ItemServiceImplTestIT {

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemMapper itemMapper;

    private User owner;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User(null, "Owner", "owner@mail.ru");
        owner = userRepository.save(owner);

        item = new Item(null, "Item1", "Description", true, owner,
                null);
        item = itemRepository.save(item);
    }

    @AfterEach
    void clear() {
        commentRepository.deleteAll();
        bookingRepository.deleteAll();
        itemRepository.deleteAll();
        itemRequestRepository.deleteAll();
        userRepository.deleteAll();

    }

    // ---------------------- CREATE ITEM -------------------------
    @Test
    void createItemShouldCreateNewItem() {
        ItemDtoPost postDto = new ItemDtoPost("Drill", "Cordless drill",
                true, null);
        ItemDto created = itemService.createItem(postDto, owner.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Drill");
        assertThat(created.getDescription()).isEqualTo("Cordless drill");
        assertThat(itemRepository.findById(created.getId())).isPresent();
    }

    // ---------------------- UPDATE ITEM -------------------------
    @Test
    void updateItemShouldUpdateExistingItem() {
        ItemDto updateDto = new ItemDto(null, "Updated name",
                "Updated desc", true, null, null);

        ItemDto updated = itemService.updateItem(item.getId(), updateDto, owner.getId());

        assertThat(updated.getId()).isEqualTo(item.getId());
        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getDescription()).isEqualTo("Updated desc");
    }

    // ---------------------- GET ITEM BY ID -----------------------
    @Test
    void getItemByIdShouldReturnOptional() {
        Optional<Item> found = itemService.getItemById(item.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Item1");
    }

    // ---------------------- GET ITEM DTO BY ID -------------------
    @Test
    void getItemDtoByIdShouldReturnDtoWithComments() {
        Comment comment = new Comment(null, "Nice item", item, owner, LocalDateTime.now());
        commentRepository.save(comment);

        ItemInfoDto dto = itemService.getItemDtoById(item.getId());

        assertThat(dto.getId()).isEqualTo(item.getId());
        assertThat(dto.getComments()).hasSize(1);
        assertThat(dto.getComments().get(0).getText()).isEqualTo("Nice item");
    }

    // ---------------------- GET ALL ITEMS BY OWNER ---------------
    @Test
    void getAllItemsByOwnerShouldReturnItemsList() {
        List<ItemInfoDto> items = itemService.getAllItemsByOwner(owner.getId());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Item1");
    }

    // ---------------------- SEARCH ITEMS -------------------------
    @Test
    void searchItemsShouldReturnMatchingItems() {
        List<ItemDto> found = itemService.searchItems("desc");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Item1");
    }

    @Test
    void searchItemsShouldReturnEmptyForBlankText() {
        List<ItemDto> found = itemService.searchItems("   ");
        assertThat(found).isEmpty();
    }

    // ---------------------- ADD COMMENT --------------------------
    @Test
    void addCommentShouldSaveCommentWhenUserHadBooking() {
        // создаём "арендатора"
        User booker = userRepository.save(new User(null, "Booker", "booker@mail.ru"));

        // добавляем прошедшее бронирование для booker
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(LocalDateTime.now().minusDays(3));
        booking.setEnd(LocalDateTime.now().minusDays(1));
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        CommentDtoPost commentPost = new CommentDtoPost("Отличная вещь!");

        CommentDto saved = itemService.addComment(item.getId(), commentPost, booker.getId());

        assertThat(saved.getText()).isEqualTo("Отличная вещь!");
        assertThat(saved.getAuthorName()).isEqualTo("Booker");
        assertThat(commentRepository.findAll()).hasSize(1);
    }

    // ---------------------- GET COMMENTS BY ITEM -----------------
    @Test
    void getCommentsByItemShouldReturnAllCommentsForItem() {
        Comment comment = commentRepository.save(new Comment(null, "Text", item, owner, LocalDateTime.now()));

        List<CommentDto> comments = itemService.getCommentsByItem(item.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getText()).isEqualTo("Text");
    }

    // ---------------------- GET COMMENTS BY OWNER ----------------
    @Test
    void getCommentsByOwnerShouldReturnAllCommentsForOwnerItems() {
        commentRepository.save(new Comment(null, "Comment text", item, owner, LocalDateTime.now()));

        List<CommentDto> comments = itemService.getCommentsByOwner(owner.getId());

        assertThat(comments).hasSize(1);
    }

    // ---------------------- FIND ITEMS BY REQUEST ID -------------
    @Test
    void findItemsByRequestIdShouldReturnItemsLinkedToRequest() {
        User requester = new User(null, "requester", "requester@mail.ru");
        requester = userRepository.save(requester);

        ItemRequest request = new ItemRequest(null, "Need chair", requester, LocalDateTime.now());
        request = itemRequestRepository.save(request);


        Item item2 = new Item(null, "Chair", "Wooden chair", true, owner, request);
        itemRepository.save(item2);

        List<Item> found = itemService.findItemsByRequestId(request.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Chair");
    }
}
