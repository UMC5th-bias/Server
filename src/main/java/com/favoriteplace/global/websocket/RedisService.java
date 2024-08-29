package com.favoriteplace.global.websocket;

import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.travel.Pilgrimage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedisService {
    @Qualifier("customRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CERTIFICATION_KEY_PREFIX = "certification:";
    private static final Duration CERTIFICATION_EXPIRATION = Duration.ofHours(24);

    // 사용자가 인증 장소에 접속한 시점을 저장
    public void saveCertificationTime(Long userId, Long pilgrimageId) {
        String key = CERTIFICATION_KEY_PREFIX + userId + ":" + pilgrimageId;
        Instant now = Instant.now();
        redisTemplate.opsForValue().set(key, now);
        redisTemplate.expire(key, CERTIFICATION_EXPIRATION);
    }

    // 인증 시점에서 24시간이 지났는지 확인
    public boolean isCertificationExpired(Member member, Pilgrimage pilgrimage) {
        String key = CERTIFICATION_KEY_PREFIX + member.getId() + ":" + pilgrimage.getId();
        Instant savedTime = (Instant) redisTemplate.opsForValue().get(key);
        return savedTime == null || savedTime.isBefore(Instant.now().minus(CERTIFICATION_EXPIRATION));
    }

    public void deleteCertificationTime(Long userId, Long pilgrimageId) {
        String key = CERTIFICATION_KEY_PREFIX + userId + ":" + pilgrimageId;
        redisTemplate.delete(key);
    }
}
