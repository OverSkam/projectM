package overskam.projectM.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.model.User;
import overskam.projectM.repository.jpa.UserRepository;

import java.util.Optional;
import java.util.UUID;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenVersionServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final TokenVersionService service = new TokenVersionService(userRepository, redis);

    @Test
    void getCurrentVersionReturnsCachedValue() {
        UUID userId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:tv:" + userId)).thenReturn("7");

        long version = service.getCurrentVersion(userId);

        assertThat(version).isEqualTo(7L);
    }

    @Test
    void getCurrentVersionLoadsFromDbAndCachesWhenMissing() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, 4L);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:tv:" + userId)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        long version = service.getCurrentVersion(userId);

        assertThat(version).isEqualTo(4L);
        verify(valueOperations).set(eq("user:tv:" + userId), eq("4"), any(Duration.class));
    }

    @Test
    void getCurrentVersionFallsBackToDbWhenRedisReadFails() {
        UUID userId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:tv:" + userId)).thenThrow(new RedisConnectionFailureException("down"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, 2L)));

        long version = service.getCurrentVersion(userId);

        assertThat(version).isEqualTo(2L);
    }

//    @Test
//    void bumpVersionPersistsAndCachesIncrementedVersion() {
//        UUID userId = UUID.randomUUID();
//        User user = user(userId, 9L);
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(redis.opsForValue()).thenReturn(valueOperations);
//
//        long version = service.bumpVersion(userId);
//
//        assertThat(version).isEqualTo(10L);
//        assertThat(user.getTokenVersion()).isEqualTo(10L);
//        verify(userRepository).save(user);
//        verify(valueOperations).set(eq("user:tv:" + userId), eq("10"), any(Duration.class));
//    }

    @Test
    void bumpVersionThrowsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bumpVersion(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    private static User user(UUID id, long tokenVersion) {
        User user = new User();
        user.setId(id);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
