package com.courtier.courtier.user.service;

import com.courtier.courtier.case_.repository.UserCaseRepository;
import com.courtier.courtier.common.exception.CourtierException;
import com.courtier.courtier.notification.repository.NotificationRepository;
import com.courtier.courtier.otp.repository.OtpTokenRepository;
import com.courtier.courtier.user.entity.User;
import com.courtier.courtier.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import com.courtier.courtier.user.dto.CachedUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UserCaseRepository userCaseRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final RedisTemplate<String, CachedUser> cachedUserRedisTemplate;

    private static final String USER_CACHE_PREFIX = "user:email:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(15);

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try cache first — but never let Redis failure break auth
        String cacheKey = USER_CACHE_PREFIX + email;

        try {
            CachedUser cached =
                    cachedUserRedisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
//                log.debug("User cache HIT for: {}", email);

                return User.builder()
                        .id(cached.id())
                        .email(cached.email())
                        .password(cached.password())
                        .fullName(cached.fullName())
                        .role(User.Role.valueOf(cached.role()))
                        .enabled(cached.enabled())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Bad cache entry for {}, evicting", email, e);

            try {
                cachedUserRedisTemplate.delete(cacheKey);
            } catch (Exception ignored) {}

        }

        // Always fall through to DB — this is the source of truth
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Try to cache — but don't fail if Redis is down
        try {

            CachedUser cachedUser = new CachedUser(
                    user.getId(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.isEnabled()
            );

            cachedUserRedisTemplate.opsForValue().set(
                    USER_CACHE_PREFIX + email,
                    cachedUser,
                    USER_CACHE_TTL
            );

            log.debug("User cached: {}", email);
        } catch (Exception e) {
            log.warn("Redis unavailable during user cache write, continuing without cache: {}", e.getMessage());
        }

        return user;
    }

    public void evictUserCache(String email) {
        try {
            cachedUserRedisTemplate.delete(USER_CACHE_PREFIX + email);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache eviction for {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void deleteMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        notificationRepository.deleteAllByUserId(user.getId());
        userCaseRepository.deleteAllByUserId(user.getId());
        otpTokenRepository.deleteAllByEmail(email);
        userRepository.delete(user);

        evictUserCache(email);
        log.info("Deleted account for {}", email);
    }
}