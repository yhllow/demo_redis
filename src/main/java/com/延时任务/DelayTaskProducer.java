package com.延时任务;

import com.cdel.redis.store.RedisUtil;

//DelayTaskProducer则是延时任务的生产者，主要用于将延时任务放到Redis中。
public class DelayTaskProducer {

	private String appName = "test";

	public void produce(String newsId, Long timeStamp){
        RedisUtil.addWithSortedSet(appName, Constants.DELAY_TASK_QUEUE, timeStamp.floatValue(), newsId);
	}

}
