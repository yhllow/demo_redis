package com.main;

import java.util.Iterator;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;

/**
 * Set功能 无序，不可保存相同元素
 * 
 * @author Administrator
 *
 */
public class SetDemo {

	public static void setOperate(Jedis jedis, ShardedJedis shardedJedis) {

		System.out.println("======================set==========================");
		// 清空数据
		System.out.println("清空库中所有数据：" + jedis.flushDB());

		System.out.println("=============增=============");
		System.out.println("向sets集合中加入元素element001：" + jedis.sadd("sets", "element001"));
		System.out.println("向sets集合中加入元素element002：" + jedis.sadd("sets", "element002"));
		System.out.println("向sets集合中加入元素element003：" + jedis.sadd("sets", "element003"));
		System.out.println("向sets集合中加入元素element004：" + jedis.sadd("sets", "element004"));
		System.out.println("查看sets集合中的所有元素:" + jedis.smembers("sets"));
		System.out.println("随机删除一个元素:" + jedis.spop("sets"));
		System.out.println("查看sets集合中的所有元素:" + jedis.smembers("sets"));
		System.out.println("随机返回一个元素:" + jedis.srandmember("sets"));
		System.out.println();

		System.out.println("=============删=============");
		System.out.println("集合sets中删除元素element003：" + jedis.srem("sets", "element003"));
		System.out.println("查看sets集合中的所有元素:" + jedis.smembers("sets"));
		/*
		 * System.out.println("sets集合中任意位置的元素出栈："+jedis.spop("sets"));//注：
		 * 出栈元素位置居然不定？--无实际意义
		 * System.out.println("查看sets集合中的所有元素:"+jedis.smembers("sets"));
		 */
		System.out.println();

		System.out.println("=============改=============");
		System.out.println();

		System.out.println("=============查=============");
		System.out.println("判断element001是否在集合sets中：" + jedis.sismember("sets", "element001"));
		System.out.println("循环查询获取sets中的每个元素：");
		Set<String> set = jedis.smembers("sets");
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			Object obj = it.next();
			System.out.println(obj);
		}
		System.out.println();

		System.out.println("=============集合运算=============");
		System.out.println("sets1中添加元素element001：" + jedis.sadd("sets1", "element001"));
		System.out.println("sets1中添加元素element002：" + jedis.sadd("sets1", "element002"));
		System.out.println("sets1中添加元素element003：" + jedis.sadd("sets1", "element003"));
		System.out.println("sets1中添加元素element002：" + jedis.sadd("sets2", "element002"));
		System.out.println("sets1中添加元素element003：" + jedis.sadd("sets2", "element003"));
		System.out.println("sets1中添加元素element004：" + jedis.sadd("sets2", "element004"));
		System.out.println("查看sets1集合中的所有元素:" + jedis.smembers("sets1"));
		System.out.println("查看sets2集合中的所有元素:" + jedis.smembers("sets2"));
		System.out.println("sets1和sets2交集：" + jedis.sinter("sets1", "sets2"));
		System.out.println("sets1和sets2并集：" + jedis.sunion("sets1", "sets2"));
		System.out.println("sets1和sets2差集：" + jedis.sdiff("sets1", "sets2"));// 差集：set1中有，set2中没有的元素
		jedis.sdiffstore("sets1'", "sets1", "sets2");// 将差集保存至sets1'，其他命令也有类似的操作
		System.out.println("sets1和sets2差集：" + jedis.smembers("sets1'"));

		//将目前并不存在于集合中的元素添加到集合里面，并返回被添加的元素个数
		System.out.println(jedis.sadd("set-key", "a", "b", "c"));
		System.out.println(jedis.sadd("set-key", "c", "d"));
		System.out.println(jedis.sadd("set-key", "e", "f", "g"));
		//返回被移除元素个数
		System.out.println(jedis.srem("set-key", "c", "d"));
		//返回元素个数
		System.out.println(jedis.scard("set-key"));
		//将元素从一个集合移动到另一个集合
		jedis.smove("set-key", "set-key2", "f");
		System.out.println(jedis.smembers("set-key2"));
	}

}
