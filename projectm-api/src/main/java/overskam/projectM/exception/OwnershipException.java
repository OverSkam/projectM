package overskam.projectM.exception;

import org.springframework.http.HttpStatus;

public class OwnershipException extends ApiException {
    public OwnershipException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
