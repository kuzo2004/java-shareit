package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void existsByEmailShouldReturnTrueIfEmailExists() {
        User user = new User(null, "Alice", "alice@mail.ru");
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("alice@mail.ru");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailShouldReturnFalseIfEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("notfound@mail.ru");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnTrueIfEmailBelongsToAnotherUser() {
        User user1 = userRepository.save(new User(null, "Bob", "bob@mail.ru"));
        User user2 = userRepository.save(new User(null, "John", "john@mail.ru"));

        boolean exists = userRepository.existsByEmailAndIdNot("bob@mail.ru", user2.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnFalseIfEmailBelongsToSameUser() {
        User user = userRepository.save(new User(null, "Bob", "bob@mail.ru"));

        boolean exists = userRepository.existsByEmailAndIdNot("bob@mail.ru", user.getId());

        assertThat(exists).isFalse();
    }
}