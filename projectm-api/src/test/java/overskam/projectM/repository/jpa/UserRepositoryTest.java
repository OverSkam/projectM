package overskam.projectM.repository.jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.AbstractIntegrationTest;
import overskam.projectM.config.JpaAuditingConfig;
import overskam.projectM.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest extends AbstractIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Counts every logout when many arrive at the same moment")
    void incrementsTokenVersionExactlyOncePerConcurrentCall() throws Exception {
        User user = new User();
        user.setEmail("email@test.com");
        user.setPassword("longPassword");
        UUID userId = userRepository.save(user).getId();
        
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                userRepository.incrementTokenVersion(userId);
                return null;
            }));
        }
        
        startGate.countDown();
        
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        
        pool.shutdown();
        
        long result = userRepository.findById(userId).get().getTokenVersion();
        
        assertEquals(20L, result);
    }
    
    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }
}
