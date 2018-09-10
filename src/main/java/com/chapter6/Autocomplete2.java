package com.chapter6;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import com.main.RedisClient;
import redis.clients.jedis.Jedis;

//自动补全程序
//对于数据量大的查询，需要使用有序集合，把有序集合里的元素的分值都设置为0，从而可以让集合按字母顺序排序
//通过向有序集合里添加元素来创建查找范围，并在取得范围内的元素后移除之前添加的元素，这是一种非常有用的技术
public class Autocomplete2 {
	
	String characters = "`abcdefghijklmnopqrstuvwxyz{";//准备一个由已知字符组成的列表
	
	//根据给定前缀生成查找范围
	public String[] find_prefix_range(String prefix){
		String[] result = new String[2];
		
		String a = prefix.substring(prefix.length()-1);
		String a1 = characters.substring(characters.indexOf(a)-1, characters.indexOf(a));
		
		result[0] = prefix.substring(0, prefix.length()-1) + a1 + "{";
		result[1] = prefix + "{";
		return result;
	}
	
	//将用户添加到工会集合
	public void join_guild(Jedis jedis, String guild, String user){
		jedis.zadd("members:" + guild, 0, user);
	}
	
	//将用户从工会集合里移除
	public void leave_guild(Jedis jedis, String guild, String user){
		jedis.zrem("members:" + guild, user);
	}
	
	//自动补全
	public void autocomplete(Jedis jedis, String guild, String prefix){
		//根据给定前缀计算起始、结束范围
		String [] result = find_prefix_range(prefix);
		String start = result[0];
		String end = result[1];
		String uuid = UUID.randomUUID().toString();//防止和别人插入的起始、结束范围冲突
		start += uuid;
		end += uuid;
		
		String zset_name = "members:" + guild;
		join_guild(jedis, guild, start);
		join_guild(jedis, guild, end);
		
		try{
			//找出两个被插入元素的排名
			Long sindex = jedis.zrank(zset_name, start);
			Long eindex = jedis.zrank(zset_name, end);
			//获取范围内的值，然后删除之前插入的起始、结束元素
			Set<String> set = jedis.zrange(zset_name, sindex+1, eindex-1);
			set.stream().forEach(System.out::println);
			jedis.zrem(zset_name, start, end);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Autocomplete2 a = new Autocomplete2();
		Stream.of(a.find_prefix_range("aaa")).forEach(System.out::print);
		System.out.println();
		
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		
		a.join_guild(jedis, "1", "zhaosi");
		a.join_guild(jedis, "1", "yhb");
		a.join_guild(jedis, "1", "zhangsulei");
		a.join_guild(jedis, "1", "chenli");
		a.join_guild(jedis, "1", "wangwu");
		a.join_guild(jedis, "1", "wangxiaohu");
		a.join_guild(jedis, "1", "wawa");
		a.join_guild(jedis, "1", "hainan");
		a.join_guild(jedis, "1", "ouyangke");
		a.join_guild(jedis, "1", "wwww");
		
		a.autocomplete(jedis, "1", "wa");
	}

}
