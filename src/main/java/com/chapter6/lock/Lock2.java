package com.chapter6.lock;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class Lock2 {
	
	//获取锁
	//timeout超时时间，超过时间也返回null
	//locktimeout锁自动过期时间，超过这个时间，自动删除锁
	public String acquire_lock_with_timeout(Jedis jedis, String lockName, long timeout, int locktimeout) throws InterruptedException{
		String id = UUID.randomUUID().toString();//128位随机标识符
		long end = (new Date()).getTime() + timeout;
		while((new Date()).getTime() < end){
			//尝试取得锁
			//如返回1，则该客户端获得锁，把lock:lockName的键值设置为id表示该键已被锁定，该客户端最后可以通过DEL lock:lockName来释放该锁。
			//如返回0，表明该锁已被其他客户端取得，这时我们可以先返回或进行重试等对方完成或等待锁超时。
			if(jedis.setnx("lock:" + lockName, id) == 1){
				//locktimeout秒后，如果锁还没有被删除，则自动删除
				jedis.expire("lock:" + lockName, locktimeout);
				return id;
			}else if(jedis.ttl("lock:" + lockName) == -1){
				//ttl命令用于获取键到期的剩余时间，返回值：TTL以毫秒为单位；-1：如果key没有到期超时；-2：如果键不存在。
				//如果没有设置过期时间，则设置过期时间
				jedis.expire("lock:" + lockName, locktimeout);
			}
			Thread.sleep(1000);
		}
		return null;
	}
	
	public static void main(String[] args) throws InterruptedException {
		//准备数据
		//买家
		//hset users:1 funds 100
		//hset users:1 name zhangsulei
		//卖家
		//hset users:2 funds 100
		//hset users:2 name chenli
		//市场
		//zadd market: 35 ItemH.2
		//zadd market: 54 ItemJ.2
		//各自的包裹
		//sadd inventory:1 ItemA
		//sadd inventory:1 ItemB
		
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		Lock2 l = new Lock2();
	}

}
