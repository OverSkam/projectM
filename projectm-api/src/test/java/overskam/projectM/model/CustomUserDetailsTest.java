package overskam.projectM.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void exposesUserAsSpringSecurityUserDetails() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setPassword("encoded");
        user.setEnabled(false);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUser()).isSameAs(user);
        assertThat(details.getUsername()).isEqualTo("user@test.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).isEmpty();
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }
}
