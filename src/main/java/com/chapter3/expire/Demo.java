package com.chapter3.expire;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;

public class Demo {
	
	public static void expire(Jedis jedis) throws InterruptedException{
		jedis.flushDB();
		jedis.setex("key", 5, "value");//5秒后过期
		Thread.sleep(3000);
		System.out.println(jedis.get("key"));
		System.out.println("还差几秒过期： " + jedis.ttl("key"));
		Thread.sleep(3000);
		System.out.println(jedis.get("key"));
	}
	
	public static void expire2(Jedis jedis) throws InterruptedException{
		jedis.flushDB();
		jedis.set("key1", "value1");
		jedis.pexpire("key1", 5000l);//5秒后过期
		Thread.sleep(3000);
		System.out.println(jedis.get("key1"));
		System.out.println("还差几毫秒过期： " + jedis.pttl("key1"));
		Thread.sleep(3000);
		System.out.println(jedis.get("key1"));
	}
	
	public static void main(String[] args) throws InterruptedException {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		Demo.expire(jedis);
		System.out.println("===============================");
		Demo.expire2(jedis);
	}

}
