package com.延时任务;

import java.text.MessageFormat;
import java.util.Date;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.cdel.util.anno.clazz.EnableRedisCache;
import com.cdel.util.helper.SpringContextUtil;

/**
以上就是通过Redis实现延时任务的思路了。这里提供的只是一个最简单的版本，实际上还有很多地方可以优化。比如，我们可以把任务的处理逻辑再放到单独的线程池中
去执行，这样的话任务消费者只需要负责任务的调度就可以了，好处就是可以减少任务执行时间偏差。还有就是，这里为了方便，任务的描述存储的只是任务的id，如果
有多种不同类型的任务，像前面说的发送资讯任务和推送消息任务，那么就要通过额外存储任务的类型来进行区分，或者使用不同的Sorted Set来存放延时任务了。

除此之外，上面的例子每次拉取延时任务时，只拉取1个，如果说某一个时刻要处理的任务数非常多，那么会有一部分任务延迟比较严重，这里可以优化成每次拉取不止1个
到期的任务，比如说10个，然后再逐个进行处理，这样的话可以极大地提升调度效率，因为如果是使用上面的方法，拉取10个任务需要10次调度，每次间隔1秒，总共需
要10秒才能把10个任务拉取完，如果改成一次拉取10个，只需要1次就能完成了，效率提升还是挺大的。

当然还可以从另一个角度来优化。大家看上面的代码，当拉取到待执行任务时，就直接执行任务，任务执行完该线程也就退出了，但是这个时候，队列里可能还有很多待执
行的任务（因为我们拉取任务时，限制了拉取的数量），所以其实在这里可以使用循环，当拉取不到待执行任务时，才结束调度，当有任务时，执行完还有顺便查询下有没
有堆积的任务，直到没有堆积任务了才结束线程。

最后一个需要考虑的地方是，上面的代码并没有对任务执行失败的情况进行处理，也就是说如果某个任务执行失败了，那么连重试的机会都没有。因此，在生产环境使用
时，还需要考虑任务处理失败的情况。有一个简单的方法是在任务处理时捕获异常，当在处理过程中出现异常时，就将该任务再放回Redis Sorted中，或者由当前线程
再重试处理。不过这样做的话，任务的时效性就不能保证了，有可能本来定在早上7点执行的任务，因为失败重试的原因，延迟到7点10分才执行完成，这个要根据业务
来进行权衡，比如可以在任务描述中增加重试次数或者是否允许重试的字段，这样在任务执行失败时，就能根据不同的任务采取不同的补偿措施了。

那么使用redis实现延时任务有什么优缺点呢？优点就是可以满足吞吐量。缺点则是存在任务丢失的风险（当redis实例挂了的时候）。因此，如果对性能要求比较高，
同时又能容忍少数情况下任务的丢失，那么可以使用这种方式来实现。
 *
 * */
@SpringBootApplication
@EnableRedisCache
public class App {

	public static void main(String[] args) throws InterruptedException {
		SpringContextUtil.setApplicationContext(SpringApplication.run(App.class, args));

		DelayTaskProducer producer = new DelayTaskProducer();
		System.out.println(MessageFormat.format("start time - {0}", new Date().getTime()));
		producer.produce("t1", new Date().getTime());
		Thread.sleep(100);
		producer.produce("t2", new Date().getTime());
		Thread.sleep(100);
		producer.produce("t3", new Date().getTime());
		Thread.sleep(100);
		producer.produce("t4", new Date().getTime());
		Thread.sleep(100);
		for (int i = 0; i < 10; i++) {
			new DelayTaskConsumer().start();
		}
	}

}
