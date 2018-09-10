package com.chapter2.cart.service;

import java.util.Date;
import redis.clients.jedis.Jedis;

public class CartService {
	
	/**更新令牌
	 * @param jedis
	 * @param user
	 * @param item
	 */
	public void updateToken(Jedis jedis, String token, String user, String item){
		Date date = new Date();
		long timestamp = date.getTime();
		
		//维持令牌与已登录用户之间的映射
		jedis.hset("login:", token, user);
		//记录令牌最后一次出现的时间
		jedis.zadd("recent:", timestamp, token);
		
		if(item != null){
			//记录用户浏览过的商品
			jedis.zadd("views:" + token, timestamp, item);
			//移除旧记录，只保留用户最近浏览过的3个商品
			jedis.zremrangeByRank("views:" + token, 0, 2);
			//记录所有商品的浏览次数，浏览最多的商品将被放在有序集合的索引为0的位置上，并且具有整个有序集合最少的分值
			jedis.zincrby("views:", -1, item);
		}
	}
	
	/**检查令牌
	 * @param jedis
	 * @param token
	 * @return
	 */
	public String checkToken(Jedis jedis, String token){
		return jedis.hget("login:", token);
	}
	
	/**更新购物车
	 * @param jedis
	 * @param token
	 * @param item
	 * @param count
	 */
	public void addToCart(Jedis jedis, String token, String item, String count){
		if(Integer.parseInt(count) <= 0){
			jedis.hdel("cart:" + token, item);
		}else{
			jedis.hset("cart:" + token, item, count);
		}
	}
	
	/**维护浏览次数最多的商品
	 * @param jedis
	 * @throws InterruptedException
	 */
	public void rescaleViewd(Jedis jedis) throws InterruptedException{
		while(true){
			//每隔5分钟删除排名在20000名之后的商品
			jedis.zremrangeByRank("views:", 0, -20000);
			Thread.sleep(300000);
		}
	}

}
