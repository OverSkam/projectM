package overskam.projectM;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class AbstractIntegrationTest {
    
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");
    
    static {
        Startables.deepStart(postgres, mongo).join();
    }
}