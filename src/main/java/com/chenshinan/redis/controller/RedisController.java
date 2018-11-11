package com.chenshinan.redis.controller;

import com.chenshinan.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shinan.chen
 * @date 2018/11/11
 */
@RestController
@RequestMapping("redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @GetMapping(value = "/set")
    public void set(@RequestParam("key") String key, @RequestParam("value") String value) {
        redisService.set(key, value);
    }

    @GetMapping(value = "/get")
    public String get(@RequestParam("key")String key){
        return redisService.get(key);
    }
}
