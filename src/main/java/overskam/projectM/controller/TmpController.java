package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class TmpController {
    
    @PostMapping
    public void mongoTest(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        log.info("We are here...");
    }
}
