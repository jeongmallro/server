package com.pitchain.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisHashRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> script;

    public void increment(String key, String hashKey, Long value) {
        redisTemplate.opsForHash().increment(key, hashKey, value);
    }

    public Map<String, String> findAll(String key) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    public List<String> getAndDeleteAll(String key) {
        return redisTemplate.execute(script, Collections.singletonList(key));
    }

}
