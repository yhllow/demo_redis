package com.chapter2.rowcache;

import redis.clients.jedis.Jedis;

import com.main.RedisClient;

public class Test {
	
	public static void main(String[] args) throws InterruptedException {
		Service s = new Service();
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		
		s.scheduleRowCache(jedis, "001", 10000);
		s.cacheRow(jedis, "更新了");
	}

}
