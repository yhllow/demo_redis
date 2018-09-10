package com.chapter8.web;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chapter6.lock.Lock;
import com.main.RedisClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import static java.util.stream.Collectors.toMap;

public class Demo {
	
	//创建用户
	public static long create_user(Jedis jedis, String login, String name) throws InterruptedException{
		String llogin = login.toLowerCase();
		Lock lock = new Lock();
		//如果加锁不成功，则说明给定的用户名已经被其他用户占用
		String s = null;
		if((s = lock.acquire_lock(jedis, "user:" + llogin, 1)) == null){
			return -1;
		}
		
		//程序用一个散列来存储小写的用户名以及用户id之间的映射，如果给定的用户名已经被映射到了某个用户id，那么程序就不再将这个用户名分配给其他人
		if(jedis.hget("users:", llogin) != null){
			lock.release_lock(jedis, "user:" + llogin, s);
			return -1;
		}
		
		//每个用户都有一个独一无二的id，这个id是通过对计数器执行自增操作产生的
		long id = jedis.incr("user:id:");
		
		Pipeline pipe = jedis.pipelined();
		pipe.multi();
		pipe.hset("users:", llogin, id + "");
		pipe.hmset("user:" + id, new HashMap(){{
			put("login", login);
			put("id", id + "");
			put("name", name);
			put("followers", "0");
			put("following", "0");
			put("posts", "0");
			put("signup", (new Date()).getTime() + "");
		}});
		pipe.exec();
		lock.release_lock(jedis, "user:" + llogin, s);
		return id;
	}
	
