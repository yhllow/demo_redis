package com.chapter6.delayqueue;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.chapter6.lock.Lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

//延迟队列
public class Demo {
	
	//item是一个json串，里面有4个参数，1唯一标识符，2处理任务的队列的名字，3处理任务的回调函数的名字，4传给回调函数的参数
	//delay延迟执行的时间
	public void exec_later(Jedis jedis, String queue, String name, Object[] args, int delay){
		String iden = UUID.randomUUID().toString();
		String item = iden + queue + name + args.toString();
		if(delay > 0){
			//延迟执行这个任务，执行时间作为score
			jedis.zadd("delayed:", (new Date()).getTime() + delay, item);
		}else{
			//无须延迟的任务，可以立刻执行
			jedis.rpush("queue:" + queue, item);
		}
	}
	
	//遍历"delayed:"，如果时间到了则移出"delayed:"，放入"queue:"，这个过程中需要加锁
	public void poll_queue(Jedis jedis) throws InterruptedException{
		while(true){
			//获取队列中的第一个任务
			Set<Tuple> set = jedis.zrangeWithScores("delayed:", 0, 0);
			//队列为空
			if(set == null){
				Thread.sleep(1000);
				continue;
			}
			//还没到执行时间
			if(set.iterator().next().getScore() > (new Date()).getTime()){
				Thread.sleep(1000);
				continue;
			}
			String item = set.iterator().next().getElement();
			//模拟实现
			String iden = item.substring(0, 64);
			String queue = item.substring(64, 74);
			
			//获取锁
			String id = null;
			Lock lock = new Lock();
			//获取锁失败，跳过后续步骤重试
			if((id = lock.acquire_lock(jedis, "market", 10*1000)) == null){
				continue;
			}
			if(jedis.zrem("delayed:", item) == 1l){
				jedis.rpush("queue:" + queue, item);
			}
			lock.release_lock(jedis, "market", id);
		}
	}

}
