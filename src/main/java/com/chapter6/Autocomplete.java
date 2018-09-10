package com.chapter6;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

//自动补全程序
//对于数据量比较少的列表可以这么做，即一次全部获取列表内容
public class Autocomplete {
	
	//构建最近联系人自动补全列表
	public void addUpdateContact(Jedis jedis, String name, String contact){
		String ac_list = "recent:" + name;
		Pipeline pipeline = jedis.pipelined();
		
		pipeline.lrem(ac_list, 1, contact);//如果联系人已经存在，那么删除它
		pipeline.lpush(ac_list, contact);//将联系人推入列表最前端
		pipeline.ltrim(ac_list, 0, 99);//只保留列表里的前100人
		
		pipeline.exec();
	}
	
	//当用户不想再看到某个人时，则删除
	public void remove_contact(Jedis jedis, String name, String contact){
		jedis.lrem("recent:" + name, 1, contact);
	}

}
