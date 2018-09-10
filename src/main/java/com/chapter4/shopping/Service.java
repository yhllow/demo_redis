package com.chapter4.shopping;

import java.util.Date;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**watch，unwatch，dicard的用法
 * @author Administrator
 *
 */
public class Service {
	
	/**将商品放到市场上进行销售
	 * @param jedis
	 * @param itemID
	 * @param sellerID
	 * @param price
	 * @return
	 */
	public boolean listItem(Jedis jedis, String itemID, String sellerID, double price){
		//每个用户拥有的商品
		String inventory = "inventory:" + sellerID;
		//商品市场，id是商品id+用户id
		String item = itemID + "." + sellerID;
		Date now = new Date();
		long end = now.getTime();
		
		Pipeline pipe = jedis.pipelined();
		while((new Date()).getTime() < end){
			try{
				// 开启watch之后，如果key的值被修改，则事务失败，exec方法返回null
				// 监视用户包裹是否发生变化
				jedis.watch(inventory);
				// 检查用户是否仍然持有将要被销售的商品
				Response<Boolean> r = pipe.sismember(inventory, itemID);
				// 如果指定商品不在用户的包裹里，那么停止对包裹键的监视并返回一个false
				if(!r.get()){
					//unwatch可以在watch执行之后，在multi执行之前对连接进行重置
					//dicard可以在multi执行之后，exec执行之前对连接进行重置
					jedis.unwatch();
					return false;
				}
				
				// 把被销售的商品添加到商品买卖市场里
				pipe.multi();
				pipe.zadd("market:", price, item);
				pipe.srem(inventory, itemID);
				//如果执行exec没有引发异常，则说明事务执行成功，并且对包裹的监视也已经结束
				if (pipe.exec() != null) {
	                return true;
	            }
			}catch(Exception e){
				return false;
			}
		}
		return false;
	}

}
