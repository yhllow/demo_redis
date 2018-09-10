package com.chapter3.sort;

import java.util.List;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;

/**
 * list排序
 * 
 * @author Administrator
 *
 */
public class Demo3 {

	public static void sort(Jedis jedis) {
		jedis.lpush("ml", "12", "11", "7", "13");

		List<String> result = jedis.sort("ml");
		for (String item : result) {
			System.out.println("item...." + item);
		}
	}
	
	public static void sort2(Jedis jedis) {
		jedis.lpush("ml", "12", "11", "7", "13");
		
		SortingParams sortingParameters = new SortingParams();
		sortingParameters.alpha();
		List<String> result = jedis.sort("ml", sortingParameters);
		for (String item : result) {
			System.out.println("item...." + item);
		}
	}

	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		Demo3.sort(jedis);
		System.out.println("============================");
		jedis.flushDB();
		Demo3.sort2(jedis);
	}

}
