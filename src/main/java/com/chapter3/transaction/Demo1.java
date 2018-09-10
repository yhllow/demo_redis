package com.chapter3.transaction;

import java.util.List;

import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**事务方式(Transactions) 
redis的事务很简单，他主要目的是保障，一个client发起的事务中的命令可以连续的执行，而中间不会插入其他client的命令。 
我们调用jedis.watch(…)方法来监控key，如果调用后key值发生变化，则整个事务会执行失败。另外，事务中某个操作失败，并不会回滚其他操作。这一点需要注意。
还有，我们可以使用discard()方法来取消事务。 
 * @author Administrator
 *
 */
//http://blog.csdn.net/truong/article/details/46711045
public class Demo1 {

	public static void test2Trans(Jedis jedis) {
		long start = System.currentTimeMillis();
		//开启事务  
		Transaction tx = jedis.multi();
		for (int i = 0; i < 100000; i++) {
			tx.set("t" + i, "t" + i);
		}
		List<Object> results = tx.exec();
		long end = System.currentTimeMillis();
		System.out.println("Transaction SET: " + ((end - start) / 1000.0)
				+ " seconds");
		System.out.println(results.size());
	}

	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		Demo1.test2Trans(jedis);
	}

}
