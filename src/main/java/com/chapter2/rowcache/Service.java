package com.chapter2.rowcache;

import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

public class Service {
	
	public void scheduleRowCache(Jedis jedis, String rowid, double delay){
		//设置数据行的延迟值
		jedis.zadd("delay:", delay, rowid);
		//立刻对需要缓存的数据行进行调度
		jedis.zadd("schedule:", (new Date()).getTime() , rowid);
	}
	
	
	/**缓存数据行，并定时刷新缓存数据
	 * @param jedis
	 * @throws InterruptedException 
	 */
	public void cacheRow(Jedis jedis, String content) throws InterruptedException{
		while(true){
			//尝试获取下一个需要被缓存的数据行以及该行的调度时间戳，命令会返回一个包含零个或一个元组的列表
			Set<Tuple> next = jedis.zrangeWithScores("schedule:", 0, 0);
			long now = (new Date()).getTime();
			
			//暂时没有需要被缓存的内容，则休息半秒
			if(next == null || next.size() == 0){
				Thread.sleep(500);
				continue;
			}
			Iterator<Tuple> it = next.iterator();
			Tuple t = null;
			while(it.hasNext()){
				t = it.next();
				break;
			}
			long score = ((Double)t.getScore()).longValue();
			if(score > now){
				Thread.sleep(500);
				continue;
			}
			
			String rowid = t.getElement();
			double delay = jedis.zscore("delay:", rowid);
			//不必再缓存这行了，把它从缓存表移除
			if(delay <= 0){
				jedis.zrem("delay:", rowid);
				jedis.zrem("schedule:", rowid);
				jedis.del("inv:" + rowid);
			}
			
			//需要缓存这行，则读取数据，更新调度时间并设置缓存值
			jedis.zadd("schedule:", now + delay, rowid);//更新下次调度时间
			Random r = new Random();
			jedis.set("inv:" + rowid, content + r.nextInt(17));//更新缓存
		}
	}

}
