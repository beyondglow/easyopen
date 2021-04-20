package com.gitee.easyopen;

import java.util.Iterator;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gitee.easyopen.util.RedisCache;

import io.netty.util.internal.StringUtil;

/**
 * @author iys
 * @date 2021年4月15日 上午11:14:48
 */
public class RedisAppSecretManager implements AppSecretManager {
	
	
	public final static String THIRD_AUTH_CACHE_KEY = "third:ezak";
	
	private RedisCache redisCache;

	
	public RedisCache getRedisCache() {
		return redisCache;
	}

	public void setRedisCache(RedisCache redisCache) {
		this.redisCache = redisCache;
	}

	@Override
	public void addAppSecret(Map<String, String> appSecretStore) {
		 throw new UnsupportedOperationException("无效操作，com.gitee.easyopen.RedisAppSecretManager.addAppSecret(Map<String, String> appSecretStore)");
	}

	@Override
	public String getSecret(String appKey) {
		
		String secret = StringUtil.EMPTY_STRING;
		
		Object o = redisCache.getCacheObject(THIRD_AUTH_CACHE_KEY);
		
		JSONArray jsonArr = JSONObject.parseArray(o.toString());
		for (Iterator it = jsonArr.iterator(); it.hasNext();) {
			JSONObject jsonObj = (JSONObject)it.next();
			if(jsonObj.getString("appkey").equals(appKey)) {
				secret = jsonObj.getString("secret");
				break;
			}
		}
		return secret;
	}

	@Override
	public boolean isValidAppKey(String appKey) {
		boolean flag = false;
		if (appKey == null || appKey == "") {
			return flag;
		}
		Object o = redisCache.getCacheObject(THIRD_AUTH_CACHE_KEY);
		
		if (null == o) {
			return flag;
		}
		
		JSONArray jsonArr = JSONObject.parseArray(o.toString());
		for (Iterator it = jsonArr.iterator(); it.hasNext();) {
			JSONObject jsonObj = (JSONObject)it.next();
			if(jsonObj.getString("appkey").equals(appKey)) {
				flag = true;
				break;
			}
		}

		return flag;
	}

}
