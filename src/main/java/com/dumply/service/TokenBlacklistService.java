package com.dumply.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expirationTimeInMs) {
        redisTemplate.opsForValue().set(token, "blacklisted",
                Duration.ofMillis(expirationTimeInMs));
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }
}
