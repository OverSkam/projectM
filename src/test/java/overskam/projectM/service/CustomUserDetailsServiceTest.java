package overskam.projectM.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserByUsernameReturnsCustomUserDetails() {
        User user = new User();
        user.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        CustomUserDetails result = (CustomUserDetails) service.loadUserByUsername("user@test.com");

        assertThat(result.getUser()).isSameAs(user);
    }

    @Test
    void loadUserByUsernameThrowsWhenUserMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found by email");
    }
}