	//创建状态信息散列
	public static long create_status(Jedis jedis, long uid, String msg){
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			pipe.hget("user:" + uid, "login");//根据用户id获取用户名
			pipe.incr("status:id:");//为这条状态信息创建一个新的id
			Response<List<Object>> rlist = pipe.exec();
			pipe.close();
			List<Object> list = rlist.get();
			String login = (String)list.get(0);
			Long statusid = (Long)list.get(1);
			
			//先验证账号是否存在
			if(login == null){
				return -1;
			}
			
			pipe.multi();
			pipe.hmset("status:" + statusid, new HashMap(){{
				put("message", msg);
				put("posted", (new Date()).getTime() + "");
				put("id", statusid + "");
				put("uid", uid + "");
				put("login", login);
			}});
			//更新用户的已发送状态信息数量
			pipe.hincrBy("user:" + uid, "posts", 1);
			pipe.exec();
			return statusid;
		}catch(Exception e){
			e.printStackTrace();
		}
		return -1;
	}
	
	//主页时间线：有序集合，成员使用statusid，分值使用消息发布的时间戳
	//还有个人时间线：主要保存个人的状态信息
	//page：查看的页码
	//count：每页显示的记录数
	public static List<Object> get_status_message(Jedis jedis, long uid, String timeline, int page, int count){
		Set<String> statuss = jedis.zrevrange(timeline + ":" + uid, (page - 1) * count, page * count - 1);
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			statuss.stream().map(statusid -> pipe.hgetAll("status:" + statusid));
			Response<List<Object>> rlist = pipe.exec();
			pipe.close();
			List<Object> result = rlist.get();
			return result;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	//关注某人
	public static void follow_user(Jedis jedis, long uid, long other_uid){
		String fkey1 = "following:" + uid;//我关注的人列表
		String fkey2 = "followers:" + other_uid;//other_uid被谁关注的列表
		
		//检查是否已经关注过了other_uid
		if(jedis.zscore(fkey1, other_uid + "") != null){
			return;
		}
		
		long time = (new Date()).getTime();
		
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			pipe.zadd(fkey1, time, other_uid + "");
			pipe.zadd(fkey2, time, uid + "");
			//从被关注用户的个人时间线里获取999条最新状态信息
			pipe.zrevrangeWithScores("profile:" + other_uid, 0, 999);
			Response<List<Object>> re = pipe.exec();
			pipe.close();
			long following = (long)re.get().get(0);
			long followers = (long)re.get().get(1);
			Set<Tuple> status_and_score = (Set<Tuple>)re.get().get(2);
			
			pipe.multi();
			pipe.hincrBy("user:" + uid, "following", following);
			pipe.hincrBy("user:" + other_uid, "followers", followers);
			//把从被关注用户里获取的最新状态信息，追加到uid的主页时间线里
			if(status_and_score != null){
				Map<String, Double> map = status_and_score.stream().collect(toMap(Tuple::getElement, Tuple::getScore));
				pipe.zadd("home:" + uid, map);
			}
			//保留最近的1000条主页时间线记录
			pipe.zremrangeByRank("home:" + uid, 0, -1001);
			pipe.exec();
			pipe.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//取消关注某人
	public static void unfollow_user(Jedis jedis, long uid, long other_uid){
		String fkey1 = "following:" + uid;//我关注的人列表
		String fkey2 = "followers:" + other_uid;//other_uid被谁关注的列表
		
		//如果没有关注过该用户，则返回
		if(jedis.zscore(fkey1, other_uid + "") == null){
			return;
		}
		
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			pipe.zrem(fkey1, other_uid + "");
			pipe.zrem(fkey2, uid + "");
			//从被关注用户的个人时间线里获取999条最新状态信息
			pipe.zrevrange("profile:" + other_uid, 0, 999);
			Response<List<Object>> re = pipe.exec();
			pipe.close();
			long following = (long)re.get().get(0);
			long followers = (long)re.get().get(1);
			Set<String> status_and_score = (Set<String>)re.get().get(2);
			
			pipe.multi();
			pipe.hincrBy("user:" + uid, "following", -following);
			pipe.hincrBy("user:" + other_uid, "followers", -followers);
			//把从被关注用户里获取的最新状态信息，追加到uid的主页时间线里
			if(status_and_score != null){
				String[] arr = status_and_score.stream().toArray(String[]::new);
				pipe.zrem("home:" + uid, arr);
			}
			pipe.exec();
			pipe.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//把状态信息推送给关注者，如果只有1000或以下的人关注，则直接推送
	//如果有1000以上人关注，则延迟推送
	public static long post_status(Jedis jedis, long uid, String msg){
		//创建一条新的状态消息
		long id = create_status(jedis, uid, msg);
		if(id == -1){
			return -1;
		}
		
		//获取消息的发布时间
		String posted = jedis.hget("status:" + id, "posted");
		if(posted == null){
			return -1;
		}
		
		//将状态信息添加到个人时间线里
		Map post = new HashMap(){{put(id, posted);}};
		jedis.zadd("profile:" + uid, post);
		//将状态信息推送给用户的关注者
		syndicate_status(jedis, uid, post, 0);
		return id;
	}
	
	//对关注者的主页时间线进行更新，start=0
	public static void syndicate_status(Jedis jedis, long uid, Map post, int start){
		//以上次被更新的最后一个关注者为起点，获取接下来的1000个关注者
		Set<String> followers = jedis.zrangeByScore("followers:" + uid, start, 1000);
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			followers.stream().forEach(follower -> {
				//将状态消息添加到所有被获取的关注者的主页时间线里面，并在需要的时候对关注者的主页时间线进行修剪，防止它超过限定的最大长度
				pipe.zadd("home:" + follower, post);
				pipe.zremrangeByRank("home:" + follower, 0, -1001);
			});
			pipe.exec();
			
			//如果需要更新的关注者数量超过1000人，那么在延迟任务里继续执行剩余的更新操作
			if(followers.size() >= 1000){
				com.chapter6.delayqueue.Demo d = new com.chapter6.delayqueue.Demo();
				d.exec_later(jedis, "default", "syndicate_status", new Object[]{jedis, uid, post, start + followers.size()}, 0);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//删除已发布的状态消息
	public static void delete_status(Jedis jedis, long uid, long statusid) throws InterruptedException{
		String key = "status:" + statusid;
		Lock lock = new Lock();
		String lockid = lock.acquire_lock(jedis, key, 1);//加锁，防止两个人同时删除一条消息
		if(lockid == null){
			return;
		}
		
		//如果不是uid发布的消息，则直接返回
		if(!jedis.hget(key, uid + "").equals(uid + "")){
			lock.release_lock(jedis, key, lockid);
			return;
		}
		
		Pipeline pipe = jedis.pipelined();
		try{
			pipe.multi();
			pipe.del(key);
			pipe.zrem("profile:" + uid, statusid + "");
			pipe.zrem("home:" + uid, statusid + "");
			//对存储着用户信息的散列进行更新，减少已发布状态消息的数量
			pipe.hincrBy("users:" + uid, "posts", -1);
			pipe.exec();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "unused", "serial", "rawtypes" })
	public static void main(String[] args) throws InterruptedException {
		RedisClient client = new RedisClient();
		Jedis jedis = client.getJedis();
		jedis.flushDB();
		double time = ((Long)(new Date()).getTime()).doubleValue();
		
		long uid = Demo.create_user(jedis, "zsl", "张苏磊");
		
		Demo.create_status(jedis, uid, "我发送的第一条消息");
		
		List<Object> msgs = Demo.get_status_message(jedis, uid, "home:", 1, 10);
		
		long other_uid = 2l;
		jedis.zadd("following:" + uid, new HashMap(){{put("11", time);put("12", time);put("13", time);}});
		jedis.zadd("followers:" + other_uid, new HashMap(){{put("21", time);put("22", time);}});
		jedis.zadd("home:" + uid, new HashMap(){{put("20000001", time);put("20000002", time);}});
		jedis.zadd("profile:" + other_uid, new HashMap(){{put("10000001", time);put("10000002", time);put("10000003", time);}});
		Demo.follow_user(jedis, uid, other_uid);
		
		Demo.unfollow_user(jedis, uid, other_uid);
	}
	
}
