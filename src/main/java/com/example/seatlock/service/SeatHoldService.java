package com.example.seatlock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class SeatHoldService {
    private final StringRedisTemplate redisTemplate;

    private static final String HOLD_KEY_PREFIX= "seat:hold:";

    private String buildKey(Long showId, Long seatId){
        return HOLD_KEY_PREFIX + showId + ":" + seatId;
    }

    public boolean acquiresSeatHold(Long showId, List<Long> seatIds, String userEmail, int ttlMinutes){
        List<String> acquiredKeys = new ArrayList<>();

        for (Long seatId: seatIds){
            String key = buildKey(showId,seatId);

            Boolean success = redisTemplate
                    .opsForValue()
                    .setIfAbsent(key,userEmail, Duration.ofMinutes(ttlMinutes));
            if(Boolean.TRUE.equals(success)){
                acquiredKeys.add(key);
                log.info("Redis hold ACQUIRED: key [{}] for user [{}] (TTL: {} mins)",
                        key, userEmail, ttlMinutes);
            }else{
                log.warn("Redis hold FAILED: key [{}] is already held by another user!", key);

                if(!acquiredKeys.isEmpty()){
                    redisTemplate.delete(acquiredKeys);
                    log.info("Rolled back {} partially acquired holds for showId: {}",
                            acquiredKeys.size(), showId);
                }
                return false;
            }
        }

        return true;
    }

    public void releaseSeatHolds(Long showId, List<Long> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> buildKey(showId,seatId))
                .toList();

        redisTemplate.delete(keys);
        log.info("Released {} holds from Redis for showId: {}", keys.size(), showId);
    }

    public boolean isHoldOwnedByOrExpired(Long showId, Long seatId, String userEmail){
        String currentOwner = getHoldOwner(showId,seatId);
        return currentOwner == null || userEmail.equals(currentOwner);
    }

    public String getHoldOwner(Long showId, Long seatId){
        return redisTemplate.opsForValue().get(buildKey(showId,seatId));
    }
}
