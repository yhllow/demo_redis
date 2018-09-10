package com.延时任务;

import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.cdel.redis.store.RedisUtil;

//DelayTaskConsumer是延时任务的消费者，这个类负责从Redis拉取到期的任务，并封装了任务消费的逻辑。
public class DelayTaskConsumer {


	private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public void start(){
        scheduledExecutorService.scheduleWithFixedDelay(new DelayTaskHandler(), 1, 1, TimeUnit.SECONDS);
    }

    public static class DelayTaskHandler implements Runnable{
    	private String appName = "test";

    	//如果是多个线程去轮询这个Sorted Set，还有考虑并发问题，假如说一个任务到期了，也被多个线程拿到了，这个时候必须保证只有一个线程能执行这个任务，这可以通过zrem命令来实现，只有删除成功了，才能执行任务，这样就能保证任务不被多个任务重复执行了。
        @Override
        public void run() {
            Set<String> ids = RedisUtil.rangeByScoreWithSortedSet(appName, Constants.DELAY_TASK_QUEUE, 0.0f, System.currentTimeMillis(), 0, 1);
            if(ids == null || ids.isEmpty()){
                return;
            }
            for(String id : ids){
                Long count = RedisUtil.zrem(appName, Constants.DELAY_TASK_QUEUE, id);
                if(count != null && count == 1){
                    System.out.println(MessageFormat.format("发布资讯。id - {0} , timeStamp - {1} , " + "threadName - {2}",id,System.currentTimeMillis(),Thread.currentThread().getName()));
                }
            }
        }
    }

}
