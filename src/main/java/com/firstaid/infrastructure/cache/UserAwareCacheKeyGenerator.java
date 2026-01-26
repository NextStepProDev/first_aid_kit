package com.firstaid.infrastructure.cache;

import com.firstaid.infrastructure.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Custom cache key generator that includes the current user's ID in the cache key.
 * This ensures that cached data is properly isolated per user for multi-tenancy.
 */
@Component("userAwareCacheKeyGenerator")
@RequiredArgsConstructor
public class UserAwareCacheKeyGenerator implements KeyGenerator {

    private final CurrentUserService currentUserService;

    @Override
    @NonNull
    public Object generate(@NonNull Object target, @NonNull Method method, Object @NonNull ... params) {
        Integer userId = currentUserService.getCurrentUserId();

        String paramsKey = Arrays.stream(params)
                .map(Objects::toString)
                .collect(Collectors.joining("_"));

        return "user_" + userId + "_" + method.getName() + "_" + paramsKey;
    }
}
