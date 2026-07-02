package overskam.projectM.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import overskam.projectM.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
