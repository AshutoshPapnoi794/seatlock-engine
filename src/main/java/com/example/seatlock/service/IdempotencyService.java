package com.example.seatlock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:key:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private String buildKey(String idempotencyKey){
        return IDEMPOTENCY_PREFIX + idempotencyKey;
    }

    public <T> T getCachedResponse(String idempotencyKey, Class<T> responseType){
        String cachedJson = redisTemplate.opsForValue().get(buildKey(idempotencyKey));

        if(cachedJson==null){
            return null;
        }

        try {
            log.info("Idempotency hit for key [{}]. Returning cached response.", idempotencyKey);
            return objectMapper.readValue(cachedJson,responseType);
        }catch (Exception e){
            log.error("Failed to deserialize cached idempotency payload for key: {}", idempotencyKey,e);
            return null;
        }
    }

    public void saveResponse(String idempotencyKey, Object responseObject){
        try {
            String json = objectMapper.writeValueAsString(responseObject);
            redisTemplate.opsForValue().set(buildKey(idempotencyKey),json,IDEMPOTENCY_TTL);
            log.info("Cached idempotency response for key [{}] (TTL: 24h)", idempotencyKey);
        }catch (Exception e){
            log.error("Failed to serialize response for idempotency key: {}", idempotencyKey, e);
        }
    }

}
