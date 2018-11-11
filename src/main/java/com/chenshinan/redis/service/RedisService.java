package com.chenshinan.redis.service;

/**
 * @author shinan.chen
 * @Date 2018/8/8
 */
public interface RedisService {

    void set(String key, String value);

    String get(String key);
}
