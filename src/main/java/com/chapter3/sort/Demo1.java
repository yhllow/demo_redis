package com.chapter3.sort;

import java.util.List;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;

/**
 * set结合String的排序
 * 
 * @author Administrator
 *
 */
public class Demo1 {

	public static void sort(Jedis jedis) {
		jedis.sadd("tom:friend:list", "123"); // tom的好友列表
		jedis.sadd("tom:friend:list", "456");
		jedis.sadd("tom:friend:list", "789");
		jedis.sadd("tom:friend:list", "101");

		jedis.set("uid:sort:123", "1000"); // 好友对应的成绩
		jedis.set("uid:sort:456", "6000");
		jedis.set("uid:sort:789", "100");
		jedis.set("uid:sort:101", "5999");

		jedis.set("uid:123", "{'uid':123,'name':'lucy'}"); // 好友的详细信息
		jedis.set("uid:456", "{'uid':456,'name':'jack'}");
		jedis.set("uid:789", "{'uid':789,'name':'marry'}");
		jedis.set("uid:101", "{'uid':101,'name':'icej'}");

		SortingParams sortingParameters = new SortingParams();
		//倒序
		sortingParameters.desc();
		//对应的redis 命令是./redis-cli sort tom:friend:list by uid:sort:* get uid:*
		sortingParameters.get("uid:*");
		sortingParameters.by("uid:sort:*");
		
		List<String> result = jedis.sort("tom:friend:list", sortingParameters);
        for(String item:result){
            System.out.println("item..."+item);
        }
	}
	
	public static void sort2(Jedis jedis) {
		jedis.sadd("tom:friend:list", "123"); // tom的好友列表
		jedis.sadd("tom:friend:list", "456");
		jedis.sadd("tom:friend:list", "789");
		jedis.sadd("tom:friend:list", "101");
		
		jedis.set("uid:sort:123", "1000"); // 好友对应的成绩
		jedis.set("uid:sort:456", "6000");
		jedis.set("uid:sort:789", "100");
		jedis.set("uid:sort:101", "5999");
		
		jedis.set("uid:123", "{'uid':123,'name':'lucy'}"); // 好友的详细信息
		jedis.set("uid:456", "{'uid':456,'name':'jack'}");
		jedis.set("uid:789", "{'uid':789,'name':'marry'}");
		jedis.set("uid:101", "{'uid':101,'name':'icej'}");
		
		SortingParams sortingParameters = new SortingParams();
		//倒序
		sortingParameters.desc();
		//对应的redis 命令是./redis-cli sort tom:friend:list by uid:sort:* get uid:*  get uid:sort:*     
		sortingParameters.get("uid:*");
		sortingParameters.get("uid:sort:*");
		sortingParameters.by("uid:sort:*");
		
		List<String> result = jedis.sort("tom:friend:list", sortingParameters);
		for(String item:result){
			System.out.println("item..."+item);
		}
	}
	
	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		Demo1.sort(jedis);
		System.out.println("============================");
		jedis.flushDB();
		Demo1.sort2(jedis);
	}

}
