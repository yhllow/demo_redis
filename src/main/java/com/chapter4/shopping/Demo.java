package com.chapter4.shopping;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

///http://my.oschina.net/sphl520/blog/312514#OSC_h3_2
public class Demo {
	
	//redis的事务很简单，他主要目的是保障，一个client发起的事务中的命令可以连续的执行，而中间不会插入其他client的命令。
	//我们调用jedis.watch(…)方法来监控key，如果调用后key值发生变化，则整个事务会执行失败。另外，事务中某个操作失败，并不会回滚其他操作。这一点需要注意。还有，我们可以使用discard()方法来取消事务。
	public void test2Trans() {
	    Jedis jedis = new Jedis("localhost");
	    long start = System.currentTimeMillis();
	    Transaction tx = jedis.multi();
	    for (int i = 0; i < 100000; i++) {
	        tx.set("t" + i, "t" + i);
	    }
	    List<Object> results = tx.exec();
	    long end = System.currentTimeMillis();
	    System.out.println("Transaction SET: " + ((end - start)/1000.0) + " seconds");
	    jedis.disconnect();
	}
	
	//有时，我们需要采用异步方式，一次发送多个指令，不同步等待其返回结果。这样可以取得非常好的执行效率。这就是管道，调用方法如下：
	public void test3Pipelined() {
	    Jedis jedis = new Jedis("localhost");
	    Pipeline pipeline = jedis.pipelined();
	    long start = System.currentTimeMillis();
	    for (int i = 0; i < 100000; i++) {
	        pipeline.set("p" + i, "p" + i);
	    }
	    List<Object> results = pipeline.syncAndReturnAll();
	    long end = System.currentTimeMillis();
	    System.out.println("Pipelined SET: " + ((end - start)/1000.0) + " seconds");
	    jedis.disconnect();
	}
	
	//就Jedis提供的方法而言，是可以做到在管道中使用事务，其代码如下：
	//但是经测试（见本文后续部分），发现其效率和单独使用事务差不多，甚至还略微差点。
	public void test4combPipelineTrans() {
		Jedis jedis = new Jedis("localhost"); 
	    long start = System.currentTimeMillis();
	    Pipeline pipeline = jedis.pipelined();
	    pipeline.multi();
	    for (int i = 0; i < 100000; i++) {
	        pipeline.set("" + i, "" + i);
	    }
	    pipeline.exec();
	    List<Object> results = pipeline.syncAndReturnAll();
	    long end = System.currentTimeMillis();
	    System.out.println("Pipelined transaction: " + ((end - start)/1000.0) + " seconds");
	    jedis.disconnect();
	}

}
