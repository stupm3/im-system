package com.stupm.service.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSequence {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    public Long getSequence(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }
}
