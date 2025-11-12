package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User owner;
    private User author;
    private ItemRequest request;
    private Item item1;
    private Item item2;
    private Comment comment1;
    private Comment comment2;

    @BeforeEach
    void setUp() {
        // создаём пользователей
        owner = userRepository.save(new User(null, "Owner", "owner@mail.ru"));
        author = userRepository.save(new User(null, "Author", "author@mail.ru"));

        // создаём вещи
        item1 = itemRepository.save(new Item(null, "Drill",
                "Electric drill", true, owner, null));
        item2 = itemRepository.save(new Item(null, "Hammer",
                "Heavy hammer", true, owner, null));

        // создаём комментарии
        comment1 = commentRepository.save(new Comment(null, "Nice drill",
                item1, author, LocalDateTime.now()));
        comment2 = commentRepository.save(new Comment(null, "Good hammer",
                item2, author, LocalDateTime.now()));
    }

    // ---------------------------------------------------------
    // findByItemId()
    // ---------------------------------------------------------
    @Test
    void findByItemIdShouldReturnCommentsOfThatItem() {
        List<Comment> comments = commentRepository.findByItemId(item1.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getText()).isEqualTo("Nice drill");
        assertThat(comments.get(0).getItem().getId()).isEqualTo(item1.getId());
        assertThat(comments.get(0).getAuthor().getId()).isEqualTo(author.getId());
    }

    // ---------------------------------------------------------
    // findByItemOwnerId()
    // ---------------------------------------------------------
    @Test
    void findByItemOwnerIdShouldReturnAllCommentsForOwnerItems() {
        List<Comment> comments = commentRepository.findByItemOwnerId(owner.getId());

        // ожидаем два комментария, т.к. оба предмета принадлежат owner
        assertThat(comments).hasSize(2);
        assertThat(comments).extracting(Comment::getText)
                            .containsExactlyInAnyOrder("Nice drill", "Good hammer");

        // проверяем, что оба комментария относятся к предметам этого владельца
        assertThat(comments).allMatch(
                c -> c.getItem().getOwner().getId().equals(owner.getId()));
    }
}
