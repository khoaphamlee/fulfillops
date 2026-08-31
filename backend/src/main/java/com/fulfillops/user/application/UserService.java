package com.fulfillops.user.application;

import com.fulfillops.user.domain.User;
import com.fulfillops.user.infrastructure.UserRepository;
import com.fulfillops.user.presentation.CreateUserRequest;
import com.fulfillops.user.presentation.UserResponse;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String USER_EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User user = User.create(canonicalizeEmail(request.email()), request.displayName());

        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (isUserEmailUniqueViolation(exception)) {
                throw new UserEmailAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(findExistingUser(id));
    }

    @Transactional(readOnly = true)
    public void requireExistingUser(UUID id) {
        findExistingUser(id);
    }

    private User findExistingUser(UUID id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private String canonicalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isUserEmailUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getKind() == ConstraintKind.UNIQUE
                    && USER_EMAIL_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
