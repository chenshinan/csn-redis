package com.chenshinan.redis.service.impl;

import com.chenshinan.redis.service.RedisService;
import com.chenshinan.redis.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author shinan.chen
 * @Date 2018/8/8
 */
@Component
public class RedisServiceImpl implements RedisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceImpl.class);
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void set(String key, String value) {
        redisUtil.set(key, value);
    }

    @Override
    public String get(String key) {
        return (String) redisUtil.get(key);
    }
}
