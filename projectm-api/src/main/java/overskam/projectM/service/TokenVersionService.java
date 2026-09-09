package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.repository.jpa.UserRepository;

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
    
    public void bumpVersion(UUID userId) {
        if (userRepository.incrementTokenVersion(userId) == 0)
            throw new NotFoundException("User not found");
        
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    evictCache(userId);
                }
            });
        else
            evictCache(userId);
        
        log.info("Bumped tokenVersion for userId={}", userId);
    }
    
    private void evictCache(UUID userId) {
        try {
            redis.delete(KEY_PREFIX + userId);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable on bump for userId={}; cache will repopulate from DB", userId, e);
        }
    }
    
    private long loadTokenFromDB(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getTokenVersion();
    }
}
