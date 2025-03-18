package com.stupm.service.utils;

import com.stupm.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WriteUserSequence {

    @Autowired
    private RedisTemplate redisTemplate;

    public void writeUserSequence(Integer appId , String userId , String type, Long seq) {
        String key = appId + ":" + Constants.RedisConstants.SeqPrefix + ":" + userId;
        redisTemplate.opsForHash().put(key, type, seq);
    }

}

