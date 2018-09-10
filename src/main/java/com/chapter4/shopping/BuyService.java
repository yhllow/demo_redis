package com.chapter4.shopping;

import java.util.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**watch，unwatch，dicard的用法
 * @author Administrator
 *
 */
public class BuyService {
	
	/**买家购买商品
	 * @param jedis
	 * @param itemID
	 * @param sellerID
	 * @param price
	 * @return
	 */
	public boolean purchaseItem(Jedis jedis, String buyerID, String itemID, String sellerID, double lprice){
		//买家个人信息
		String buyer = "users:" + buyerID;
		//卖家个人信息
		String seller = "users:" + sellerID;
		//商品市场，id是商品id+用户id
		String item = itemID + "." + sellerID;
		//每个用户拥有的商品
		String inventory = "inventory:" + buyerID;
		Date now = new Date();
		long end = now.getTime() + 10000;
		
		Pipeline pipe = jedis.pipelined();
		while((new Date()).getTime() < end){
			try{
				// 对商品买卖市场以及买家的个人信息进行监视
				jedis.watch("market:", buyer);
				
				//检查买家想要购买的商品的价格是否发生变化，以及买家是否有足够的钱来购买这件商品
				double price = pipe.zscore("market:", item).get();
				double funds = Double.parseDouble(pipe.hget(buyer, "funds").get());
				if(price != lprice || price > funds){
					jedis.unwatch();
					return false;
				}
				
				//先将买家支付的钱转移给卖家，然后将被购买的商品移交给买家
				pipe.multi();
				pipe.hincrBy(seller, "funds", (int)price);
				pipe.hincrBy(buyer, "funds", (int)-price);
				pipe.sadd(inventory, itemID);
				pipe.zrem("market:", item);
				pipe.exec();
				return true;
			}catch(Exception e){
				//如果买家的个人信息或者商品买卖市场在交易的过程中出现了变化，那么进行重试
			}
		}
		return false;
	}

}
