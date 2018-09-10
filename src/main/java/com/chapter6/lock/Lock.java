package com.chapter6.lock;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class Lock {
	
	//获取锁
	//timeout超时时间，超过时间也返回null
	public String acquire_lock(Jedis jedis, String lockName, long timeout) throws InterruptedException{
		String id = UUID.randomUUID().toString();//128位随机标识符
		long end = (new Date()).getTime() + timeout;
		while((new Date()).getTime() < end){
			//尝试取得锁
			//如返回1，则该客户端获得锁，把lock:lockName的键值设置为id表示该键已被锁定，该客户端最后可以通过DEL lock:lockName来释放该锁。
			//如返回0，表明该锁已被其他客户端取得，这时我们可以先返回或进行重试等对方完成或等待锁超时。
			if(jedis.setnx("lock:" + lockName, id) == 1){
				return id;
			}
			Thread.sleep(1000);
		}
		return null;
	}
	
	public boolean purchaseItemWithLock(Jedis jedis, String buyerID, String itemID, String sellerID) throws InterruptedException{
		//买家个人信息
		String buyer = "users:" + buyerID;
		//卖家个人信息
		String seller = "users:" + sellerID;
		//商品市场，id是商品id+用户id
		String item = itemID + "." + sellerID;
		//每个用户拥有的商品
		String inventory = "inventory:" + buyerID;
		//获取锁
		String id = null;
		if((id = acquire_lock(jedis, "market", 10*1000)) == null){
			return false;
		}
		
		Pipeline pipe = jedis.pipelined();
		try{
			//检查商品是否还在出售，以及买家是否有足够的钱来购买这件商品
			pipe.multi();
			pipe.zscore("market:", item);
			pipe.hget(buyer, "funds");
			Response<List<Object>> res = pipe.exec();
			pipe.close();
			List<Object> result = res.get();
			Double price = (Double)result.get(0);
			Double funds = Double.parseDouble((String)result.get(1));
			if(price == null || price > funds){
				return false;
			}
			
			//先将买家支付的钱转移给卖家，然后将被购买的商品移交给买家
			pipe.multi();
			pipe.hincrByFloat(seller, "funds", price);
			pipe.hincrByFloat(buyer, "funds", -price);
			pipe.sadd(inventory, itemID);
			pipe.zrem("market:", item);
			pipe.exec();
			pipe.close();
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			release_lock(jedis, "market", id);
		}
		
		return true;
	}
	
	//删除锁
	//在程序持有锁期间，其它客户端可能擅自对锁进行修改(方法有问题)
	public void release_lock(Jedis jedis, String lockName, String id){
		String lockName2 = "lock:" + lockName;
		while(true){
			try{
				jedis.watch(lockName2);
				Transaction t = jedis.multi();
				if(t.get(lockName2).equals(id)){
					t.del(lockName2);
					t.exec();
					return;
				}
				jedis.unwatch();
				break;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
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
		Lock l = new Lock();
		l.purchaseItemWithLock(jedis, "1", "ItemH", "2");
	}

}
