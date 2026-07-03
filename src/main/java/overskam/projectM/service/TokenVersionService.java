package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.model.User;
import overskam.projectM.repository.UserRepository;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class TokenVersionService {
    private static final String KEY_PREFIX = "user:tv:";
    private static final Duration CACHE_TTL = Duration.ofDays(8);

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;

    public long getCurrentVersion(UUID userId) {
        String key = KEY_PREFIX + userId;
    
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null)
                return Long.parseLong(cached);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable on read for userId={}, falling back to DB", userId, e);
            return loadTokenFromDB(userId);
        }
        
        long fromDb = loadTokenFromDB(userId);
        try {
            redis.opsForValue().set(key, Long.toString(fromDb), CACHE_TTL);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable on cache populate for userId={}", userId, e);
        }
        return fromDb;
    }
    
    @Transactional
    public long bumpVersion(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        long newTokenVersion = user.getTokenVersion() + 1;
        user.setTokenVersion(newTokenVersion);
        userRepository.save(user);
        
        String key = KEY_PREFIX + userId;
        try {
            redis.opsForValue().set(key, Long.toString(newTokenVersion), CACHE_TTL);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable on bump for userId={}; cache will repopulate from DB on next read", userId, e);
        }
        log.info("Bumped tokenVersion for userId={} to {}", userId, newTokenVersion);
        return newTokenVersion;
    }
    
    private long loadTokenFromDB(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getTokenVersion();
    }
}
