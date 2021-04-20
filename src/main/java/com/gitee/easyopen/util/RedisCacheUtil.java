package com.gitee.easyopen.util;

import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import com.gitee.easyopen.ApiContext;

public class RedisCacheUtil {
	
	private  RedisTemplate redisTemplate;
	
	
	
	public void init () {
		 ApplicationContext ctx = ApiContext.getApplicationContext();
		 RedisTemplate redisTemplate = (RedisTemplate)ctx.getBean("redisTemplate");
         if (redisTemplate == null) {
             throw new NullPointerException("redisTemplate不能为null，是否缺少spring-boot-starter-data-redis依赖");
         }
		ctx.getBean("");
	}

}
