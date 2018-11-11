package com.chenshinan.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author shinan.chen
 * @date 2018/11/11
 */
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ValueOperations<String, Object> valueOperations;

    /**
     * 查询key,支持模糊查询
     */
    public Set<String> keys(String key) {
        return redisTemplate.keys(key);
    }

    /**
     * 字符串添加信息
     *
     * @param key     key
     * @param obj     可以是单个的值，也可以是任意类型的对象
     * @param timeout 过期时间，单位秒
     */
    public void set(String key, Object obj, Long timeout) {
        valueOperations.set(key, obj, timeout, TimeUnit.SECONDS);
    }

    /**
     * 字符串添加信息
     *
     * @param key key
     * @param obj 可以是单个的值，也可以是任意类型的对象
     */
    public void set(String key, Object obj) {
        valueOperations.set(key, obj);
    }

    /**
     * 字符串获取值
     *
     * @param key key
     */
    public Object get(String key) {
        return valueOperations.get(key);
    }
}
