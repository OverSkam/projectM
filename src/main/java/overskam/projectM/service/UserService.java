package overskam.projectM.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import overskam.projectM.repository.UserRepository;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
