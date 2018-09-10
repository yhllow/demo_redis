package com.chapter3.pipe;

import java.util.List;
import com.main.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**管道(Pipelining) 
有时，我们需要采用异步方式，一次发送多个指令，不同步等待其返回结果。这样可以取得非常好的执行效率。这就是管道
 * @author Administrator
 *
 */
//http://blog.csdn.net/truong/article/details/46711045
public class Demo1 {

	public static void test2Trans(Jedis jedis) {
		Pipeline pipeline = jedis.pipelined(); 
	    long start = System.currentTimeMillis(); 
	    for (int i = 0; i < 100000; i++) { 
	        pipeline.set("p" + i, "p" + i); 
	    } 
	    List<Object> results = pipeline.syncAndReturnAll(); 
	    long end = System.currentTimeMillis(); 
	    System.out.println("Pipelined SET: " + ((end - start)/1000.0) + " seconds"); 
		System.out.println(results.size());
	}

	public static void main(String[] args) {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		Demo1.test2Trans(jedis);
	}

}
