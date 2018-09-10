package com.chapter3.sort;

import java.util.List;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;

/**
 * list结合hash的排序
 * 
 * @author Administrator
 *
 */
public class Demo2 {

	public static void sort(Jedis jedis) {
		jedis.lpush("ml", "12", "11", "7", "13");

		jedis.hset("userrr7", "name", "xhanjie");
		jedis.hset("userrr11", "name", "hanjie");
		jedis.hset("userrr12", "name", "86");
		jedis.hset("userrr13", "name", "x86");

		SortingParams sortingParameters = new SortingParams();
		//对应的redis客户端命令是：sort ml get user*->name
		sortingParameters.get("userrr*->name");
		List<String> result = jedis.sort("ml", sortingParameters);
		for (String item : result) {
			System.out.println("item...." + item);
		}
	}
	
	public static void sort2(Jedis jedis) {
		jedis.lpush("ml", "12", "11", "7", "13");

		jedis.hset("userrr7", "name", "xhanjie");
		jedis.hset("userrr11", "name", "hanjie");
		jedis.hset("userrr12", "name", "86");
		jedis.hset("userrr13", "name", "x86");

		SortingParams sortingParameters = new SortingParams();
		//对应的redis客户端命令是：sort ml get user*->name
		sortingParameters.alpha();//根据字母表顺序对元素进行排序，不加这个则直接按数字大小排序
		sortingParameters.get("userrr*->name");
		List<String> result = jedis.sort("ml", sortingParameters);
		for (String item : result) {
			System.out.println("item...." + item);
		}
	}
	
	public static void sort3(Jedis jedis) {
		jedis.lpush("ml", "12", "11", "7", "13");

		jedis.hset("userrr7", "name", "xhanjie");
		jedis.hset("userrr11", "name", "hanjie");
		jedis.hset("userrr12", "name", "86");
		jedis.hset("userrr13", "name", "x86");
		
		jedis.hset("userrr:sort7", "field", "3");
		jedis.hset("userrr:sort11", "field", "200");
		jedis.hset("userrr:sort12", "field", "50");
		jedis.hset("userrr:sort13", "field", "9000");

		SortingParams sortingParameters = new SortingParams();
		//对应的redis客户端命令是：sort ml by userrr:sort*->field get user*->name
		sortingParameters.by("userrr:sort*->field");
		sortingParameters.get("userrr*->name");
		List<String> result = jedis.sort("ml", sortingParameters);
		for (String item : result) {
			System.out.println("item...." + item);
		}
	}

	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		Demo2.sort(jedis);
		System.out.println("============================");
		jedis.flushDB();
		Demo2.sort2(jedis);
		System.out.println("============================");
		jedis.flushDB();
		Demo2.sort3(jedis);
	}

}
