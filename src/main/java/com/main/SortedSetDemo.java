package com.main;

import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

public class SortedSetDemo {

	public static void sortedSetOperate(Jedis jedis, ShardedJedis shardedJedis) {
		System.out.println("======================zset==========================");
		// 清空数据
		System.out.println(jedis.flushDB());

		System.out.println("=============增=============");
		System.out.println("zset中添加元素element001：" + shardedJedis.zadd("zset", 7.0, "element001"));
		System.out.println("zset中添加元素element002：" + shardedJedis.zadd("zset", 8.0, "element002"));
		System.out.println("zset中添加元素element003：" + shardedJedis.zadd("zset", 2.0, "element003"));
		System.out.println("zset中添加元素element004：" + shardedJedis.zadd("zset", 3.0, "element004"));
		System.out.println("zset集合中的所有元素：" + shardedJedis.zrange("zset", 0, -1));// 按照权重值排序
		shardedJedis.zincrby("zset", 20, "element003");//给“element003”的分值增加20分
		System.out.println("zset集合中的所有元素：" + shardedJedis.zrange("zset", 0, -1));// 按照权重值排序
		System.out.println("返回分值在2-7之间的元素：" + shardedJedis.zrangeByScore("zset", 2, 7));// 返回分值在2-7之间的元素
		System.out.println("返回分值在2-7之间的元素(由大到小排序)：" + shardedJedis.zrevrangeByScore("zset", 7, 2));// 返回分值在2-7之间的元素
		System.out.println("返回分值在2-7之间的，offset表示从符合条件的第offset个成员开始返回，同时返回count个元素(由大到小排序)：" + shardedJedis.zrevrangeByScore("zset", 7, 2, 0, 1));// 返回分值在2-7之间的元素
		System.out.println("zset集合中的所有元素(由大到小排序)：" + shardedJedis.zrevrange("zset", 0, -1));// 按照权重值排序
		System.out.println("查看zset集合中element001的排名：" + shardedJedis.zrank("zset", "element001"));
		System.out.println("查看zset集合中element001的排名(由大到小排序)：" + shardedJedis.zrevrank("zset", "element001"));
		System.out.println();

		System.out.println("=============删=============");
		//zrem返回删除元素的数量
		System.out.println("zset中删除元素element002：" + shardedJedis.zrem("zset", "element002"));
		System.out.println("zset集合中的所有元素：" + shardedJedis.zrange("zset", 0, -1));
		System.out.println();

		System.out.println("=============改=============");
		System.out.println();

		System.out.println("=============查=============");
		System.out.println("统计zset集合中的元素中个数：" + shardedJedis.zcard("zset"));
		System.out.println("统计zset集合中权重某个范围内（1.0——5.0），元素的个数：" + shardedJedis.zcount("zset", 1.0, 5.0));
		System.out.println("查看zset集合中element004的权重：" + shardedJedis.zscore("zset", "element004"));
		System.out.println("查看下标1到2范围内的元素值：" + shardedJedis.zrange("zset", 1, 2));
		Set<Tuple> set = shardedJedis.zrangeWithScores("zset", 1, 2);
		for (Tuple tuple : set) {
			System.out.print(tuple.getElement() + "------");
			System.out.println(tuple.getScore());
		}
		
		//删除有序集合中排名介于1-2之间的元素
		shardedJedis.zremrangeByRank("zset", 1, 2);
		//删除有序集合中分值介于1-20之间的元素
		shardedJedis.zremrangeByScore("zset", 1, 20);
	}

}
