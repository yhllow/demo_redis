package com.main;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;

public class Test {
	
	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		ShardedJedis shardedJedis = client.getShardedJedis();
		//KeyDemo.keyOperate(jedis, shardedJedis);
		//StringDemo.stringOperate(jedis, shardedJedis);
		//ListDemo.listOperate(jedis, shardedJedis);
		SetDemo.setOperate(jedis, shardedJedis);
		//HashDemo.hashOperate(jedis, shardedJedis);
		//SortedSetDemo.sortedSetOperate(jedis, shardedJedis);
	}

}
