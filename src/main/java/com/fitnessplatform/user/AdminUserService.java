package com.fitnessplatform.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> findAll(String email) {
        if (email == null || email.isBlank()) {
            return userRepository.findAllByOrderByCreatedAtDescIdAsc();
        }

        return userRepository
                .findByEmailContainingIgnoreCaseOrderByCreatedAtDescIdAsc(
                        email.strip()
                );
    }

    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
